package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pixel/point conversions for each photo preset (CLAUDE.md Phase 13 tests). Confirms the presets
 * carry the spec's millimetre dimensions and that the shared [Units] maths produces the expected
 * exact-pixel output at the default 300 dpi (`px = round(mm * dpi / 25.4)`).
 */
class PhotoSizeTest {

    @Test
    fun presetDimensions_matchSpec() {
        assertEquals(35.0, PhotoSize.PASSPORT_INDIA.widthMm)
        assertEquals(45.0, PhotoSize.PASSPORT_INDIA.heightMm)
        assertEquals(51.0, PhotoSize.VISA.widthMm)
        assertEquals(51.0, PhotoSize.VISA.heightMm)
        assertEquals(20.0, PhotoSize.STAMP.widthMm)
        assertEquals(25.0, PhotoSize.STAMP.heightMm)
        assertNull(PhotoSize.CUSTOM.widthMm)
        assertNull(PhotoSize.CUSTOM.heightMm)
    }

    @Test
    fun passport_pixelsAt300Dpi() {
        // 35 mm -> 413.39 -> 413 ; 45 mm -> 531.49 -> 531
        assertEquals(413, Units.mmToPixels(35.0, 300))
        assertEquals(531, Units.mmToPixels(45.0, 300))
    }

    @Test
    fun visa_pixelsAt300Dpi() {
        // 51 mm -> 602.36 -> 602 (square)
        assertEquals(602, Units.mmToPixels(51.0, 300))
        assertEquals(602, Units.mmToPixels(51.0, 300))
    }

    @Test
    fun stamp_pixelsAt300Dpi() {
        // 20 mm -> 236.22 -> 236 ; 25 mm -> 295.28 -> 295
        assertEquals(236, Units.mmToPixels(20.0, 300))
        assertEquals(295, Units.mmToPixels(25.0, 300))
    }

    @Test
    fun aspectRatios() {
        assertEquals(35.0 / 45.0, PhotoSize.PASSPORT_INDIA.aspectRatio!!, 1e-9)
        assertEquals(1.0, PhotoSize.VISA.aspectRatio!!, 1e-9)
        assertEquals(20.0 / 25.0, PhotoSize.STAMP.aspectRatio!!, 1e-9)
        assertNull(PhotoSize.CUSTOM.aspectRatio)
    }
}
