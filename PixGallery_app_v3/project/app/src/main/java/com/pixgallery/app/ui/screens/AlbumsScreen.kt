package com.pixgallery.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.AlbumItem

@Composable
fun AlbumsScreen(
    albums: List<AlbumItem>,
    isLoading: Boolean,
    favoritesCount: Int,
    favoritesCoverUri: Uri?,
    onAlbumClick: (AlbumItem) -> Unit,
    onFavoritesClick: () -> Unit,
    onOtherAlbums: () -> Unit,
    onTrashBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Albums",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when {
            isLoading -> LoadingBox()
            albums.isEmpty() && favoritesCount == 0 -> EmptyState("No albums found")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (favoritesCount > 0) {
                    item(key = "favorites_album_tile") {
                        FavoritesTile(
                            coverUri = favoritesCoverUri,
                            count = favoritesCount,
                            onClick = onFavoritesClick
                        )
                    }
                }
                items(albums, key = { it.bucketId }) { album ->
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable { onAlbumClick(album) }
                    ) {
                        AsyncImage(
                            model = album.coverUri,
                            contentDescription = album.bucketName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            album.bucketName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            "${album.itemCount}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/** Special "album" tile for favorited photos/videos - shown first, ahead of real
 *  folder-based albums, same as most gallery apps (Google Photos, Redmi Gallery, etc). */
@Composable
private fun FavoritesTile(coverUri: Uri?, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(6.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "Favorites",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        Text(
            "Favorites",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
