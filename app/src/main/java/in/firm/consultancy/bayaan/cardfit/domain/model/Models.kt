package `in`.firm.consultancy.bayaan.cardfit.domain.model

/**
 * Domain models from CLAUDE.md section 5.
 *
 * NOTE: this package is pure Kotlin with NO Android imports (CLAUDE.md section 4), so it is fully
 * unit-testable on the JVM. Section 5 illustrates [ScannedSide] with an Android `Uri`; to honour the
 * no-Android-imports rule, the image reference is modelled here as a [String] (the URI string). The
 * `data/` layer maps to/from `android.net.Uri` at the boundary.
 */

enum class FitMode { ACTUAL_SIZE, FIT_WIDTH, FIT_PAGE }

/**
 * What is being scanned: a fixed-size PVC card (both sides on one page at true size), a
 * runtime-sized custom card, a full-page document (each page fits the sheet; multiple pages allowed),
 * or a receipt (smaller than the page; the user sets its real-world width; multiple allowed).
 * [slug]s appear in exported filenames and DataStore keys; [label]s are the user-facing display names.
 */
@kotlinx.serialization.Serializable
enum class CardType(
    val widthMm: Double?,
    val heightMm: Double?,
    val slug: String,
    val label: String,
    val fitMode: FitMode,
) {
    PAN(85.6, 54.0, "pan", "PAN", FitMode.ACTUAL_SIZE),
    AADHAAR(85.6, 54.0, "aadhaar", "Aadhaar", FitMode.ACTUAL_SIZE), // PVC card only
    VOTER_ID(85.6, 54.0, "voter-id", "Voter ID (EPIC)", FitMode.ACTUAL_SIZE), // new PVC card only
    CUSTOM(null, null, "custom", "Custom card", FitMode.ACTUAL_SIZE), // dimensions supplied at runtime
    FULL_PAGE_DOCUMENT(null, null, "full-page-document", "Full page document", FitMode.FIT_PAGE),
    RECEIPT(null, null, "receipt", "Receipt", FitMode.FIT_WIDTH),
    ;

    /** PVC cards with rounded corners — corner trimming defaults ON for these. */
    val roundedByDefault: Boolean get() = this == PAN || this == AADHAAR || this == VOTER_ID
}

enum class PaperSize(val widthMm: Double, val heightMm: Double) {
    A4(210.0, 297.0),
    A5(148.0, 210.0),
    LETTER(215.9, 279.4),
    LEGAL(215.9, 355.6),
    // CUSTOM paper handled separately with runtime mm.
}

enum class OutputMode { PRINT, UPLOAD } // user may select one or both

enum class OutputFormat { PDF, JPEG }

/** Page orientation for full-document layouts; LANDSCAPE swaps the paper's width and height. */
@kotlinx.serialization.Serializable
enum class PageOrientation { PORTRAIT, LANDSCAPE }

data class RenderConfig(
    val mode: OutputMode,
    val paper: PaperSize,
    val format: OutputFormat,
    val dpi: Int, // default 300 print, 200 upload
    val grayscale: Boolean,
    val cropMarks: Boolean, // print only
    val maxFileSizeKb: Int?, // upload only
    val roundCorners: Boolean = false, // trim PVC-card rounded corners to white (ID-1 radius)
    val searchableText: Boolean = false, // PDF only: embed an invisible OCR text layer (Phase 11)
    // Sizing override for aspect-ratio detection (Phase 12); resolved against the scanned sides.
    val sizeOverride: `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride =
        `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride.AUTOMATIC,
    // Page orientation for CardType.FULL_PAGE_DOCUMENT; PORTRAIT elsewhere.
    val pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
    // Legacy fractional page-fill control (unused by the new document renderer); kept at 100.
    val contentScalePercent: Int = 100,
)

/**
 * [imageUri] is the URI string of the corrected, cropped side (see note above). [widthPx]/[heightPx]
 * are the cropped image's pixel dimensions, used for aspect-ratio classification (Phase 12); they
 * default to 0 (unknown) for callers that don't need classification.
 */
data class ScannedSide(val imageUri: String, val widthPx: Int = 0, val heightPx: Int = 0)

/**
 * One page of a standalone multi-page document (FULL_PAGE_DOCUMENT / RECEIPT). [side] is the
 * auto-enhanced cropped scan; [widthMm] is the receipt's chosen real-world width (null for full-page
 * pages, which fit the sheet); [edit] holds non-destructive edit params re-applied at render time.
 * Pure Kotlin (no Android imports) so it stays JVM-testable.
 */
data class DocumentPage(
    val side: ScannedSide,
    val widthMm: Double? = null,
    val edit: `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams =
        `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams(),
)

data class ScanSession(
    val cardType: CardType,
    val front: ScannedSide?,
    val back: ScannedSide?,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
    // Pages of a standalone multi-page document (empty for cards and for the task flow, which read
    // only [front]/[back]). Purely additive: the card render path never touches this.
    val documentPages: List<DocumentPage> = emptyList(),
)
