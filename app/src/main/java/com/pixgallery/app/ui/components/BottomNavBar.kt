package com.pixgallery.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class GalleryTab(val label: String) {
    PHOTOS("Photos"),
    ALBUMS("Albums"),
    RECOMMENDED("Recommended")
}

@Composable
fun GalleryBottomNavBar(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == GalleryTab.PHOTOS,
            onClick = { onTabSelected(GalleryTab.PHOTOS) },
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
            label = { Text("Photos") }
        )
        NavigationBarItem(
            selected = selectedTab == GalleryTab.ALBUMS,
            onClick = { onTabSelected(GalleryTab.ALBUMS) },
            icon = { Icon(Icons.Filled.Collections, contentDescription = null) },
            label = { Text("Albums") }
        )
        NavigationBarItem(
            selected = selectedTab == GalleryTab.RECOMMENDED,
            onClick = { onTabSelected(GalleryTab.RECOMMENDED) },
            icon = { Icon(Icons.Filled.Recommend, contentDescription = null) },
            label = { Text("Recommended") }
        )
    }
}
