package com.pixgallery.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The filter presets available in the editor. Each maps to a color matrix that is
 *  used both for the live preview (Compose) and for the final baked-in save (Android). */
enum class EditFilter(val label: String) {
    ORIGINAL("Original"),
    BW("B&W"),
    SEPIA("Sepia"),
    BRIGHT("Bright"),
    COOL("Cool"),
    WARM("Warm"),
    CONTRAST("Contrast");

    fun androidColorMatrix(): android.graphics.ColorMatrix {
        val cm = android.graphics.ColorMatrix()
        when (this) {
            ORIGINAL -> { /* identity */ }
            BW -> cm.setSaturation(0f)
            SEPIA -> {
                cm.setSaturation(0.3f)
                val sepia = android.graphics.ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(sepia)
            }
            BRIGHT -> cm.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 35f,
                    0f, 1f, 0f, 0f, 35f,
                    0f, 0f, 1f, 0f, 35f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            COOL -> cm.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 8f,
                    0f, 0f, 1f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            WARM -> cm.set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 30f,
                    0f, 1f, 0f, 0f, 12f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            CONTRAST -> {
                val c = 1.35f
                val t = (-0.5f * c + 0.5f) * 255f
                cm.set(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
        return cm
    }

    /** Same matrix, converted for use as a Compose ColorFilter (same 4x5 row-major layout). */
    fun composeColorMatrix(): androidx.compose.ui.graphics.ColorMatrix {
        val values = FloatArray(20)
        androidColorMatrix().getValues(values)
        return androidx.compose.ui.graphics.ColorMatrix(values)
    }
}

/** Result of a save attempt so the UI can show success/failure without throwing. */
sealed class SaveResult {
    data class Success(val uri: Uri) : SaveResult()
    data object Failure : SaveResult()
}

object ImageEditUtils {

    /** Loads a bitmap for editing, corrected for EXIF rotation and downsampled so a
     *  12MP+ camera photo doesn't blow up memory while the editor is open. */
    suspend fun loadBitmapForEditing(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sampleSize = 1
                val w = bounds.outWidth
                val h = bounds.outHeight
                if (w > 0 && h > 0) {
                    while ((w / sampleSize) > maxDimension * 2 || (h / sampleSize) > maxDimension * 2) {
                        sampleSize *= 2
                    }
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val raw = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext null

                val rotation = readExifRotationDegrees(context, uri)
                if (rotation == 0) raw else rotateBitmap(raw, rotation.toFloat())
            } catch (e: Exception) {
                null
            }
        }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** Rotates a bitmap by the given degrees (use 90/-90 for the left/right buttons). */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /** Crops a bitmap to the given pixel rect, clamped to stay inside the bitmap bounds. */
    fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        val left = rect.left.coerceIn(0, bitmap.width - 1)
        val top = rect.top.coerceIn(0, bitmap.height - 1)
        val width = rect.width().coerceIn(1, bitmap.width - left)
        val height = rect.height().coerceIn(1, bitmap.height - top)
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        if (cropped !== bitmap) bitmap.recycle()
        return cropped
    }

    /** Bakes a filter permanently into a new bitmap (used right before saving). */
    fun bakeFilter(bitmap: Bitmap, filter: EditFilter): Bitmap {
        if (filter == EditFilter.ORIGINAL) return bitmap
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(filter.androidColorMatrix())
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /** Saves the edited bitmap as a brand new file under Pictures/PixGallery.
     *  Saving as a new file (rather than overwriting the original) means we never need
     *  special write permission on a file we don't own - it always just works. */
    suspend fun saveAsNewImage(context: Context, bitmap: Bitmap): SaveResult = withContext(Dispatchers.IO) {
        val displayName = "PixGallery_edit_${System.currentTimeMillis()}.jpg"
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PixGallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val uri = context.contentResolver.insert(collection, values)
                ?: return@withContext SaveResult.Failure

            val wrote = context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            } ?: false

            if (!wrote) {
                context.contentResolver.delete(uri, null, null)
                return@withContext SaveResult.Failure
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val update = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, update, null, null)
            }
            SaveResult.Success(uri)
        } catch (e: Exception) {
            SaveResult.Failure
        }
    }
}
