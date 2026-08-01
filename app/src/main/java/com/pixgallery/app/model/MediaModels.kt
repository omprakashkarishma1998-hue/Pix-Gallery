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
