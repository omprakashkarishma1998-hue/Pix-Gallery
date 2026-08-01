package com.pixgallery.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.util.EditFilter
import com.pixgallery.app.util.ImageEditUtils
import com.pixgallery.app.util.SaveResult
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private enum class EditTool { NONE, CROP, FILTERS }

private data class AspectPreset(val label: String, val ratio: Float?) // null = free

private val ASPECT_PRESETS = listOf(
    AspectPreset("Free", null),
    AspectPreset("1:1", 1f),
    AspectPreset("4:3", 4f / 3f),
    AspectPreset("3:4", 3f / 4f),
    AspectPreset("16:9", 16f / 9f)
)

/**
 * A real, self-contained photo editor: rotate left/right, crop (drag handles + aspect
 * presets), and filter effects. Saves the result as a new file so we never need
 * write permission on a photo we didn't create.
 */
@Composable
fun PhotoEditScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScopeSafe()

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var bitmapVersion by remember { mutableStateOf(0) }

    var activeTool by remember { mutableStateOf(EditTool.NONE) }
    var selectedFilter by remember { mutableStateOf(EditFilter.ORIGINAL) }
    var selectedAspect by remember { mutableStateOf<Float?>(null) }

    // Container (available drawing area) size, captured once laid out.
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // The crop rectangle in container-local pixel coordinates. Reset whenever we
    // (re)enter crop mode or the underlying bitmap changes (rotate/crop applied).
    var cropRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(item.uri) {
        val bmp = ImageEditUtils.loadBitmapForEditing(context, item.uri)
        if (bmp == null) {
            loadFailed = true
        } else {
            baseBitmap = bmp
        }
    }

    fun imageRectIn(container: IntSize, bmp: Bitmap): Rect {
        if (container.width == 0 || container.height == 0) return Rect.Zero
        val cw = container.width.toFloat()
        val ch = container.height.toFloat()
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = min(cw / bw, ch / bh)
        val dw = bw * scale
        val dh = bh * scale
        val left = (cw - dw) / 2f
        val top = (ch - dh) / 2f
        return Rect(Offset(left, top), Size(dw, dh))
    }

    fun resetCropToPreset(ratio: Float?) {
        val bmp = baseBitmap ?: return
        val imgRect = imageRectIn(containerSize, bmp)
        if (imgRect == Rect.Zero) return
        if (ratio == null) {
            cropRect = imgRect
            return
        }
        val maxW = imgRect.width
        val maxH = imgRect.height
        var w = maxW
        var h = w / ratio
        if (h > maxH) {
            h = maxH
            w = h * ratio
        }
        val cx = imgRect.left + imgRect.width / 2f
        val cy = imgRect.top + imgRect.height / 2f
        cropRect = Rect(
            Offset(cx - w / 2f, cy - h / 2f),
            Size(w, h)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val bmp = baseBitmap
        when {
            loadFailed -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Couldn't open this photo for editing.", color = Color.White)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onBack) { Text("Back") }
            }

            bmp == null -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )

            else -> {
                val imageBitmap = remember(bmp, bitmapVersion) { bmp.asImageBitmap() }

                Column(Modifier.fillMaxSize()) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Edit", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(onClick = {
                                val current = baseBitmap ?: return@TextButton
                                isSaving = true
                                scope.launch {
                                    val finalBitmap = ImageEditUtils.bakeFilter(current, selectedFilter)
                                    when (val result = ImageEditUtils.saveAsNewImage(context, finalBitmap)) {
                                        is SaveResult.Success -> {
                                            isSaving = false
                                            Toast.makeText(context, "Saved to Pictures/PixGallery", Toast.LENGTH_SHORT).show()
                                            onSaved()
                                        }
                                        SaveResult.Failure -> {
                                            isSaving = false
                                            Toast.makeText(context, "Couldn't save the edited photo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }) {
                                Text("Save", color = Color.White)
                            }
                        }
                    }

                    // Image + crop overlay area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val newSize = coords.size
                                if (newSize != containerSize) {
                                    containerSize = newSize
                                    if (activeTool == EditTool.CROP) resetCropToPreset(selectedAspect)
                                }
                            }
                    ) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            colorFilter = if (selectedFilter == EditFilter.ORIGINAL) null
                                else ColorFilter.colorMatrix(selectedFilter.composeColorMatrix()),
                            modifier = Modifier.fillMaxSize()
                        )

                        if (activeTool == EditTool.CROP) {
                            val imgRect = imageRectIn(containerSize, bmp)
                            val rect = cropRect ?: imgRect
                            CropOverlay(
                                imageBounds = imgRect,
                                cropRect = rect,
                                lockedAspect = selectedAspect,
                                onCropRectChange = { cropRect = it }
                            )
                        }
                    }

                    // Tool-specific controls
                    when (activeTool) {
                        EditTool.CROP -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                            ) {
                                Spacer(Modifier.width(4.dp))
                                ASPECT_PRESETS.forEach { preset ->
                                    val selected = selectedAspect == preset.ratio
                                    Text(
                                        text = preset.label,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedAspect = preset.ratio
                                                resetCropToPreset(preset.ratio)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = {
                                    activeTool = EditTool.NONE
                                    cropRect = null
                                    selectedAspect = null
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cancel", color = Color.White)
                                }
                                TextButton(onClick = {
                                    val current = baseBitmap ?: return@TextButton
                                    val imgRect = imageRectIn(containerSize, current)
                                    val rect = cropRect ?: imgRect
                                    if (imgRect.width > 0f && imgRect.height > 0f) {
                                        val scaleFactor = current.width / imgRect.width
                                        val left = ((rect.left - imgRect.left) * scaleFactor)
                                        val top = ((rect.top - imgRect.top) * scaleFactor)
                                        val right = ((rect.right - imgRect.left) * scaleFactor)
                                        val bottom = ((rect.bottom - imgRect.top) * scaleFactor)
                                        val pixelRect = android.graphics.Rect(
                                            left.roundToInt(), top.roundToInt(),
                                            right.roundToInt(), bottom.roundToInt()
                                        )
                                        baseBitmap = ImageEditUtils.cropBitmap(current, pixelRect)
                                        bitmapVersion++
                                    }
                                    activeTool = EditTool.NONE
                                    cropRect = null
                                    selectedAspect = null
                                }) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Apply", color = Color.White)
                                }
                            }
                        }

                        EditTool.FILTERS -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                            ) {
                                EditFilter.values().forEach { filter ->
                                    val selected = selectedFilter == filter
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .border(
                                                    width = if (selected) 2.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedFilter = filter }
                                        ) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = filter.label,
                                                contentScale = ContentScale.Crop,
                                                colorFilter = if (filter == EditFilter.ORIGINAL) null
                                                    else ColorFilter.colorMatrix(filter.composeColorMatrix()),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(filter.label, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        EditTool.NONE -> {}
                    }

                    // Bottom main toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
                    ) {
                        EditToolButton(
                            icon = Icons.Filled.Crop,
                            label = "Crop",
                            selected = activeTool == EditTool.CROP
                        ) {
                            activeTool = if (activeTool == EditTool.CROP) EditTool.NONE else EditTool.CROP
                            if (activeTool == EditTool.CROP) resetCropToPreset(selectedAspect)
                        }
                        EditToolButton(
                            icon = Icons.Filled.RotateLeft,
                            label = "Rotate L"
                        ) {
                            baseBitmap?.let {
                                baseBitmap = ImageEditUtils.rotateBitmap(it, -90f)
                                bitmapVersion++
                                if (activeTool == EditTool.CROP) resetCropToPreset(selectedAspect)
                            }
                        }
                        EditToolButton(
                            icon = Icons.Filled.RotateRight,
                            label = "Rotate R"
                        ) {
                            baseBitmap?.let {
                                baseBitmap = ImageEditUtils.rotateBitmap(it, 90f)
                                bitmapVersion++
                                if (activeTool == EditTool.CROP) resetCropToPreset(selectedAspect)
                            }
                        }
                        EditToolButton(
                            icon = Icons.Filled.FilterVintage,
                            label = "Effects",
                            selected = activeTool == EditTool.FILTERS
                        ) {
                            activeTool = if (activeTool == EditTool.FILTERS) EditTool.NONE else EditTool.FILTERS
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White
            )
        }
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/** Draggable/resizable crop rectangle overlay: dims everything outside the rect,
 *  lets the user drag the interior to move it and the four corner handles to resize it. */
