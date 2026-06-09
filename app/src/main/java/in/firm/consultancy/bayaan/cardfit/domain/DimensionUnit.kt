package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Length units offered on the custom-size inputs. The domain always works in millimetres; these
 * convert only at the UI boundary (CLAUDE.md item: keep internal math in mm).
 *
 * 1 cm = 10 mm, 1 inch = 25.4 mm.
 */
enum class DimensionUnit(val label: String, val mmPerUnit: Double) {
    CM("cm", 10.0),
    INCH("inch", 25.4),
    ;

    /** Convert a value expressed in this unit to millimetres. */
    fun toMm(value: Double): Double = value * mmPerUnit

    /** Convert millimetres to a value expressed in this unit. */
    fun fromMm(mm: Double): Double = mm / mmPerUnit
}
