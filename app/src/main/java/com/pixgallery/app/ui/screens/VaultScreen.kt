package com.pixgallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem

private const val PIN_LENGTH = 4

/**
 * Vault entry point. Handles all three states itself:
 *  - no PIN set yet -> ask the user to create one
 *  - PIN set but not unlocked this session -> ask for it
 *  - unlocked -> show the hidden items grid
 *
 * [isPinSet]/[onCreatePin]/[onCheckPin] are backed by [GalleryViewModel]'s
 * hashed-PIN storage so the actual PIN never lives in plain prefs.
 */
@Composable
fun VaultScreen(
    hiddenItems: List<MediaItem>,
    isPinSet: Boolean,
    onCreatePin: (String) -> Unit,
    onCheckPin: (String) -> Boolean,
    onUnhide: (MediaItem) -> Unit,
    onDeleteForever: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var unlocked by remember { mutableStateOf(false) }

    if (!unlocked) {
        VaultPinScreen(
            isPinSet = isPinSet,
            onCreatePin = { pin ->
                onCreatePin(pin)
                unlocked = true
            },
            onCheckPin = { pin ->
                val ok = onCheckPin(pin)
                if (ok) unlocked = true
                ok
            },
            onBack = onBack,
            modifier = modifier
        )
    } else {
        VaultContentScreen(
            items = hiddenItems,
            onUnhide = onUnhide,
            onDeleteForever = onDeleteForever,
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Composable
private fun VaultPinScreen(
    isPinSet: Boolean,
    onCreatePin: (String) -> Unit,
    onCheckPin: (String) -> Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Two-step "create" flow: enter a PIN, then confirm it.
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when {
        !isPinSet && firstPin == null -> "Create a Vault PIN"
        !isPinSet -> "Confirm your PIN"
        else -> "Enter Vault PIN"
    }
    val subtitle = when {
        !isPinSet -> "Hidden photos & videos are only visible after this PIN is entered."
        else -> "Locked with your Vault PIN"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(PIN_LENGTH) { i ->
                Surface(
                    shape = CircleShape,
                    color = if (i < pin.length) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))

        NumberPad(
            onDigit = { digit ->
                if (pin.length < PIN_LENGTH) {
                    pin += digit
                    error = null
                }
                if (pin.length == PIN_LENGTH) {
                    val entered = pin
                    if (!isPinSet) {
                        if (firstPin == null) {
                            firstPin = entered
                            pin = ""
                        } else if (firstPin == entered) {
                            onCreatePin(entered)
                        } else {
                            error = "PINs didn't match - try again"
                            firstPin = null
                            pin = ""
                        }
                    } else {
                        if (!onCheckPin(entered)) {
                            error = "Wrong PIN"
                            pin = ""
                        }
                    }
                }
            },
            onBackspace = {
                if (pin.isNotEmpty()) pin = pin.dropLast(1)
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NumberPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "back")
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.size(72.dp).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "" -> {}
                            "back" -> IconButton(onClick = onBackspace) {
                                Icon(Icons.Filled.Backspace, contentDescription = "Backspace")
                            }
                            else -> Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxSize().clickable { onDigit(key) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(key, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultContentScreen(
    items: List<MediaItem>,
    onUnhide: (MediaItem) -> Unit,
    onDeleteForever: (MediaItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<MediaItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Vault", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Only you can see what's in here. Long-press isn't needed - use the icons on each item.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        if (items.isEmpty()) {
            EmptyState("Nothing hidden yet.\nSelect photos in your gallery and choose \"Hide\" to move them here.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(items, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(2.dp).aspectRatio(1f)) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        VaultActionIcon(
                            icon = Icons.Filled.Visibility,
                            contentDescription = "Unhide",
                            modifier = Modifier.align(Alignment.BottomStart),
                            onClick = { onUnhide(item) }
                        )
                        VaultActionIcon(
                            icon = Icons.Filled.DeleteForever,
                            contentDescription = "Delete forever",
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onClick = { pendingDelete = item }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete forever?") },
            text = { Text("This photo/video will be permanently removed from your device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteForever(item)
                    pendingDelete = null
                }) { Text("Delete forever") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VaultActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = modifier.padding(4.dp).size(28.dp).clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.padding(5.dp)
        )
    }
}