@Composable
private fun CropOverlay(
    imageBounds: Rect,
    cropRect: Rect,
    lockedAspect: Float?,
    onCropRectChange: (Rect) -> Unit
) {
    val density = LocalDensity.current
    val minSizePx = with(density) { 48.dp.toPx() }
    val handleTouchPx = with(density) { 28.dp.toPx() }

    // Read the latest rect/bounds from inside the (long-lived) drag coroutines without
    // making them a pointerInput key - keying on a value that changes every drag frame
    // would restart the gesture detector mid-drag and make dragging feel broken.
    val latestCropRect = androidx.compose.runtime.rememberUpdatedState(cropRect)
    val latestImageBounds = androidx.compose.runtime.rememberUpdatedState(imageBounds)
    val latestLockedAspect = androidx.compose.runtime.rememberUpdatedState(lockedAspect)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dim everything, then "cut out" the crop rect by drawing it separately below.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        // Bright window showing the crop area (re-draws image region undimmed via border only,
        // keeping this lightweight - the dimmed overlay above still shows through elsewhere).
        Box(
            modifier = Modifier
                .offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
                .size(
                    with(density) { cropRect.width.toDp() },
                    with(density) { cropRect.height.toDp() }
                )
                .background(Color.Transparent)
                .border(1.dp, Color.White)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val rect = latestCropRect.value
                        val bounds = latestImageBounds.value
                        val newLeft = (rect.left + dragAmount.x)
                            .coerceIn(bounds.left, bounds.right - rect.width)
                        val newTop = (rect.top + dragAmount.y)
                            .coerceIn(bounds.top, bounds.bottom - rect.height)
                        onCropRectChange(Rect(Offset(newLeft, newTop), rect.size))
                    }
                }
        )

        // Corner handles
        listOf(
            Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd
        ).forEach { corner ->
            val handleCenter = when (corner) {
                Alignment.TopStart -> Offset(cropRect.left, cropRect.top)
                Alignment.TopEnd -> Offset(cropRect.right, cropRect.top)
                Alignment.BottomStart -> Offset(cropRect.left, cropRect.bottom)
                else -> Offset(cropRect.right, cropRect.bottom)
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (handleCenter.x - handleTouchPx / 2f).roundToInt(),
                            (handleCenter.y - handleTouchPx / 2f).roundToInt()
                        )
                    }
                    .size(with(density) { handleTouchPx.toDp() })
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val startRect = latestCropRect.value
                            val bounds = latestImageBounds.value
                            val aspect = latestLockedAspect.value
                            var left = startRect.left
                            var top = startRect.top
                            var right = startRect.right
                            var bottom = startRect.bottom

                            when (corner) {
                                Alignment.TopStart -> {
                                    left = (left + dragAmount.x).coerceIn(bounds.left, right - minSizePx)
                                    top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minSizePx)
                                }
                                Alignment.TopEnd -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, bounds.right)
                                    top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minSizePx)
                                }
                                Alignment.BottomStart -> {
                                    left = (left + dragAmount.x).coerceIn(bounds.left, right - minSizePx)
                                    bottom = (bottom + dragAmount.y).coerceIn(top + minSizePx, bounds.bottom)
                                }
                                else -> {
                                    right = (right + dragAmount.x).coerceIn(left + minSizePx, bounds.right)
                                    bottom = (bottom + dragAmount.y).coerceIn(top + minSizePx, bounds.bottom)
                                }
                            }

                            if (aspect != null) {
                                val w = right - left
                                val h = w / aspect
                                when (corner) {
                                    Alignment.TopStart -> top = bottom - h
                                    Alignment.TopEnd -> top = bottom - h
                                    Alignment.BottomStart -> bottom = top + h
                                    else -> bottom = top + h
                                }
                            }

                            onCropRectChange(Rect(left, top, right, bottom))
                        }
                    }
                    .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, Color.Black, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

/** Small helper so this file doesn't need an extra import block juggling act at the call site. */
@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
