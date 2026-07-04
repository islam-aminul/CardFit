package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportSettingsJsonTest {

    @Test
    fun cardSettings_roundTrip() {
        val settings = CardExportSettings(
            modes = setOf(OutputMode.PRINT, OutputMode.UPLOAD),
            papers = setOf(PaperSize.A5),
            formats = setOf(OutputFormat.JPEG),
            grayscale = true,
            cropMarks = true,
            roundCorners = false,
            maxFileSizeKb = 250,
            sizeOverride = SizeOverride.FORCE_CR80,
            pageOrientation = PageOrientation.LANDSCAPE,
            contentScalePercent = 60,
            customWidthMm = 90.0,
            customHeightMm = 60.0,
        )
        assertEquals(settings, ExportSettingsJson.decodeCard(ExportSettingsJson.encodeCard(settings)))
    }

    @Test
    fun photoSettings_roundTrip() {
        val settings = PhotoExportSettings(
            modes = setOf(OutputMode.PRINT),
            uploadMaxKb = 100,
            printPaper = PhotoPaper.POSTCARD_4X6,
            requestedCopies = 8,
            cutMarks = false,
            customWidthMm = 40.0,
            customHeightMm = 50.0,
        )
        assertEquals(settings, ExportSettingsJson.decodePhoto(ExportSettingsJson.encodePhoto(settings)))
    }

    @Test
    fun decode_toleratesUnknownKeys() {
        val decoded = ExportSettingsJson.decodeCard("""{"grayscale":true,"someFutureField":42}""")
        assertEquals(CardExportSettings(grayscale = true), decoded)
    }

    @Test
    fun decode_corruptBlob_returnsNull() {
        assertNull(ExportSettingsJson.decodeCard("not json"))
        assertNull(ExportSettingsJson.decodePhoto("""{"modes":["NOT_A_MODE"]}"""))
    }
}
