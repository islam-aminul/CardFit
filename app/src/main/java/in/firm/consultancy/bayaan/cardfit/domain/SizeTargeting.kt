package `in`.firm.consultancy.bayaan.cardfit.domain

import kotlin.math.roundToInt

/**
 * Outcome of the upload size-targeting search (CLAUDE.md section 7).
 *
 * @param dpi the effective DPI the output was produced at (possibly reduced from the start DPI).
 * @param quality the chosen JPEG quality.
 * @param sizeBytes the resulting compressed size at [dpi]/[quality].
 * @param belowFloor true when the cap could not be met even at the legibility floor, so a
 *   best-effort smallest-legible version was produced and the user should be warned.
 */
data class SizeTargetResult(
    val dpi: Int,
    val quality: Int,
    val sizeBytes: Int,
    val belowFloor: Boolean,
)

/**
 * Orchestrates the upload size-targeting flow from CLAUDE.md section 7:
 *
 *  1. At the current DPI, [chooseQuality] finds the highest quality whose size fits the cap.
 *  2. If even the minimum quality overflows, reduce the DPI by [stepFraction] (~15%) and retry,
 *     down to [floorDpi] (~150 dpi effective — never below).
 *  3. If still impossible at the floor, return the best-effort smallest-legible output (floor DPI,
 *     minimum quality) flagged with [SizeTargetResult.belowFloor] so the caller can warn.
 *
 * [overheadBytes] is subtracted from [maxBytes] before comparing — used for PDF-upload, where the
 * compared JPEG bitmap is embedded into a PDF that adds a little structural overhead.
 *
 * [compress] renders at a DPI and compresses at a quality, returning the resulting byte size. It is
 * injected so the orchestration is unit-testable with a fake compressor (no Android bitmaps).
 */
fun targetJpegSize(
    maxBytes: Int,
    startDpi: Int,
    floorDpi: Int = Defaults.READABILITY_FLOOR_DPI,
    minQuality: Int = Defaults.MIN_QUALITY,
    overheadBytes: Int = 0,
    stepFraction: Double = 0.15,
    compress: (dpi: Int, quality: Int) -> Int,
): SizeTargetResult {
    require(startDpi >= 1) { "startDpi must be >= 1" }
    require(floorDpi >= 1) { "floorDpi must be >= 1" }
    require(stepFraction in 0.01..0.9) { "stepFraction must be in 0.01..0.9" }

    val cap = (maxBytes - overheadBytes).coerceAtLeast(1)
    var dpi = startDpi.coerceAtLeast(floorDpi)

    while (true) {
        when (val result = chooseQuality(cap, minQuality) { q -> compress(dpi, q) }) {
            is QualityResult.Success ->
                return SizeTargetResult(dpi, result.quality, result.sizeBytes, belowFloor = false)

            QualityResult.NeedsDownscale -> {
                if (dpi <= floorDpi) {
                    // Best effort: smallest legible version at the floor.
                    val size = compress(floorDpi, minQuality)
                    return SizeTargetResult(floorDpi, minQuality, size, belowFloor = true)
                }
                val next = (dpi * (1.0 - stepFraction)).roundToInt()
                dpi = next.coerceAtLeast(floorDpi)
            }
        }
    }
}
