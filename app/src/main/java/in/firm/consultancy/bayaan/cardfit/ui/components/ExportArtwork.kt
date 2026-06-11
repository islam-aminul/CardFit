package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * ORIGINAL, generic, schematic illustrations of what each export OPTION produces, drawn entirely with
 * Compose Canvas primitives (same spirit as [CardArtwork]). They are deliberately abstract OUTCOME
 * diagrams — a page tiled with photo cells, a single image with an upload arrow, a document with text
 * lines — so the user can guess the result of a choice at a glance. They are NOT a preview of the
 * user's actual file and contain no real logos, emblems, or seals (CLAUDE.md sections 2 and 15).
 *
 * Each takes an [accent] colour; the hosting [IllustratedTile] passes a colour that reads correctly on
 * the tile's current (selected / unselected / disabled) background, so the art needs no theme access.
 */

/** Photo PRINT: a portrait sheet tiled with a grid of portrait photo cells (copies to cut). */
@Composable
fun PhotoPrintArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawPhotoSheet(accent) }

/** Photo UPLOAD: a single portrait photo with an upward arrow (one compressed image file). */
@Composable
fun PhotoUploadArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawSingleUpload(accent) }

/** ID/document PRINT: a portrait sheet with both card sides stacked and centred at true size. */
@Composable
fun CardPrintArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawCardSheet(accent) }

/** ID/document UPLOAD: the two card sides cropped to content with an upward arrow (compressed file). */
@Composable
fun CardUploadArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawCardsUpload(accent) }

/** PDF format: a document page with a folded corner and selectable text lines. */
@Composable
fun PdfArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawPdf(accent) }

/** JPEG format: a framed image with a sun and mountains (a single flat raster image). */
@Composable
fun JpegArt(accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawJpeg(accent) }

/** A paper size: a page rectangle scaled to its true [ratio] (width / height) so shapes differ. */
@Composable
fun PaperArt(ratio: Float, accent: Color, modifier: Modifier = Modifier) =
    Canvas(modifier) { drawPaper(ratio, accent) }

// --- drawing helpers (millimetre-agnostic; everything is relative to the canvas size) ---

/** Largest rectangle of the given aspect (w/h) that fits centred within [heightFraction] of height. */
private fun DrawScope.pageRect(aspect: Float, heightFraction: Float = 0.92f): Rect {
    val maxH = size.height * heightFraction
    val maxW = size.width * 0.92f
    var ph = maxH
    var pw = ph * aspect
    if (pw > maxW) {
        pw = maxW
        ph = pw / aspect
    }
    val left = (size.width - pw) / 2f
    val top = (size.height - ph) / 2f
    return Rect(left, top, left + pw, top + ph)
}

/** A faintly-filled page outline (theme-agnostic: tint + stroke in the accent colour). */
private fun DrawScope.drawPage(r: Rect, accent: Color) {
    val corner = r.width * 0.05f
    drawRoundRect(
        color = accent.copy(alpha = 0.08f),
        topLeft = Offset(r.left, r.top),
        size = Size(r.width, r.height),
        cornerRadius = CornerRadius(corner, corner),
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.6f),
        topLeft = Offset(r.left, r.top),
        size = Size(r.width, r.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = (r.width * 0.02f).coerceAtLeast(1.5f)),
    )
}

private fun DrawScope.roundedBar(color: Color, left: Float, top: Float, width: Float, height: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f),
    )
}

/** A small portrait "photo": tinted box, outline, and an anonymous head-and-shoulders silhouette. */
private fun DrawScope.photoCell(accent: Color, x: Float, y: Float, w: Float, h: Float) {
    val corner = w * 0.12f
    drawRoundRect(accent.copy(alpha = 0.30f), Offset(x, y), Size(w, h), CornerRadius(corner, corner))
    val cx = x + w / 2f
    val headR = w * 0.17f
    val headCy = y + h * 0.36f
    clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + w, y + h, CornerRadius(corner, corner))) }) {
        drawCircle(accent.copy(alpha = 0.7f), headR, Offset(cx, headCy))
        drawArc(
            color = accent.copy(alpha = 0.7f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - w * 0.27f, headCy + headR * 0.5f),
            size = Size(w * 0.54f, h * 0.5f),
        )
    }
}

