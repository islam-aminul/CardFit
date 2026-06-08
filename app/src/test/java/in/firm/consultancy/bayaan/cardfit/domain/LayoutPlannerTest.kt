package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutPlannerTest {

    private val cr80 = 85.6 / 54.0

    @Test
    fun panPrint_isActualSize_withCardDims_andPaperPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            frontAspect = cr80,
            backAspect = cr80,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(85.6, input.cardWidthMm)
        assertEquals(54.0, input.cardHeightMm)
        assertEquals(210.0, input.pageWidthMm, 0.0)
        assertEquals(297.0, input.pageHeightMm, 0.0)
        assertEquals(2, input.cards.size)
    }

    @Test
    fun panUpload_switchesToFitWidth_andDropsFixedDims() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.UPLOAD,
            paper = PaperSize.A4,
            frontAspect = cr80,
            backAspect = null,
        )
        assertEquals(FitMode.FIT_WIDTH, input.fitMode)
        assertNull(input.cardWidthMm)
        assertNull(input.cardHeightMm)
        assertEquals(1, input.cards.size)
    }

    @Test
    fun customPrint_usesRuntimeDimensions() {
        val input = LayoutPlanner.plan(
            cardType = CardType.CUSTOM,
            mode = OutputMode.PRINT,
            paper = PaperSize.A5,
            frontAspect = 1.5,
            backAspect = null,
            customWidthMm = 100.0,
            customHeightMm = 70.0,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(100.0, input.cardWidthMm)
        assertEquals(70.0, input.cardHeightMm)
        assertEquals(148.0, input.pageWidthMm, 0.0)
    }

    @Test
    fun admitCardPrint_isFitPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.ADMIT_CARD,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            frontAspect = 0.7,
            backAspect = null,
        )
        assertEquals(FitMode.FIT_PAGE, input.fitMode)
        assertNull(input.cardWidthMm)
    }

    @Test
    fun freePrint_isFitWidth() {
        val input = LayoutPlanner.plan(
            cardType = CardType.FREE,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            frontAspect = 1.6,
            backAspect = null,
        )
        assertEquals(FitMode.FIT_WIDTH, input.fitMode)
    }

    @Test
    fun plannedInput_feedsLayoutCalculator() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            frontAspect = cr80,
            backAspect = cr80,
        )
        val layout = LayoutCalculator.calculate(input)
        assertEquals(2, layout.cards.size)
        // Two CR-80 cards centered on A4 (matches LayoutTest expectations).
        assertEquals(62.2, layout.cards[0].xMm, 0.001)
        assertEquals(90.5, layout.cards[0].yMm, 0.001)
    }
}
