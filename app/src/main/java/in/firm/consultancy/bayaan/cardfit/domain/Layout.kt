package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode

/**
 * Layout calculation from CLAUDE.md section 6.
 *
 * Coordinate space: millimetres, origin at the page's TOP-LEFT, x grows right, y grows down.
 * Renderers convert mm -> points (PDF) or mm -> px (raster) via [Units]; both Android Canvas and
 * PdfDocument use a top-left origin, so the mapping is direct.
 */

/** A rectangle in page-millimetre space (top-left origin). */
data class LayoutRect(
    val xMm: Double,
    val yMm: Double,
    val widthMm: Double,
    val heightMm: Double,
)

/**
 * Intrinsic shape of one scanned side after crop. [aspectRatio] = imageWidth / imageHeight.
 * Used by FIT_WIDTH and FIT_PAGE. Ignored by ACTUAL_SIZE (which draws at fixed physical size and
 * expects the source image to have been centre-cropped to the card's aspect ratio).
 */
data class CardImage(val aspectRatio: Double) {
    init {
        require(aspectRatio > 0.0) { "aspectRatio must be > 0" }
    }
}

/**
 * Inputs for a single page layout.
 *
 * @param cards the sides to place, in order (e.g. [front] or [front, back]); 1 or 2 entries.
 * @param cardWidthMm / cardHeightMm required for [FitMode.ACTUAL_SIZE] (the exact physical size).
 */
data class LayoutInput(
    val fitMode: FitMode,
    val pageWidthMm: Double,
    val pageHeightMm: Double,
    val cards: List<CardImage>,
    val cardWidthMm: Double? = null,
    val cardHeightMm: Double? = null,
    val gapMm: Double = Defaults.CARD_GAP_MM,
    val marginMm: Double = Defaults.UPLOAD_MARGIN_MM,
    // Phase 12: exact per-side sizes (mm) for ACTUAL_SIZE — lets front/back differ by orientation.
    // When null, [cardWidthMm]/[cardHeightMm] are used for every side.
    val perSideSizesMm: List<Pair<Double, Double>>? = null,
    // Phase 12: for ACTUAL_SIZE, crop the canvas tightly to the content (upload) instead of
    // centring on the full page (print).
    val cropToContent: Boolean = false,
)

/**
 * Result of a layout. For [FitMode.FIT_WIDTH] the page is cropped to content, so [pageHeightMm]
 * may differ from the input page height; for the other modes it equals the input page size.
 */
data class PageLayout(
    val pageWidthMm: Double,
    val pageHeightMm: Double,
    val cards: List<LayoutRect>,
)

object LayoutCalculator {

    fun calculate(input: LayoutInput): PageLayout {
        require(input.cards.size in 1..2) { "Layout supports 1 or 2 sides, got ${input.cards.size}" }
        return when (input.fitMode) {
            FitMode.ACTUAL_SIZE -> actualSize(input)
            FitMode.FIT_WIDTH -> fitWidth(input)
            FitMode.FIT_PAGE -> fitPage(input)
        }
    }

    /**
     * Each card drawn at its exact physical mm size (per-side sizes when given, e.g. CR-80 fronts and
     * backs captured in different orientations). Sides are centred horizontally and stacked with a
     * gap. With [LayoutInput.cropToContent] the canvas is cropped tightly to the stack + margins
     * (upload); otherwise the stack is centred vertically on the full page (print).
     */
    private fun actualSize(input: LayoutInput): PageLayout {
        val n = input.cards.size
        val sizes: List<Pair<Double, Double>> = input.perSideSizesMm ?: run {
            val w = requireNotNull(input.cardWidthMm) { "ACTUAL_SIZE requires cardWidthMm" }
            val h = requireNotNull(input.cardHeightMm) { "ACTUAL_SIZE requires cardHeightMm" }
            List(n) { w to h }
        }
        require(sizes.size == n) { "perSideSizesMm must have one entry per side" }

        val stackHeight = sizes.sumOf { it.second } + (n - 1) * input.gapMm
        val maxWidth = sizes.maxOf { it.first }

        return if (input.cropToContent) {
            val canvasWidth = maxWidth + 2 * input.marginMm
            val canvasHeight = stackHeight + 2 * input.marginMm
            var y = input.marginMm
            val rects = sizes.map { (w, h) ->
                val x = input.marginMm + (maxWidth - w) / 2.0
                LayoutRect(x, y, w, h).also { y += h + input.gapMm }
            }
            PageLayout(canvasWidth, canvasHeight, rects)
        } else {
            var y = (input.pageHeightMm - stackHeight) / 2.0
            val rects = sizes.map { (w, h) ->
                val x = (input.pageWidthMm - w) / 2.0
                LayoutRect(x, y, w, h).also { y += h + input.gapMm }
            }
            PageLayout(input.pageWidthMm, input.pageHeightMm, rects)
        }
    }

    /**
     * Each card scaled to (page width - 2*margin), aspect preserved; stacked with a gap; canvas
     * height cropped to content (+ margins). Page width stays the paper width.
     */
    private fun fitWidth(input: LayoutInput): PageLayout {
        val contentW = input.pageWidthMm - 2 * input.marginMm
        val heights = input.cards.map { contentW / it.aspectRatio }
        val n = input.cards.size
        val contentHeight = heights.sum() + (n - 1) * input.gapMm
        val pageHeight = contentHeight + 2 * input.marginMm

        val rects = ArrayList<LayoutRect>(n)
        var y = input.marginMm
        for (i in 0 until n) {
            rects.add(LayoutRect(input.marginMm, y, contentW, heights[i]))
            y += heights[i] + input.gapMm
        }
        return PageLayout(input.pageWidthMm, pageHeight, rects)
    }

    /**
     * Fit-to-AREA (Phase 12, print NON_STANDARD): scale the ENTIRE vertical stack — every side at its
     * own aspect ratio, plus the gaps — to fit the printable area (page minus margins), then centre
     * on the full page. The reference stack fills the usable width; the uniform scale is
     * `min(usableWidth / stackWidth, usableHeight / stackHeight)` (here stackWidth == usableWidth, so
     * the scale only shrinks when the stack would be too tall). This keeps portrait cards from
     * overflowing the page, unlike a plain fit-to-width.
     */
    private fun fitPage(input: LayoutInput): PageLayout {
        val availW = input.pageWidthMm - 2 * input.marginMm
        val availH = input.pageHeightMm - 2 * input.marginMm
        val n = input.cards.size

        // Reference layout: each side fills the usable width.
        val refHeights = input.cards.map { availW / it.aspectRatio }
        val gapsTotal = (n - 1) * input.gapMm
        val refStackHeight = refHeights.sum() + gapsTotal

        // Uniform scale of the whole stack (cards + gaps); never upscale beyond full width.
        val scale = minOf(1.0, availH / refStackHeight)
        val scaledWidth = availW * scale
        val scaledHeights = refHeights.map { it * scale }
        val scaledGap = input.gapMm * scale

        val stackHeight = scaledHeights.sum() + (n - 1) * scaledGap
        var y = (input.pageHeightMm - stackHeight) / 2.0
        val x = (input.pageWidthMm - scaledWidth) / 2.0
        val rects = scaledHeights.map { h ->
            LayoutRect(x, y, scaledWidth, h).also { y += h + scaledGap }
        }
        return PageLayout(input.pageWidthMm, input.pageHeightMm, rects)
    }
}
