package com.pixgallery.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType
import com.pixgallery.app.ui.components.VideoPlayerView
import com.pixgallery.app.util.ShareUtils
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    startIndex: Int,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onBack: () -> Unit,
    onDelete: (MediaItem) -> Unit,
    onEditPhoto: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(pagerState.currentPage)
    // Resets to visible every time the user swipes to a different page, and is
    // driven to false automatically while a video on the current page is playing.
    var controlsVisible by remember(pagerState.currentPage) { mutableStateOf(true) }

    // While the current page's photo is pinch-zoomed in, the pager itself should
    // stop swiping horizontally so panning around the zoomed image doesn't
    // accidentally flip to the next photo.
    var pagerScrollEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerScrollEnabled
        ) { page ->
            val item = items[page]
            if (item.type == MediaType.VIDEO) {
                VideoPlayerView(
                    uri = item.uri,
                    modifier = Modifier.fillMaxSize(),
                    onPlayingStateChanged = { isPlaying ->
                        // Only let the currently visible page control the bar -
                        // otherwise an off-screen page's poll could fight with it.
                        if (page == pagerState.currentPage) {
                            controlsVisible = !isPlaying
                        }
                    }
                )
            } else {
                ZoomableImage(
                    item = item,
                    onTap = { controlsVisible = !controlsVisible },
                    onZoomStateChanged = { zoomed ->
                        if (page == pagerState.currentPage) {
                            pagerScrollEnabled = !zoomed
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top bar (and its gradient) hides together with the bottom action bar
        // whenever the user taps the middle of the photo/video.
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box {
                // Dark gradient behind the top bar so the back button stays visible on light photos
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }
        }

        // Bottom gradient + action bar: hidden while a video is playing, shown
        // again the instant it's paused or a new page is swiped to.
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Box {
                // Dark gradient behind the bottom action bar so icons/labels stay visible on light photos
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

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
                            currentItem?.let {
                                if (it.type == MediaType.IMAGE) {
                                    onEditPhoto(it)
                                } else {
                                    // No in-app video editor yet - hand off to whatever's installed.
                                    ShareUtils.editItem(context, it)
                                }
                            }
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

/**
 * A photo that can be pinch-zoomed and panned, double-tapped to toggle zoom,
 * and single-tapped to toggle the surrounding viewer chrome. Zoom/pan state is
 * keyed to the item so it resets automatically when the pager moves to a
 * different photo.
 */
@Composable
private fun ZoomableImage(
    item: MediaItem,
    onTap: () -> Unit,
    onZoomStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        offset = if (newScale <= 1f) Offset.Zero else offset + panChange
    }

    LaunchedEffect(scale) {
        onZoomStateChanged(scale > 1.01f)
    }

    AsyncImage(
        model = item.uri,
        contentDescription = item.name,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .scale(scale)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .transformable(state = transformableState)
    )
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
