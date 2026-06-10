package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    private val tol = 0.001
    private val a4 = PaperSize.A4
    private val cr80Aspect = 85.6 / 54.0

    private fun assertRect(
        expected: LayoutRect,
        actual: LayoutRect,
    ) {
        assertEquals("x", expected.xMm, actual.xMm, tol)
        assertEquals("y", expected.yMm, actual.yMm, tol)
        assertEquals("width", expected.widthMm, actual.widthMm, tol)
        assertEquals("height", expected.heightMm, actual.heightMm, tol)
    }

    // ---------- ACTUAL_SIZE ----------

    @Test
    fun actualSize_twoCards_cr80_onA4_centeredStack() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect), CardImage(cr80Aspect)),
                cardWidthMm = 85.6,
                cardHeightMm = 54.0,
            ),
        )
        // stack height = 2*54 + 8 = 116; topY = (297-116)/2 = 90.5; x = (210-85.6)/2 = 62.2
        assertEquals(2, result.cards.size)
        assertRect(LayoutRect(62.2, 90.5, 85.6, 54.0), result.cards[0])
        assertRect(LayoutRect(62.2, 152.5, 85.6, 54.0), result.cards[1])
        // page height unchanged for ACTUAL_SIZE
        assertEquals(297.0, result.pageHeightMm, tol)
        assertEquals(210.0, result.pageWidthMm, tol)
    }

    @Test
    fun actualSize_oneCard_centeredOnPage() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect)),
                cardWidthMm = 85.6,
                cardHeightMm = 54.0,
            ),
        )
        assertEquals(1, result.cards.size)
        // y = (297-54)/2 = 121.5
        assertRect(LayoutRect(62.2, 121.5, 85.6, 54.0), result.cards[0])
    }

    @Test
    fun actualSize_customGap_isHonored() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect), CardImage(cr80Aspect)),
                cardWidthMm = 85.6,
                cardHeightMm = 54.0,
                gapMm = 0.0,
            ),
        )
        // stack height = 108; topY = (297-108)/2 = 94.5; second card directly below first
        assertRect(LayoutRect(62.2, 94.5, 85.6, 54.0), result.cards[0])
        assertRect(LayoutRect(62.2, 148.5, 85.6, 54.0), result.cards[1])
    }

    @Test
    fun actualSize_missingCardDims_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LayoutCalculator.calculate(
                LayoutInput(
                    fitMode = FitMode.ACTUAL_SIZE,
                    pageWidthMm = a4.widthMm,
                    pageHeightMm = a4.heightMm,
                    cards = listOf(CardImage(cr80Aspect)),
                    cardWidthMm = null,
                    cardHeightMm = null,
                ),
            )
        }
    }

    // ---------- FIT_WIDTH ----------

    @Test
    fun fitWidth_twoCards_scaledToContentWidth_andCanvasCroppedToContent() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_WIDTH,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect), CardImage(cr80Aspect)),
            ),
        )
        val contentW = 198.0 // 210 - 2*6
        val h = contentW / cr80Aspect // 124.906542...
        assertRect(LayoutRect(6.0, 6.0, contentW, h), result.cards[0])
        assertRect(LayoutRect(6.0, 6.0 + h + 8.0, contentW, h), result.cards[1])
        // canvas height cropped to content: margins + 2 cards + gap
        assertEquals(2 * h + 8.0 + 12.0, result.pageHeightMm, tol)
        assertEquals(210.0, result.pageWidthMm, tol)
        assertTrue("FIT_WIDTH should crop page height", result.pageHeightMm < a4.heightMm)
    }

    @Test
    fun fitWidth_oneCard() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_WIDTH,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect)),
            ),
        )
        val h = 198.0 / cr80Aspect
        assertRect(LayoutRect(6.0, 6.0, 198.0, h), result.cards[0])
        assertEquals(h + 12.0, result.pageHeightMm, tol)
    }

    // ---------- FIT_PAGE ----------

    @Test
    fun fitPage_oneLandscapeCard_widthFitsNoScale_centeredVertically() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_PAGE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect)),
            ),
        )
        val h = 198.0 / cr80Aspect // 124.906... < availH 285 -> no scale
        val y = (297.0 - h) / 2.0
        assertRect(LayoutRect(6.0, y, 198.0, h), result.cards[0])
        assertEquals(297.0, result.pageHeightMm, tol) // full page retained
    }

    @Test
    fun fitPage_oneTallCard_scaledDownToHeight_centeredHorizontally() {
        val aspect = 0.5 // portrait
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_PAGE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(aspect)),
            ),
        )
        // width-fit height = 198/0.5 = 396 > availH 285 -> scale = 285/396
        val scale = 285.0 / 396.0
        val w = 198.0 * scale // 142.5
        val h = 285.0
        val x = (210.0 - w) / 2.0
        val y = (297.0 - h) / 2.0 // 6.0
        assertRect(LayoutRect(x, y, w, h), result.cards[0])
        assertEquals(142.5, w, tol)
    }

    @Test
    fun fitPage_twoTallCards_uniformScaleIncludingGap_noOverflow() {
        val aspect = 0.7
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_PAGE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(aspect), CardImage(aspect)),
            ),
        )
        // Reference stack (each at usable width 198): refH = 198/0.7 each; refStack = 2*refH + gap 8.
        // Uniform scale = availH(285) / refStack scales EVERYTHING (cards AND gap).
        val availW = 198.0
        val gap = 8.0
        val refH = availW / aspect
        val refStack = 2 * refH + gap
        val scale = 285.0 / refStack
        val w = availW * scale
        val h = refH * scale
        val sGap = gap * scale
        val x = (210.0 - w) / 2.0
        val stackH = 2 * h + sGap
        assertEquals(285.0, stackH, tol) // fills usable height exactly, no overflow
        val topY = (297.0 - stackH) / 2.0
        assertRect(LayoutRect(x, topY, w, h), result.cards[0])
        assertRect(LayoutRect(x, topY + h + sGap, w, h), result.cards[1])
        // bottom of stack stays on the page
        assertTrue(topY >= 0.0 && topY + stackH <= 297.0)
    }

    @Test
    fun fitPage_a5_landscapeAndPortrait_noOverflow() {
        val a5 = PaperSize.A5
        for (aspect in listOf(1.585, 0.7, 1.3)) {
            for (cards in listOf(listOf(CardImage(aspect)), listOf(CardImage(aspect), CardImage(aspect)))) {
                val r = LayoutCalculator.calculate(
                    LayoutInput(FitMode.FIT_PAGE, a5.widthMm, a5.heightMm, cards),
                )
                r.cards.forEach { c ->
                    assertTrue("x in bounds", c.xMm >= -tol && c.xMm + c.widthMm <= a5.widthMm + tol)
                    assertTrue("y in bounds", c.yMm >= -tol && c.yMm + c.heightMm <= a5.heightMm + tol)
                }
                // horizontally centred (all sides share the same width here)
                val c0 = r.cards[0]
                assertEquals((a5.widthMm - c0.widthMm) / 2.0, c0.xMm, tol)
            }
        }
    }

    // ---------- ACTUAL_SIZE: per-side sizes + crop-to-content (Phase 12) ----------

    @Test
    fun actualSize_perSideSizes_differentOrientations_centeredOnFullPage() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect), CardImage(1.0 / cr80Aspect)),
                perSideSizesMm = listOf(85.6 to 54.0, 54.0 to 85.6),
            ),
        )
        // stack height = 54 + 8 + 85.6 = 147.6; topY = (297-147.6)/2 = 74.7
        assertRect(LayoutRect((210.0 - 85.6) / 2.0, 74.7, 85.6, 54.0), result.cards[0])
        assertRect(LayoutRect((210.0 - 54.0) / 2.0, 74.7 + 54.0 + 8.0, 54.0, 85.6), result.cards[1])
        assertEquals(297.0, result.pageHeightMm, tol)
    }

    @Test
    fun actualSize_cropToContent_tightCanvas() {
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(cr80Aspect), CardImage(cr80Aspect)),
                perSideSizesMm = listOf(85.6 to 54.0, 85.6 to 54.0),
                cropToContent = true,
            ),
        )
        // canvas = content + margins(6): width = 85.6 + 12 = 97.6; height = (54+8+54) + 12 = 128
        assertEquals(97.6, result.pageWidthMm, tol)
        assertEquals(128.0, result.pageHeightMm, tol)
        assertRect(LayoutRect(6.0, 6.0, 85.6, 54.0), result.cards[0])
        assertRect(LayoutRect(6.0, 6.0 + 54.0 + 8.0, 85.6, 54.0), result.cards[1])
    }

    @Test
    fun actualSize_twoCr80Portrait_fitsA5_noOverflow() {
        // Spec check: two CR-80 portrait cards (54 x 85.6) stack ~179.2 mm < A5 height 210 mm.
        val a5 = PaperSize.A5
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.ACTUAL_SIZE,
                pageWidthMm = a5.widthMm,
                pageHeightMm = a5.heightMm,
                cards = listOf(CardImage(1.0 / cr80Aspect), CardImage(1.0 / cr80Aspect)),
                perSideSizesMm = listOf(54.0 to 85.6, 54.0 to 85.6),
            ),
        )
        val stackHeight = 85.6 + 8.0 + 85.6 // 179.2
        val topY = (a5.heightMm - stackHeight) / 2.0
        assertEquals(15.4, topY, tol)
        assertTrue(topY >= 0.0 && topY + stackHeight <= a5.heightMm)
    }

    // ---------- guards ----------

    @Test
    fun calculate_emptyCards_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LayoutCalculator.calculate(
                LayoutInput(
                    fitMode = FitMode.FIT_WIDTH,
                    pageWidthMm = a4.widthMm,
                    pageHeightMm = a4.heightMm,
                    cards = emptyList(),
                ),
            )
        }
    }

    @Test
    fun calculate_threeCards_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LayoutCalculator.calculate(
                LayoutInput(
                    fitMode = FitMode.FIT_WIDTH,
                    pageWidthMm = a4.widthMm,
                    pageHeightMm = a4.heightMm,
                    cards = listOf(CardImage(1.0), CardImage(1.0), CardImage(1.0)),
                ),
            )
        }
    }

    @Test
    fun cardImage_nonPositiveAspect_throws() {
        assertThrows(IllegalArgumentException::class.java) { CardImage(0.0) }
    }
}
