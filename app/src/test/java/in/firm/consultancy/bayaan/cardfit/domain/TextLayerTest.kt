package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TextLayerTest {

    private val tol = 1e-6

    private fun assertRect(expected: PtRect, actual: PtRect) {
        assertEquals("left", expected.left, actual.left, tol)
        assertEquals("top", expected.top, actual.top, tol)
        assertEquals("right", expected.right, actual.right, tol)
        assertEquals("bottom", expected.bottom, actual.bottom, tol)
    }

    // ---- No crop needed (image aspect == side rect aspect), as in FIT_WIDTH/FIT_PAGE ----

    @Test
    fun noCrop_fullImageBoxMapsToFullRect() {
        // image 1000x600 (aspect 1.6667), side rect 200x120 (aspect 1.6667)
        val side = PtRect(0.0, 0.0, 200.0, 120.0)
        val mapped = mapImageRectToPage(BoxPx(0, 0, 1000, 600), 1000, 600, side)
        assertRect(PtRect(0.0, 0.0, 200.0, 120.0), mapped)
    }

    @Test
    fun noCrop_cornersMapToCorners() {
        val side = PtRect(10.0, 20.0, 210.0, 140.0) // 200x120
        // top-left 1px box
        assertRect(PtRect(10.0, 20.0, 10.2, 20.2), mapImageRectToPage(BoxPx(0, 0, 1, 1), 1000, 600, side))
        // bottom-right 1px box (999..1000, 599..600)
        assertRect(
            PtRect(209.8, 139.8, 210.0, 140.0),
            mapImageRectToPage(BoxPx(999, 599, 1000, 600), 1000, 600, side),
        )
    }

    @Test
    fun noCrop_centerMapsToCenter() {
        val side = PtRect(0.0, 0.0, 200.0, 120.0)
        val mapped = mapImageRectToPage(BoxPx(500, 300, 500, 300), 1000, 600, side)
        assertEquals(100.0, mapped.left, tol) // center x
        assertEquals(60.0, mapped.top, tol) // center y
    }

    // ---- Center-crop needed (ACTUAL_SIZE: square image into a 2:1 rect) ----

    @Test
    fun crop_centerMapsToRectCenter() {
        // image 1000x1000 (aspect 1.0) into side rect 100x50 (aspect 2.0) -> crop height
        val side = PtRect(10.0, 20.0, 110.0, 70.0)
        val mapped = mapImageRectToPage(BoxPx(500, 500, 500, 500), 1000, 1000, side)
        assertEquals(60.0, mapped.left, tol) // (10+110)/2
        assertEquals(45.0, mapped.top, tol) // (20+70)/2
    }

    @Test
    fun crop_cropRegionFillsRect() {
        // The retained (cropped) region is x:0..1000, y:250..750; it should fill the side rect.
        val side = PtRect(10.0, 20.0, 110.0, 70.0)
        val mapped = mapImageRectToPage(BoxPx(0, 250, 1000, 750), 1000, 1000, side)
        assertRect(PtRect(10.0, 20.0, 110.0, 70.0), mapped)
    }

    @Test
    fun crop_horizontalCropForWideImage() {
        // image 1200x600 (aspect 2.0) into side rect 100x100 (aspect 1.0) -> crop width
        // retained region x:300..900, y:0..600 fills the rect
        val side = PtRect(0.0, 0.0, 100.0, 100.0)
        val mapped = mapImageRectToPage(BoxPx(300, 0, 900, 600), 1200, 600, side)
        assertRect(PtRect(0.0, 0.0, 100.0, 100.0), mapped)
        // center stays center
        val c = mapImageRectToPage(BoxPx(600, 300, 600, 300), 1200, 600, side)
        assertEquals(50.0, c.left, tol)
        assertEquals(50.0, c.top, tol)
    }

    // ---- Filter hook ----

    @Test
    fun passThroughFilter_returnsSameElements() {
        val elements = listOf(OcrElement("JOHN", BoxPx(0, 0, 10, 5)), OcrElement("DOE", BoxPx(12, 0, 20, 5)))
        val out = PassThroughTextFilter.filter(elements, CardType.PAN)
        assertSame(elements, out)
    }

    @Test
    fun customFilter_canExcludeElements() {
        // Demonstrates the masking hook: a filter that drops digit-bearing tokens.
        val masking = TextLayerFilter { els, _ -> els.filter { e -> e.text.none { it.isDigit() } } }
        val elements = listOf(
            OcrElement("JOHN", BoxPx(0, 0, 10, 5)),
            OcrElement("1234", BoxPx(0, 6, 10, 11)),
        )
        val out = masking.filter(elements, CardType.AADHAAR)
        assertEquals(1, out.size)
        assertEquals("JOHN", out[0].text)
    }
}
