package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Global multi-image size loop for the combined PDF (CLAUDE.md Phase 14 tests). The fake compressor
 * models a total over several embedded images: total grows with quality and with DPI.
 */
class CombinedSizeTargetingTest {

    /** Fake total compressor: sum over [pages] images, each ~ (dpi^2) * (quality/100). */
    private fun totalCompressor(pages: Int): (Int, Int) -> Int = { dpi, quality ->
        val perImage = (dpi.toLong() * dpi.toLong() * quality / 100L).toInt()
        perImage * pages
    }

    @Test
    fun picksHighestSharedQualityUnderCap_atStartDpi() {
        val pages = 3
        val compress = totalCompressor(pages)
        // At dpi 300, total at quality q = 3 * (90000 * q / 100) = 2700 * q. Cap 270_000 -> q <= 100.
        val result = targetCombinedSize(maxBytes = 270_000, startDpi = 300, compressTotal = compress)
        assertEquals(300, result.dpi)
        assertEquals(100, result.quality)
        assertTrue(compress(result.dpi, result.quality) <= 270_000)
        assertTrue(!result.belowFloor)
    }

    @Test
    fun totalRespectsCap_afterChoosingQuality() {
        val compress = totalCompressor(4)
        val cap = 500_000
        val result = targetCombinedSize(maxBytes = cap, startDpi = 300, compressTotal = compress)
        // The chosen (dpi, quality) total must fit the cap, and one quality step up must exceed it
        // (or be capped at 100).
        assertTrue(compress(result.dpi, result.quality) <= cap)
        if (result.quality < 100) {
            assertTrue(compress(result.dpi, result.quality + 1) > cap)
        }
    }

    @Test
    fun needsDownscale_steppedDownByDpi() {
        // Make even min quality at 300 dpi overflow, forcing a DPI step-down (~15%).
        val compress = totalCompressor(5)
        // At 300 dpi, min quality 30 total = 5 * (90000*30/100) = 5 * 27000 = 135_000.
        // Pick a cap just under that so the loop must reduce DPI.
        val result = targetCombinedSize(maxBytes = 120_000, startDpi = 300, compressTotal = compress)
        assertTrue("dpi should have stepped below the start", result.dpi < 300)
        assertTrue(result.dpi >= Defaults.READABILITY_FLOOR_DPI)
        assertTrue(compress(result.dpi, result.quality) <= 120_000 || result.belowFloor)
    }

    @Test
    fun belowFloor_whenImpossibleEvenAtFloor() {
        // A cap smaller than the total at the floor DPI + min quality -> best-effort, flagged.
        val compress = totalCompressor(5)
        val floorMin = compress(Defaults.READABILITY_FLOOR_DPI, Defaults.MIN_QUALITY)
        val result = targetCombinedSize(maxBytes = floorMin / 2, startDpi = 300, compressTotal = compress)
        assertEquals(Defaults.READABILITY_FLOOR_DPI, result.dpi)
        assertEquals(Defaults.MIN_QUALITY, result.quality)
        assertTrue(result.belowFloor)
    }

    @Test
    fun overheadReservesHeadroom() {
        val compress = totalCompressor(2)
        // With overhead, the effective cap is lower, so the chosen total must fit cap - overhead.
        val cap = 200_000
        val overhead = 50_000
        val result = targetCombinedSize(
            maxBytes = cap, startDpi = 300, overheadBytes = overhead, compressTotal = compress,
        )
        assertTrue(compress(result.dpi, result.quality) <= cap - overhead || result.belowFloor)
    }
}
