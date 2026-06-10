package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import kotlin.math.max
import kotlin.math.min

/** Whether a scanned side matches the CR-80 (standard PVC card) aspect, or is non-standard. */
enum class CardFormat { CR80, NON_STANDARD }

/** Detected orientation of a scanned side. */
enum class Orientation { LANDSCAPE, PORTRAIT }

/** Result of classifying one cropped side by its pixel dimensions. */
data class SideClassification(
    val format: CardFormat,
    val orientation: Orientation,
    val ratio: Double,
)

/** User control over sizing. AUTOMATIC uses detection (Phase 12). */
enum class SizeOverride { AUTOMATIC, FORCE_CR80, CUSTOM }

/** The resolved sizing strategy for a session. */
enum class SizingMode { CR80, NON_STANDARD, CUSTOM }

/**
 * Aspect-ratio card-format detection (CLAUDE.md Phase 12). Pure and JVM-testable.
 *
 * The CR-80 band [1.50, 1.75] is intentionally isolated here so it can be tuned after real-card
 * testing:
 *  - CR-80's true ratio is 85.6/54 = 1.585.
 *  - 1.50 is the midpoint between CR-80 (1.585) and A4 (297/210 = 1.414): below it a rectangle is
 *    closer to A4, so it is safer treated as NON_STANDARD (keeps admit cards, Aadhaar letters, and
 *    old portrait paper voter cards out of CR-80).
 *  - 1.75 is deliberately generous (~+10% above 1.585) since no common ID sits above CR-80; the
 *    headroom absorbs over-cropping that elongates the rectangle.
 */
object CardClassifier {

    const val CR80_LOWER_RATIO: Double = 1.50
    const val CR80_UPPER_RATIO: Double = 1.75

    // "Near CR-80" window: just outside the band on each side, where a non-standard ratio is still
    // plausibly a mis-cropped CR-80 card (so the UI offers the Force CR-80 correction). Kept clear of
    // A4 (1.414) so genuine documents don't trigger it.
    const val CR80_NEAR_LOWER_RATIO: Double = 1.45
    const val CR80_NEAR_UPPER_RATIO: Double = 1.85

    /** CR-80 physical size: 85.6 x 54 mm (long edge x short edge). */
    const val CR80_LONG_MM: Double = 85.6
    const val CR80_SHORT_MM: Double = 54.0

    fun classify(widthPx: Int, heightPx: Int): SideClassification {
        require(widthPx > 0 && heightPx > 0) { "dimensions must be positive" }
        val longer = max(widthPx, heightPx).toDouble()
        val shorter = min(widthPx, heightPx).toDouble()
        val ratio = longer / shorter
        val orientation = if (widthPx >= heightPx) Orientation.LANDSCAPE else Orientation.PORTRAIT
        val format = if (ratio in CR80_LOWER_RATIO..CR80_UPPER_RATIO) {
            CardFormat.CR80
        } else {
            CardFormat.NON_STANDARD
        }
        return SideClassification(format, orientation, ratio)
    }

    /** A non-standard ratio close enough to CR-80 to plausibly be a mis-cropped PVC card. */
    fun isNearCr80(ratio: Double): Boolean = ratio in CR80_NEAR_LOWER_RATIO..CR80_NEAR_UPPER_RATIO

    /** CR-80 size in mm for the given orientation: (width, height). */
    fun cr80SizeMm(orientation: Orientation): Pair<Double, Double> = when (orientation) {
        Orientation.LANDSCAPE -> CR80_LONG_MM to CR80_SHORT_MM
        Orientation.PORTRAIT -> CR80_SHORT_MM to CR80_LONG_MM
    }

    /**
     * Default sizing override when a card type is chosen: AUTOMATIC (detection) for everything except
     * Custom. For a correctly-detected CR-80 card, AUTOMATIC already resolves to CR-80, so the UI
     * shows Automatic selected; Force CR-80 is offered only when detection is a near-CR-80 miss.
     */
    fun defaultOverride(cardType: CardType): SizeOverride = when (cardType) {
        CardType.CUSTOM -> SizeOverride.CUSTOM
        else -> SizeOverride.AUTOMATIC
    }

    /**
     * Resolve the effective sizing for a session from the card type, the front side's pixel
     * dimensions (the session default), and the user's override.
     */
    fun resolveSizingMode(
        cardType: CardType,
        frontWidthPx: Int?,
        frontHeightPx: Int?,
        override: SizeOverride,
    ): SizingMode = when (override) {
        SizeOverride.CUSTOM -> SizingMode.CUSTOM
        SizeOverride.FORCE_CR80 -> SizingMode.CR80
        SizeOverride.AUTOMATIC -> {
            if (cardType == CardType.CUSTOM) {
                SizingMode.CUSTOM
            } else if (frontWidthPx != null && frontHeightPx != null &&
                frontWidthPx > 0 && frontHeightPx > 0
            ) {
                when (classify(frontWidthPx, frontHeightPx).format) {
                    CardFormat.CR80 -> SizingMode.CR80
                    CardFormat.NON_STANDARD -> SizingMode.NON_STANDARD
                }
            } else {
                SizingMode.NON_STANDARD
            }
        }
    }
}
