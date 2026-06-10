package `in`.firm.consultancy.bayaan.cardfit.domain

import kotlin.math.roundToInt

/**
 * Aspect-locked crop maths for the photo editor (CLAUDE.md Phase 13). Pure Kotlin (pixel space) so
 * it is JVM-testable; the editor applies the returned rectangle to the working bitmap.
 *
 * A crop rectangle in source-image pixels (top-left origin). Always within the source bounds.
 */
data class CropRect(val xPx: Int, val yPx: Int, val widthPx: Int, val heightPx: Int)

/**
 * The largest centred crop of a [srcWidthPx] × [srcHeightPx] image whose aspect ratio equals
 * [aspectWidth] : [aspectHeight] (e.g. 35 : 45 for an India passport photo). Crops the minimal amount
 * — shaves width when the source is too wide, height when it is too tall — and never upscales or
 * stretches. Used to lock free crops to the selected photo size's aspect ratio.
 */
fun aspectCrop(
    srcWidthPx: Int,
    srcHeightPx: Int,
    aspectWidth: Double,
    aspectHeight: Double,
): CropRect {
    require(srcWidthPx > 0 && srcHeightPx > 0) { "source dimensions must be positive" }
    require(aspectWidth > 0 && aspectHeight > 0) { "aspect must be positive" }

    val targetAspect = aspectWidth / aspectHeight
    val srcAspect = srcWidthPx.toDouble() / srcHeightPx.toDouble()
    return if (srcAspect > targetAspect) {
        // Source too wide: keep full height, shave the sides.
        val w = (srcHeightPx * targetAspect).roundToInt().coerceIn(1, srcWidthPx)
        val x = ((srcWidthPx - w) / 2.0).roundToInt().coerceIn(0, srcWidthPx - w)
        CropRect(x, 0, w, srcHeightPx)
    } else {
        // Source too tall (or exact): keep full width, shave top/bottom.
        val h = (srcWidthPx / targetAspect).roundToInt().coerceIn(1, srcHeightPx)
        val y = ((srcHeightPx - h) / 2.0).roundToInt().coerceIn(0, srcHeightPx - h)
        CropRect(0, y, srcWidthPx, h)
    }
}
