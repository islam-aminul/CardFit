package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType

/**
 * Searchable-PDF text-layer model and geometry (CLAUDE.md Phase 11). Pure Kotlin (no Android), so the
 * coordinate mapping is unit-testable on the JVM. The renderer converts [PtRect] to `android.graphics`
 * types at the boundary.
 */

/** A bounding box in source-image pixels (top-left origin), mirroring an OCR element's bounds. */
data class BoxPx(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** One recognized token (word/element) plus its pixel bounding box in the source image. */
data class OcrElement(val text: String, val box: BoxPx)

/**
 * OCR result for a single placed side: the recognized elements with boxes, plus the pixel dimensions
 * of the image those boxes are expressed in. [imageWidthPx]/[imageHeightPx] are the upright,
 * rotation-corrected dimensions (the scanner emits upright pages, so no extra rotation is applied).
 */
data class OcrTextLayer(
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val elements: List<OcrElement>,
)

/** A rectangle in PDF points (top-left origin), produced by [mapImageRectToPage]. */
data class PtRect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width: Double get() = right - left
    val height: Double get() = bottom - top
}

/**
 * Map an image-pixel box to PDF points using the SAME placement transform the renderer uses to draw
 * that side's image into [sideRect] (CLAUDE.md Phase 11 item 3): the image is center-cropped to the
 * side rect's aspect ratio (matching `centerCropSrcRect`) and then scaled to fill the rect. So the
 * text aligns with the visible picture, and each side uses its own rect.
 *
 * Rotation: the pipeline OCRs the same upright image the renderer draws, so boxes are already in the
 * drawn pixel space — no extra rotation term is needed here.
 */
fun mapImageRectToPage(
    box: BoxPx,
    imageWidthPx: Int,
    imageHeightPx: Int,
    sideRect: PtRect,
): PtRect {
    require(imageWidthPx > 0 && imageHeightPx > 0) { "image dimensions must be positive" }

    val dstAspect = sideRect.width / sideRect.height
    val srcAspect = imageWidthPx.toDouble() / imageHeightPx.toDouble()

    // Replicate the renderer's center-crop of the source image to the destination aspect ratio.
    val cropX: Double
    val cropY: Double
    val cropW: Double
    val cropH: Double
    if (srcAspect > dstAspect) {
        cropW = imageHeightPx * dstAspect
        cropH = imageHeightPx.toDouble()
        cropX = (imageWidthPx - cropW) / 2.0
        cropY = 0.0
    } else {
        cropW = imageWidthPx.toDouble()
        cropH = imageWidthPx / dstAspect
        cropX = 0.0
        cropY = (imageHeightPx - cropH) / 2.0
    }

    fun mapX(px: Double): Double = sideRect.left + (px - cropX) / cropW * sideRect.width
    fun mapY(py: Double): Double = sideRect.top + (py - cropY) / cropH * sideRect.height

    return PtRect(
        left = mapX(box.left.toDouble()),
        top = mapY(box.top.toDouble()),
        right = mapX(box.right.toDouble()),
        bottom = mapY(box.bottom.toDouble()),
    )
}

/**
 * Hook for filtering OCR elements before they are written into the PDF text layer. Raw OCR is never
 * passed straight through — it always goes through a filter, so a future Aadhaar-masking feature can
 * exclude masked content (e.g. the ID number/DOB) from the searchable layer.
 */
fun interface TextLayerFilter {
    fun filter(elements: List<OcrElement>, cardType: CardType): List<OcrElement>
}

/** Default filter: emit everything (no masking yet). */
object PassThroughTextFilter : TextLayerFilter {
    override fun filter(elements: List<OcrElement>, cardType: CardType): List<OcrElement> = elements
}
