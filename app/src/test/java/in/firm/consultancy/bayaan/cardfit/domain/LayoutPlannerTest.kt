package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutPlannerTest {

    private val cr80Landscape = SideInfo(856, 540) // ratio 1.585 -> CR-80 landscape
    private val cr80Portrait = SideInfo(540, 856) // CR-80 portrait
    private val a4Portrait = SideInfo(2100, 2970) // ratio 1.414 -> non-standard portrait

    @Test
    fun cr80Print_isActualSize_perSideExactSize_fullPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape, cr80Landscape),
            sizeOverride = SizeOverride.FORCE_CR80,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(listOf(85.6 to 54.0, 85.6 to 54.0), input.perSideSizesMm)
        assertEquals(false, input.cropToContent) // print -> full page
        assertEquals(210.0, input.pageWidthMm, 0.0)
    }

    @Test
    fun cr80Upload_cropsToContent() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.UPLOAD,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape),
            sizeOverride = SizeOverride.FORCE_CR80,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(true, input.cropToContent)
    }

    @Test
    fun cr80_perSideOrientation_respected() {
        val input = LayoutPlanner.plan(
            cardType = CardType.AADHAAR,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape, cr80Portrait),
            sizeOverride = SizeOverride.FORCE_CR80,
        )
        // Front landscape 85.6x54, back portrait 54x85.6.
        assertEquals(listOf(85.6 to 54.0, 54.0 to 85.6), input.perSideSizesMm)
    }

    @Test
    fun voterIdAutomatic_cr80Capture_isActualSize() {
        val input = LayoutPlanner.plan(
            cardType = CardType.VOTER_ID,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape),
            sizeOverride = SizeOverride.AUTOMATIC,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(listOf(85.6 to 54.0), input.perSideSizesMm)
    }

    @Test
    fun voterIdAutomatic_paperCapture_print_isFitPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.VOTER_ID,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
        )
        assertEquals(FitMode.FIT_PAGE, input.fitMode) // fit-to-area for non-standard print
        assertEquals(null, input.perSideSizesMm)
    }

    @Test
    fun nonStandardUpload_isFitWidth() {
        val input = LayoutPlanner.plan(
            cardType = CardType.VOTER_ID,
            mode = OutputMode.UPLOAD,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
        )
        assertEquals(FitMode.FIT_WIDTH, input.fitMode)
    }

    @Test
    fun customOverride_usesRuntimeDimensions() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.PRINT,
            paper = PaperSize.A5,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.CUSTOM,
            customWidthMm = 100.0,
            customHeightMm = 70.0,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(listOf(100.0 to 70.0), input.perSideSizesMm)
        assertEquals(148.0, input.pageWidthMm, 0.0)
    }

    @Test
    fun landscapeOrientation_swapsPageDimensions() {
        val input = LayoutPlanner.plan(
            cardType = CardType.FULL_PAGE_DOCUMENT,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
            pageOrientation = PageOrientation.LANDSCAPE,
        )
        assertEquals(297.0, input.pageWidthMm, 0.0)
        assertEquals(210.0, input.pageHeightMm, 0.0)
        assertEquals(FitMode.FIT_PAGE, input.fitMode)
    }

    @Test
    fun smallDocument_appliesContentScale_othersDoNot() {
        val free = LayoutPlanner.plan(
            cardType = CardType.RECEIPT,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
            contentScalePercent = 60,
        )
        assertEquals(0.6, free.widthScale, 0.0)

        val admit = LayoutPlanner.plan(
            cardType = CardType.FULL_PAGE_DOCUMENT,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
            contentScalePercent = 60, // ignored: size control is FREE-only
        )
        assertEquals(1.0, admit.widthScale, 0.0)
    }

    @Test
    fun smallDocumentContentScale_clampedToLowerBound() {
        val input = LayoutPlanner.plan(
            cardType = CardType.RECEIPT,
            mode = OutputMode.UPLOAD,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait),
            sizeOverride = SizeOverride.AUTOMATIC,
            contentScalePercent = 5,
        )
        assertEquals(LayoutPlanner.MIN_CONTENT_SCALE_PERCENT / 100.0, input.widthScale, 0.0)
    }

    @Test
    fun smallDocumentScaledPrint_neverOverflowsPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.RECEIPT,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(a4Portrait, a4Portrait), // two tall sides: auto-fit must shrink first
            sizeOverride = SizeOverride.AUTOMATIC,
            contentScalePercent = 100,
        )
        val layout = LayoutCalculator.calculate(input)
        assertEquals(297.0, layout.pageHeightMm, 0.0)
        val bottom = layout.cards.maxOf { it.yMm + it.heightMm }
        assertTrue("content overflows the page", bottom <= 297.0 + 0.0001)
    }

    @Test
    fun plannedInput_feedsLayoutCalculator_cr80CenteredOnA4() {
        val input = LayoutPlanner.plan(
            cardType = CardType.PAN,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape, cr80Landscape),
            sizeOverride = SizeOverride.FORCE_CR80,
        )
        val layout = LayoutCalculator.calculate(input)
        assertEquals(2, layout.cards.size)
        assertEquals(62.2, layout.cards[0].xMm, 0.001)
        assertEquals(89.5, layout.cards[0].yMm, 0.001)
    }
}
