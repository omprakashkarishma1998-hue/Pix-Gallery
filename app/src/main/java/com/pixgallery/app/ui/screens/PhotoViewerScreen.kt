package com.pixgallery.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType
import com.pixgallery.app.ui.components.VideoPlayerView
import com.pixgallery.app.util.ShareUtils
import java.text.DateFormat
import java.util.Date

@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    startIndex: Int,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onBack: () -> Unit,
    onDelete: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(pagerState.currentPage)

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            if (item.type == MediaType.VIDEO) {
                VideoPlayerView(uri = item.uri, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        // Bottom action bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                ViewerAction(Icons.Filled.Send, "Send") {
                    currentItem?.let { ShareUtils.shareSingle(context, it) }
                }
                ViewerAction(Icons.Filled.Edit, "Edit") {
                    currentItem?.let { ShareUtils.editItem(context, it) }
                }
                ViewerAction(
                    icon = if (currentItem != null && currentItem.id in favoriteIds) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = "Favorites"
                ) {
                    currentItem?.let { onToggleFavorite(it.id) }
                }
                ViewerAction(Icons.Filled.Delete, "Delete") {
                    currentItem?.let {
                        onDelete(it)
                        if (items.size <= 1) onBack()
                    }
                }
                Box {
                    ViewerAction(Icons.Filled.MoreVert, "More") {
                        showMoreMenu = true
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Details") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showDetailsDialog = true
                            }
                        )
                        if (currentItem?.type == MediaType.IMAGE) {
                            DropdownMenuItem(
                                text = { Text("Set as wallpaper") },
                                leadingIcon = { Icon(Icons.Filled.Wallpaper, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    currentItem.let { ShareUtils.setAsWallpaper(context, it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog && currentItem != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Details") },
            text = {
                Column {
                    Text("Name: ${currentItem.name}")
                    Text("Type: ${if (currentItem.type == MediaType.VIDEO) "Video" else "Photo"}")
                    Text("Date: ${DateFormat.getDateTimeInstance().format(Date(currentItem.dateTakenMillis))}")
                    Text("Album: ${currentItem.bucketName}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
