package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    private val tol = 0.01

    // --- mm -> points (reference values from CLAUDE.md section 6) ---

    @Test
    fun a4_mmToPoints_matchesReference() {
        assertEquals(595.28, Units.mmToPoints(210.0), tol)
        assertEquals(841.89, Units.mmToPoints(297.0), tol)
    }

    @Test
    fun cr80_mmToPoints_matchesReference() {
        assertEquals(242.65, Units.mmToPoints(85.6), tol)
        assertEquals(153.07, Units.mmToPoints(54.0), tol)
    }

    @Test
    fun mmToPoints_zero_isZero() {
        assertEquals(0.0, Units.mmToPoints(0.0), 0.0)
    }

    @Test
    fun mmToPoints_oneInch_is72Points() {
        assertEquals(72.0, Units.mmToPoints(25.4), tol)
    }

    // --- mm -> pixels (reference values from CLAUDE.md sections 6 and 12) ---

    @Test
    fun cr80_mmToPixels_at300dpi_is1011x638() {
        assertEquals(1011, Units.mmToPixels(85.6, 300))
        assertEquals(638, Units.mmToPixels(54.0, 300))
    }

    @Test
    fun a4_mmToPixels_at300dpi_is2480x3508() {
        assertEquals(2480, Units.mmToPixels(210.0, 300))
        assertEquals(3508, Units.mmToPixels(297.0, 300))
    }

    @Test
    fun mmToPixels_oneInch_equalsDpi() {
        assertEquals(300, Units.mmToPixels(25.4, 300))
        assertEquals(200, Units.mmToPixels(25.4, 200))
    }

    @Test
    fun mmToPixels_rounds() {
        // 10 mm @ 300 dpi = 118.110... -> 118
        assertEquals(118, Units.mmToPixels(10.0, 300))
    }
}
