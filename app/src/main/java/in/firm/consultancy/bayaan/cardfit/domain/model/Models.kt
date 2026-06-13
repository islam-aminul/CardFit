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

@kotlinx.serialization.Serializable
enum class CardType(
    val widthMm: Double?,
    val heightMm: Double?,
    val slug: String,
    val fitMode: FitMode,
) {
    PAN(85.6, 54.0, "pan", FitMode.ACTUAL_SIZE),
    AADHAAR(85.6, 54.0, "aadhaar", FitMode.ACTUAL_SIZE), // PVC card only
    EPIC(85.6, 54.0, "epic", FitMode.ACTUAL_SIZE), // new PVC card only
    ADMIT_CARD(null, null, "admit-card", FitMode.FIT_PAGE),
    CUSTOM(null, null, "custom", FitMode.ACTUAL_SIZE), // dimensions supplied at runtime
    FREE(null, null, "free", FitMode.FIT_WIDTH),
    ;

    /** PVC cards with rounded corners — corner trimming defaults ON for these. */
    val roundedByDefault: Boolean get() = this == PAN || this == AADHAAR || this == EPIC
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
)

/**
 * [imageUri] is the URI string of the corrected, cropped side (see note above). [widthPx]/[heightPx]
 * are the cropped image's pixel dimensions, used for aspect-ratio classification (Phase 12); they
 * default to 0 (unknown) for callers that don't need classification.
 */
data class ScannedSide(val imageUri: String, val widthPx: Int = 0, val heightPx: Int = 0)

data class ScanSession(
    val cardType: CardType,
    val front: ScannedSide?,
    val back: ScannedSide?,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
)
