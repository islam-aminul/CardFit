package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DimensionFormatTest {

    @Test
    fun formatValue_convertsMmToCm() {
        assertEquals("8.56", formatValue(85.6, DimensionUnit.CM))
        assertEquals("5.4", formatValue(54.0, DimensionUnit.CM))
    }

    @Test
    fun formatValue_convertsMmToInch() {
        // 85.6 / 25.4 = 3.37007..., 54 / 25.4 = 2.12598...
        assertEquals("3.37", formatValue(85.6, DimensionUnit.INCH))
        assertEquals("2.13", formatValue(54.0, DimensionUnit.INCH))
    }

    @Test
    fun wholeNumbers_dropTheDecimalPoint() {
        assertEquals("2", formatValue(20.0, DimensionUnit.CM))
        assertEquals("30", formatValue(300.0, DimensionUnit.CM))
        assertEquals("1", formatValue(25.4, DimensionUnit.INCH))
    }

    @Test
    fun trailingZeros_areTrimmed() {
        // 85.0mm -> 8.50cm must read "8.5", not "8.50".
        assertEquals("8.5", formatValue(85.0, DimensionUnit.CM))
        assertEquals("0.5", formatValue(5.0, DimensionUnit.CM))
    }

    @Test
    fun roundsToTwoDecimals() {
        // 3.3700787... inch -> 3.37, never the raw binary expansion.
        assertEquals("3.37", formatValue(85.6, DimensionUnit.INCH))
        assertEquals("1.27", formatValue(12.7, DimensionUnit.CM))
    }

    @Test
    fun formatLength_appendsUnitLabel() {
        assertEquals("8.56 cm", formatLength(85.6, DimensionUnit.CM))
        assertEquals("3.37 inch", formatLength(85.6, DimensionUnit.INCH))
    }

    @Test
    fun formatSize_sharesOneSuffix() {
        assertEquals("8.56 × 5.4 cm", formatSize(85.6, 54.0, DimensionUnit.CM))
        assertEquals("3.37 × 2.13 inch", formatSize(85.6, 54.0, DimensionUnit.INCH))
    }

    @Test
    fun formatInUnit_doesNotConvert() {
        // Used for values already in the display unit (typed text) and for dimensionless ratios.
        assertEquals("1.59", formatInUnit(1.5857))
        assertEquals("3", formatInUnit(3.0))
    }

    @Test
    fun conversion_roundTripsThroughMm() {
        for (unit in DimensionUnit.entries) {
            for (mm in listOf(20.0, 54.0, 85.6, 148.0, 300.0)) {
                assertEquals(mm, unit.toMm(unit.fromMm(mm)), 1e-9)
            }
        }
    }
}
