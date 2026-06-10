package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic auto-levels stretch (CLAUDE.md Phase 13). */
class AutoEnhanceTest {

    @Test
    fun emptyHistogram_isIdentity() {
        assertEquals(LevelsAdjustment.IDENTITY, autoLevels(IntArray(256)))
    }

    @Test
    fun fullRangeImage_isApproxIdentity() {
        // A flat distribution already spanning 0..255 needs essentially no stretch.
        val hist = IntArray(256) { 100 }
        val adj = autoLevels(hist)
        // black point ~1, white point ~254 -> scale slightly above 1.
        assertTrue(adj.scale in 1.0f..1.05f)
    }

    @Test
    fun lowContrastImage_isStretched() {
        // All mass between 100 and 150 -> stretch that band across 0..255.
        val hist = IntArray(256)
        for (i in 100..150) hist[i] = 1000
        val adj = autoLevels(hist)
        assertTrue("scale should expand range", adj.scale > 4.0f)
        // Maps the low point near 0: in*scale + offset for the low bin ~ 0.
        val low = 100
        val mappedLow = low * adj.scale + adj.offset
        assertEquals(0.0f, mappedLow, 6.0f)
    }

    @Test
    fun deterministic_sameInputSameOutput() {
        val hist = IntArray(256) { if (it in 60..200) 50 else 0 }
        assertEquals(autoLevels(hist), autoLevels(hist))
    }
}
