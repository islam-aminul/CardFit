package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeTargetingTest {

    // Fake compressor: monotonic in both dpi and quality. size = dpi * quality.
    private val dpiTimesQuality: (Int, Int) -> Int = { dpi, q -> dpi * q }

    @Test
    fun fitsAtStartDpi_returnsHighestQuality() {
        // cap 10000 at dpi 200 -> highest q with 200*q <= 10000 is 50.
        val result = targetJpegSize(maxBytes = 10_000, startDpi = 200, compress = dpiTimesQuality)
        assertEquals(200, result.dpi)
        assertEquals(50, result.quality)
        assertEquals(10_000, result.sizeBytes)
        assertFalse(result.belowFloor)
    }

    @Test
    fun downscalesUntilItFits() {
        // cap 5000. dpi200: min 200*30=6000 > cap -> downscale.
        // dpi170: 170*30=5100 > cap -> downscale. dpi -> round(170*.85)=145 -> floor 150.
        // dpi150: 150*30=4500 <= cap, highest q: 150*q<=5000 -> q=33 (4950).
        val result = targetJpegSize(maxBytes = 5000, startDpi = 200, floorDpi = 150, compress = dpiTimesQuality)
        assertEquals(150, result.dpi)
        assertEquals(33, result.quality)
        assertEquals(4950, result.sizeBytes)
        assertFalse(result.belowFloor)
    }

    @Test
    fun impossibleEvenAtFloor_returnsBestEffortBelowFloor() {
        // cap 1000: nothing fits even at floor min quality (150*30=4500). Best effort at floor.
        val result = targetJpegSize(maxBytes = 1000, startDpi = 200, floorDpi = 150, compress = dpiTimesQuality)
        assertEquals(150, result.dpi)
        assertEquals(30, result.quality)
        assertEquals(4500, result.sizeBytes)
        assertTrue(result.belowFloor)
    }

    @Test
    fun overheadBytes_reduceEffectiveCap() {
        // maxBytes 10000 but overhead 5000 -> effective cap 5000 -> same as the downscale case.
        val result = targetJpegSize(
            maxBytes = 10_000,
            startDpi = 200,
            floorDpi = 150,
            overheadBytes = 5000,
            compress = dpiTimesQuality,
        )
        assertEquals(150, result.dpi)
        assertEquals(33, result.quality)
        assertFalse(result.belowFloor)
    }

    @Test
    fun rendersOncePerDpi_notPerQuality() {
        val rendersByDpi = mutableMapOf<Int, Int>()
        var compressCalls = 0
        val compress: (Int, Int) -> Int = { dpi, q ->
            compressCalls++
            // Count "renders" as the first time a dpi is seen (the real impl caches per dpi).
            rendersByDpi.merge(dpi, 1, Int::plus)
            dpi * q
        }
        targetJpegSize(maxBytes = 5000, startDpi = 200, floorDpi = 150, compress = compress)
        // Each dpi is probed several times (binary search) but only a handful of distinct dpis used.
        assertTrue("too many distinct dpis: ${rendersByDpi.keys}", rendersByDpi.keys.size <= 4)
        assertTrue(compressCalls > 0)
    }

    @Test
    fun minQualityZeroBoundary_isRejectedByChooseQuality() {
        // chooseQuality requires minQuality in 1..100; targetJpegSize forwards it.
        try {
            targetJpegSize(maxBytes = 1000, startDpi = 200, minQuality = 0, compress = dpiTimesQuality)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
