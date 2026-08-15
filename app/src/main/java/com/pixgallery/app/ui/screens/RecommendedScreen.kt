package com.pixgallery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecommendedScreen(
    onTrashBin: () -> Unit,
    onVault: () -> Unit = {},
    onDuplicateFinder: () -> Unit = {},
    onMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Text(
                "Recommended",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        item {
            MoreRow(Icons.Filled.History, "Memories", "Photos from this day in past years", onClick = onMemories)
        }
        item {
            MoreRow(Icons.Filled.ContentCopy, "Duplicate Finder", "Find and clean up duplicate photos/videos", onClick = onDuplicateFinder)
        }
        item {
            MoreRow(Icons.Filled.Lock, "Vault", "PIN-locked private photos & videos", onClick = onVault)
        }
        item {
            MoreRow(Icons.Filled.Delete, "Trash bin", "View items in Trash bin", onClick = onTrashBin)
        }
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}
