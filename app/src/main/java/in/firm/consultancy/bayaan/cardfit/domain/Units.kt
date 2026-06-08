package `in`.firm.consultancy.bayaan.cardfit.domain

import kotlin.math.roundToInt

/**
 * Unit conversions from CLAUDE.md section 6.
 *
 * Reference values:
 *  - A4 = 210x297 mm = 595.28 x 841.89 pt
 *  - CR-80 = 85.6 x 54 mm = 242.65 x 153.07 pt = 1011 x 638 px @ 300 dpi
 */
object Units {
    const val MM_PER_INCH: Double = 25.4
    const val POINTS_PER_INCH: Double = 72.0

    /** mm -> PDF points: `pt = mm * 72.0 / 25.4`. */
    fun mmToPoints(mm: Double): Double = mm * POINTS_PER_INCH / MM_PER_INCH

    /**
     * mm -> pixels at a given dpi: `px = round(mm * dpi / 25.4)`.
     * [roundToInt] is equivalent to `Math.round` for these non-negative magnitudes.
     */
    fun mmToPixels(mm: Double, dpi: Int): Int = (mm * dpi / MM_PER_INCH).roundToInt()
}
