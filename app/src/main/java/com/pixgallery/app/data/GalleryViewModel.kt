package com.pixgallery.app.data

import android.app.Application
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixgallery.app.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMedia: StateFlow<List<MediaItem>> = _allMedia

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _trashedIds = MutableStateFlow<Set<Long>>(emptySet())
    val trashedIds: StateFlow<Set<Long>> = _trashedIds

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
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
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _allMedia.value = repository.loadAllMedia()
            _isLoading.value = false
        }
    }

    fun groupedByDate() = repository.groupByDate(
        _allMedia.value.filter { it.id !in _trashedIds.value }
    )

    fun groupedByAlbum() = repository.groupByAlbum(
        _allMedia.value.filter { it.id !in _trashedIds.value }
    )

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

    fun moveSelectedToTrash() {
        _trashedIds.value = _trashedIds.value + _selectedIds.value
        _selectedIds.value = emptySet()
    }

    /** Trashes a single item directly, independent of the multi-select state.
     *  Used by the photo/video viewer's Delete button so it can't be affected
     *  by whatever is (or isn't) currently selected elsewhere in the app. */
    fun moveToTrash(id: Long) {
        _trashedIds.value = _trashedIds.value + id
    }

    fun restoreFromTrash(id: Long) {
        _trashedIds.value = _trashedIds.value - id
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
    }

    fun trashedItems() = _allMedia.value.filter { it.id in _trashedIds.value }
}
