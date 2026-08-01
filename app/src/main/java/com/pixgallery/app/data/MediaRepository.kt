package com.pixgallery.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.pixgallery.app.model.AlbumItem
import com.pixgallery.app.model.DateGroup
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reads photos & videos from the device using MediaStore.
 * Works fully offline - no third party media SDK needed.
 */
class MediaRepository(private val context: Context) {

    suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items += queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE)
        items += queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO)
        items.sortedByDescending { it.dateTakenMillis }
    }

    private fun queryMedia(collection: android.net.Uri, type: MediaType): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            if (type == MediaType.VIDEO) MediaStore.Video.VideoColumns.DURATION else MediaStore.MediaColumns._ID
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)
        }.getOrNull()?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (type == MediaType.VIDEO)
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                result += MediaItem(
                    id = id,
                    uri = uri,
                    name = cursor.getString(nameCol) ?: "",
                    dateTakenMillis = cursor.getLong(dateCol) * 1000L,
                    type = type,
                    durationMillis = duration,
                    bucketId = cursor.getString(bucketIdCol) ?: "",
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                )
            }
        }
        return result
    }

    fun groupByDate(items: List<MediaItem>): List<DateGroup> {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - DAY_MILLIS
        val fmt = SimpleDateFormat("MMMM d", Locale.getDefault())

        return items.groupBy { item ->
            val day = startOfDay(item.dateTakenMillis)
            when (day) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> fmt.format(Date(item.dateTakenMillis))
            }
        }.map { (label, groupItems) -> DateGroup(label, groupItems) }
    }

    fun groupByAlbum(items: List<MediaItem>): List<AlbumItem> {
        return items.groupBy { it.bucketId }
            .map { (bucketId, groupItems) ->
                AlbumItem(
                    bucketId = bucketId,
                    bucketName = groupItems.first().bucketName,
                    coverUri = groupItems.first().uri,
                    itemCount = groupItems.size
                )
            }
            .sortedByDescending { it.itemCount }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
