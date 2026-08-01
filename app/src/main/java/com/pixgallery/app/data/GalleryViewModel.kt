package com.pixgallery.app.data

import android.app.Application
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

    fun restoreFromTrash(id: Long) {
        _trashedIds.value = _trashedIds.value - id
    }

    fun emptyTrash() {
        _trashedIds.value = emptySet()
    }

    fun trashedItems() = _allMedia.value.filter { it.id in _trashedIds.value }
}
