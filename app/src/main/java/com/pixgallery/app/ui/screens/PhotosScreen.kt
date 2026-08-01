package com.pixgallery.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.ui.components.MediaGridCell

@Composable
fun PhotosScreen(
    groups: List<com.pixgallery.app.model.DateGroup>,
    isLoading: Boolean,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Photos", style = MaterialTheme.typography.headlineMedium)
            Spacer()
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search photos") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
        }

        when {
            isLoading -> LoadingBox()
            groups.isEmpty() -> EmptyState("No photos or videos found yet")
            else -> {
                val filteredGroups = if (query.isBlank()) groups else groups.map { g ->
                    g.copy(items = g.items.filter { it.name.contains(query, ignoreCase = true) })
                }.filter { it.items.isNotEmpty() }

                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    for (group in filteredGroups) {
                        item(key = "header-${group.label}") {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        item(key = "grid-${group.label}") {
                            InlineGrid(
                                items = group.items,
                                selectedIds = selectedIds,
                                selectionMode = selectionMode,
                                onItemClick = onItemClick,
                                onItemLongClick = onItemLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineGrid(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit
) {
    // Non-scrolling grid nested inside LazyColumn using a fixed-height approach via LazyVerticalGrid
    // We use a simple wrap with LazyVerticalGrid disabled scroll since parent LazyColumn scrolls.
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
        val rows = (items.size + 2) / 3
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height((rows * 120).dp),
            userScrollEnabled = false,
            contentPadding = PaddingValues(0.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MediaGridCell(
                    item = item,
                    isSelected = item.id in selectedIds,
                    selectionMode = selectionMode,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(2.dp))
}

@Composable
fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