/** An upward arrow centred on [cx], from [bottom] up to [top]. */
private fun DrawScope.upArrow(accent: Color, cx: Float, top: Float, bottom: Float) {
    val sw = (size.width * 0.03f).coerceAtLeast(2f)
    val head = size.width * 0.06f
    drawLine(accent, Offset(cx, bottom), Offset(cx, top), sw)
    drawLine(accent, Offset(cx, top), Offset(cx - head, top + head), sw)
    drawLine(accent, Offset(cx, top), Offset(cx + head, top + head), sw)
}

private fun DrawScope.drawPhotoSheet(accent: Color) {
    val r = pageRect(210f / 297f)
    drawPage(r, accent)
    val cols = 3
    val rows = 4
    val padX = r.width * 0.10f
    val padY = r.height * 0.08f
    val gapX = r.width * 0.06f
    val gapY = r.height * 0.05f
    val cellW = (r.width - 2 * padX - (cols - 1) * gapX) / cols
    val cellH = (r.height - 2 * padY - (rows - 1) * gapY) / rows
    var cy = r.top + padY
    for (row in 0 until rows) {
        var cx = r.left + padX
        for (col in 0 until cols) {
            photoCell(accent, cx, cy, cellW, cellH)
            cx += cellW + gapX
        }
        cy += cellH + gapY
    }
}

private fun DrawScope.drawSingleUpload(accent: Color) {
    val w = size.width
    val h = size.height
    val ph = h * 0.58f
    val pw = ph * (35f / 45f) // passport-ish portrait
    val left = (w - pw) / 2f
    val top = h * 0.36f
    photoCell(accent, left, top, pw, ph)
    upArrow(accent, w / 2f, top = h * 0.08f, bottom = top - h * 0.05f)
}

/** A mini landscape ID card: tinted body, outline, small photo box, and field bars. */
private fun DrawScope.cardGlyph(accent: Color, x: Float, y: Float, w: Float, h: Float) {
    val corner = h * 0.14f
    drawRoundRect(accent.copy(alpha = 0.22f), Offset(x, y), Size(w, h), CornerRadius(corner, corner))
    drawRoundRect(
        color = accent.copy(alpha = 0.6f),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = (w * 0.018f).coerceAtLeast(1f)),
    )
    val pw = w * 0.2f
    val phh = h * 0.55f
    drawRoundRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = Offset(x + w * 0.08f, y + (h - phh) / 2f),
        size = Size(pw, phh),
        cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
    )
    val lx = x + w * 0.36f
    val lineH = h * 0.12f
    var ly = y + h * 0.22f
    for (frac in listOf(0.85f, 0.6f, 0.72f)) {
        roundedBar(accent.copy(alpha = 0.5f), lx, ly, (w * 0.5f) * frac, lineH)
        ly += lineH + h * 0.1f
    }
}

private fun DrawScope.drawCardSheet(accent: Color) {
    val r = pageRect(210f / 297f)
    drawPage(r, accent)
    val cardW = r.width * 0.62f
    val cardH = cardW * (54f / 85.6f)
    val cx = r.left + (r.width - cardW) / 2f
    val gap = cardH * 0.5f
    val totalH = cardH * 2 + gap
    var cy = r.top + (r.height - totalH) / 2f
    repeat(2) {
        cardGlyph(accent, cx, cy, cardW, cardH)
        cy += cardH + gap
    }
}

