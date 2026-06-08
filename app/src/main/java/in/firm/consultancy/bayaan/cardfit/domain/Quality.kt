package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * JPEG file-size targeting from CLAUDE.md section 7.
 */
sealed interface QualityResult {
    /** The highest quality whose compressed size is <= the cap, with that resulting size. */
    data class Success(val quality: Int, val sizeBytes: Int) : QualityResult

    /** Even at [minQuality] the output exceeds the cap; the caller must downscale and retry. */
    data object NeedsDownscale : QualityResult
}

/**
 * Binary-search the JPEG quality in `[minQuality..100]` and return the HIGHEST quality whose
 * compressed size is `<= maxBytes`. If `compress(minQuality) > maxBytes`, signal
 * [QualityResult.NeedsDownscale].
 *
 * Assumes [compress] is monotonic non-decreasing in quality (higher quality -> larger size), which
 * holds for JPEG. [compress] is injected so this is testable with a fake compressor.
 *
 * @param compress maps a quality in `[minQuality..100]` to the resulting byte size.
 */
fun chooseQuality(
    maxBytes: Int,
    minQuality: Int = Defaults.MIN_QUALITY,
    compress: (quality: Int) -> Int,
): QualityResult {
    require(minQuality in 1..100) { "minQuality must be in 1..100, was $minQuality" }

    if (compress(minQuality) > maxBytes) return QualityResult.NeedsDownscale

    var lo = minQuality
    var hi = 100
    var bestQuality = minQuality
    var bestSize = compress(minQuality)
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val size = compress(mid)
        if (size <= maxBytes) {
            bestQuality = mid
            bestSize = size
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return QualityResult.Success(bestQuality, bestSize)
}
