package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType

/**
 * ORIGINAL, schematic FINAL-OUTPUT illustrations for the card/document selection tiles: each one is
 * a portrait paper sheet showing what the export produces — both sides of a card stacked top and
 * bottom, a full document fitted within the page margins, or a smaller adjustable document. Same
 * spirit and constraints as [CardArtwork]: Compose Canvas primitives only, deliberately abstract,
 * NO real government logos, emblems, or seals (CLAUDE.md sections 2 and 15). Accents are shared
 * with [CardArtwork] so each type keeps its identity across screens.
 */
@Composable
fun CardOutputArtwork(type: CardType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        when (type) {
            CardType.PAN -> drawCardOutputSheet(PanAccent, withChip = true, photoOnRight = false)
            CardType.AADHAAR -> drawCardOutputSheet(AadhaarAccent, withChip = false, photoOnRight = true)
            CardType.VOTER_ID -> drawCardOutputSheet(VoterIdAccent, withChip = false, photoOnRight = false)
            CardType.CUSTOM -> drawCustomOutputSheet(CustomAccent)
            CardType.FULL_PAGE_DOCUMENT -> drawFullPageDocumentOutput(FullPageDocumentAccent)
            CardType.RECEIPT -> drawReceiptOutput(ReceiptAccent)
        }
    }
}

private const val PAGE_ASPECT = 210f / 297f // portrait A4

/** The two stacked card positions (front top, back bottom) on the page, in one place. */
private fun cardSlots(page: Rect): Pair<Rect, Rect> {
    val cardW = page.width * 0.62f
    val cardH = cardW * (54f / 85.6f)
    val x = page.left + (page.width - cardW) / 2f
    val gap = cardH * 0.45f
    val totalH = cardH * 2 + gap
    val top = page.top + (page.height - totalH) / 2f
    return Rect(x, top, x + cardW, top + cardH) to
        Rect(x, top + cardH + gap, x + cardW, top + totalH)
}

/** Both sides of an ID card on one sheet: detailed front on top, bars-only back below. */
private fun DrawScope.drawCardOutputSheet(accent: Color, withChip: Boolean, photoOnRight: Boolean) {
    val page = pageRect(PAGE_ASPECT)
    drawPage(page, accent)
    val (front, back) = cardSlots(page)
    idGlyph(accent, front, withChip = withChip, photoOnRight = photoOnRight, isBack = false)
    idGlyph(accent, back, withChip = false, photoOnRight = photoOnRight, isBack = true)
}

/**
 * A mini ID-card glyph. The front carries the identity marks (photo box, chip); the back is field
 * bars only, so the stacked pair clearly reads as the two sides of one card.
 */
private fun DrawScope.idGlyph(accent: Color, r: Rect, withChip: Boolean, photoOnRight: Boolean, isBack: Boolean) {
    val corner = r.height * 0.12f
    val cardPath = Path().apply {
        addRoundRect(RoundRect(r.left, r.top, r.right, r.bottom, CornerRadius(corner, corner)))
    }
    clipPath(cardPath) {
        drawRect(accent.copy(alpha = 0.14f), topLeft = Offset(r.left, r.top), size = Size(r.width, r.height))
        // Header band across the top.
        drawRect(accent, topLeft = Offset(r.left, r.top), size = Size(r.width, r.height * 0.2f))

        val pad = r.width * 0.06f
        if (!isBack) {
            // Photo box with the anonymous silhouette omitted at this scale — a plain white box reads.
            val photoW = r.width * 0.22f
            val photoH = r.height * 0.52f
            val photoLeft = if (photoOnRight) r.right - pad - photoW else r.left + pad
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(photoLeft, r.top + r.height * 0.3f),
                size = Size(photoW, photoH),
                cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
            )
            // Field bars beside the photo.
            val barsLeft = if (photoOnRight) r.left + pad else photoLeft + photoW + pad
            val barsRight = if (photoOnRight) photoLeft - pad else r.right - pad
            val barH = r.height * 0.08f
            var y = r.top + r.height * 0.34f
            for (frac in listOf(1.0f, 0.7f, 0.85f)) {
                roundedBar(accent.copy(alpha = 0.45f), barsLeft, y, (barsRight - barsLeft) * frac, barH)
                y += barH + r.height * 0.08f
            }
            if (withChip) {
                drawRoundRect(
                    color = ChipGold,
                    topLeft = Offset(r.left + pad, r.top + r.height * 0.74f),
                    size = Size(r.width * 0.14f, r.height * 0.14f),
                    cornerRadius = CornerRadius(corner * 0.4f, corner * 0.4f),
                )
            }
        } else {
            // Back: full-width field bars only.
            val barH = r.height * 0.09f
            var y = r.top + r.height * 0.32f
            for (frac in listOf(0.9f, 1.0f, 0.65f)) {
                roundedBar(accent.copy(alpha = 0.45f), r.left + pad, y, (r.width - 2 * pad) * frac, barH)
                y += barH + r.height * 0.09f
            }
        }
    }
    drawRoundRect(
        color = accent.copy(alpha = 0.6f),
        topLeft = Offset(r.left, r.top),
        size = Size(r.width, r.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = (r.width * 0.015f).coerceAtLeast(1f)),
    )
}

