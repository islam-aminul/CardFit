package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameTest {

    // 2026-06-08 14:30:05
    private val ts = FileTimestamp(year = 2026, month = 6, day = 8, hour = 14, minute = 30, second = 5)

    @Test
    fun uploadJpeg_matchesSpecExample() {
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
        )
        assertEquals("aminul-islam-pan-upload-260608-1430.jpeg", name)
    }

    @Test
    fun printPdf_matchesSpecExample() {
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            mode = OutputMode.PRINT,
            format = OutputFormat.PDF,
            timestamp = ts,
        )
        assertEquals("aminul-islam-pan-print-260608-1430.pdf", name)
    }

    @Test
    fun zeroPadsAllComponents() {
        val early = FileTimestamp(year = 2026, month = 1, day = 3, hour = 9, minute = 5, second = 2)
        val name = FilenameBuilder.build(
            name = "Sam",
            cardTypeSlug = "epic",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = early,
        )
        assertEquals("sam-epic-upload-260103-0905.jpeg", name)
    }

    @Test
    fun emptyName_becomesDocument() {
        val name = FilenameBuilder.build(
            name = "",
            cardTypeSlug = "custom",
            mode = OutputMode.PRINT,
            format = OutputFormat.PDF,
            timestamp = ts,
        )
        assertEquals("document-custom-print-260608-1430.pdf", name)
    }

    @Test
    fun collisionInSameMinute_appendsSeconds() {
        val taken = setOf("aminul-islam-pan-upload-260608-1430.jpeg")
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("aminul-islam-pan-upload-260608-1430-05.jpeg", name)
    }

    @Test
    fun collisionOnSecondsToo_appendsCounter() {
        val taken = setOf(
            "aminul-islam-pan-upload-260608-1430.jpeg",
            "aminul-islam-pan-upload-260608-1430-05.jpeg",
        )
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("aminul-islam-pan-upload-260608-1430-05-2.jpeg", name)
    }

    @Test
    fun collisionCounter_incrementsUntilFree() {
        val taken = setOf(
            "aminul-islam-pan-upload-260608-1430.jpeg",
            "aminul-islam-pan-upload-260608-1430-05.jpeg",
            "aminul-islam-pan-upload-260608-1430-05-2.jpeg",
            "aminul-islam-pan-upload-260608-1430-05-3.jpeg",
        )
        val name = FilenameBuilder.build(
            name = "Aminul Islam",
            cardTypeSlug = "pan",
            mode = OutputMode.UPLOAD,
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("aminul-islam-pan-upload-260608-1430-05-4.jpeg", name)
    }

    @Test
    fun purposeAndExtMappings() {
        assertEquals("print", FilenameBuilder.purposeOf(OutputMode.PRINT))
        assertEquals("upload", FilenameBuilder.purposeOf(OutputMode.UPLOAD))
        assertEquals("pdf", FilenameBuilder.extOf(OutputFormat.PDF))
        assertEquals("jpeg", FilenameBuilder.extOf(OutputFormat.JPEG))
    }
}
