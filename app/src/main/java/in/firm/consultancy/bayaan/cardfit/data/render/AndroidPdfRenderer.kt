package `in`.firm.consultancy.bayaan.cardfit.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import `in`.firm.consultancy.bayaan.cardfit.data.PdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.domain.PageLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
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
 */
class AndroidPdfRenderer(private val context: Context) : PdfRenderer {

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput =
        withContext(Dispatchers.Default) {
            val front = decodeSampledBitmap(context, requireNotNull(session.front).imageUri)
                ?: error("Could not read the front image")
            val back = session.back?.let { decodeSampledBitmap(context, it.imageUri) }
            val sides = listOfNotNull(front, back)
            try {
                val layout = planLayout(session, config, sides)
                val paint = cardPaint(config.grayscale)
                val pageWidthPt = Units.mmToPoints(layout.pageWidthMm).roundToInt().coerceAtLeast(1)
                val pageHeightPt = Units.mmToPoints(layout.pageHeightMm).roundToInt().coerceAtLeast(1)

                if (config.mode == OutputMode.UPLOAD) {
                    renderUploadPdf(layout, sides, paint, config, pageWidthPt, pageHeightPt)
                } else {
                    renderPrintPdf(layout, sides, paint, config, pageWidthPt, pageHeightPt)
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
}
