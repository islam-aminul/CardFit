package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
 * ORIGINAL, generic, stylized card illustrations drawn entirely with Compose Canvas primitives.
 *
 * These are deliberately abstract: a tinted card body, an anonymous person silhouette, and abstract
 * "field" bars. They intentionally contain NO real government logos, emblems, holograms, seals, or
 * identifying marks of any actual ID (CLAUDE.md sections 2 and 15). Each card type gets a distinct
 * accent colour and a small layout variation so the tiles are recognisable without copying anything.
 */

// Original accent palette (not tied to any real document's colours).
private val PanAccent = Color(0xFF1565C0) // blue
private val AadhaarAccent = Color(0xFF00897B) // teal
private val EpicAccent = Color(0xFF3949AB) // indigo
private val AdmitAccent = Color(0xFFEF6C00) // orange
private val CustomAccent = Color(0xFF546E7A) // blue-grey
private val FreeAccent = Color(0xFF2E7D32) // green
private val ChipGold = Color(0xFFCBA135)

@Composable
fun CardArtwork(type: CardType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        when (type) {
            CardType.PAN -> drawIdCard(PanAccent, withChip = true, photoOnRight = false)
            CardType.AADHAAR -> drawIdCard(AadhaarAccent, withChip = false, photoOnRight = true)
            CardType.EPIC -> drawIdCard(EpicAccent, withChip = false, photoOnRight = false)
            CardType.ADMIT_CARD -> drawDocumentPage(AdmitAccent)
            CardType.CUSTOM -> drawCustomCard(CustomAccent)
            CardType.FREE -> drawFreeCard(FreeAccent)
        }
    }
}

private fun DrawScope.cardPath(w: Float, h: Float, corner: Float): Path =
    Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(corner, corner))) }

private fun DrawScope.roundedBar(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f),
    )
}

/** A generic horizontal ID card: tinted body, header band, photo with silhouette, field bars. */
private fun DrawScope.drawIdCard(accent: Color, withChip: Boolean, photoOnRight: Boolean) {
    val w = size.width
    val h = size.height
    val corner = h * 0.09f
    clipPath(cardPath(w, h, corner)) {
        // Card body and header band.
        drawRect(accent.copy(alpha = 0.12f), size = Size(w, h))
        drawRect(accent, size = Size(w, h * 0.20f))

        val pad = w * 0.06f
        val photoW = w * 0.26f
        val photoH = h * 0.52f
        val photoTop = h * 0.30f
        val photoLeft = if (photoOnRight) w - pad - photoW else pad

        // Photo placeholder with anonymous silhouette.
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(photoLeft, photoTop),
            size = Size(photoW, photoH),
            cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
        )
        val cx = photoLeft + photoW / 2f
        val headR = photoW * 0.22f
        val headCy = photoTop + photoH * 0.32f
        drawCircle(accent.copy(alpha = 0.55f), headR, Offset(cx, headCy))
        drawArc(
            color = accent.copy(alpha = 0.55f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - photoW * 0.30f, headCy + headR * 0.5f),
            size = Size(photoW * 0.60f, photoH * 0.55f),
        )

        // Field bars beside the photo.
        val barsLeft = if (photoOnRight) pad else photoLeft + photoW + w * 0.06f
        val barsRight = if (photoOnRight) photoLeft - w * 0.06f else w - pad
        val barsW = barsRight - barsLeft
        val barH = h * 0.07f
        var by = h * 0.34f
        for (frac in listOf(1.0f, 0.75f, 0.55f, 0.9f)) {
            roundedBar(accent.copy(alpha = 0.45f), barsLeft, by, barsW * frac, barH)
            by += barH + h * 0.06f
        }

        // Optional chip motif (generic, not a real smart-card layout).
        if (withChip) {
            drawRoundRect(
                color = ChipGold,
                topLeft = Offset(pad, h * 0.78f),
                size = Size(w * 0.14f, h * 0.13f),
                cornerRadius = CornerRadius(corner * 0.4f, corner * 0.4f),
            )
        }
    }
}

