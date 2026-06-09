package `in`.firm.consultancy.bayaan.cardfit.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import `in`.firm.consultancy.bayaan.cardfit.data.Ocr
import `in`.firm.consultancy.bayaan.cardfit.data.PdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.domain.OcrTextLayer
import `in`.firm.consultancy.bayaan.cardfit.domain.PageLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.TextLayerFilter
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.mapImageRectToPage
import `in`.firm.consultancy.bayaan.cardfit.domain.PtRect
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.targetJpegSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * PDF renderer (CLAUDE.md sections 6–8) using the framework [PdfDocument] (no iText/PDFBox).
 *
 *  - PRINT: page sized to the layout in points; each card drawn at its exact physical size, with
 *    optional 0.3pt corner crop-mark ticks just outside each card.
 *  - UPLOAD: FIT_WIDTH layout composed to a single bitmap, JPEG-compressed under the size cap (with
 *    a small overhead margin for PDF structure), embedded into the page.
 *
 * When [RenderConfig.searchableText] is set, an invisible (alpha-0) but real, selectable/searchable
 * OCR text layer is drawn over each side (Phase 11). OCR runs on each placed side on a background
 * dispatcher, and the recognized elements pass through [TextLayerFilter] (the masking hook) before
 * being written — raw OCR is never embedded directly.
 */
