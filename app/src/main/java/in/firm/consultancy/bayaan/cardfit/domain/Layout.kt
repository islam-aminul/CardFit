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
     * Each card drawn at its exact physical mm size; centred horizontally; the whole stack centred
     * vertically on the full page.
     */
    private fun actualSize(input: LayoutInput): PageLayout {
        val w = requireNotNull(input.cardWidthMm) { "ACTUAL_SIZE requires cardWidthMm" }
        val h = requireNotNull(input.cardHeightMm) { "ACTUAL_SIZE requires cardHeightMm" }
        val n = input.cards.size
        val stackHeight = n * h + (n - 1) * input.gapMm
        val topY = (input.pageHeightMm - stackHeight) / 2.0
        val x = (input.pageWidthMm - w) / 2.0

        val rects = (0 until n).map { i ->
            LayoutRect(x, topY + i * (h + input.gapMm), w, h)
        }
        return PageLayout(input.pageWidthMm, input.pageHeightMm, rects)
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
     * Scale the image(s) to fit within the page minus margins, aspect preserved, and centre.
     * Generalised to 1 or 2 cards: width-fit each card, then if the stack is taller than the
     * available height, scale every card down uniformly so the stack (cards + gaps) fits exactly.
     */
    private fun fitPage(input: LayoutInput): PageLayout {
        val availW = input.pageWidthMm - 2 * input.marginMm
        val availH = input.pageHeightMm - 2 * input.marginMm
        val n = input.cards.size

        var widths = input.cards.map { availW }
        var heights = input.cards.map { availW / it.aspectRatio }

        val gapsTotal = (n - 1) * input.gapMm
        val cardsHeight = heights.sum()
        val availForCards = availH - gapsTotal
        if (cardsHeight > availForCards) {
            val scale = availForCards / cardsHeight
            widths = widths.map { it * scale }
            heights = heights.map { it * scale }
        }

        val stackHeight = heights.sum() + gapsTotal
        var y = (input.pageHeightMm - stackHeight) / 2.0
        val rects = ArrayList<LayoutRect>(n)
        for (i in 0 until n) {
            val x = (input.pageWidthMm - widths[i]) / 2.0
            rects.add(LayoutRect(x, y, widths[i], heights[i]))
            y += heights[i] + input.gapMm
        }
        return PageLayout(input.pageWidthMm, input.pageHeightMm, rects)
    }
}
