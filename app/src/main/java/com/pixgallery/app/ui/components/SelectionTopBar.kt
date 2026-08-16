package com.pixgallery.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onSelectAll: () -> Unit,
    onHide: () -> Unit = {}
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
        MoreMenuItem(onSelectAll = onSelectAll, onHide = onHide)
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

/** The "More" (⋮) button, shared by the bottom action bar and the top count
 *  bar - opens a real dropdown instead of doing nothing when tapped. */
@Composable
private fun MoreMenuItem(onSelectAll: () -> Unit, onHide: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Select all") },
                    leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSelectAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hide (move to Vault)") },
                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onHide()
                    }
                )
            }
        }
        Text("More", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SelectionCountBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onHide: () -> Unit = {}
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
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Select all") },
                    leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSelectAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hide (move to Vault)") },
                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onHide()
                    }
                )
            }
        }
    }
}
