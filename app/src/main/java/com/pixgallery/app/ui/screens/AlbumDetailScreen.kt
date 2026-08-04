package com.pixgallery.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.ui.components.MediaGrid
import com.pixgallery.app.ui.components.SelectionActionBar
import com.pixgallery.app.ui.components.SelectionCountBar

@Composable
fun AlbumDetailScreen(
    albumName: String,
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onBack: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onCloseSelection: () -> Unit,
    onSendSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onCreativity: () -> Unit = {},
    onAddToAlbum: () -> Unit = {},
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (selectionMode) {
                SelectionCountBar(
                    count = selectedIds.size,
                    onClose = onCloseSelection,
                    onSelectAll = onSelectAll
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(albumName, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        bottomBar = {
            if (selectionMode) {
                Surface(tonalElevation = 3.dp) {
                    SelectionActionBar(
                        onSend = onSendSelected,
                        onCreativity = onCreativity,
                        onAddToAlbum = onAddToAlbum,
                        onDelete = onDeleteSelected,
                        onSelectAll = onSelectAll
                    )
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState("This album is empty")
        } else {
            MediaGrid(
                items = items,
                selectedIds = selectedIds,
                selectionMode = selectionMode,
                onClick = onItemClick,
                onLongClick = onItemLongClick,
                modifier = Modifier.padding(padding),
                gridState = gridState
            )
        }
    }
}
