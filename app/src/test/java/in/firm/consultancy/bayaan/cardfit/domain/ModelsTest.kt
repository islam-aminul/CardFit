package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Guards the domain model values declared in CLAUDE.md section 5 against accidental drift. */
class ModelsTest {

    @Test
    fun cr80CardTypes_haveCorrectDimsSlugAndFitMode() {
        for (type in listOf(CardType.PAN, CardType.AADHAAR, CardType.VOTER_ID)) {
            assertEquals(85.6, type.widthMm!!, 0.0)
            assertEquals(54.0, type.heightMm!!, 0.0)
            assertEquals(FitMode.ACTUAL_SIZE, type.fitMode)
        }
        assertEquals("pan", CardType.PAN.slug)
        assertEquals("aadhaar", CardType.AADHAAR.slug)
        assertEquals("voter-id", CardType.VOTER_ID.slug)
    }

    @Test
    fun fullPageDocument_isFitPage_withNoFixedDims() {
        assertNull(CardType.FULL_PAGE_DOCUMENT.widthMm)
        assertNull(CardType.FULL_PAGE_DOCUMENT.heightMm)
        assertEquals("full-page-document", CardType.FULL_PAGE_DOCUMENT.slug)
        assertEquals(FitMode.FIT_PAGE, CardType.FULL_PAGE_DOCUMENT.fitMode)
    }

    @Test
    fun custom_isActualSize_receipt_isFitWidth() {
        assertEquals(FitMode.ACTUAL_SIZE, CardType.CUSTOM.fitMode)
        assertEquals("custom", CardType.CUSTOM.slug)
        assertEquals(FitMode.FIT_WIDTH, CardType.RECEIPT.fitMode)
        assertEquals("receipt", CardType.RECEIPT.slug)
    }

    @Test
    fun cardTypes_haveDisplayLabels() {
        assertEquals("Voter ID (EPIC)", CardType.VOTER_ID.label)
        assertEquals("Full page document", CardType.FULL_PAGE_DOCUMENT.label)
        assertEquals("Receipt", CardType.RECEIPT.label)
        assertEquals("Custom card", CardType.CUSTOM.label)
    }

    @Test
    fun paperSizes_matchSpec() {
        assertEquals(210.0, PaperSize.A4.widthMm, 0.0)
        assertEquals(297.0, PaperSize.A4.heightMm, 0.0)
        assertEquals(148.0, PaperSize.A5.widthMm, 0.0)
        assertEquals(210.0, PaperSize.A5.heightMm, 0.0)
        assertEquals(215.9, PaperSize.LETTER.widthMm, 0.0)
        assertEquals(279.4, PaperSize.LETTER.heightMm, 0.0)
        assertEquals(215.9, PaperSize.LEGAL.widthMm, 0.0)
        assertEquals(355.6, PaperSize.LEGAL.heightMm, 0.0)
    }
}
