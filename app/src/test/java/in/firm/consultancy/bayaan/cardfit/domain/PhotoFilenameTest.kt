package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Photo-flow filename template (CLAUDE.md Phase 13): `{nameSlug}-photo-{purpose}-{yyMMdd}-{HHmm}.{ext}`. */
class PhotoFilenameTest {

    private val ts = FileTimestamp(year = 2026, month = 6, day = 8, hour = 14, minute = 30, second = 5)

    @Test
    fun uploadJpeg_withName() {
        val name = FilenameBuilder.buildPhoto("Aminul Islam", OutputMode.UPLOAD, OutputFormat.JPEG, ts)
        assertEquals("aminul-islam-photo-upload-260608-1430.jpeg", name)
    }

    @Test
    fun printPdf_withName() {
        val name = FilenameBuilder.buildPhoto("Aminul Islam", OutputMode.PRINT, OutputFormat.PDF, ts)
        assertEquals("aminul-islam-photo-print-260608-1430.pdf", name)
    }

    @Test
    fun emptyName_fallsBackToPhoto() {
        val name = FilenameBuilder.buildPhoto("", OutputMode.UPLOAD, OutputFormat.JPEG, ts)
        assertEquals("photo-photo-upload-260608-1430.jpeg", name)
    }

    @Test
    fun collisionInSameMinute_appendsSeconds() {
        val taken = setOf("photo-photo-upload-260608-1430.jpeg")
        val name = FilenameBuilder.buildPhoto("", OutputMode.UPLOAD, OutputFormat.JPEG, ts, exists = { it in taken })
        assertEquals("photo-photo-upload-260608-1430-05.jpeg", name)
    }
}
