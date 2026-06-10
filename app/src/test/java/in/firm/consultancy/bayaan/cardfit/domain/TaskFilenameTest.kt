package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** Task individual + combined filename templates (CLAUDE.md Phase 14 tests). */
class TaskFilenameTest {

    private val ts = FileTimestamp(year = 2026, month = 6, day = 8, hour = 14, minute = 30, second = 5)

    @Test
    fun individual_template() {
        val name = FilenameBuilder.buildTaskIndividual(
            personName = "Aminul Islam",
            docSlug = "pan",
            taskName = "Scholarship 2026",
            format = OutputFormat.JPEG,
            timestamp = ts,
        )
        assertEquals("aminul-islam-pan-scholarship-2026-upload-260608-1430.jpeg", name)
    }

    @Test
    fun individual_photoEntry_usesPhotoSlug() {
        val name = FilenameBuilder.buildTaskIndividual(
            personName = "Sister",
            docSlug = "photo",
            taskName = "scholarship-2026",
            format = OutputFormat.JPEG,
            timestamp = ts,
        )
        assertEquals("sister-photo-scholarship-2026-upload-260608-1430.jpeg", name)
    }

    @Test
    fun individual_emptyPerson_becomesDocument() {
        val name = FilenameBuilder.buildTaskIndividual(
            personName = "",
            docSlug = "epic",
            taskName = "visa",
            format = OutputFormat.JPEG,
            timestamp = ts,
        )
        assertEquals("document-epic-visa-upload-260608-1430.jpeg", name)
    }

    @Test
    fun combined_uploadAndPrint() {
        val upload = FilenameBuilder.buildTaskCombined("Scholarship 2026", OutputMode.UPLOAD, ts)
        assertEquals("scholarship-2026-combined-upload-260608-1430.pdf", upload)
        val print = FilenameBuilder.buildTaskCombined("Scholarship 2026", OutputMode.PRINT, ts)
        assertEquals("scholarship-2026-combined-print-260608-1430.pdf", print)
    }

    @Test
    fun combined_emptyTask_fallsBackToTask() {
        val name = FilenameBuilder.buildTaskCombined("", OutputMode.UPLOAD, ts)
        assertEquals("task-combined-upload-260608-1430.pdf", name)
    }

    @Test
    fun individual_collisionAppendsSeconds() {
        val taken = setOf("sam-pan-task-a-upload-260608-1430.jpeg")
        val name = FilenameBuilder.buildTaskIndividual(
            personName = "Sam",
            docSlug = "pan",
            taskName = "task-a",
            format = OutputFormat.JPEG,
            timestamp = ts,
            exists = { it in taken },
        )
        assertEquals("sam-pan-task-a-upload-260608-1430-05.jpeg", name)
    }

    @Test
    fun combined_collisionAppendsCounter() {
        val taken = setOf(
            "t-combined-upload-260608-1430.pdf",
            "t-combined-upload-260608-1430-05.pdf",
        )
        val name = FilenameBuilder.buildTaskCombined("t", OutputMode.UPLOAD, ts, exists = { it in taken })
        assertEquals("t-combined-upload-260608-1430-05-2.pdf", name)
    }
}
