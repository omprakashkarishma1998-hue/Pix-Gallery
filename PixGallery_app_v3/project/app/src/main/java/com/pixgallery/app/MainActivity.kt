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
    data object Favorites : Route()
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

    // Auto-refresh whenever the app comes back to the foreground (user re-opens it after
    // closing/backgrounding it, switching apps, taking a new photo elsewhere, etc.) so the
    // gallery always reflects what's actually on the device instead of a stale snapshot.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionUtils.hasMediaPermissions(context)
                if (hasPermission) viewModel.loadMedia()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by rememberSaveable { mutableStateOf(GalleryTab.PHOTOS) }
    var route by remember { mutableStateOf<Route>(Route.Main) }
    var selectionMode by remember { mutableStateOf(false) }

    // Confirmation state for moving item(s) to trash - shown as a Delete/Cancel dialog
    // *before* anything actually gets trashed (separate from the system dialog that
    // guards permanent delete inside the Trash bin screen).
    var pendingSingleTrashItem by remember { mutableStateOf<MediaItem?>(null) }
    var showTrashSelectedConfirm by remember { mutableStateOf(false) }

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
        pendingDeleteRequest?.let { deleteLauncher.launch(it) }
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
                    pendingSingleTrashItem = item
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
                        route = Route.Viewer(ViewerSource.Album(r.bucketId), idx)
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
                    showTrashSelectedConfirm = true
                },
                gridState = albumGridStates.getOrPut(r.bucketId) { LazyGridState() }
            )
        }

        Route.Favorites -> {
            val favItems = allMedia.filter { it.id in favoriteIds && it.id !in trashedIds }
            AlbumDetailScreen(
                albumName = "Favorites",
                items = favItems,
                selectedIds = selectedIds,
                selectionMode = selectionMode,
                onBack = { route = Route.Main },
                onItemClick = { item ->
                    if (selectionMode) {
                        viewModel.toggleSelection(item.id)
                    } else {
                        val idx = favItems.indexOf(item)
                        route = Route.Viewer(ViewerSource.Favorites, idx)
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
                    showTrashSelectedConfirm = true
                },
                gridState = albumGridStates.getOrPut("__favorites__") { LazyGridState() }
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
            // Derived from the already-collected `allMedia`/`trashedIds` State so that
            // restoring or emptying an item triggers recomposition immediately - calling
            // viewModel.trashedItems() directly here was the bug: it read the StateFlow's
            // raw .value instead of a collected State, so Compose never saw it change.
            val trashItems = allMedia.filter { it.id in trashedIds }
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
                                onCreativity = {},
                                onAddToAlbum = {},
                                onDelete = {
                                    showTrashSelectedConfirm = true
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
                        val favItemsForTile = allMedia.filter { it.id in favoriteIds && it.id !in trashedIds }
                        AlbumsScreen(
                            albums = viewModel.groupedByAlbum(),
                            isLoading = isLoading,
                            favoritesCount = favItemsForTile.size,
                            favoritesCoverUri = favItemsForTile.firstOrNull()?.uri,
                            onAlbumClick = { album: AlbumItem ->
                                route = Route.AlbumDetail(album.bucketId, album.bucketName)
                            },
                            onFavoritesClick = { route = Route.Favorites },
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

    // Confirm before trashing a single item (Photo/Video viewer's Delete button).
    pendingSingleTrashItem?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingSingleTrashItem = null },
            title = { androidx.compose.material3.Text("Delete this item?") },
            text = { androidx.compose.material3.Text("It will be moved to the Trash bin, where it stays for up to 30 days before being permanently deleted.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.moveToTrash(item.id)
                    pendingSingleTrashItem = null
                    route = Route.Main
                }) { androidx.compose.material3.Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingSingleTrashItem = null }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    // Confirm before trashing the currently multi-selected items (Photos tab / Album detail).
    if (showTrashSelectedConfirm) {
        val count = selectedIds.size
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTrashSelectedConfirm = false },
            title = { androidx.compose.material3.Text(if (count == 1) "Delete this item?" else "Delete $count items?") },
            text = { androidx.compose.material3.Text("They will be moved to the Trash bin, where they stay for up to 30 days before being permanently deleted.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.moveSelectedToTrash()
                    selectionMode = false
                    showTrashSelectedConfirm = false
                }) { androidx.compose.material3.Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTrashSelectedConfirm = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}
