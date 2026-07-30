package `in`.firm.consultancy.bayaan.cardfit.domain

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Renders millimetre values in the user's chosen [DimensionUnit]. The domain always stores and
 * computes in mm; this is the single conversion + formatting point at the UI boundary, replacing the
 * per-screen `trimMm` / `formatNumber` / `fmt` helpers that used to drift apart.
 *
 * Values are rounded to at most [MAX_DECIMALS] places with trailing zeros trimmed, so a card reads
 * "8.56 cm" rather than "8.56000000001 cm" and a whole number reads "5" rather than "5.00".
 */
private const val MAX_DECIMALS = 2

/** The numeric part only, e.g. `85.6mm` in [DimensionUnit.CM] → `"8.56"`. */
fun formatValue(mm: Double, unit: DimensionUnit): String = trimTrailingZeros(unit.fromMm(mm))

/** A single length with its unit suffix, e.g. `"8.56 cm"`. */
fun formatLength(mm: Double, unit: DimensionUnit): String = "${formatValue(mm, unit)} ${unit.label}"

/** A width × height pair sharing one unit suffix, e.g. `"8.56 × 5.4 cm"`. */
fun formatSize(widthMm: Double, heightMm: Double, unit: DimensionUnit): String =
    "${formatValue(widthMm, unit)} × ${formatValue(heightMm, unit)} ${unit.label}"

/**
 * Formats a value already expressed in [unit] (not mm) — used by the size inputs, which hold what
 * the user typed rather than the stored millimetres.
 */
fun formatInUnit(value: Double): String = trimTrailingZeros(value)

private fun trimTrailingZeros(value: Double): String {
    var scale = 1.0
    repeat(MAX_DECIMALS) { scale *= 10.0 }
    val rounded = (value * scale).roundToLong() / scale
    // Whole numbers drop the decimal point entirely ("5", not "5.0").
    if (abs(rounded - rounded.roundToLong()) < 1e-9) return rounded.roundToLong().toString()
    return rounded.toString().trimEnd('0').trimEnd('.')
}