/** A generic portrait document/page for admit cards (fit-to-page). */
private fun DrawScope.drawDocumentPage(accent: Color) {
    val w = size.width
    val h = size.height
    // Background tint.
    drawRect(accent.copy(alpha = 0.10f), size = Size(w, h))

    val pageW = w * 0.52f
    val pageH = h * 0.86f
    val pageLeft = (w - pageW) / 2f
    val pageTop = (h - pageH) / 2f
    val corner = pageW * 0.06f

    val pagePath = Path().apply {
        addRoundRect(RoundRect(pageLeft, pageTop, pageLeft + pageW, pageTop + pageH, CornerRadius(corner, corner)))
    }
    clipPath(pagePath) {
        drawRect(Color.White, topLeft = Offset(pageLeft, pageTop), size = Size(pageW, pageH))
        // Header band.
        drawRect(accent, topLeft = Offset(pageLeft, pageTop), size = Size(pageW, pageH * 0.14f))
        // Small photo box top-right.
        val photoW = pageW * 0.24f
        drawRoundRect(
            color = accent.copy(alpha = 0.25f),
            topLeft = Offset(pageLeft + pageW - photoW - pageW * 0.08f, pageTop + pageH * 0.20f),
            size = Size(photoW, photoW * 1.2f),
            cornerRadius = CornerRadius(corner * 0.4f, corner * 0.4f),
        )
        // Text lines.
        val lineLeft = pageLeft + pageW * 0.10f
        val lineH = pageH * 0.045f
        var ly = pageTop + pageH * 0.22f
        for (frac in listOf(0.45f, 0.4f, 0.5f, 0.0f, 0.8f, 0.7f, 0.85f, 0.6f)) {
            if (frac > 0f) roundedBar(accent.copy(alpha = 0.35f), lineLeft, ly, pageW * 0.80f * frac, lineH)
            ly += lineH + pageH * 0.055f
        }
    }
}

/** A card outline with dashed border and dimension arrows, implying a user-defined size. */
private fun DrawScope.drawCustomCard(accent: Color) {
    val w = size.width
    val h = size.height
    val corner = h * 0.09f
    drawRect(accent.copy(alpha = 0.08f), size = Size(w, h))

    val dash = PathEffect.dashPathEffect(floatArrayOf(w * 0.04f, w * 0.03f), 0f)
    drawRoundRect(
        color = accent,
        topLeft = Offset(w * 0.08f, h * 0.10f),
        size = Size(w * 0.84f, h * 0.80f),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = w * 0.012f, pathEffect = dash),
    )

    val strokeW = w * 0.012f
    // Horizontal double arrow.
    val hy = h * 0.5f
    arrowLine(accent, Offset(w * 0.22f, hy), Offset(w * 0.78f, hy), strokeW, w * 0.05f)
    // Vertical double arrow.
    val vx = w * 0.5f
    arrowLine(accent, Offset(vx, h * 0.26f), Offset(vx, h * 0.74f), strokeW, w * 0.05f)
}

/** A loose, free-form layout: dashed border with content blocks placed freely. */
private fun DrawScope.drawFreeCard(accent: Color) {
    val w = size.width
    val h = size.height
    val corner = h * 0.09f
    drawRect(accent.copy(alpha = 0.10f), size = Size(w, h))

    val dash = PathEffect.dashPathEffect(floatArrayOf(w * 0.04f, w * 0.03f), 0f)
    drawRoundRect(
        color = accent,
        topLeft = Offset(w * 0.08f, h * 0.10f),
        size = Size(w * 0.84f, h * 0.80f),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = w * 0.012f, pathEffect = dash),
    )
    // Freely-placed content blocks.
    drawRoundRect(
        color = accent.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.18f, h * 0.26f),
        size = Size(w * 0.30f, h * 0.22f),
        cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.25f),
        topLeft = Offset(w * 0.50f, h * 0.42f),
        size = Size(w * 0.32f, h * 0.30f),
        cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
    )
    drawCircle(accent.copy(alpha = 0.45f), w * 0.07f, Offset(w * 0.30f, h * 0.66f))
}

/** Draws a straight line with arrowheads at both ends. */
private fun DrawScope.arrowLine(color: Color, start: Offset, end: Offset, strokeWidth: Float, headSize: Float) {
    drawLine(color, start, end, strokeWidth)
    arrowHead(color, tip = start, towards = end, strokeWidth, headSize)
    arrowHead(color, tip = end, towards = start, strokeWidth, headSize)
}

private fun DrawScope.arrowHead(color: Color, tip: Offset, towards: Offset, strokeWidth: Float, headSize: Float) {
    val dx = towards.x - tip.x
    val dy = towards.y - tip.y
    val len = kotlin.math.hypot(dx, dy).coerceAtLeast(0.0001f)
    val ux = dx / len
    val uy = dy / len
    // Perpendicular unit vector.
    val px = -uy
    val py = ux
    val baseX = tip.x + ux * headSize
    val baseY = tip.y + uy * headSize
    drawLine(color, tip, Offset(baseX + px * headSize * 0.6f, baseY + py * headSize * 0.6f), strokeWidth)
    drawLine(color, tip, Offset(baseX - px * headSize * 0.6f, baseY - py * headSize * 0.6f), strokeWidth)
}
