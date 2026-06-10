package `in`.firm.consultancy.bayaan.cardfit.data

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.flow.Flow

/**
 * Boundaries to the Android / ML Kit world (CLAUDE.md section 4). Every heavy or platform-specific
 * capability is expressed as an interface here so it can be faked in JVM tests. Real Android-backed
 * implementations arrive in later phases; until then only fakes exist.
 *
 * Image references are passed as URI strings (the domain layer is Android-free; the `android.net.Uri`
 * is mapped to/from a string at this boundary).
 */

// The Scanner seam lives in `data/scanner/Scanner.kt` (it is bound to Android Intent/Activity types).

/** Wraps ML Kit Text Recognition (bundled Latin model). */
interface Ocr {
    /** Recognized text lines for the given image, in reading order. Empty when nothing is found. */
    suspend fun recognize(imageUri: String): List<String>

    /**
     * Recognized elements with pixel bounding boxes plus the source image's pixel dimensions, for
     * building the searchable-PDF text layer (Phase 11). Empty layer when nothing is found.
     */
    suspend fun recognizeLayer(imageUri: String): `in`.firm.consultancy.bayaan.cardfit.domain.OcrTextLayer
}

/**
 * Result of a render: the file bytes plus an optional [sizeWarning] surfaced when an upload size cap
 * could not be met and a best-effort smallest-legible version was produced (CLAUDE.md section 7).
 * (Plain class, not a data class, to avoid array equals/hashCode pitfalls.)
 */
class RenderedOutput(val bytes: ByteArray, val sizeWarning: String? = null)

/** Common base so callers can pick a renderer by format and invoke it polymorphically. */
interface Renderer {
    suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput
}

/** Renders a [ScanSession] to a PDF document (framework PdfDocument in the real implementation). */
interface PdfRenderer : Renderer

/** Renders a [ScanSession] to a JPEG image (Bitmap/Canvas + JPEG compression in the real impl). */
interface JpegRenderer : Renderer

/** Persists generated files via MediaStore (Downloads on API 29+) and prepares share copies. */
interface FileSaver {
    /**
     * Whether a file with [fileName] already exists in the Downloads collection (collision check).
     * Non-suspending so it can back the synchronous [`FilenameBuilder.build`] `exists` predicate;
     * call it from a background dispatcher (it may perform a MediaStore query).
     */
    fun exists(fileName: String): Boolean

    /** Saves [bytes] as [fileName] with [mimeType] to Downloads; returns a human-readable location. */
    suspend fun save(fileName: String, mimeType: String, bytes: ByteArray): String

    /**
     * Writes [bytes] to a private cache file and returns a FileProvider content-URI string suitable
     * for sharing via ACTION_SEND (grant read permission to the receiver).
     */
    suspend fun cacheForShare(fileName: String, mimeType: String, bytes: ByteArray): String
}

/** User preferences backed by DataStore in the real implementation. */
data class UserPrefs(
    val defaultPaper: PaperSize = PaperSize.A4,
    val defaultFormat: OutputFormat = OutputFormat.PDF,
    val defaultGrayscale: Boolean = false,
    val defaultMaxFileSizeKb: Int? = null,
    val lastName: String = "",
    val searchableText: Boolean = true, // Phase 11: persisted searchable-PDF preference (default ON)
)

interface Prefs {
    val prefs: Flow<UserPrefs>
    suspend fun update(transform: (UserPrefs) -> UserPrefs)
}

/** MIME types for saved outputs. */
object MimeTypes {
    const val PDF = "application/pdf"
    const val JPEG = "image/jpeg"

    fun forFormat(format: OutputFormat): String = when (format) {
        OutputFormat.PDF -> PDF
        OutputFormat.JPEG -> JPEG
    }
}
