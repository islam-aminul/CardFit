package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoGrid
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.BorderSubtle
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight100
import kotlin.math.roundToInt

/**
 * The sheet the user is about to print, drawn at the paper's true proportions with every photo in
 * its real position. Previously the photo flow offered paper, copies and cut-mark controls for an
 * output that was never shown at all.
 *
 * Positions come from [PhotoGrid.cells] — the same call `PhotoRenderers.renderPrintPdf` makes — so
 * the preview and the exported PDF cannot drift apart. Millimetres map to pixels by a single
 * `canvasWidth / paperWidthMm` factor.
 */
@Composable
fun PhotoSheetPreview(
    grid: PhotoGrid,
    count: Int,
    cutMarks: Boolean,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val usedRows = if (grid.perRow <= 0) 0 else (count + grid.perRow - 1) / grid.perRow
    val cells = grid.cells(usedRows).take(count)

    SheetSurface(
        aspect = (grid.paperWidthMm / grid.paperHeightMm).toFloat(),
        modifier = modifier,
    ) {
        val scale = size.width / grid.paperWidthMm.toFloat()
        val cellW = grid.photoWidthMm.toFloat() * scale
        val cellH = grid.photoHeightMm.toFloat() * scale
        cells.forEach { cell ->
            drawPhotoCell(
                photo = photo,
                left = cell.xMm.toFloat() * scale,
                top = cell.yMm.toFloat() * scale,
                width = cellW,
                height = cellH,
                cutMarks = cutMarks,
            )
        }
    }
}

/**
 * The upload counterpart: one photo at its true aspect, since an upload export is a single image
 * rather than a sheet.
 */
@Composable
fun SinglePhotoPreview(
    widthMm: Double,
    heightMm: Double,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    if (widthMm <= 0 || heightMm <= 0) return
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio((widthMm / heightMm).toFloat())
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp)),
        ) {
            drawPhotoCell(photo, 0f, 0f, size.width, size.height, cutMarks = false)
        }
    }
}

/** A white page of the given [aspect] (width / height), centred with a subtle border. */
@Composable
private fun SheetSurface(
    aspect: Float,
    modifier: Modifier = Modifier,
    content: DrawScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp)),
            onDraw = content,
        )
    }
}

/**
 * One photo cell, centre-cropped to the cell's aspect exactly as `centerCropSrcRect` does in the
 * renderer, so what is previewed is what is printed. Falls back to a flat placeholder while the
 * edited bitmap is still being produced.
 */
private fun DrawScope.drawPhotoCell(
    photo: ImageBitmap?,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cutMarks: Boolean,
) {
    if (width <= 0f || height <= 0f) return
    if (photo != null) {
        val targetAspect = width / height
        val photoAspect = photo.width.toFloat() / photo.height.toFloat()
        val srcW: Float
        val srcH: Float
        if (photoAspect > targetAspect) {
            srcH = photo.height.toFloat()
            srcW = srcH * targetAspect
        } else {
            srcW = photo.width.toFloat()
            srcH = srcW / targetAspect
        }
        drawImage(
            image = photo,
            srcOffset = IntOffset(
                ((photo.width - srcW) / 2f).roundToInt().coerceAtLeast(0),
                ((photo.height - srcH) / 2f).roundToInt().coerceAtLeast(0),
            ),
            srcSize = IntSize(
                srcW.roundToInt().coerceIn(1, photo.width),
                srcH.roundToInt().coerceIn(1, photo.height),
            ),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
        )
    } else {
        drawRect(Midnight100, topLeft = Offset(left, top), size = Size(width, height))
    }
    // Matches the renderer's light-gray cut guide.
    if (cutMarks) {
        drawRect(
            color = Color(0xFFC8C8C8),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 1f),
        )
    }
}
