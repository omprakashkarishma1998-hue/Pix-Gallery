package com.pixgallery.app.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.model.MediaType
import java.io.InputStream

/**
 * Wraps the standard Android intents so buttons in the UI actually
 * do something (share, edit, set wallpaper) instead of being decorative.
 */
object ShareUtils {

    private fun mimeTypeFor(item: MediaItem): String =
        if (item.type == MediaType.VIDEO) "video/*" else "image/*"

    /** Opens the system share sheet ("Send") for a single item. */
    fun shareSingle(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeTypeFor(item)
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    /** Opens the system share sheet for several items at once. */
    fun shareMultiple(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        val uris = ArrayList<Uri>(items.map { it.uri })
        val allImages = items.all { it.type == MediaType.IMAGE }
        val allVideos = items.all { it.type == MediaType.VIDEO }
        val type = when {
            allImages -> "image/*"
            allVideos -> "video/*"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            this.type = type
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    /** Opens the item in any installed photo/video editor via ACTION_EDIT. */
    fun editItem(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(item.uri, mimeTypeFor(item))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Edit with")
        if (intent.resolveActivity(context.packageManager) != null || chooser.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, "No editor app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /** Sets an image as the device wallpaper. Only meaningful for images. */
    fun setAsWallpaper(context: Context, item: MediaItem) {
        if (item.type != MediaType.IMAGE) {
            Toast.makeText(context, "Only photos can be set as wallpaper", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val input: InputStream? = context.contentResolver.openInputStream(item.uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(input)
            input?.close()
            if (bitmap == null) {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
                return
            }
            WallpaperManager.getInstance(context).setBitmap(bitmap)
            Toast.makeText(context, "Wallpaper set", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Opens the Android share sheet so the user can send a feedback / bug report. */
    fun openAndroidShareForFile(context: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
