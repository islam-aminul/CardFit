package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Non-destructive photo edit state (CLAUDE.md Phase 13). Pure Kotlin: just the parameters; the data
 * layer applies them to a working bitmap and always keeps the original untouched, so any edit can be
 * reverted by resetting these values.
 *
 * @param rotationDegrees clockwise rotation in {0, 90, 180, 270}.
 * @param crop optional crop rectangle in the *rotated* source's pixel space; null = full frame (free).
 * @param brightnessPercent -100..100 (0 = unchanged).
 * @param contrastPercent -100..100 (0 = unchanged).
 * @param saturationPercent -100..100 (0 = unchanged; -100 = grayscale, +100 = doubled).
 * @param autoEnhance deterministic histogram level-stretch (see [autoLevels]).
 * @param removeBackground replace the segmented background with solid white (opt-in; ML Kit).
 */
data class PhotoEditParams(
    val rotationDegrees: Int = 0,
    val crop: CropRect? = null,
    val brightnessPercent: Int = 0,
    val contrastPercent: Int = 0,
    val saturationPercent: Int = 0,
    val autoEnhance: Boolean = false,
    val removeBackground: Boolean = false,
) {
    /** True when no adjustment, crop, rotation or background change is set (output equals original). */
    val isIdentity: Boolean
        get() = rotationDegrees == 0 && crop == null && brightnessPercent == 0 &&
            contrastPercent == 0 && saturationPercent == 0 && !autoEnhance && !removeBackground
}

/** Selected photo size resolved to concrete millimetres (presets carry their own; Custom supplies them). */
data class ResolvedPhotoSize(val size: PhotoSize, val widthMm: Double, val heightMm: Double) {
    val aspectRatio: Double get() = widthMm / heightMm

    companion object {
        /** Resolve [size], using [customWidthMm]/[customHeightMm] only for [PhotoSize.CUSTOM]. */
        fun of(size: PhotoSize, customWidthMm: Double? = null, customHeightMm: Double? = null): ResolvedPhotoSize {
            val w = size.widthMm ?: requireNotNull(customWidthMm) { "Custom size needs a width" }
            val h = size.heightMm ?: requireNotNull(customHeightMm) { "Custom size needs a height" }
            return ResolvedPhotoSize(size, w, h)
        }
    }
}
