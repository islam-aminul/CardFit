package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Deterministic histogram-based auto-enhance (CLAUDE.md Phase 13). Pure Kotlin so the level-stretch
 * maths is JVM-testable; the data layer builds the luminance histogram from the bitmap and applies
 * the returned linear map through a `ColorMatrix`.
 *
 * A per-channel linear map `out = in * scale + offset` (operating on 0..255 values). Applied equally
 * to R, G and B it stretches the tonal range without shifting hue.
 */
data class LevelsAdjustment(val scale: Float, val offset: Float) {
    companion object {
        /** The identity map (no change). */
        val IDENTITY = LevelsAdjustment(1f, 0f)
    }
}

/**
 * Compute an auto-levels stretch from a 256-bin luminance [histogram]. Finds the black/white points
 * at the [lowPercentile]/[highPercentile] of the cumulative distribution (default 0.5 % / 99.5 %, so
 * a few outlier pixels don't dominate) and maps `[low..high] -> [0..255]` linearly:
 *
 *   scale  = 255 / (high - low)
 *   offset = -low * scale
 *
 * Returns [LevelsAdjustment.IDENTITY] when the histogram is empty or already spans the full range
 * (`high <= low`), so a flat or already-stretched image is left untouched — the toggle is a no-op
 * rather than producing garbage.
 *
 * Deterministic: the same histogram always yields the same adjustment.
 */
fun autoLevels(
    histogram: IntArray,
    lowPercentile: Double = 0.005,
    highPercentile: Double = 0.995,
): LevelsAdjustment {
    require(histogram.size == 256) { "histogram must have 256 bins, was ${histogram.size}" }
    require(lowPercentile in 0.0..1.0 && highPercentile in 0.0..1.0 && lowPercentile < highPercentile) {
        "percentiles must satisfy 0 <= low < high <= 1"
    }

    val total = histogram.sumOf { it.toLong() }
    if (total <= 0L) return LevelsAdjustment.IDENTITY

    val lowTarget = lowPercentile * total
    val highTarget = highPercentile * total

    var low = 0
    var high = 255
    var cumulative = 0L
    var lowFound = false
    for (i in 0..255) {
        cumulative += histogram[i]
        if (!lowFound && cumulative >= lowTarget) {
            low = i
            lowFound = true
        }
        if (cumulative >= highTarget) {
            high = i
            break
        }
    }

    if (high <= low) return LevelsAdjustment.IDENTITY
    val scale = 255.0 / (high - low)
    val offset = -low * scale
    return LevelsAdjustment(scale.toFloat(), offset.toFloat())
}
