package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityTest {

    @Test
    fun returnsHighestQualityUnderCap_linearCompressor() {
        // size(q) = q * 10 bytes; cap 550 -> q=55 (550), q=56 -> 560 > cap
        val result = chooseQuality(maxBytes = 550) { q -> q * 10 }
        assertEquals(QualityResult.Success(55, 550), result)
    }

    @Test
    fun exactFitAtMaxQuality() {
        // size(q) = q * 10; cap 1000 -> q=100 exactly fits
        val result = chooseQuality(maxBytes = 1000) { q -> q * 10 }
        assertEquals(QualityResult.Success(100, 1000), result)
    }

    @Test
    fun allQualitiesFit_returns100() {
        val result = chooseQuality(maxBytes = 1_000_000) { q -> q * 10 }
        assertEquals(QualityResult.Success(100, 1000), result)
    }

    @Test
    fun onlyMinQualityFits() {
        // size(q) = q * 10; cap 305 -> q=30 (300) fits, q=31 -> 310 > cap
        val result = chooseQuality(maxBytes = 305) { q -> q * 10 }
        assertEquals(QualityResult.Success(30, 300), result)
    }

    @Test
    fun minQualityExceedsCap_needsDownscale() {
        // size(q) = q * 100; cap 1000; size(30) = 3000 > cap
        val result = chooseQuality(maxBytes = 1000) { q -> q * 100 }
        assertEquals(QualityResult.NeedsDownscale, result)
    }

    @Test
    fun nonLinearMonotonicCompressor() {
        // size(q) = q*q; cap 2500 -> q=50 (2500), q=51 -> 2601 > cap
        val result = chooseQuality(maxBytes = 2500) { q -> q * q }
        assertEquals(QualityResult.Success(50, 2500), result)
    }

    @Test
    fun customMinQuality_boundaryExceedsCap() {
        val result = chooseQuality(maxBytes = 40, minQuality = 50) { q -> q }
        assertEquals(QualityResult.NeedsDownscale, result)
    }

    @Test
    fun customMinQuality_fits() {
        val result = chooseQuality(maxBytes = 75, minQuality = 50) { q -> q }
        assertEquals(QualityResult.Success(75, 75), result)
    }

    @Test
    fun invalidMinQuality_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            chooseQuality(maxBytes = 100, minQuality = 0) { it }
        }
        assertThrows(IllegalArgumentException::class.java) {
            chooseQuality(maxBytes = 100, minQuality = 101) { it }
        }
    }

    @Test
    fun usesBinarySearch_notLinearScan() {
        var calls = 0
        val result = chooseQuality(maxBytes = 550) { q ->
            calls++
            q * 10
        }
        assertEquals(QualityResult.Success(55, 550), result)
        // Binary search over [30..100] should query far fewer than the ~71 candidate qualities.
        assertTrue("expected binary search (few calls), got $calls", calls <= 12)
    }
}
