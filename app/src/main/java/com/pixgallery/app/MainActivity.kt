package com.pixgallery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixgallery.app.data.GalleryViewModel
import com.pixgallery.app.model.AlbumItem
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.ui.components.GalleryBottomNavBar
import com.pixgallery.app.ui.components.GalleryTab
import com.pixgallery.app.ui.components.SelectionActionBar
import com.pixgallery.app.ui.components.SelectionCountBar
import com.pixgallery.app.ui.screens.AlbumDetailScreen
import com.pixgallery.app.ui.screens.AlbumsScreen
import com.pixgallery.app.ui.screens.PermissionScreen
import com.pixgallery.app.ui.screens.PhotoViewerScreen
import com.pixgallery.app.ui.screens.PhotosScreen
import com.pixgallery.app.ui.screens.RecommendedScreen
import com.pixgallery.app.ui.screens.SettingsScreen
import com.pixgallery.app.ui.screens.TrashBinScreen
import com.pixgallery.app.ui.theme.PixGalleryTheme
import com.pixgallery.app.util.PermissionUtils

private sealed class Route {
    data object Main : Route()
    data class AlbumDetail(val bucketId: String, val bucketName: String) : Route()
    data class Viewer(val items: List<MediaItem>, val startIndex: Int) : Route()
    data object Settings : Route()
    data object TrashBin : Route()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryApp()
        }
    }
}

@Composable
private fun GalleryApp() {
    // null = follow system theme, true = force dark, false = force light
    var darkModeOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = darkModeOverride ?: systemIsDark

    PixGalleryTheme(darkTheme = useDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            GalleryContent(
                darkModeOverride = darkModeOverride,
                onDarkModeOverrideChange = { darkModeOverride = it }
            )
        }
    }
}

