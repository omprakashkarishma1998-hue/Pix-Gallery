package com.pixgallery.app.util

import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType

/** One slice of the storage breakdown - e.g. "Screenshots: 340 MB across 128 items". */
data class StorageCategory(
    val label: String,
    val totalBytes: Long,
    val itemCount: Int
)

data class StorageReport(
    val totalBytes: Long,
    val photoBytes: Long,
    val videoBytes: Long,
    val categories: List<StorageCategory>,
    val largestFiles: List<MediaItem>
)

/**
 * Groups the user's photos/videos by folder pattern into human-friendly
 * buckets (Screenshots, Camera, WhatsApp, Downloads, Other) and reports how
 * much space each one is using. This is a genuinely different tool from the
 * Duplicate Finder - it answers "where did my storage go?" instead of
 * "which files are copies of each other?", and helps the user find single
 * large files worth cleaning up (long videos, big screen recordings, etc.).
 */
object StorageAnalyzer {

    fun analyze(items: List<MediaItem>, largeFileThresholdBytes: Long = 20L * 1024 * 1024): StorageReport {
        val totalBytes = items.sumOf { it.sizeBytes }
        val photoBytes = items.filter { it.type == MediaType.IMAGE }.sumOf { it.sizeBytes }
        val videoBytes = items.filter { it.type == MediaType.VIDEO }.sumOf { it.sizeBytes }

        val categories = items
            .groupBy { categoryFor(it.bucketName) }
            .map { (label, group) -> StorageCategory(label, group.sumOf { it.sizeBytes }, group.size) }
            .sortedByDescending { it.totalBytes }

        val largestFiles = items
            .filter { it.sizeBytes >= largeFileThresholdBytes }
            .sortedByDescending { it.sizeBytes }
            .take(50)

        return StorageReport(
            totalBytes = totalBytes,
            photoBytes = photoBytes,
            videoBytes = videoBytes,
            categories = categories,
            largestFiles = largestFiles
        )
    }

    private fun categoryFor(bucketName: String): String {
        val lower = bucketName.lowercase()
        return when {
            lower.contains("screenshot") -> "Screenshots"
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("camera") || lower.contains("dcim") -> "Camera"
            lower.contains("download") -> "Downloads"
            else -> "Other (${bucketName.ifBlank { "Unknown" }})"
        }
    }

    /** Formats a byte count as e.g. "128.4 MB" / "1.2 GB". */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> "%.1f GB".format(bytes / gb)
            bytes >= mb -> "%.1f MB".format(bytes / mb)
            else -> "%.0f KB".format(bytes / kb)
        }
    }
}
