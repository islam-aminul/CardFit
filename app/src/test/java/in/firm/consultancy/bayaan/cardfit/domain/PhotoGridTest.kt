package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Single-page photo grid + copy-count adjustment maths (CLAUDE.md Phase 13 tests). */
class PhotoGridTest {

    // A4 and a 4x6 inch postcard (101.6 x 152.4 mm).
    private val a4 = gridLayout(35.0, 45.0, 210.0, 297.0)
    private val postcard = gridLayout(35.0, 45.0, 101.6, 152.4)

    @Test
    fun passportOnA4_counts() {
        assertEquals(5, a4.perRow)
        assertEquals(6, a4.rows)
        assertEquals(30, a4.perPage)
        assertTrue(a4.fits)
    }

    @Test
    fun passportOnPostcard_counts() {
        assertEquals(2, postcard.perRow)
        assertEquals(2, postcard.rows)
        assertEquals(4, postcard.perPage)
        assertTrue(postcard.fits)
    }

    @Test
    fun a4FullGrid_centeredNoOverlap_withinPage() {
        val cells = a4.cells()
        assertEquals(30, cells.size)
        // Centred: left edge of first column mirrors the right margin of the last column.
        val firstX = cells.first().xMm
        val lastRightX = cells.last().xMm + a4.photoWidthMm
        assertEquals(firstX, 210.0 - lastRightX, 1e-9)
        // Centred vertically too.
        val firstY = cells.first().yMm
        val lastBottomY = cells.last().yMm + a4.photoHeightMm
        assertEquals(firstY, 297.0 - lastBottomY, 1e-9)
        assertNoOverlap(cells, a4.photoWidthMm, a4.photoHeightMm, 210.0, 297.0)
    }

    @Test
    fun postcardFullGrid_centeredNoOverlap_withinPage() {
        val cells = postcard.cells()
        assertEquals(4, cells.size)
        assertNoOverlap(cells, postcard.photoWidthMm, postcard.photoHeightMm, 101.6, 152.4)
        val firstX = cells.first().xMm
        val lastRightX = cells.last().xMm + postcard.photoWidthMm
        assertEquals(firstX, 101.6 - lastRightX, 1e-9)
    }

    @Test
    fun partialRowsBlock_isCentered() {
        // Two of six possible rows on A4, centred on the full page.
        val cells = a4.cells(blockRows = 2)
        assertEquals(10, cells.size)
        val firstY = cells.first().yMm
        val lastBottomY = cells.last().yMm + a4.photoHeightMm
        assertEquals(firstY, 297.0 - lastBottomY, 1e-9)
    }

    // --- copy-count adjustment rule ---

    @Test
    fun roundsUpToFillRow() {
        // perRow 5, request 7 -> 10, with the fill-the-row message.
        val result = resolveCopies(7, a4, "A4") as CopiesResult.Ok
        assertEquals(10, result.finalCount)
        assertEquals("Adjusted to 10 to fill the row.", result.message)
    }

    @Test
    fun capsAtOnePage() {
        // perPage 25, request 30 -> 25, with the cap message.
        val grid = PhotoGrid(perRow = 5, rows = 5, photoWidthMm = 35.0, photoHeightMm = 45.0,
            paperWidthMm = 210.0, paperHeightMm = 297.0, gapMm = 3.0)
        assertEquals(25, grid.perPage)
        val result = resolveCopies(30, grid, "A4") as CopiesResult.Ok
        assertEquals(25, result.finalCount)
        assertEquals("Adjusted to 25 — the most that fit on one A4 sheet.", result.message)
    }

    @Test
    fun capWinsOverRoundUp() {
        // request 27, perRow 5 (round-up candidate 30), perPage 25 -> caps at 25, not 30.
        val grid = PhotoGrid(perRow = 5, rows = 5, photoWidthMm = 35.0, photoHeightMm = 45.0,
            paperWidthMm = 210.0, paperHeightMm = 297.0, gapMm = 3.0)
        val result = resolveCopies(27, grid, "A4") as CopiesResult.Ok
        assertEquals(25, result.finalCount)
        assertEquals("Adjusted to 25 — the most that fit on one A4 sheet.", result.message)
    }

    @Test
    fun fullGridRequest_noMessage() {
        // request exactly perPage -> no adjustment, no message.
        val result = resolveCopies(30, a4, "A4") as CopiesResult.Ok
        assertEquals(30, result.finalCount)
        assertNull(result.message)
    }

    @Test
    fun exactRowMultiple_noMessage() {
        // request 5 (= one full row, <= perPage) -> unchanged, no message.
        val result = resolveCopies(5, a4, "A4") as CopiesResult.Ok
        assertEquals(5, result.finalCount)
        assertNull(result.message)
    }

    @Test
    fun oversizedPhoto_doesNotFit() {
        val tooBig = gridLayout(300.0, 400.0, 101.6, 152.4)
        assertTrue(!tooBig.fits)
        val result = resolveCopies(4, tooBig, "4×6 in")
        assertTrue(result is CopiesResult.DoesNotFit)
        assertEquals(
            "This photo size doesn't fit on 4×6 in. Choose larger paper or a smaller size.",
            (result as CopiesResult.DoesNotFit).message,
        )
    }

    @Test
    fun zeroOrNegative_isInvalid() {
        assertTrue(resolveCopies(0, a4, "A4") is CopiesResult.Invalid)
        assertTrue(resolveCopies(-3, a4, "A4") is CopiesResult.Invalid)
    }

    /** Assert no two cells overlap and every cell stays within the page. */
    private fun assertNoOverlap(
        cells: List<GridCell>,
        w: Double,
        h: Double,
        pageW: Double,
        pageH: Double,
    ) {
        for (c in cells) {
            assertTrue("cell within page x", c.xMm >= -1e-9 && c.xMm + w <= pageW + 1e-9)
            assertTrue("cell within page y", c.yMm >= -1e-9 && c.yMm + h <= pageH + 1e-9)
        }
        for (i in cells.indices) {
            for (j in i + 1 until cells.size) {
                val a = cells[i]
                val b = cells[j]
                val overlap = a.xMm < b.xMm + w && b.xMm < a.xMm + w &&
                    a.yMm < b.yMm + h && b.yMm < a.yMm + h
                assertTrue("cells $i and $j overlap", !overlap)
            }
        }
    }
}