@Composable
private fun GalleryContent(
    darkModeOverride: Boolean?,
    onDarkModeOverrideChange: (Boolean?) -> Unit
) {
    val viewModel: GalleryViewModel = viewModel()

    var hasPermission by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionUtils.hasMediaPermissions(context)
        if (hasPermission) viewModel.loadMedia()
    }

    LaunchedEffect(Unit) {
        hasPermission = PermissionUtils.hasMediaPermissions(context)
        if (hasPermission) viewModel.loadMedia()
    }

    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.PHOTOS) }
    var route by remember { mutableStateOf<Route>(Route.Main) }
    var selectionMode by remember { mutableStateOf(false) }

    val allMedia by viewModel.allMedia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val trashedIds by viewModel.trashedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    if (!hasPermission) {
        PermissionScreen(onGrantClick = {
            permissionLauncher.launch(PermissionUtils.requiredMediaPermissions())
        })
        return
    }

    // Hardware/gesture back button:
    // 1. If items are selected, back cancels selection first (instead of closing the app).
    // 2. If we're on any screen other than Main, back returns to Main.
    // 3. Only when already on Main with nothing selected does back fall through
    //    to the system default (which exits the app) - normal expected behavior.
    BackHandler(enabled = selectionMode || route !is Route.Main) {
        when {
            selectionMode -> {
                selectionMode = false
                viewModel.clearSelection()
            }
            else -> {
                route = Route.Main
            }
        }
    }

    when (val r = route) {
        is Route.Viewer -> {
            PhotoViewerScreen(
                items = r.items,
                startIndex = r.startIndex,
                favoriteIds = favoriteIds,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBack = { route = Route.Main },
                onDelete = { item ->
                    viewModel.toggleSelection(item.id)
                    viewModel.moveSelectedToTrash()
                }
            )
        }

        is Route.AlbumDetail -> {
            val albumItems = allMedia.filter { it.bucketId == r.bucketId && it.id !in trashedIds }
            AlbumDetailScreen(
                albumName = r.bucketName,
                items = albumItems,
                selectedIds = selectedIds,
                selectionMode = selectionMode,
                onBack = { route = Route.Main },
                onItemClick = { item ->
                    if (selectionMode) {
                        viewModel.toggleSelection(item.id)
                    } else {
                        val idx = albumItems.indexOf(item)
                        route = Route.Viewer(albumItems, idx)
                    }
                },
                onItemLongClick = {
                    selectionMode = true
                    viewModel.toggleSelection(it.id)
                },
                onCloseSelection = {
                    selectionMode = false
                    viewModel.clearSelection()
                },
                onSendSelected = {
                    val selectedItems = allMedia.filter { it.id in selectedIds }
                    com.pixgallery.app.util.ShareUtils.shareMultiple(context, selectedItems)
                },
                onDeleteSelected = {
                    viewModel.moveSelectedToTrash()
                    selectionMode = false
                }
            )
        }

        is Route.Settings -> {
            SettingsScreen(
                onBack = { route = Route.Main },
                darkModeOverride = darkModeOverride,
                onDarkModeOverrideChange = onDarkModeOverrideChange
            )
        }

        is Route.TrashBin -> {
            TrashBinScreen(
                items = viewModel.trashedItems(),
                onBack = { route = Route.Main },
                onEmptyTrash = { viewModel.emptyTrash() },
                onRestore = { viewModel.restoreFromTrash(it.id) }
            )
        }

        Route.Main -> {
            Scaffold(
                bottomBar = {
                    if (!selectionMode) {
                        GalleryBottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    } else {
                        androidx.compose.material3.Surface(tonalElevation = 3.dp) {
                            SelectionActionBar(
                                onSend = {
                                    val selectedItems = allMedia.filter { it.id in selectedIds }
                                    com.pixgallery.app.util.ShareUtils.shareMultiple(context, selectedItems)
                                },
                                onCreativity = {},
                                onAddToAlbum = {},
                                onDelete = {
                                    viewModel.moveSelectedToTrash()
                                    selectionMode = false
                                },
                                onMore = {}
                            )
                        }
                    }
                },
                topBar = {
                    if (selectionMode) {
                        androidx.compose.material3.Surface(tonalElevation = 3.dp) {
                            SelectionCountBar(
                                count = selectedIds.size,
                                onClose = {
                                    selectionMode = false
                                    viewModel.clearSelection()
                                },
                                onMore = {}
                            )
                        }
                    } else {
                        androidx.compose.material3.TopAppBar(
                            title = {},
                            actions = {
                                androidx.compose.material3.IconButton(onClick = { route = Route.Settings }) {
                                    androidx.compose.material3.Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        )
                    }
                }
            ) { padding ->
                val visibleMedia = allMedia.filter { it.id !in trashedIds }
                when (selectedTab) {
                    GalleryTab.PHOTOS -> {
                        PhotosScreen(
                            groups = viewModel.groupedByDate(),
                            isLoading = isLoading,
                            selectedIds = selectedIds,
                            selectionMode = selectionMode,
                            onItemClick = { item ->
                                if (selectionMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    val idx = visibleMedia.indexOf(item)
                                    route = Route.Viewer(visibleMedia, idx)
                                }
                            },
                            onItemLongClick = {
                                selectionMode = true
                                viewModel.toggleSelection(it.id)
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }

                    GalleryTab.ALBUMS -> {
                        AlbumsScreen(
                            albums = viewModel.groupedByAlbum(),
                            isLoading = isLoading,
                            onAlbumClick = { album: AlbumItem ->
                                route = Route.AlbumDetail(album.bucketId, album.bucketName)
                            },
                            onOtherAlbums = {},
                            onTrashBin = { route = Route.TrashBin },
                            modifier = Modifier.padding(padding)
                        )
                    }

                    GalleryTab.RECOMMENDED -> {
                        RecommendedScreen(
                            recentItems = visibleMedia.take(6),
                            onTrashBin = { route = Route.TrashBin },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}
