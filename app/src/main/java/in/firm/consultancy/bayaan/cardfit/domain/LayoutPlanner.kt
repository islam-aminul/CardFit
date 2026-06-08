package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize

/**
 * Builds a [LayoutInput] for the renderers (pure; aspect ratios are passed in as numbers so this
 * stays JVM-testable).
 *
 * Fit-mode selection (CLAUDE.md sections 6 and 8):
 *  - UPLOAD output always uses FIT_WIDTH (a compact, full-width image for portal upload).
 *  - PRINT output uses the card's natural fit mode: ACTUAL_SIZE (PAN/Aadhaar/EPIC/Custom),
 *    FIT_PAGE (admit card), or FIT_WIDTH (free).
 */
object LayoutPlanner {

    fun plan(
        cardType: CardType,
        mode: OutputMode,
        paper: PaperSize,
        frontAspect: Double,
        backAspect: Double?,
        customWidthMm: Double? = null,
        customHeightMm: Double? = null,
    ): LayoutInput {
        val fitMode = if (mode == OutputMode.UPLOAD) FitMode.FIT_WIDTH else cardType.fitMode

        val cards = buildList {
            add(CardImage(frontAspect))
            if (backAspect != null) add(CardImage(backAspect))
        }

        val isActual = fitMode == FitMode.ACTUAL_SIZE
        val cardWidthMm = if (isActual) (cardType.widthMm ?: customWidthMm) else null
        val cardHeightMm = if (isActual) (cardType.heightMm ?: customHeightMm) else null

        return LayoutInput(
            fitMode = fitMode,
            pageWidthMm = paper.widthMm,
            pageHeightMm = paper.heightMm,
            cards = cards,
            cardWidthMm = cardWidthMm,
            cardHeightMm = cardHeightMm,
        )
    }
}
