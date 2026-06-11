package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.NormCrop
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_ZOOM = 6f

/**
 * A fixed square viewport showing the photo at its true aspect ratio with a centred crop aperture
 * locked to [cropAspect]. The user pinch-zooms and drags the *photo* beneath the aperture (the aperture
 * is always fully covered, so the result never stretches and never shows gaps). The visible region
 * through the aperture is reported as a [NormCrop] in the rotated source's normalized space.
 *
 * The transform recentres whenever [cropAspect], [imageAspect] or [resetKey] changes (e.g. on size
 * change, rotation, or Revert); colour edits that swap [image] but keep [imageAspect] leave it alone.
 */
@Composable
fun PhotoCropFrame(
    image: ImageBitmap?,
    imageAspect: Float,
    cropAspect: Float,
    resetKey: Int,
    busy: Boolean,
    onCrop: (NormCrop) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val vw = constraints.maxWidth.toFloat()
        val vh = constraints.maxHeight.toFloat()

        // Aperture: centred, aspect == cropAspect, 10% margin on the limiting side of the viewport.
        val margin = 0.10f
        val availW = vw * (1f - 2f * margin)
        val availH = vh * (1f - 2f * margin)
        val apW: Float
        val apH: Float
        if (availW / availH > cropAspect) {
            apH = availH; apW = apH * cropAspect
        } else {
            apW = availW; apH = apW / cropAspect
        }
        val apLeft = (vw - apW) / 2f
        val apTop = (vh - apH) / 2f

        // Image size (at zoom 1) that just covers the aperture, preserving the photo's aspect.
        val coverH = max(apH, apW / imageAspect)
        val coverW = coverH * imageAspect

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        fun clampOffset(o: Offset, s: Float): Offset {
            val maxX = max(0f, (coverW * s - apW) / 2f)
            val maxY = max(0f, (coverH * s - apH) / 2f)
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }

        // Recentre whenever the framing parameters change.
        LaunchedEffect(cropAspect, imageAspect, resetKey, vw, vh) {
            scale = 1f
            offset = Offset.Zero
        }

        // Report the visible region as a normalized crop whenever the transform/aperture changes.
        LaunchedEffect(scale, offset, cropAspect, imageAspect, vw, vh) {
            if (vw <= 0f || vh <= 0f) return@LaunchedEffect
            val imgW = coverW * scale
            val imgH = coverH * scale
            val imgLeft = vw / 2f - imgW / 2f + offset.x
            val imgTop = vh / 2f - imgH / 2f + offset.y
            val l = ((apLeft - imgLeft) / imgW).coerceIn(0f, 1f)
            val t = ((apTop - imgTop) / imgH).coerceIn(0f, 1f)
            val r = ((apLeft + apW - imgLeft) / imgW).coerceIn(0f, 1f)
            val b = ((apTop + apH - imgTop) / imgH).coerceIn(0f, 1f)
            onCrop(NormCrop(l, t, r, b))
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cropAspect, imageAspect, vw, vh) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                        offset = clampOffset(offset + pan, scale)
                    }
                },
        ) {
            val imgW = coverW * scale
            val imgH = coverH * scale
            val imgLeft = vw / 2f - imgW / 2f + offset.x
            val imgTop = vh / 2f - imgH / 2f + offset.y

            image?.let {
                drawImage(
                    image = it,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(it.width, it.height),
                    dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
                    dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt()),
                )
            }

            // Dim everything outside the aperture.
            val scrim = Color.Black.copy(alpha = 0.5f)
            drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(vw, apTop))
            drawRect(scrim, topLeft = Offset(0f, apTop + apH), size = Size(vw, vh - (apTop + apH)))
            drawRect(scrim, topLeft = Offset(0f, apTop), size = Size(apLeft, apH))
            drawRect(scrim, topLeft = Offset(apLeft + apW, apTop), size = Size(vw - (apLeft + apW), apH))

            // Aperture border + rule-of-thirds guides.
            drawRect(Color.White, topLeft = Offset(apLeft, apTop), size = Size(apW, apH), style = Stroke(2f))
            for (i in 1..2) {
                val x = apLeft + apW * i / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(x, apTop), Offset(x, apTop + apH))
                val y = apTop + apH * i / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(apLeft, y), Offset(apLeft + apW, y))
            }
        }

        if (busy) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
        }
    }
}
