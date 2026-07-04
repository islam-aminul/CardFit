package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Last-used export settings, remembered separately per option (one blob per [model.CardType], one
 * per [PhotoSize]) so reopening the same option restores exactly what the user chose last time.
 * Pure Kotlin — persistence lives behind `data.ExportSettingsStore`.
 *
 * COMPATIBILITY: these are serialized to JSON in DataStore. Enum values are stored by NAME and the
 * DataStore keys use the option slugs — from the first public release on, renaming either requires
 * a data migration.
 */

@Serializable
data class CardExportSettings(
    val modes: Set<OutputMode> = emptySet(),
    val papers: Set<PaperSize> = setOf(PaperSize.A4),
    val formats: Set<OutputFormat> = emptySet(),
    val grayscale: Boolean = false,
    val cropMarks: Boolean = false,
    /** null = use the card type's [model.CardType.roundedByDefault]. */
    val roundCorners: Boolean? = null,
    val maxFileSizeKb: Int? = null,
    val sizeOverride: SizeOverride = SizeOverride.AUTOMATIC,
    val pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
    val contentScalePercent: Int = 100,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
)

@Serializable
data class PhotoExportSettings(
    val modes: Set<OutputMode> = emptySet(),
    val uploadMaxKb: Int? = null,
    val printPaper: PhotoPaper = PhotoPaper.A4,
    val requestedCopies: Int = 4,
    val cutMarks: Boolean = true,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
)

/** JSON codec for the settings blobs. Lenient on decode: unknown keys skipped, corrupt data → null. */
object ExportSettingsJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeCard(settings: CardExportSettings): String = json.encodeToString(settings)

    fun decodeCard(raw: String): CardExportSettings? =
        runCatching { json.decodeFromString<CardExportSettings>(raw) }.getOrNull()

    fun encodePhoto(settings: PhotoExportSettings): String = json.encodeToString(settings)

    fun decodePhoto(raw: String): PhotoExportSettings? =
        runCatching { json.decodeFromString<PhotoExportSettings>(raw) }.getOrNull()
}
