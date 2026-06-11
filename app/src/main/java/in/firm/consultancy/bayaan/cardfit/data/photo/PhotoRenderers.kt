package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.createBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.data.render.PRINT_JPEG_QUALITY
import `in`.firm.consultancy.bayaan.cardfit.data.render.centerCropSrcRect
import `in`.firm.consultancy.bayaan.cardfit.data.render.decodeSampledBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.render.jpegByteCount
import `in`.firm.consultancy.bayaan.cardfit.data.render.jpegBytes
import `in`.firm.consultancy.bayaan.cardfit.data.render.mmToPtF
import `in`.firm.consultancy.bayaan.cardfit.data.render.writeJpegDpi
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoGrid
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.targetJpegSize
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Android renderers for the photo flow (CLAUDE.md Phase 13). Memory-conscious like the document
 * renderers: the edited source photo is decoded once and reused for every grid cell; the page
 * canvas is RGB_565; size probes use a counting stream. Shared helpers come from `data.render`.
 *
 *  - UPLOAD: a single JPEG sized to exact pixels (`mm * dpi / 25.4`), optional max-KB cap via the
 *    shared [targetJpegSize] loop (same legibility floor + warning), DPI written via ExifInterface.
 *  - PRINT: a SINGLE-PAGE PDF grid of exactly `finalCount` photos at exact physical size, anchored to
 *    the top margin, each framed by a thin light-gray border, drawn with the framework `PdfDocument`.
 */
internal object PhotoRenderers {

    /** Largest dimension to decode the edited photo at; ample for a full-page print grid. */
    private const val PHOTO_MAX_DIM = 2400

    fun renderUploadJpeg(
        context: Context,
        editedUri: String,
        widthMm: Double,
        heightMm: Double,
        dpi: Int,
        maxFileSizeKb: Int?,
    ): RenderedOutput {
        val photo = decodeSampledBitmap(context, editedUri, PHOTO_MAX_DIM)
            ?: error("Could not read the edited photo")
        try {
            var cachedDpi = -1
            var cached: Bitmap? = null
            fun pageAt(d: Int): Bitmap {
                if (d != cachedDpi || cached == null) {
                    cached?.recycle()
                    cached = composePhoto(photo, widthMm, heightMm, d)
                    cachedDpi = d
                }
                return cached!!
            }
            try {
                val capBytes = maxFileSizeKb?.let { it * 1024 }
                if (capBytes == null) {
                    val page = pageAt(dpi)
                    val bytes = jpegBytes(page, PRINT_JPEG_QUALITY)
                    return RenderedOutput(writeJpegDpi(context, bytes, dpi))
                }
                val target = targetJpegSize(
                    maxBytes = capBytes,
                    startDpi = dpi,
                    compress = { d, q -> jpegByteCount(pageAt(d), q) },
                )
                val finalPage = pageAt(target.dpi)
                val bytes = jpegBytes(finalPage, target.quality)
                val tagged = writeJpegDpi(context, bytes, target.dpi)
                val warning = if (target.belowFloor) {
                    "Couldn't reach the $maxFileSizeKb KB cap; saved the smallest legible " +
                        "version (~${target.dpi} dpi)."
                } else {
                    null
                }
                return RenderedOutput(tagged, warning)
            } finally {
                cached?.recycle()
            }
        } finally {
            photo.recycle()
        }
    }

    /** Compose a single photo, center-cropped (never stretched) to exact pixels, on white. */
    private fun composePhoto(photo: Bitmap, widthMm: Double, heightMm: Double, dpi: Int): Bitmap {
        val wPx = Units.mmToPixels(widthMm, dpi).coerceAtLeast(1)
        val hPx = Units.mmToPixels(heightMm, dpi).coerceAtLeast(1)
        val bmp = createBitmap(wPx, hPx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
        val dst = RectF(0f, 0f, wPx.toFloat(), hPx.toFloat())
        canvas.drawBitmap(photo, centerCropSrcRect(photo, dst.width(), dst.height()), dst, paint)
        return bmp
    }

    fun renderPrintPdf(
        context: Context,
        editedUri: String,
        widthMm: Double,
        heightMm: Double,
        grid: PhotoGrid,
        finalCount: Int,
        cutMarks: Boolean,
    ): RenderedOutput {
        val photo = decodeSampledBitmap(context, editedUri, PHOTO_MAX_DIM)
            ?: error("Could not read the edited photo")
        try {
            val usedRows = if (grid.perRow <= 0) 0 else (finalCount + grid.perRow - 1) / grid.perRow
            val cells = grid.cells(usedRows).take(finalCount)

            val pageWidthPt = Units.mmToPoints(grid.paperWidthMm).roundToInt().coerceAtLeast(1)
            val pageHeightPt = Units.mmToPoints(grid.paperHeightMm).roundToInt().coerceAtLeast(1)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true; isDither = true }

            for (cell in cells) {
                val dst = RectF(
                    mmToPtF(cell.xMm),
                    mmToPtF(cell.yMm),
                    mmToPtF(cell.xMm + widthMm),
                    mmToPtF(cell.yMm + heightMm),
                )
                canvas.drawBitmap(photo, centerCropSrcRect(photo, dst.width(), dst.height()), dst, paint)
                if (cutMarks) drawPhotoBorder(canvas, dst)
            }

            document.finishPage(page)
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            document.close()
            return RenderedOutput(out.toByteArray())
        } finally {
            photo.recycle()
        }
    }

    /** A thin, continuous light-gray border around all four sides of each photo (cut guide). */
    private fun drawPhotoBorder(canvas: Canvas, rect: RectF) {
        val border = Paint().apply {
            color = Color.rgb(200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }
        canvas.drawRect(rect, border)
    }
}
