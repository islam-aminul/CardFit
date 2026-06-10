package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameTest {

    // 2026-06-08 14:30:05
    private val ts = FileTimestamp(year = 2026, month = 6, day = 8, hour = 14, minute = 30, second = 5)

    @Test
    fun uploadJpeg_includesPaperSlug() {
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            paperSlug = "a4",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
        )
        assertEquals("aminul-islam-pan-a4-upload-260608-1430.jpeg", name)
    }

    @Test
    fun printPdf_includesPaperSlug() {
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            paperSlug = "a5",
            mode = OutputMode.PRINT,
            format = OutputFormat.PDF,
            timestamp = ts,
        )
        assertEquals("aminul-islam-pan-a5-print-260608-1430.pdf", name)
    }

    @Test
    fun zeroPadsAllComponents() {
        val early = FileTimestamp(year = 2026, month = 1, day = 3, hour = 9, minute = 5, second = 2)
        val name = FilenameBuilder.build(
            name = "Sam",
            cardTypeSlug = "epic",
            paperSlug = "a4",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = early,
        )
        assertEquals("sam-epic-a4-upload-260103-0905.jpeg", name)
    }

    @Test
    fun emptyName_becomesDocument() {
        val name = FilenameBuilder.build(
            name = "",
            cardTypeSlug = "custom",
            paperSlug = "legal",
            mode = OutputMode.PRINT,
            format = OutputFormat.PDF,
            timestamp = ts,
        )
        assertEquals("document-custom-legal-print-260608-1430.pdf", name)
    }

    @Test
    fun collisionInSameMinute_appendsSeconds() {
        val taken = setOf("aminul-islam-pan-a4-upload-260608-1430.jpeg")
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            paperSlug = "a4",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("aminul-islam-pan-a4-upload-260608-1430-05.jpeg", name)
    }

    @Test
    fun collisionOnSecondsToo_appendsCounter() {
        val taken = setOf(
            "aminul-islam-pan-a4-upload-260608-1430.jpeg",
            "aminul-islam-pan-a4-upload-260608-1430-05.jpeg",
        )
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            paperSlug = "a4",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("aminul-islam-pan-a4-upload-260608-1430-05-2.jpeg", name)
    }

    @Test
    fun differentPaper_givesDistinctNames() {
        val a4 = FilenameBuilder.build("Sam", "pan", "a4", OutputMode.PRINT, OutputFormat.PDF, ts)
        val a5 = FilenameBuilder.build("Sam", "pan", "a5", OutputMode.PRINT, OutputFormat.PDF, ts)
        assertEquals("sam-pan-a4-print-260608-1430.pdf", a4)
        assertEquals("sam-pan-a5-print-260608-1430.pdf", a5)
    }

    @Test
    fun purposeAndExtMappings() {
        assertEquals("print", FilenameBuilder.purposeOf(OutputMode.PRINT))
        assertEquals("upload", FilenameBuilder.purposeOf(OutputMode.UPLOAD))
        assertEquals("pdf", FilenameBuilder.extOf(OutputFormat.PDF))
        assertEquals("jpeg", FilenameBuilder.extOf(OutputFormat.JPEG))
    }
}
