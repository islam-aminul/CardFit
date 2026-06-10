package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aspect-locked crop maths for the photo editor (CLAUDE.md Phase 13 tests). */
class PhotoCropTest {

    @Test
    fun passportAspect_onWideSource_cropsWidthCentered() {
        // 1000x1000 source locked to 35:45 (portrait, ratio 0.777). Source too wide -> shave sides.
        val crop = aspectCrop(1000, 1000, 35.0, 45.0)
        assertEquals(1000, crop.heightPx) // full height kept
        assertEquals(778, crop.widthPx) // round(1000 * 35/45)
        assertEquals(111, crop.xPx) // (1000 - 778) / 2
        assertEquals(0, crop.yPx)
        assertResultAspect(crop, 35.0 / 45.0)
    }

    @Test
    fun passportAspect_onTallSource_cropsHeightCentered() {
        // 350x900 source, ratio 0.389 < target 0.777 -> too tall, shave top/bottom.
        val crop = aspectCrop(350, 900, 35.0, 45.0)
        assertEquals(350, crop.widthPx) // full width kept
        assertEquals(450, crop.heightPx) // round(350 * 45/35)
        assertEquals(0, crop.xPx)
        assertEquals(225, crop.yPx) // (900 - 450) / 2
        assertResultAspect(crop, 35.0 / 45.0)
    }

    @Test
    fun visaSquare_onWideSource_centersSquare() {
        val crop = aspectCrop(1200, 800, 51.0, 51.0)
        assertEquals(800, crop.widthPx)
        assertEquals(800, crop.heightPx)
        assertEquals(200, crop.xPx)
        assertEquals(0, crop.yPx)
        assertResultAspect(crop, 1.0)
    }

    @Test
    fun stampAspect_exactRatioSource_keepsWholeImage() {
        // 200x250 already 20:25 -> no cropping.
        val crop = aspectCrop(200, 250, 20.0, 25.0)
        assertEquals(0, crop.xPx)
        assertEquals(0, crop.yPx)
        assertEquals(200, crop.widthPx)
        assertEquals(250, crop.heightPx)
    }

    @Test
    fun cropAlwaysWithinBounds() {
        val crop = aspectCrop(640, 481, 51.0, 51.0)
        assertTrue(crop.xPx >= 0)
        assertTrue(crop.yPx >= 0)
        assertTrue(crop.xPx + crop.widthPx <= 640)
        assertTrue(crop.yPx + crop.heightPx <= 481)
    }

    private fun assertResultAspect(crop: CropRect, target: Double) {
        val actual = crop.widthPx.toDouble() / crop.heightPx.toDouble()
        // Allow ~1 px rounding slack relative to the target aspect.
        assertEquals(target, actual, 0.01)
    }
}
