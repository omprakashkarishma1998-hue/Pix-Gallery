package com.pixgallery.app.util

import android.content.Context
import com.pixgallery.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** One set of items that are exact duplicates of each other. */
data class DuplicateGroup(
    val items: List<MediaItem>
)

/**
 * Finds exact duplicate photos/videos already on the device (e.g. the same
 * photo saved twice, or re-downloaded from WhatsApp). Works fully offline.
 *
 * Detection is a two-pass content hash: first bucket candidates by file size
 * (cheap), then only fully hash (SHA-256) files that share a size with at
 * least one other file. This keeps a full-library scan fast - most files
 * never need to be read at all.
 */
object DuplicateFinder {

    suspend fun findDuplicates(
        context: Context,
        items: List<MediaItem>,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Pass 1: group by file size - free, no file reads.
        val bySize = items.groupBy { item ->
            runCatching {
                resolver.openAssetFileDescriptor(item.uri, "r")?.use { it.length }
            }.getOrNull() ?: -1L
        }.filterKeys { it > 0 }

        val candidates = bySize.values.filter { it.size > 1 }.flatten()
        var checked = 0
        val total = candidates.size

        // Pass 2: only hash files whose size collides with another file.
        val byHash = HashMap<String, MutableList<MediaItem>>()
        for (item in candidates) {
            val hash = runCatching {
                resolver.openInputStream(item.uri)?.use { stream ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
            }.getOrNull()
            checked++
            onProgress(checked, total)
            if (hash != null) {
                byHash.getOrPut(hash) { mutableListOf() }.add(item)
            }
        }

        byHash.values
            .filter { it.size > 1 }
            .map { group -> DuplicateGroup(group.sortedByDescending { it.dateTakenMillis }) }
            .sortedByDescending { it.items.size }
    }
}
