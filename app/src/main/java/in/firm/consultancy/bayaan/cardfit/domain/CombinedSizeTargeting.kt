package `in`.firm.consultancy.bayaan.cardfit.domain

/**
 * Global size-targeting for the combined multi-page PDF (CLAUDE.md Phase 14, item 5).
 *
 * Unlike a single document, a combined PDF embeds one image PER PAGE. The cap applies to the whole
 * file, so a single shared JPEG quality is chosen for ALL embedded images at once: [compressTotal]
 * renders every page's image at a DPI and returns the SUM of their compressed sizes at a quality.
 *
 * This is exactly the single-document loop from [targetJpegSize] (binary-search quality via
 * [chooseQuality], step DPI down by [stepFraction] when even the minimum quality overflows, never
 * below the legibility [floorDpi], and flag [SizeTargetResult.belowFloor] for a best-effort result) —
 * only the compressor is global. Reusing it keeps the floor/warning behaviour identical to single
 * documents, and keeps this JVM-testable with a fake total-compressor.
 *
 * @param overheadBytes structural PDF overhead to reserve below the cap (page tree, fonts, etc.).
 */
fun targetCombinedSize(
    maxBytes: Int,
    startDpi: Int,
    floorDpi: Int = Defaults.READABILITY_FLOOR_DPI,
    minQuality: Int = Defaults.MIN_QUALITY,
    overheadBytes: Int = 0,
    stepFraction: Double = 0.15,
    compressTotal: (dpi: Int, quality: Int) -> Int,
): SizeTargetResult = targetJpegSize(
    maxBytes = maxBytes,
    startDpi = startDpi,
    floorDpi = floorDpi,
    minQuality = minQuality,
    overheadBytes = overheadBytes,
    stepFraction = stepFraction,
    compress = compressTotal,
)
