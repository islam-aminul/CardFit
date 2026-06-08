package `in`.firm.consultancy.bayaan.cardfit.data.export

import `in`.firm.consultancy.bayaan.cardfit.data.FakeFileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.FakeJpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.FakePdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExporterTest {

    private val ts = FileTimestamp(2026, 6, 8, 14, 30, 5)
    private val session = ScanSession(
        CardType.PAN,
        front = ScannedSide("uri://front"),
        back = ScannedSide("uri://back"),
    )

    private fun exporter(saver: FakeFileSaver = FakeFileSaver()) = Triple(
        Exporter(FakePdfRenderer(), FakeJpegRenderer(), saver, clock = { ts }),
        saver,
        Unit,
    )

    private fun printPdf() = RenderConfig(OutputMode.PRINT, PaperSize.A4, OutputFormat.PDF, 300, false, true, null)
    private fun uploadJpeg() = RenderConfig(OutputMode.UPLOAD, PaperSize.A4, OutputFormat.JPEG, 200, false, false, 200)

    @Test
    fun bothModes_produceTwoNamedFiles_savedToDownloads() = runTest {
        val (exporter, saver) = exporter()
        val files = exporter.export(session, "Aminul Islam", listOf(printPdf(), uploadJpeg()))

        assertEquals(2, files.size)
        assertEquals("aminul-islam-pan-print-260608-1430.pdf", files[0].fileName)
        assertEquals("aminul-islam-pan-upload-260608-1430.jpeg", files[1].fileName)
        assertEquals("content://saved/aminul-islam-pan-print-260608-1430.pdf", files[0].savedLocation)
        assertEquals(2, saver.saved.size)
        assertEquals("application/pdf", saver.saved[0].mimeType)
        assertEquals("image/jpeg", saver.saved[1].mimeType)
    }

    @Test
    fun emptyNameBecomesDocument() = runTest {
        val (exporter, _) = exporter()
        val files = exporter.export(session, "", listOf(uploadJpeg()))
        assertEquals("document-pan-upload-260608-1430.jpeg", files[0].fileName)
    }

    @Test
    fun collisionInDownloads_appendsSeconds() = runTest {
        val saver = FakeFileSaver().apply { seedExisting("aminul-islam-pan-print-260608-1430.pdf") }
        val (exporter, _) = exporter(saver)
        val files = exporter.export(session, "Aminul Islam", listOf(printPdf()))
        assertEquals("aminul-islam-pan-print-260608-1430-05.pdf", files[0].fileName)
    }

    @Test
    fun sizeWarning_isPropagated() = runTest {
        // A JPEG renderer that reports a best-effort size warning.
        val warningJpeg = object : `in`.firm.consultancy.bayaan.cardfit.data.JpegRenderer {
            override suspend fun render(session: ScanSession, config: RenderConfig) =
                `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput(ByteArray(5), "too big")
        }
        val saver = FakeFileSaver()
        val exporter = Exporter(FakePdfRenderer(), warningJpeg, saver, clock = { ts })
        val files = exporter.export(session, "Sam", listOf(uploadJpeg()))
        assertEquals("too big", files[0].warning)
    }

    @Test
    fun prepareShare_cachesEachFile() = runTest {
        val (exporter, saver) = exporter()
        val items = exporter.prepareShare(session, "Sam", listOf(printPdf(), uploadJpeg()))
        assertEquals(2, items.size)
        assertTrue(items[0].uri.startsWith("content://share/"))
        assertEquals(2, saver.shared.size)
        assertEquals("application/pdf", items[0].mimeType)
        assertEquals("image/jpeg", items[1].mimeType)
    }

    @Test
    fun routesFormatsToCorrectRenderer() = runTest {
        val pdf = FakePdfRenderer()
        val jpeg = FakeJpegRenderer()
        val exporter = Exporter(pdf, jpeg, FakeFileSaver(), clock = { ts })
        exporter.export(session, "Sam", listOf(printPdf(), uploadJpeg()))
        assertEquals(1, pdf.rendered.size) // the PDF config
        assertEquals(1, jpeg.rendered.size) // the JPEG config
    }
}
