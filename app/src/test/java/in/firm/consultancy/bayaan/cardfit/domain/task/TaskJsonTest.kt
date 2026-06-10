package `in`.firm.consultancy.bayaan.cardfit.domain.task

import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Task metadata (de)serialization round-trip (CLAUDE.md Phase 14 tests). */
class TaskJsonTest {

    private val task = Task(
        id = "t-1",
        name = "Scholarship 2026",
        createdAt = 1_700_000_000_000L,
        defaultMaxFileSizeKb = 200,
        documents = listOf(
            DocumentEntry(
                id = "e-1",
                personName = "Aminul Islam",
                kind = EntryKind.DOCUMENT,
                cardType = CardType.PAN,
                frontImage = "e-1-front.jpg",
                frontWidthPx = 1011,
                frontHeightPx = 638,
                backImage = "e-1-back.jpg",
                backWidthPx = 1011,
                backHeightPx = 638,
                sizeOverride = SizeOverride.FORCE_CR80,
                maxFileSizeKbOverride = 150,
            ),
            DocumentEntry(
                id = "e-2",
                personName = "Sister",
                kind = EntryKind.PHOTO,
                frontImage = "e-2-photo.jpg",
                frontWidthPx = 413,
                frontHeightPx = 531,
                photoWidthMm = 35.0,
                photoHeightMm = 45.0,
            ),
        ),
    )

    @Test
    fun roundTrip_preservesEverything() {
        val json = TaskJson.encode(task)
        val decoded = TaskJson.decode(json)
        assertEquals(task, decoded)
    }

    @Test
    fun roundTrip_emptyTask() {
        val empty = Task(id = "t-0", name = "", createdAt = 0L)
        assertEquals(empty, TaskJson.decode(TaskJson.encode(empty)))
    }

    @Test
    fun docSlug_byKind() {
        assertEquals("pan", task.documents[0].docSlug)
        assertEquals("photo", task.documents[1].docSlug)
    }

    @Test
    fun ignoresUnknownKeys() {
        // Forward-compat: an unknown field from a newer version is tolerated.
        val withExtra = """{"id":"t-9","name":"x","createdAt":5,"futureField":true}"""
        val decoded = TaskJson.decode(withExtra)
        assertEquals("t-9", decoded.id)
        assertEquals(0, decoded.documents.size)
    }

    @Test
    fun imageFiles_listsBothSides() {
        assertEquals(listOf("e-1-front.jpg", "e-1-back.jpg"), task.documents[0].imageFiles())
        assertEquals(listOf("e-2-photo.jpg"), task.documents[1].imageFiles())
    }
}
