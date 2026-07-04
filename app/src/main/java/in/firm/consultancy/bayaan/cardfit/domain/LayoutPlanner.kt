package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize

/** One scanned side's pixel dimensions, used to derive its aspect ratio and orientation. */
data class SideInfo(val widthPx: Int, val heightPx: Int) {
    val aspectRatio: Double get() = widthPx.toDouble() / heightPx.toDouble()
    val orientation: Orientation
        get() = if (widthPx >= heightPx) Orientation.LANDSCAPE else Orientation.PORTRAIT
}

/**
 * Builds a [LayoutInput] from a session's sides and the resolved sizing (CLAUDE.md Phase 12). Pure —
 * pixel dimensions are passed in, so this stays JVM-testable.
 *
 * Sizing (always a vertical stack, centred horizontally):
 *  - CR-80 (detected or forced): each side at exactly 85.6 x 54 mm in ITS OWN orientation;
 *    print centres the stack on the full page, upload crops the canvas to content.
 *  - CUSTOM: each side at the manual mm size; print full page, upload crop-to-content.
 *  - NON_STANDARD upload: fit-to-width, crop the canvas to content.
 *  - NON_STANDARD print: fit-to-AREA — scale the whole stack to the printable area, then centre.
 *  - RECEIPT: the user's [contentScalePercent] shrinks the content on the page —
 *    fit-to-width at that fraction for upload, fit-to-AREA times that fraction for print.
 *
 * [pageOrientation] LANDSCAPE swaps the paper's width and height (full-document rotation).
 */
object LayoutPlanner {

    fun plan(
        cardType: CardType,
        mode: OutputMode,
        paper: PaperSize,
        sides: List<SideInfo>,
        sizeOverride: SizeOverride,
        customWidthMm: Double? = null,
        customHeightMm: Double? = null,
        pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
        contentScalePercent: Int = 100,
    ): LayoutInput {
        require(sides.isNotEmpty()) { "at least one side required" }
        val front = sides.first()
        val sizingMode = CardClassifier.resolveSizingMode(
            cardType = cardType,
            frontWidthPx = front.widthPx,
            frontHeightPx = front.heightPx,
            override = sizeOverride,
        )
        val cards = sides.map { CardImage(it.aspectRatio) }
        val isUpload = mode == OutputMode.UPLOAD
        val landscape = pageOrientation == PageOrientation.LANDSCAPE
        val pageW = if (landscape) paper.heightMm else paper.widthMm
        val pageH = if (landscape) paper.widthMm else paper.heightMm

        return when (sizingMode) {
            SizingMode.CR80 -> LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = pageW,
                pageHeightMm = pageH,
                cards = cards,
                perSideSizesMm = sides.map { CardClassifier.cr80SizeMm(it.orientation) },
                cropToContent = isUpload,
            )

            SizingMode.CUSTOM -> {
                val w = customWidthMm ?: CardClassifier.CR80_LONG_MM
                val h = customHeightMm ?: CardClassifier.CR80_SHORT_MM
                LayoutInput(
                    fitMode = FitMode.ACTUAL_SIZE,
                    pageWidthMm = pageW,
                    pageHeightMm = pageH,
                    cards = cards,
                    perSideSizesMm = sides.map { w to h },
                    cropToContent = isUpload,
                )
            }

            SizingMode.NON_STANDARD -> LayoutInput(
                fitMode = if (isUpload) FitMode.FIT_WIDTH else FitMode.FIT_PAGE,
                pageWidthMm = pageW,
                pageHeightMm = pageH,
                cards = cards,
                // Only the small-document type exposes the size control; others stay at 1.0.
                widthScale = if (cardType == CardType.RECEIPT) {
                    contentScalePercent.coerceIn(MIN_CONTENT_SCALE_PERCENT, 100) / 100.0
                } else {
                    1.0
                },
            )
        }
    }

    /** Lower bound of the small-document size control (percent of the maximum content size). */
    const val MIN_CONTENT_SCALE_PERCENT = 25
}
