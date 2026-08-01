package com.pixgallery.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Bottom action bar shown while items are selected (Send / Creativity / Add to album / Delete / More). */
@Composable
fun SelectionActionBar(
    onSend: () -> Unit,
    onCreativity: () -> Unit,
    onAddToAlbum: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        ActionBarItem(Icons.Filled.Send, "Send", onSend)
        ActionBarItem(Icons.Filled.AutoAwesome, "Creativity", onCreativity)
        ActionBarItem(Icons.Filled.AddToPhotos, "Add to album", onAddToAlbum)
        ActionBarItem(Icons.Filled.Delete, "Delete", onDelete)
        ActionBarItem(Icons.Filled.MoreVert, "More", onMore)
    }
}

@Composable
private fun ActionBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SelectionCountBar(
    count: Int,
    onClose: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close selection")
        }
        Text(
            text = "$count items selected",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onMore) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}
