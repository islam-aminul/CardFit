package `in`.firm.consultancy.bayaan.cardfit.data

import `in`.firm.consultancy.bayaan.cardfit.data.scanner.ScanSlot
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakesTest {

    private fun session() = ScanSession(CardType.PAN, null, null)
    private fun config() = RenderConfig(
        mode = OutputMode.UPLOAD,
        paper = PaperSize.A4,
        format = OutputFormat.JPEG,
        dpi = 200,
        grayscale = false,
        cropMarks = false,
        maxFileSizeKb = 200,
    )

    @Test
    fun scanner_persistsPerSlot() = runTest {
        val scanner = FakeScanner()
        assertEquals("content://fake/front", scanner.persistFirstPage(null, ScanSlot.FRONT))
        assertEquals("content://fake/back", scanner.persistFirstPage(null, ScanSlot.BACK))
        assertEquals(listOf(ScanSlot.FRONT, ScanSlot.BACK), scanner.persisted)
    }

    @Test
    fun ocr_returnsCannedLines() = runTest {
        val ocr = FakeOcr(listOf("Name", "AMINUL ISLAM"))
        assertEquals(listOf("Name", "AMINUL ISLAM"), ocr.recognize("content://front"))
    }

    @Test
    fun renderers_recordCallsAndReturnBytes() = runTest {
        val pdf = FakePdfRenderer(output = ByteArray(7))
        val jpeg = FakeJpegRenderer(output = ByteArray(9))
        assertEquals(7, pdf.render(session(), config()).bytes.size)
        assertEquals(9, jpeg.render(session(), config()).bytes.size)
        assertEquals(1, pdf.rendered.size)
        assertEquals(1, jpeg.rendered.size)
    }

    @Test
    fun fileSaver_tracksExistenceAndSaves() = runTest {
        val saver = FakeFileSaver()
        assertFalse(saver.exists("a.pdf"))
        val uri = saver.save("a.pdf", MimeTypes.PDF, ByteArray(10))
        assertEquals("content://saved/a.pdf", uri)
        assertTrue(saver.exists("a.pdf"))
        assertEquals(1, saver.saved.size)
        assertEquals(10, saver.saved.first().size)
        assertEquals(MimeTypes.PDF, saver.saved.first().mimeType)
    }

    @Test
    fun prefs_roundTripUpdate() = runTest {
        val prefs = FakePrefs()
        assertEquals(PaperSize.A4, prefs.prefs.first().defaultPaper)
        prefs.update { it.copy(defaultPaper = PaperSize.A5, lastName = "Sam") }
        val updated = prefs.prefs.first()
        assertEquals(PaperSize.A5, updated.defaultPaper)
        assertEquals("Sam", updated.lastName)
    }

    @Test
    fun mimeTypes_mapFormats() {
        assertEquals("application/pdf", MimeTypes.forFormat(OutputFormat.PDF))
        assertEquals("image/jpeg", MimeTypes.forFormat(OutputFormat.JPEG))
    }
}
