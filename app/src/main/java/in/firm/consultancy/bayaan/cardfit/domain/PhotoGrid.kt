package `in`.firm.consultancy.bayaan.cardfit.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Single-page photo PRINT grid maths (CLAUDE.md Phase 13). Pure Kotlin (millimetre space, page
 * top-left origin) so it is fully JVM-testable and shared by the renderer.
 *
 * Coordinate space matches [LayoutCalculator]: origin top-left, x grows right, y grows down.
 */

/** One photo slot's top-left position, in page millimetres. Size is the grid's photo size. */
data class GridCell(val xMm: Double, val yMm: Double)

/**
 * Result of [gridLayout]: how many photos fit on one sheet and where they sit. A grid [fits] only
 * when at least one photo fits in both axes ([perRow] and [rows] are positive).
 */
data class PhotoGrid(
    val perRow: Int,
    val rows: Int,
    val photoWidthMm: Double,
    val photoHeightMm: Double,
    val paperWidthMm: Double,
    val paperHeightMm: Double,
    val gapMm: Double,
) {
    /** Photos that fit on one page = [perRow] × [rows]. */
    val perPage: Int get() = perRow * rows

    /** False when the photo is larger than the printable area (so nothing fits). */
    val fits: Boolean get() = perRow > 0 && rows > 0

    /**
     * Centred top-left positions for a block of [blockRows] full rows (`blockRows <= rows`), laid
     * left-to-right then top-to-bottom. The block is centred on the whole page in both axes, so a
     * partial-height render (fewer rows than fit) is still centred. Defaults to the full grid.
     */
    fun cells(blockRows: Int = rows): List<GridCell> {
        if (!fits || blockRows <= 0) return emptyList()
        val r = min(blockRows, rows)
        val blockW = perRow * photoWidthMm + (perRow - 1) * gapMm
        val blockH = r * photoHeightMm + (r - 1) * gapMm
        val startX = (paperWidthMm - blockW) / 2.0
        val startY = (paperHeightMm - blockH) / 2.0
        val out = ArrayList<GridCell>(perRow * r)
        for (row in 0 until r) {
            val y = startY + row * (photoHeightMm + gapMm)
            for (col in 0 until perRow) {
                out.add(GridCell(startX + col * (photoWidthMm + gapMm), y))
            }
        }
        return out
    }
}

/**
 * Compute the photo grid for one sheet. `n` photos fit across the usable width when
 * `n*photo + (n-1)*gap <= usable`, i.e. `n <= (usable + gap) / (photo + gap)`; same per column.
 * When the photo is wider/taller than the printable area the corresponding count is 0 (no fit).
 *
 * @param marginMm printable-area inset on every edge (default 6 mm).
 * @param gapMm gap between adjacent photos (default 3 mm).
 */
fun gridLayout(
    photoWidthMm: Double,
    photoHeightMm: Double,
    paperWidthMm: Double,
    paperHeightMm: Double,
    marginMm: Double = 6.0,
    gapMm: Double = 3.0,
): PhotoGrid {
    val usableW = paperWidthMm - 2 * marginMm
    val usableH = paperHeightMm - 2 * marginMm
    val perRow = countThatFit(usableW, photoWidthMm, gapMm)
    val rows = countThatFit(usableH, photoHeightMm, gapMm)
    return PhotoGrid(
        perRow = perRow,
        rows = rows,
        photoWidthMm = photoWidthMm,
        photoHeightMm = photoHeightMm,
        paperWidthMm = paperWidthMm,
        paperHeightMm = paperHeightMm,
        gapMm = gapMm,
    )
}

private fun countThatFit(usableMm: Double, itemMm: Double, gapMm: Double): Int {
    if (itemMm <= 0.0 || usableMm < itemMm) return 0
    return floor((usableMm + gapMm) / (itemMm + gapMm)).toInt()
}

/**
 * Outcome of reconciling a requested copy count against the grid (CLAUDE.md Phase 13). The requested
 * count is first rounded UP to fill the last row, then capped at one page:
 *
 *   candidate = ceil(requested / perRow) * perRow
 *   final     = min(candidate, perPage)
 */
sealed interface CopiesResult {
    /** A valid count was produced. [finalCount] photos will be printed; [message] is the (optional) adjustment note. */
    data class Ok(val finalCount: Int, val message: String?) : CopiesResult

    /** The photo is larger than the printable area; nothing fits and the count cannot be adjusted. */
    data class DoesNotFit(val message: String) : CopiesResult

    /** The requested count was less than 1. */
    data class Invalid(val message: String) : CopiesResult
}

/**
 * Reconcile a [requested] copy count against [grid] for a sheet labelled [paperLabel].
 *
 *  - `requested < 1` -> [CopiesResult.Invalid] (reject inline; never adjust).
 *  - photo doesn't fit -> [CopiesResult.DoesNotFit] (never adjust).
 *  - round up to fill the last row, then cap at one page; notify only when the final differs from
 *    the request (more -> "fill the row"; fewer -> "most that fit").
 */
fun resolveCopies(requested: Int, grid: PhotoGrid, paperLabel: String): CopiesResult {
    if (requested < 1) {
        return CopiesResult.Invalid("Enter a number of copies (1 or more).")
    }
    if (!grid.fits) {
        return CopiesResult.DoesNotFit(
            "This photo size doesn't fit on $paperLabel. Choose larger paper or a smaller size.",
        )
    }
    val perRow = grid.perRow
    val candidate = ceil(requested.toDouble() / perRow).toInt() * perRow
    val finalCount = min(candidate, grid.perPage)
    val message = when {
        finalCount > requested -> "Adjusted to $finalCount to fill the row."
        finalCount < requested -> "Adjusted to $finalCount — the most that fit on one $paperLabel sheet."
        else -> null
    }
    return CopiesResult.Ok(finalCount, message)
}
