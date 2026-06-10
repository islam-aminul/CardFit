package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
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
    fun epicAutomatic_cr80Capture_isActualSize() {
        val input = LayoutPlanner.plan(
            cardType = CardType.EPIC,
            mode = OutputMode.PRINT,
            paper = PaperSize.A4,
            sides = listOf(cr80Landscape),
            sizeOverride = SizeOverride.AUTOMATIC,
        )
        assertEquals(FitMode.ACTUAL_SIZE, input.fitMode)
        assertEquals(listOf(85.6 to 54.0), input.perSideSizesMm)
    }

    @Test
    fun epicAutomatic_paperCapture_print_isFitPage() {
        val input = LayoutPlanner.plan(
            cardType = CardType.EPIC,
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
            cardType = CardType.EPIC,
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
        assertEquals(90.5, layout.cards[0].yMm, 0.001)
    }
}
