package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Photo flow (CLAUDE.md Phase 13). Standard ID-photo presets plus a runtime Custom. All dimensions
 * are millimetres — the single internal unit (cm/inch entry converts at the UI boundary via
 * [DimensionUnit]). Pure Kotlin, no Android imports, so the whole sizing model is JVM-testable.
 *
 * Preset dimensions follow the task spec exactly:
 *  - Passport (India) 35 x 45 mm
 *  - Visa 2 x 2 inch, quoted as 51 x 51 mm
 *  - Stamp 20 x 25 mm
 */
@kotlinx.serialization.Serializable
enum class PhotoSize(
    val slug: String,
    val label: String,
    val widthMm: Double?,
    val heightMm: Double?,
) {
    PASSPORT_INDIA("passport", "Passport (India)", 35.0, 45.0),
    VISA("visa", "Visa (2×2 in)", 51.0, 51.0),
    STAMP("stamp", "Stamp", 20.0, 25.0),
    CUSTOM("custom", "Custom", null, null), // dimensions supplied at runtime
    ;

    /** Aspect ratio (width / height) for the fixed presets; null for [CUSTOM]. */
    val aspectRatio: Double?
        get() = if (widthMm != null && heightMm != null) widthMm / heightMm else null
}

/**
 * Paper choices for the single-page photo PRINT grid (CLAUDE.md Phase 13). A dedicated enum (rather
 * than the document [model.PaperSize]) because the photo flow offers a 4×6 inch postcard and omits
 * Legal. The 4×6 postcard is 101.6 × 152.4 mm.
 */
enum class PhotoPaper(
    val slug: String,
    val label: String,
    val widthMm: Double,
    val heightMm: Double,
) {
    A4("a4", "A4", 210.0, 297.0),
    A5("a5", "A5", 148.0, 210.0),
    POSTCARD_4X6("4x6", "4×6 in", 101.6, 152.4),
    LETTER("letter", "Letter", 215.9, 279.4),
}