private fun DrawScope.drawCardsUpload(accent: Color) {
    val w = size.width
    val h = size.height
    val cardW = w * 0.5f
    val cardH = cardW * (54f / 85.6f)
    val gap = cardH * 0.4f
    val totalH = cardH * 2 + gap
    val cx = (w - cardW) / 2f
    var cy = h * 0.34f
    repeat(2) {
        cardGlyph(accent, cx, cy, cardW, cardH)
        cy += cardH + gap
    }
    upArrow(accent, w / 2f, top = h * 0.07f, bottom = cy - totalH - h * 0.04f)
}

private fun DrawScope.drawPdf(accent: Color) {
    val r = pageRect(0.78f, heightFraction = 0.9f)
    val fold = r.width * 0.28f
    val outline = Path().apply {
        moveTo(r.left, r.top)
        lineTo(r.right - fold, r.top)
        lineTo(r.right, r.top + fold)
        lineTo(r.right, r.bottom)
        lineTo(r.left, r.bottom)
        close()
    }
    drawPath(outline, accent.copy(alpha = 0.1f))
    drawPath(outline, accent.copy(alpha = 0.6f), style = Stroke(width = (r.width * 0.025f).coerceAtLeast(1.5f)))
    // Folded corner.
    val foldTri = Path().apply {
        moveTo(r.right - fold, r.top)
        lineTo(r.right, r.top + fold)
        lineTo(r.right - fold, r.top + fold)
        close()
    }
    drawPath(foldTri, accent.copy(alpha = 0.3f))
    // Text lines (selectable layer).
    val lx = r.left + r.width * 0.16f
    val lineH = r.height * 0.06f
    var ly = r.top + r.height * 0.42f
    for (frac in listOf(0.85f, 0.7f, 0.8f, 0.55f)) {
        roundedBar(accent.copy(alpha = 0.5f), lx, ly, (r.width * 0.68f) * frac, lineH)
        ly += lineH + r.height * 0.07f
    }
}

private fun DrawScope.drawJpeg(accent: Color) {
    val r = pageRect(1.3f, heightFraction = 0.82f)
    val corner = r.width * 0.05f
    drawRoundRect(accent.copy(alpha = 0.1f), Offset(r.left, r.top), Size(r.width, r.height), CornerRadius(corner, corner))
    clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(r.left, r.top, r.right, r.bottom, CornerRadius(corner, corner))) }) {
        // Sun.
        drawCircle(accent.copy(alpha = 0.7f), r.width * 0.08f, Offset(r.left + r.width * 0.28f, r.top + r.height * 0.3f))
        // Mountains.
        val base = r.bottom - r.height * 0.16f
        val m1 = Path().apply {
            moveTo(r.left + r.width * 0.12f, base)
            lineTo(r.left + r.width * 0.42f, r.top + r.height * 0.48f)
            lineTo(r.left + r.width * 0.66f, base)
            close()
        }
        drawPath(m1, accent.copy(alpha = 0.5f))
        val m2 = Path().apply {
            moveTo(r.left + r.width * 0.5f, base)
            lineTo(r.left + r.width * 0.72f, r.top + r.height * 0.58f)
            lineTo(r.left + r.width * 0.92f, base)
            close()
        }
        drawPath(m2, accent.copy(alpha = 0.35f))
    }
    drawRoundRect(
        color = accent.copy(alpha = 0.6f),
        topLeft = Offset(r.left, r.top),
        size = Size(r.width, r.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = (r.width * 0.02f).coerceAtLeast(1.5f)),
    )
}

private fun DrawScope.drawPaper(ratio: Float, accent: Color) {
    val r = pageRect(ratio, heightFraction = 0.9f)
    drawPage(r, accent)
    val lx = r.left + r.width * 0.16f
    val lineH = r.height * 0.05f
    var ly = r.top + r.height * 0.2f
    for (frac in listOf(0.75f, 0.55f, 0.65f)) {
        roundedBar(accent.copy(alpha = 0.4f), lx, ly, (r.width * 0.68f) * frac, lineH)
        ly += lineH + r.height * 0.09f
    }
}
