package com.pixgallery.app.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pixgallery.app.model.AlbumItem
import com.pixgallery.app.model.DateGroup
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType
import com.pixgallery.app.model.MemoryGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Result of attempting to permanently delete media from the device. */
sealed class DeleteResult {
    /** Deletion is already done, no user confirmation was needed. */
    data object Done : DeleteResult()
    /** The system needs the user to confirm via a dialog before the files can be removed
     *  (required on Android 10 for files we don't own, and always on Android 11+). */
    data class NeedsConfirmation(val intentSender: android.content.IntentSender) : DeleteResult()
    data object Failed : DeleteResult()
}

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
            MediaStore.MediaColumns.SIZE,
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
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val durationCol = if (type == MediaType.VIDEO)
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                result += MediaItem(
                    id = id,
                    uri = uri,
                    name = cursor.getString(nameCol) ?: "",
                    dateTakenMillis = cursor.getLong(dateCol) * 1000L,
                    type = type,
                    durationMillis = duration,
                    bucketId = cursor.getString(bucketIdCol) ?: "",
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                    sizeBytes = size
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

    /** Actually removes the given items from the device (not just from this app's trash list).
     *  On Android 11+, batches everything into a single system confirmation dialog. On Android 10,
     *  falls back to catching RecoverableSecurityException and asking for that instead. Below that,
     *  plain contentResolver.delete() is enough. */
    suspend fun requestPermanentDelete(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Done
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                DeleteResult.NeedsConfirmation(pendingIntent.intentSender)
            } else {
                var allDeleted = true
                for (uri in uris) {
                    val rows = context.contentResolver.delete(uri, null, null)
                    if (rows <= 0) allDeleted = false
                }
                if (allDeleted) DeleteResult.Done else DeleteResult.Failed
            }
        } catch (e: RecoverableSecurityException) {
            DeleteResult.NeedsConfirmation(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            DeleteResult.Failed
        }
    }

    /** Groups items shot on the same calendar month/day as today, but in an
     *  earlier year - e.g. "3 years ago today". Sorted most-recent-memory first. */
    fun groupOnThisDay(items: List<MediaItem>): List<MemoryGroup> {
        val today = Calendar.getInstance()
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val thisYear = today.get(Calendar.YEAR)

        val cal = Calendar.getInstance()
        return items
            .filter { item ->
                cal.timeInMillis = item.dateTakenMillis
                cal.get(Calendar.MONTH) == todayMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == todayDay &&
                    cal.get(Calendar.YEAR) != thisYear
            }
            .groupBy { item ->
                cal.timeInMillis = item.dateTakenMillis
                cal.get(Calendar.YEAR)
            }
            .map { (year, groupItems) ->
                val yearsAgo = thisYear - year
                MemoryGroup(
                    yearsAgo = yearsAgo,
                    label = if (yearsAgo == 1) "1 year ago" else "$yearsAgo years ago",
                    items = groupItems.sortedByDescending { it.dateTakenMillis }
                )
            }
            .sortedBy { it.yearsAgo }
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