/** Custom card: the same two stacked slots as dashed outlines, dimension arrows in the top one. */
private fun DrawScope.drawCustomOutputSheet(accent: Color) {
    val page = pageRect(PAGE_ASPECT)
    drawPage(page, accent)
    val (front, back) = cardSlots(page)
    val dash = PathEffect.dashPathEffect(floatArrayOf(front.width * 0.06f, front.width * 0.045f), 0f)
    val sw = (front.width * 0.018f).coerceAtLeast(1f)
    for (r in listOf(front, back)) {
        drawRoundRect(
            color = accent,
            topLeft = Offset(r.left, r.top),
            size = Size(r.width, r.height),
            cornerRadius = CornerRadius(r.height * 0.12f, r.height * 0.12f),
            style = Stroke(width = sw, pathEffect = dash),
        )
    }
    val head = front.width * 0.07f
    arrowLine(
        accent,
        Offset(front.left + front.width * 0.16f, front.center.y),
        Offset(front.right - front.width * 0.16f, front.center.y),
        sw,
        head,
    )
    arrowLine(
        accent,
        Offset(front.center.x, front.top + front.height * 0.22f),
        Offset(front.center.x, front.bottom - front.height * 0.22f),
        sw,
        head,
    )
}

/** Full document: one large document scaled to fit within the page's (dashed) printable margins. */
private fun DrawScope.drawFullPageDocumentOutput(accent: Color) {
    val page = pageRect(PAGE_ASPECT)
    drawPage(page, accent)
    // Dashed printable-area inset.
    val inset = page.width * 0.07f
    val margin = Rect(page.left + inset, page.top + inset, page.right - inset, page.bottom - inset)
    drawRect(
        color = accent.copy(alpha = 0.5f),
        topLeft = Offset(margin.left, margin.top),
        size = Size(margin.width, margin.height),
        style = Stroke(
            width = (page.width * 0.012f).coerceAtLeast(1f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(page.width * 0.04f, page.width * 0.03f), 0f),
        ),
    )
    // The document fills the margin box (it was scaled to fit, aspect kept).
    documentGlyph(accent, margin.deflate(page.width * 0.025f))
}

/** Small document: a smaller sheet-centred document plus a diagonal resize arrow (adjustable size). */
private fun DrawScope.drawReceiptOutput(accent: Color) {
    val page = pageRect(PAGE_ASPECT)
    drawPage(page, accent)
    val docW = page.width * 0.56f
    val docH = page.height * 0.42f
    val doc = Rect(
        page.left + (page.width - docW) / 2f,
        page.top + (page.height - docH) / 2f,
        page.left + (page.width + docW) / 2f,
        page.top + (page.height + docH) / 2f,
    )
    documentGlyph(accent, doc)
    // Diagonal double-ended arrow from the document's corner toward the page corner: resizable.
    val sw = (page.width * 0.014f).coerceAtLeast(1f)
    arrowLine(
        accent,
        Offset(doc.right + page.width * 0.02f, doc.bottom + page.width * 0.02f),
        Offset(page.right - page.width * 0.06f, page.bottom - page.width * 0.06f),
        sw,
        page.width * 0.05f,
    )
}

/** A generic document: white sheet, accent header band, text bars. */
private fun DrawScope.documentGlyph(accent: Color, r: Rect) {
    val corner = r.width * 0.04f
    val path = Path().apply {
        addRoundRect(RoundRect(r.left, r.top, r.right, r.bottom, CornerRadius(corner, corner)))
    }
    clipPath(path) {
        drawRect(Color.White, topLeft = Offset(r.left, r.top), size = Size(r.width, r.height))
        drawRect(accent, topLeft = Offset(r.left, r.top), size = Size(r.width, r.height * 0.12f))
        val lineLeft = r.left + r.width * 0.1f
        val lineH = r.height * 0.05f
        var y = r.top + r.height * 0.22f
        for (frac in listOf(0.8f, 0.65f, 0.75f, 0.5f, 0.7f)) {
            if (y + lineH > r.bottom - r.height * 0.06f) break
            roundedBar(accent.copy(alpha = 0.35f), lineLeft, y, r.width * 0.8f * frac, lineH)
            y += lineH + r.height * 0.06f
        }
    }
    drawRoundRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(r.left, r.top),
        size = Size(r.width, r.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = (r.width * 0.015f).coerceAtLeast(1f)),
    )
}
