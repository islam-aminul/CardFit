package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Documented defaults from CLAUDE.md section 14. Pure constants, no Android dependencies.
 */
object Defaults {
    /** Gap between stacked cards. */
    const val CARD_GAP_MM: Double = 10.0

    /** Margin around content for upload (FIT_WIDTH) and FIT_PAGE layouts. */
    const val UPLOAD_MARGIN_MM: Double = 6.0

    /** Safe printer margin for the photo print sheet — just clears the typical unprintable edge. */
    const val PHOTO_PRINT_MARGIN_MM: Double = 5.0

    const val PRINT_DPI: Int = 300
    const val UPLOAD_DPI: Int = 200

    /** Lowest JPEG quality the size-search is allowed to consider. */
    const val MIN_QUALITY: Int = 30

    /** Effective DPI below which a card is considered no longer legible. */
    const val READABILITY_FLOOR_DPI: Int = 150

    /** Sensible bounds (in mm) for a user-entered custom card size. */
    const val CUSTOM_MIN_MM: Double = 20.0
    const val CUSTOM_MAX_MM: Double = 300.0
}
