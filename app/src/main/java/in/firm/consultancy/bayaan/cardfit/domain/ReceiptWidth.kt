package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize

/**
 * Receipt sizing (CardType.RECEIPT). A receipt is placed on the output sheet at its real-world size:
 * the user picks a WIDTH and the height follows the scan's aspect ratio. Presets are common widths
 * (thermal rolls, an inch measure, and the widths of familiar paper sizes); [CUSTOM] takes a runtime
 * width. Pure Kotlin (millimetre space) — fully JVM-testable.
 */
enum class ReceiptWidth(val slug: String, val label: String, val widthMm: Double?) {
    THERMAL_57("57mm", "57 mm (thermal)", 57.0),
    THERMAL_80("80mm", "80 mm (thermal)", 80.0),
    TWO_INCH("2in", "2 in", 50.8),
    POSTCARD_102("102mm", "Postcard (102 mm)", 101.6),
    A5_148("148mm", "A5 width (148 mm)", 148.0),
    LETTER_216("216mm", "Letter width (216 mm)", 215.9),
    CUSTOM("custom", "Custom", null),
}

/**
 * The receipt's true physical size (width, height) in mm from its chosen [widthMm] and the scan's
 * pixel dimensions: height = width ÷ (widthPx / heightPx). Falls back to a 1:1 aspect when the pixel
 * dimensions are unknown (0), which keeps the maths defined without crashing.
 */
fun receiptRealSizeMm(widthMm: Double, widthPx: Int, heightPx: Int): Pair<Double, Double> {
    val aspect = if (widthPx > 0 && heightPx > 0) widthPx.toDouble() / heightPx else 1.0
    return widthMm to (widthMm / aspect)
}

/**
 * Whether a single receipt of true size [realWmm] × [realHmm] fits (at least once) within [paper]'s
 * printable area. A receipt is one cell, so this reuses [gridLayout]'s fit check.
 */
fun receiptFitsPaper(realWmm: Double, realHmm: Double, paper: PaperSize): Boolean =
    gridLayout(realWmm, realHmm, paper.widthMm, paper.heightMm).fits

/**
 * Papers that must be disabled for the current receipt [pages]: a paper is disabled if ANY page
 * (at its true size) doesn't fit it. Pages without a chosen width are ignored (not yet sized).
 */
fun papersDisabledForReceipts(pages: List<DocumentPage>, papers: List<PaperSize>): Set<PaperSize> =
    papers.filter { paper ->
        pages.any { page ->
            val w = page.widthMm ?: return@any false
            val (realW, realH) = receiptRealSizeMm(w, page.side.widthPx, page.side.heightPx)
            !receiptFitsPaper(realW, realH, paper)
        }
    }.toSet()
