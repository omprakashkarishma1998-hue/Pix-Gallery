package com.pixgallery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MemoryGroup

/** "On this day" - resurfaces photos/videos shot on today's date in previous years. */
@Composable
fun MemoriesScreen(
    memories: List<MemoryGroup>,
    onItemClick: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Memories", style = MaterialTheme.typography.titleLarge)
        }

        if (memories.isEmpty()) {
            EmptyState("No memories for today yet.\nCome back on a day you took photos in a previous year!")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                memories.forEach { group ->
                    item {
                        Text(
                            group.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((120 * ((group.items.size + 2) / 3)).dp),
                            userScrollEnabled = false
                        ) {
                            items(group.items, key = { it.id }) { item ->
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
