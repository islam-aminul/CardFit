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
    fun fitPage_twoTallCards_scaledToFitWithGap() {
        val aspect = 0.7
        val result = LayoutCalculator.calculate(
            LayoutInput(
                fitMode = FitMode.FIT_PAGE,
                pageWidthMm = a4.widthMm,
                pageHeightMm = a4.heightMm,
                cards = listOf(CardImage(aspect), CardImage(aspect)),
            ),
        )
        // width-fit h = 198/0.7 = 282.857 each; sum = 565.714 + gap 8 > availH 285
        // availForCards = 285 - 8 = 277; both equal -> each height = 138.5
        val h = 138.5
        val scale = 277.0 / (2 * (198.0 / aspect))
        val w = 198.0 * scale
        val x = (210.0 - w) / 2.0
        // stack height = 2*138.5 + 8 = 285 -> topY = (297-285)/2 = 6
        assertRect(LayoutRect(x, 6.0, w, h), result.cards[0])
        assertRect(LayoutRect(x, 6.0 + h + 8.0, w, h), result.cards[1])
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
