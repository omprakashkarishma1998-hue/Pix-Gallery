package com.pixgallery.app.data

import android.app.Application
import android.content.Context
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixgallery.app.model.AlbumItem
import com.pixgallery.app.model.FAVORITES_BUCKET_ID
import com.pixgallery.app.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    // Persists trashed/favorite ids to disk so they survive the app process being
    // killed and restarted - previously these lived only in in-memory StateFlows,
    // which is why trashed items "un-deleted themselves" whenever the app was
    // closed and reopened (a fresh ViewModel started with an empty trash set).
    private val prefs = application.getSharedPreferences("pixgallery_state", Context.MODE_PRIVATE)

    private fun loadIdSet(key: String): Set<Long> =
        prefs.getStringSet(key, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    private fun persistIdSet(key: String, ids: Set<Long>) {
        prefs.edit().putStringSet(key, ids.map { it.toString() }.toSet()).apply()
    }

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMedia: StateFlow<List<MediaItem>> = _allMedia

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _trashedIds = MutableStateFlow(loadIdSet(KEY_TRASHED))
    val trashedIds: StateFlow<Set<Long>> = _trashedIds

    private val _favoriteIds = MutableStateFlow(loadIdSet(KEY_FAVORITES))
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    // Permanent-delete confirmation: Android requires a system dialog before we can actually
    // remove files from MediaStore. This flow is a one-shot event the UI observes to launch it.
    private val _pendingDeleteRequest = MutableStateFlow<IntentSenderRequest?>(null)
    val pendingDeleteRequest: StateFlow<IntentSenderRequest?> = _pendingDeleteRequest
    private var idsAwaitingDeleteConfirmation: Set<Long> = emptySet()

    private val _deleteFailedEvent = MutableStateFlow(0)
    val deleteFailedEvent: StateFlow<Int> = _deleteFailedEvent

    fun toggleFavorite(id: Long) {
        _favoriteIds.value = if (id in _favoriteIds.value) {
            _favoriteIds.value - id
        } else {
            _favoriteIds.value + id
        }
        persistIdSet(KEY_FAVORITES, _favoriteIds.value)
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = repository.loadAllMedia()
            _allMedia.value = loaded
            _isLoading.value = false

            // Drop any trashed/favorite ids whose underlying file no longer exists
            // (e.g. removed by another app) so the persisted sets don't accumulate
            // stale entries forever.
            val existingIds = loaded.mapTo(mutableSetOf()) { it.id }
            val prunedTrashed = _trashedIds.value.filter { it in existingIds }.toSet()
            if (prunedTrashed != _trashedIds.value) {
                _trashedIds.value = prunedTrashed
                persistIdSet(KEY_TRASHED, prunedTrashed)
            }
            val prunedFavorites = _favoriteIds.value.filter { it in existingIds }.toSet()
            if (prunedFavorites != _favoriteIds.value) {
                _favoriteIds.value = prunedFavorites
                persistIdSet(KEY_FAVORITES, prunedFavorites)
            }
        }
    }

    fun groupedByDate() = repository.groupByDate(
        _allMedia.value.filter { it.id !in _trashedIds.value }
    )

    fun groupedByAlbum(): List<AlbumItem> {
        val visible = _allMedia.value.filter { it.id !in _trashedIds.value }
        val realAlbums = repository.groupByAlbum(visible)
        val favoriteItems = visible.filter { it.id in _favoriteIds.value }
        // "Favorites" is a synthetic album built from starred items, not a real
        // MediaStore bucket - only show it once the user has favorited something,
        // and always pin it first like the real gallery apps do.
        return if (favoriteItems.isNotEmpty()) {
            val favoritesAlbum = AlbumItem(
                bucketId = FAVORITES_BUCKET_ID,
                bucketName = "Favorites",
                coverUri = favoriteItems.first().uri,
                itemCount = favoriteItems.size
            )
            listOf(favoritesAlbum) + realAlbums
        } else {
            realAlbums
        }
    }

    /** Visible (non-trashed) items the user has favorited, newest first - same
     *  ordering [MediaRepository.loadAllMedia] already sorted [_allMedia] into. */
    fun favoriteItems(): List<MediaItem> =
        _allMedia.value.filter { it.id in _favoriteIds.value && it.id !in _trashedIds.value }

    fun toggleSelection(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    /** Selects every id in [ids] (used by the "Select all" action). */
    fun selectAll(ids: Collection<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun moveSelectedToTrash() {
        _trashedIds.value = _trashedIds.value + _selectedIds.value
        _selectedIds.value = emptySet()
        persistIdSet(KEY_TRASHED, _trashedIds.value)
    }

    /** Trashes a single item directly, independent of the multi-select state.
     *  Used by the photo/video viewer's Delete button so it can't be affected
     *  by whatever is (or isn't) currently selected elsewhere in the app. */
    fun moveToTrash(id: Long) {
        _trashedIds.value = _trashedIds.value + id
        persistIdSet(KEY_TRASHED, _trashedIds.value)
    }

    fun restoreFromTrash(id: Long) {
        _trashedIds.value = _trashedIds.value - id
        persistIdSet(KEY_TRASHED, _trashedIds.value)
    }

    /** Kicks off permanently deleting the given items (used for both "delete forever" on a
     *  single trash item and "Empty trash bin"). May trigger a system confirmation dialog -
     *  the UI should observe [pendingDeleteRequest] and launch it when non-null. */
    fun permanentlyDelete(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val uris = _allMedia.value.filter { it.id in ids }.map { it.uri }
        idsAwaitingDeleteConfirmation = ids
        viewModelScope.launch {
            when (val result = repository.requestPermanentDelete(uris)) {
                DeleteResult.Done -> applyConfirmedDelete()
                is DeleteResult.NeedsConfirmation -> {
                    _pendingDeleteRequest.value = IntentSenderRequest.Builder(result.intentSender).build()
                }
                DeleteResult.Failed -> {
                    idsAwaitingDeleteConfirmation = emptySet()
                    _deleteFailedEvent.value = _deleteFailedEvent.value + 1
                }
            }
        }
    }

    /** Call after the system confirmation dialog returns RESULT_OK. */
    fun onDeleteConfirmed() {
        applyConfirmedDelete()
    }

    /** Call if the user cancels the system confirmation dialog. */
    fun onDeleteCancelled() {
        idsAwaitingDeleteConfirmation = emptySet()
        _pendingDeleteRequest.value = null
    }

    private fun applyConfirmedDelete() {
        val ids = idsAwaitingDeleteConfirmation
        _allMedia.value = _allMedia.value.filterNot { it.id in ids }
        _trashedIds.value = _trashedIds.value - ids
        _favoriteIds.value = _favoriteIds.value - ids
        idsAwaitingDeleteConfirmation = emptySet()
        _pendingDeleteRequest.value = null
        persistIdSet(KEY_TRASHED, _trashedIds.value)
        persistIdSet(KEY_FAVORITES, _favoriteIds.value)
    }

    fun trashedItems() = _allMedia.value.filter { it.id in _trashedIds.value }

    companion object {
        private const val KEY_TRASHED = "trashed_ids"
        private const val KEY_FAVORITES = "favorite_ids"
    }
}
