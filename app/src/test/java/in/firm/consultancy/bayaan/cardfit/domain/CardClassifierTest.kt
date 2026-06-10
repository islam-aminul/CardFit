package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Test

class CardClassifierTest {

    // ---- band edges (inclusive [1.50, 1.75]) ----

    @Test
    fun cr80TrueRatio_isCr80() {
        // 856 x 540 -> 1.585 (the reference CR-80 ratio)
        val c = CardClassifier.classify(856, 540)
        assertEquals(CardFormat.CR80, c.format)
        assertEquals(Orientation.LANDSCAPE, c.orientation)
        assertEquals(1.585, c.ratio, 0.01)
    }

    @Test
    fun lowerEdge_1_50_isCr80_butJustBelowIsNonStandard() {
        assertEquals(CardFormat.CR80, CardClassifier.classify(1500, 1000).format) // 1.50
        assertEquals(CardFormat.NON_STANDARD, CardClassifier.classify(1499, 1000).format) // 1.499
    }

    @Test
    fun upperEdge_1_75_isCr80_butJustAboveIsNonStandard() {
        assertEquals(CardFormat.CR80, CardClassifier.classify(1750, 1000).format) // 1.75
        assertEquals(CardFormat.NON_STANDARD, CardClassifier.classify(1751, 1000).format) // 1.751
    }

    @Test
    fun a4Ratio_isNonStandard() {
        // A4 = 297/210 = 1.414
        assertEquals(CardFormat.NON_STANDARD, CardClassifier.classify(2970, 2100).format)
    }

    @Test
    fun portrait1_3_isNonStandard() {
        // ~1.3 portrait card
        val c = CardClassifier.classify(1000, 1300)
        assertEquals(CardFormat.NON_STANDARD, c.format)
        assertEquals(Orientation.PORTRAIT, c.orientation)
        assertEquals(1.3, c.ratio, 0.001)
    }

    // ---- orientation ----

    @Test
    fun orientation_landscapeWhenWiderThanTall_portraitOtherwise() {
        assertEquals(Orientation.LANDSCAPE, CardClassifier.classify(856, 540).orientation)
        assertEquals(Orientation.PORTRAIT, CardClassifier.classify(540, 856).orientation)
    }

    @Test
    fun cr80PortraitCapture_isStillCr80_inPortrait() {
        // Same card captured portrait (540 x 856) -> ratio still 1.585 -> CR-80, portrait.
        val c = CardClassifier.classify(540, 856)
        assertEquals(CardFormat.CR80, c.format)
        assertEquals(Orientation.PORTRAIT, c.orientation)
    }

    // ---- cr80SizeMm ----

    @Test
    fun cr80SizeMm_perOrientation() {
        assertEquals(85.6 to 54.0, CardClassifier.cr80SizeMm(Orientation.LANDSCAPE))
        assertEquals(54.0 to 85.6, CardClassifier.cr80SizeMm(Orientation.PORTRAIT))
    }

    // ---- defaults + resolution ----

    @Test
    fun defaultOverride_isAutomaticExceptCustom() {
        assertEquals(SizeOverride.AUTOMATIC, CardClassifier.defaultOverride(CardType.PAN))
        assertEquals(SizeOverride.AUTOMATIC, CardClassifier.defaultOverride(CardType.AADHAAR))
        assertEquals(SizeOverride.AUTOMATIC, CardClassifier.defaultOverride(CardType.EPIC))
        assertEquals(SizeOverride.AUTOMATIC, CardClassifier.defaultOverride(CardType.ADMIT_CARD))
        assertEquals(SizeOverride.AUTOMATIC, CardClassifier.defaultOverride(CardType.FREE))
        assertEquals(SizeOverride.CUSTOM, CardClassifier.defaultOverride(CardType.CUSTOM))
    }

    @Test
    fun isNearCr80_window() {
        assertEquals(true, CardClassifier.isNearCr80(1.47)) // just below band
        assertEquals(true, CardClassifier.isNearCr80(1.80)) // just above band
        assertEquals(false, CardClassifier.isNearCr80(1.414)) // A4 — not near
        assertEquals(false, CardClassifier.isNearCr80(1.30)) // tall portrait — not near
        assertEquals(false, CardClassifier.isNearCr80(2.0)) // very elongated — not near
    }

    @Test
    fun resolve_forceCr80_alwaysCr80() {
        assertEquals(
            SizingMode.CR80,
            CardClassifier.resolveSizingMode(CardType.PAN, 2970, 2100, SizeOverride.FORCE_CR80),
        )
    }

    @Test
    fun resolve_custom_alwaysCustom() {
        assertEquals(
            SizingMode.CUSTOM,
            CardClassifier.resolveSizingMode(CardType.PAN, 856, 540, SizeOverride.CUSTOM),
        )
    }

    @Test
    fun resolve_automatic_usesDetection() {
        // EPIC scanned as CR-80 -> CR80
        assertEquals(
            SizingMode.CR80,
            CardClassifier.resolveSizingMode(CardType.EPIC, 856, 540, SizeOverride.AUTOMATIC),
        )
        // EPIC scanned as large portrait paper (ratio 1.4) -> NON_STANDARD
        assertEquals(
            SizingMode.NON_STANDARD,
            CardClassifier.resolveSizingMode(CardType.EPIC, 2100, 2970, SizeOverride.AUTOMATIC),
        )
    }

    @Test
    fun resolve_automatic_customCardType_isCustom() {
        assertEquals(
            SizingMode.CUSTOM,
            CardClassifier.resolveSizingMode(CardType.CUSTOM, 856, 540, SizeOverride.AUTOMATIC),
        )
    }

    @Test
    fun resolve_automatic_noFront_isNonStandard() {
        assertEquals(
            SizingMode.NON_STANDARD,
            CardClassifier.resolveSizingMode(CardType.EPIC, null, null, SizeOverride.AUTOMATIC),
        )
    }
}