class AndroidPdfRenderer(
    private val context: Context,
    private val ocr: Ocr,
    private val textFilter: TextLayerFilter,
) : PdfRenderer {

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput =
        withContext(Dispatchers.Default) {
            val front = decodeSampledBitmap(context, requireNotNull(session.front).imageUri)
                ?: error("Could not read the front image")
            val back = session.back?.let { decodeSampledBitmap(context, it.imageUri) }
            val sides = listOfNotNull(front, back)

            // OCR each placed side (front, then back) for the optional text layer. Off the main
            // thread via recognizeLayer; never logged.
            val sideUris = listOfNotNull(session.front?.imageUri, session.back?.imageUri)
            val ocrLayers: List<OcrTextLayer> = if (config.searchableText) {
                sideUris.map { ocr.recognizeLayer(it) }
            } else {
                emptyList()
            }

            try {
                val layout = planLayout(session, config, sides)
                val paint = cardPaint(config.grayscale)
                val pageWidthPt = Units.mmToPoints(layout.pageWidthMm).roundToInt().coerceAtLeast(1)
                val pageHeightPt = Units.mmToPoints(layout.pageHeightMm).roundToInt().coerceAtLeast(1)

                if (config.mode == OutputMode.UPLOAD) {
                    renderUploadPdf(layout, sides, paint, config, pageWidthPt, pageHeightPt, ocrLayers, session.cardType)
                } else {
                    renderPrintPdf(layout, sides, paint, config, pageWidthPt, pageHeightPt, ocrLayers, session.cardType)
                }
            } finally {
                front.recycle()
                back?.recycle()
            }
        }

    private fun renderPrintPdf(
        layout: PageLayout,
        sides: List<Bitmap>,
        paint: Paint,
        config: RenderConfig,
        pageWidthPt: Int,
        pageHeightPt: Int,
        ocrLayers: List<OcrTextLayer>,
        cardType: CardType,
    ): RenderedOutput {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        layout.cards.forEachIndexed { i, rect ->
            val src = sides[i]
            val dst = RectF(
                mmToPtF(rect.xMm),
                mmToPtF(rect.yMm),
                mmToPtF(rect.xMm + rect.widthMm),
                mmToPtF(rect.yMm + rect.heightMm),
            )
            canvas.drawBitmap(src, centerCropSrcRect(src, dst.width(), dst.height()), dst, paint)
            if (config.cropMarks) drawCropMarks(canvas, dst)
        }

        drawTextLayer(canvas, layout, ocrLayers, cardType)

        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return RenderedOutput(out.toByteArray())
    }

    private fun renderUploadPdf(
        layout: PageLayout,
        sides: List<Bitmap>,
        paint: Paint,
        config: RenderConfig,
        pageWidthPt: Int,
        pageHeightPt: Int,
        ocrLayers: List<OcrTextLayer>,
        cardType: CardType,
    ): RenderedOutput {
        var cachedDpi = -1
        var cached: Bitmap? = null
        fun pageAt(dpi: Int): Bitmap {
            if (dpi != cachedDpi || cached == null) {
                cached?.recycle()
                cached = composePageBitmap(layout, sides, dpi, paint)
                cachedDpi = dpi
            }
            return cached!!
        }

        try {
            val capBytes = config.maxFileSizeKb?.let { it * 1024 }
            val dpi: Int
            val quality: Int
            var warning: String? = null

            if (capBytes == null) {
                dpi = config.dpi
                quality = PRINT_JPEG_QUALITY
            } else {
                // Leave headroom for the PDF wrapper around the embedded JPEG.
                val overhead = (capBytes * 0.05).toInt().coerceIn(1024, 8192)
                val target = targetJpegSize(
                    maxBytes = capBytes,
                    startDpi = config.dpi,
                    overheadBytes = overhead,
                    compress = { d, q -> jpegByteCount(pageAt(d), q) },
                )
                dpi = target.dpi
                quality = target.quality
                if (target.belowFloor) {
                    warning = "Couldn't reach the ${config.maxFileSizeKb} KB cap for the PDF; saved " +
                        "the smallest legible version (~${target.dpi} dpi)."
                }
            }

            // Bake the chosen JPEG compression into the embedded image, then place it on the page.
            val composed = pageAt(dpi)
            val jpeg = jpegBytes(composed, quality)
            val baked = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: composed
            try {
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                val dst = RectF(0f, 0f, pageWidthPt.toFloat(), pageHeightPt.toFloat())
                page.canvas.drawBitmap(baked, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                drawTextLayer(page.canvas, layout, ocrLayers, cardType)
                document.finishPage(page)
                val out = ByteArrayOutputStream()
                document.writeTo(out)
                document.close()
                return RenderedOutput(out.toByteArray(), warning)
            } finally {
                if (baked !== composed) baked.recycle()
            }
        } finally {
            cached?.recycle()
        }
    }

    /** Thin 0.3pt corner ticks placed just outside each card corner (print crop marks). */
    private fun drawCropMarks(canvas: Canvas, rect: RectF) {
        val mark = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.3f
            isAntiAlias = true
        }
        val len = 6f
        val gap = 2f
        // Top-left
        canvas.drawLine(rect.left - gap - len, rect.top, rect.left - gap, rect.top, mark)
        canvas.drawLine(rect.left, rect.top - gap - len, rect.left, rect.top - gap, mark)
        // Top-right
        canvas.drawLine(rect.right + gap, rect.top, rect.right + gap + len, rect.top, mark)
        canvas.drawLine(rect.right, rect.top - gap - len, rect.right, rect.top - gap, mark)
        // Bottom-left
        canvas.drawLine(rect.left - gap - len, rect.bottom, rect.left - gap, rect.bottom, mark)
        canvas.drawLine(rect.left, rect.bottom + gap, rect.left, rect.bottom + gap + len, mark)
        // Bottom-right
        canvas.drawLine(rect.right + gap, rect.bottom, rect.right + gap + len, rect.bottom, mark)
        canvas.drawLine(rect.right, rect.bottom + gap, rect.right, rect.bottom + gap + len, mark)
    }

    /**
     * Draw the invisible OCR text layer for each placed side. Each side maps its own boxes into its
     * own layout rect (in points) via [mapImageRectToPage], so text aligns with that side's picture.
     * Elements pass through [textFilter] first (masking hook). Drawing real text (alpha-0) keeps it
     * selectable/searchable while invisible.
     */
    private fun drawTextLayer(
        canvas: Canvas,
        layout: PageLayout,
        ocrLayers: List<OcrTextLayer>,
        cardType: CardType,
    ) {
        if (ocrLayers.isEmpty()) return
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.TRANSPARENT // alpha 0: real text in the content stream, but invisible
        }
        layout.cards.forEachIndexed { i, rect ->
            val layer = ocrLayers.getOrNull(i) ?: return@forEachIndexed
            if (layer.imageWidthPx <= 0 || layer.imageHeightPx <= 0) return@forEachIndexed
            val sideRect = PtRect(
                left = mmToPtF(rect.xMm).toDouble(),
                top = mmToPtF(rect.yMm).toDouble(),
                right = mmToPtF(rect.xMm + rect.widthMm).toDouble(),
                bottom = mmToPtF(rect.yMm + rect.heightMm).toDouble(),
            )
            for (element in textFilter.filter(layer.elements, cardType)) {
                val mapped = mapImageRectToPage(element.box, layer.imageWidthPx, layer.imageHeightPx, sideRect)
                drawInvisibleText(canvas, element.text, mapped, textPaint)
            }
        }
    }

    /** Draw [text] as real (selectable) but invisible glyphs, sized/stretched to fill [rect]. */
    private fun drawInvisibleText(canvas: Canvas, text: String, rect: PtRect, paint: Paint) {
        val height = rect.height.toFloat()
        val width = rect.width.toFloat()
        if (height <= 0f || width <= 0f || text.isEmpty()) return

        paint.textScaleX = 1f
        paint.textSize = height
        val measured = paint.measureText(text)
        if (measured > 0f) {
            paint.textScaleX = (width / measured).coerceIn(0.05f, 20f)
        }
        // Baseline near the bottom of the mapped box so the run sits over the visible word.
        canvas.drawText(text, rect.left.toFloat(), rect.bottom.toFloat(), paint)
    }
}
