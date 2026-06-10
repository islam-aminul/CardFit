package `in`.firm.consultancy.bayaan.cardfit.domain.task

import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Task mode (CLAUDE.md Phase 14): a named multi-document application set, e.g. "scholarship-2026".
 * Pure Kotlin + kotlinx-serialization (Apache-2.0), so the whole model is JVM-testable and persists
 * as on-device JSON. Image bytes live as app-private files; entries reference them by bare filename
 * (relative to the task's directory), never by absolute path — so nothing here leaks a device path,
 * and NO identity numbers are ever stored.
 */
@Serializable
data class Task(
    val id: String,
    val name: String,
    val createdAt: Long,
    val documents: List<DocumentEntry> = emptyList(),
    /** Per-task default upload size cap (KB); an entry may override it (item 4). Null = no cap. */
    val defaultMaxFileSizeKb: Int? = null,
)

/** Whether an entry came from the scan/document flow or the Phase 13 photo flow. */
@Serializable
enum class EntryKind { DOCUMENT, PHOTO }

/**
 * One application document: a scanned card (front + optional back) OR a Phase 13 photo, plus the
 * per-document person name (family members differ; OCR-suggested, editable) and the detected/chosen
 * type. Image references are bare filenames within the task directory.
 *
 * For [EntryKind.DOCUMENT]: [cardType] is set and [frontImage] (+ optional [backImage]) hold the
 * cropped sides with their pixel dimensions (for aspect classification). For [EntryKind.PHOTO]:
 * [frontImage] holds the single edited photo and [photoWidthMm]/[photoHeightMm] its physical size.
 */
@Serializable
data class DocumentEntry(
    val id: String,
    val personName: String = "",
    val kind: EntryKind,
    val cardType: CardType? = null,
    val frontImage: String? = null,
    val frontWidthPx: Int = 0,
    val frontHeightPx: Int = 0,
    val backImage: String? = null,
    val backWidthPx: Int = 0,
    val backHeightPx: Int = 0,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
    val sizeOverride: SizeOverride = SizeOverride.AUTOMATIC,
    // Photo entries only:
    val photoWidthMm: Double? = null,
    val photoHeightMm: Double? = null,
    // Export: per-entry override of the task's default upload cap (KB). Null = use task default.
    val maxFileSizeKbOverride: Int? = null,
) {
    /** Filename document-slug for exports: the card-type slug, or "photo" for photo entries. */
    val docSlug: String
        get() = when (kind) {
            EntryKind.PHOTO -> "photo"
            EntryKind.DOCUMENT -> cardType?.slug ?: "document"
        }

    /** All image filenames this entry owns (for copy/delete bookkeeping). */
    fun imageFiles(): List<String> = listOfNotNull(frontImage, backImage)
}

/**
 * JSON (de)serialization for [Task]. Centralised so the format (lenient, stable defaults) is one
 * thing, and the data layer just provides the file I/O. Pure — JVM-testable round-trip.
 */
object TaskJson {
    val format: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(task: Task): String = format.encodeToString(Task.serializer(), task)

    fun decode(json: String): Task = format.decodeFromString(Task.serializer(), json)
}
