package com.pixgallery.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.pixgallery.app.model.FAVORITES_BUCKET_ID
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.ui.components.GalleryBottomNavBar
import com.pixgallery.app.ui.components.GalleryTab
import com.pixgallery.app.ui.components.SelectionActionBar
import com.pixgallery.app.ui.components.SelectionCountBar
import com.pixgallery.app.ui.screens.AlbumDetailScreen
import com.pixgallery.app.ui.screens.AlbumsScreen
import com.pixgallery.app.ui.screens.PermissionScreen
import com.pixgallery.app.ui.screens.PhotoEditScreen
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
    data class Viewer(val source: ViewerSource, val startIndex: Int) : Route()
    data class Edit(val item: MediaItem) : Route()
    data object Settings : Route()
    data object TrashBin : Route()
}

// Viewer no longer captures a frozen snapshot list. Instead it remembers *where*
// the items come from, so the list is recomputed live on every recomposition -
// this is what makes Delete (and Favorite) immediately reflect in the viewer.
private sealed class ViewerSource {
    data object AllMedia : ViewerSource()
    data class Album(val bucketId: String) : ViewerSource()
    data object Favorites : ViewerSource()
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Auto-refresh: re-scan MediaStore every time the app comes back to the
    // foreground (opened from recents/home, or returning from another app) -
    // not just on first launch. This catches photos/videos added, edited, or
    // removed outside the app while it was in the background.
    // Skipped right after we launch the system "delete forever" dialog: that
    // dialog also triggers ON_RESUME when it closes, and racing a full
    // MediaStore reload against the delete confirmation could re-show an item
    // that was just permanently deleted (the trash bin "doesn't refresh" bug).
    var suppressNextAutoRefresh by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (suppressNextAutoRefresh) {
                    suppressNextAutoRefresh = false
                } else if (PermissionUtils.hasMediaPermissions(context)) {
                    hasPermission = true
                    viewModel.loadMedia()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.PHOTOS) }
    var route by remember { mutableStateOf<Route>(Route.Main) }
    var selectionMode by remember { mutableStateOf(false) }
    var pendingTrashConfirm by remember { mutableStateOf(false) }

    val allMedia by viewModel.allMedia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val trashedIds by viewModel.trashedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val pendingDeleteRequest by viewModel.pendingDeleteRequest.collectAsState()
    val deleteFailedEvent by viewModel.deleteFailedEvent.collectAsState()

    // Hoisted here (outside the `when(route)` below) so navigating to the viewer and back
    // doesn't recreate these and lose the scroll position - that recreation was the bug.
    val photosListState: LazyListState = rememberLazyListState()
    val albumGridStates = remember { mutableMapOf<String, LazyGridState>() }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDeleteConfirmed()
        } else {
            viewModel.onDeleteCancelled()
        }
    }
    LaunchedEffect(pendingDeleteRequest) {
        pendingDeleteRequest?.let {
            suppressNextAutoRefresh = true
            deleteLauncher.launch(it)
        }
    }
    LaunchedEffect(deleteFailedEvent) {
        if (deleteFailedEvent > 0) {
            Toast.makeText(context, "Couldn't delete - please try again", Toast.LENGTH_SHORT).show()
        }
    }

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
            val viewerItems = when (val source = r.source) {
                ViewerSource.AllMedia -> allMedia.filter { it.id !in trashedIds }
                is ViewerSource.Album -> allMedia.filter { it.bucketId == source.bucketId && it.id !in trashedIds }
                ViewerSource.Favorites -> allMedia.filter { it.id in favoriteIds && it.id !in trashedIds }
            }
            PhotoViewerScreen(
                items = viewerItems,
                startIndex = r.startIndex,
                favoriteIds = favoriteIds,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBack = { route = Route.Main },
                onDelete = { item ->
                    viewModel.moveToTrash(item.id)
                },
                onEditPhoto = { item ->
                    route = Route.Edit(item)
                }
            )
        }

        is Route.Edit -> {
            PhotoEditScreen(
                item = r.item,
                onBack = { route = Route.Main },
                onSaved = {
                    viewModel.loadMedia()
                    route = Route.Main
                }
            )
        }

        is Route.AlbumDetail -> {
            val isFavoritesAlbum = r.bucketId == FAVORITES_BUCKET_ID
            val albumItems = if (isFavoritesAlbum) {
                allMedia.filter { it.id in favoriteIds && it.id !in trashedIds }
            } else {
                allMedia.filter { it.bucketId == r.bucketId && it.id !in trashedIds }
            }
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
                        val source = if (isFavoritesAlbum) ViewerSource.Favorites else ViewerSource.Album(r.bucketId)
                        route = Route.Viewer(source, idx)
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
                    pendingTrashConfirm = true
                },
                onSelectAll = {
                    viewModel.selectAll(albumItems.map { it.id })
                },
                onCreativity = {
                    Toast.makeText(context, "Creativity tools are coming soon", Toast.LENGTH_SHORT).show()
                },
                onAddToAlbum = {
                    Toast.makeText(context, "Add to album is coming soon", Toast.LENGTH_SHORT).show()
                },
                gridState = albumGridStates.getOrPut(r.bucketId) { LazyGridState() }
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
            val trashItems = viewModel.trashedItems()
            TrashBinScreen(
                items = trashItems,
                onBack = { route = Route.Main },
                onEmptyTrash = { viewModel.permanentlyDelete(trashItems.map { it.id }.toSet()) },
                onRestore = { viewModel.restoreFromTrash(it.id) },
                onDeleteForever = { viewModel.permanentlyDelete(setOf(it.id)) }
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
                                onCreativity = {
                                    Toast.makeText(context, "Creativity tools are coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onAddToAlbum = {
                                    Toast.makeText(context, "Add to album is coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    pendingTrashConfirm = true
                                },
                                onSelectAll = {
                                    viewModel.selectAll(allMedia.filter { it.id !in trashedIds }.map { it.id })
                                }
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
                                onSelectAll = {
                                    viewModel.selectAll(allMedia.filter { it.id !in trashedIds }.map { it.id })
                                }
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
                                    route = Route.Viewer(ViewerSource.AllMedia, idx)
                                }
                            },
                            onItemLongClick = {
                                selectionMode = true
                                viewModel.toggleSelection(it.id)
                            },
                            modifier = Modifier.padding(padding),
                            listState = photosListState
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
                            onTrashBin = { route = Route.TrashBin },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }

    if (pendingTrashConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingTrashConfirm = false },
            title = { androidx.compose.material3.Text("Move to bin?") },
            text = { androidx.compose.material3.Text("It will be removed from all folders.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.moveSelectedToTrash()
                    selectionMode = false
                    pendingTrashConfirm = false
                }) { androidx.compose.material3.Text("Move to bin") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingTrashConfirm = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}
