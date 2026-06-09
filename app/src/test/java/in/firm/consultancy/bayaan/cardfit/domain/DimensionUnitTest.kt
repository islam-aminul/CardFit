package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DimensionUnitTest {

    private val tol = 1e-9

    @Test
    fun cm_toMm() {
        assertEquals(50.0, DimensionUnit.CM.toMm(5.0), tol)
        assertEquals(85.6, DimensionUnit.CM.toMm(8.56), tol)
        assertEquals(0.0, DimensionUnit.CM.toMm(0.0), tol)
    }

    @Test
    fun inch_toMm() {
        assertEquals(25.4, DimensionUnit.INCH.toMm(1.0), tol)
        assertEquals(50.8, DimensionUnit.INCH.toMm(2.0), tol)
        // CR-80 width 85.6 mm ≈ 3.3701 in
        assertEquals(85.6, DimensionUnit.INCH.toMm(85.6 / 25.4), tol)
    }

    @Test
    fun cm_fromMm() {
        assertEquals(5.0, DimensionUnit.CM.fromMm(50.0), tol)
        assertEquals(8.56, DimensionUnit.CM.fromMm(85.6), tol)
    }

    @Test
    fun inch_fromMm() {
        assertEquals(1.0, DimensionUnit.INCH.fromMm(25.4), tol)
        assertEquals(2.0, DimensionUnit.INCH.fromMm(50.8), tol)
    }

    @Test
    fun roundTrip_valueToMmAndBack() {
        for (unit in DimensionUnit.entries) {
            for (value in listOf(0.5, 1.0, 5.4, 8.56, 21.0)) {
                assertEquals(value, unit.fromMm(unit.toMm(value)), tol)
            }
        }
    }

    @Test
    fun roundTrip_mmToUnitAndBack() {
        for (unit in DimensionUnit.entries) {
            for (mm in listOf(20.0, 54.0, 85.6, 210.0, 300.0)) {
                assertEquals(mm, unit.toMm(unit.fromMm(mm)), tol)
            }
        }
    }

    @Test
    fun labels() {
        assertEquals("cm", DimensionUnit.CM.label)
        assertEquals("inch", DimensionUnit.INCH.label)
    }
}
