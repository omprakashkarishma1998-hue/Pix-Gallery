package com.pixgallery.app.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTakenMillis: Long,
    val type: MediaType,
    val durationMillis: Long = 0L,
    val bucketId: String,
    val bucketName: String
)

data class DateGroup(
    val label: String,
    val items: List<MediaItem>
)

data class AlbumItem(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri,
    val itemCount: Int
)

/** Reserved bucket id for the synthetic "Favorites" album, which isn't a real
 *  MediaStore bucket - it's built from whatever the user has starred. */
const val FAVORITES_BUCKET_ID = "__pixgallery_favorites__"
