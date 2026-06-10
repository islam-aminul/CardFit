package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.NormCrop

/**
 * Interactive crop rectangle drawn over the photo preview (CLAUDE.md Phase 13). Reports the crop in
 * normalized image coordinates (0..1) so the ViewModel can map it to source pixels. Drag the body to
 * move and the corners to resize; when [lockAspect] is non-null the rectangle keeps that width/height
 * aspect (the displayed image aspect equals the source aspect, so a fixed displayed aspect yields the
 * correct pixel-crop aspect).
 *
 * Must sit inside a container already sized to the image content (e.g. `aspectRatio(imageAspect)`), so
 * the overlay's pixel space matches the visible image exactly.
 */
@Composable
fun CropOverlay(
    crop: NormCrop,
    lockAspect: Float?,
    onCropChange: (NormCrop) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        if (wPx <= 0f || hPx <= 0f) return@BoxWithConstraints

        val handlePx = with(density) { 24.dp.toPx() }
        val minFrac = 0.1f

        fun rectPx() = Rect(crop.left * wPx, crop.top * hPx, crop.right * wPx, crop.bottom * hPx)

        fun emit(left: Float, top: Float, right: Float, bottom: Float) {
            val l = left.coerceIn(0f, 1f)
            val t = top.coerceIn(0f, 1f)
            val r = right.coerceIn(0f, 1f)
            val b = bottom.coerceIn(0f, 1f)
            if (r - l >= minFrac && b - t >= minFrac) onCropChange(NormCrop(l, t, r, b))
        }

        // Resize from a corner, optionally preserving the locked aspect (in displayed pixels).
        fun resize(corner: Corner, dxFrac: Float, dyFrac: Float) {
            var l = crop.left
            var t = crop.top
            var r = crop.right
            var b = crop.bottom
            when (corner) {
                Corner.TL -> { l += dxFrac; t += dyFrac }
                Corner.TR -> { r += dxFrac; t += dyFrac }
                Corner.BL -> { l += dxFrac; b += dyFrac }
                Corner.BR -> { r += dxFrac; b += dyFrac }
            }
            if (lockAspect != null) {
                // Keep width_px / height_px == lockAspect (wPx/hPx folds the box aspect in).
                val widthFrac = r - l
                val targetHeightFrac = (widthFrac * wPx / lockAspect) / hPx
                when (corner) {
                    Corner.TL, Corner.TR -> t = b - targetHeightFrac
                    Corner.BL, Corner.BR -> b = t + targetHeightFrac
                }
            }
            emit(l, t, r, b)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(wPx, hPx) {
                    // Move the whole crop rectangle, clamped within bounds without resizing.
                    detectDragGestures { _, drag ->
                        val w = crop.right - crop.left
                        val h = crop.bottom - crop.top
                        var l = crop.left + drag.x / wPx
                        var t = crop.top + drag.y / hPx
                        l = l.coerceIn(0f, 1f - w)
                        t = t.coerceIn(0f, 1f - h)
                        onCropChange(NormCrop(l, t, l + w, t + h))
                    }
                },
        ) {
            val rect = rectPx()
            val dim = Color.Black.copy(alpha = 0.5f)
            drawRect(dim, topLeft = Offset.Zero, size = Size(size.width, rect.top))
            drawRect(dim, topLeft = Offset(0f, rect.bottom), size = Size(size.width, size.height - rect.bottom))
            drawRect(dim, topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
            drawRect(dim, topLeft = Offset(rect.right, rect.top), size = Size(size.width - rect.right, rect.height))
            drawRect(Color.White, topLeft = rect.topLeft, size = rect.size, style = Stroke(2f))
            for (i in 1..2) {
                val x = rect.left + rect.width * i / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(x, rect.top), Offset(x, rect.bottom))
                val y = rect.top + rect.height * i / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(rect.left, y), Offset(rect.right, y))
            }
            for (corner in Corner.entries) {
                drawCircle(Color.White, radius = 7f, center = corner.point(rect))
            }
        }

        // Invisible corner hit areas, each with its own drag detector.
        Corner.entries.forEach { corner ->
            val center = corner.point(rectPx())
            val sizeDp = with(density) { (handlePx * 2).toDp() }
            val offsetXDp = with(density) { (center.x - handlePx).toDp() }
            val offsetYDp = with(density) { (center.y - handlePx).toDp() }
            Box(
                modifier = Modifier
                    .offset(offsetXDp, offsetYDp)
                    .size(sizeDp)
                    .pointerInput(corner, crop) {
                        detectDragGestures { _, drag -> resize(corner, drag.x / wPx, drag.y / hPx) }
                    },
            )
        }
    }
}

private enum class Corner {
    TL, TR, BL, BR;

    fun point(rect: Rect): Offset = when (this) {
        TL -> rect.topLeft
        TR -> Offset(rect.right, rect.top)
        BL -> Offset(rect.left, rect.bottom)
        BR -> Offset(rect.right, rect.bottom)
    }
}
