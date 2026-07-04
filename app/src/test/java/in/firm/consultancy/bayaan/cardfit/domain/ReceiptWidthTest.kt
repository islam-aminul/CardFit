package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptWidthTest {

    @Test
    fun realSize_heightFollowsScanAspect() {
        // A tall 57 mm bill scanned at 800×2400 px (aspect 1:3) → height ≈ 171 mm.
        val (w, h) = receiptRealSizeMm(57.0, 800, 2400)
        assertEquals(57.0, w, 0.0)
        assertEquals(171.0, h, 0.001)
    }

    @Test
    fun realSize_zeroPixels_fallsBackToSquare() {
        val (w, h) = receiptRealSizeMm(57.0, 0, 0)
        assertEquals(57.0, w, 0.0)
        assertEquals(57.0, h, 0.0) // aspect defaults to 1.0
    }

    @Test
    fun fitsPaper_truthTable() {
        // 57 × 171 mm fits within A4's printable area but not A5 (A5 usable height 210-12=198 → fits;
        // use a taller bill to exceed A5). A 57 mm bill at aspect 1:4 = 228 mm tall exceeds A5 (210).
        assertTrue(receiptFitsPaper(57.0, 171.0, PaperSize.A4))
        assertTrue(receiptFitsPaper(57.0, 171.0, PaperSize.A5))
        assertFalse(receiptFitsPaper(57.0, 228.0, PaperSize.A5)) // taller than A5
        assertTrue(receiptFitsPaper(57.0, 228.0, PaperSize.A4)) // still fits A4 (297 tall)
    }

    @Test
    fun disabledPapers_disableIfAnyPageDoesNotFit() {
        val shortReceipt = DocumentPage(ScannedSide("a", 800, 1600), widthMm = 57.0) // 57 × 114 mm
        val tallReceipt = DocumentPage(ScannedSide("b", 500, 2500), widthMm = 57.0) // 57 × 285 mm
        // The tall receipt exceeds A5 (210 tall) → A5 disabled; A4 (297 tall) fits both.
        val disabled = papersDisabledForReceipts(listOf(shortReceipt, tallReceipt), PaperSize.entries)
        assertTrue(PaperSize.A5 in disabled)
        assertFalse(PaperSize.A4 in disabled)
    }

    @Test
    fun disabledPapers_ignoresPagesWithoutWidth() {
        val unsized = DocumentPage(ScannedSide("a", 500, 2500), widthMm = null)
        assertTrue(papersDisabledForReceipts(listOf(unsized), PaperSize.entries).isEmpty())
    }

    @Test
    fun presetWidths_matchExpectedMillimetres() {
        assertEquals(57.0, ReceiptWidth.THERMAL_57.widthMm)
        assertEquals(80.0, ReceiptWidth.THERMAL_80.widthMm)
        assertEquals(50.8, ReceiptWidth.TWO_INCH.widthMm)
        assertEquals(null, ReceiptWidth.CUSTOM.widthMm)
    }
}
