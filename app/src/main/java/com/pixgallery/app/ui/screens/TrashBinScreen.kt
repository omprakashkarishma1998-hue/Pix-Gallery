package com.pixgallery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem

@Composable
fun TrashBinScreen(
    items: List<MediaItem>,
    onBack: () -> Unit,
    onEmptyTrash: () -> Unit,
    onRestore: (MediaItem) -> Unit,
    onDeleteForever: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingSingleDelete by remember { mutableStateOf<MediaItem?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Trash bin", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            "Items can be stored here for up to 30 days (Free users). Tap Restore to bring an " +
                "item back, or the trash icon to delete it forever.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        if (items.isEmpty()) {
            EmptyState("Trash bin is empty")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        TrashActionIcon(
                            icon = Icons.Filled.Restore,
                            contentDescription = "Restore",
                            modifier = Modifier.align(Alignment.BottomStart),
                            onClick = { onRestore(item) }
                        )
                        TrashActionIcon(
                            icon = Icons.Filled.DeleteForever,
                            contentDescription = "Delete forever",
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onClick = { pendingSingleDelete = item }
                        )
                    }
                }
            }

            Button(
                onClick = { showEmptyTrashConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text("  Empty trash bin (delete all forever)")
            }
        }
    }

    pendingSingleDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingSingleDelete = null },
            title = { Text("Delete forever?") },
            text = { Text("This photo/video will be permanently removed from your device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteForever(item)
                    pendingSingleDelete = null
                }) { Text("Delete forever") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSingleDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty trash bin?") },
            text = { Text("All ${items.size} item(s) in the trash will be permanently deleted from your device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onEmptyTrash()
                    showEmptyTrashConfirm = false
                }) { Text("Delete all forever") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TrashActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = modifier
            .padding(4.dp)
            .size(28.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.padding(5.dp)
        )
    }
}
