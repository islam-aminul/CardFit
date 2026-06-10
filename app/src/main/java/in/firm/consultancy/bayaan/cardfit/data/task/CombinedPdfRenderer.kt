package `in`.firm.consultancy.bayaan.cardfit.data.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.createBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.data.render.cardPaint
import `in`.firm.consultancy.bayaan.cardfit.data.render.centerCropSrcRect
import `in`.firm.consultancy.bayaan.cardfit.data.render.composePageBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.render.decodeSampledBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.render.jpegByteCount
import `in`.firm.consultancy.bayaan.cardfit.data.render.jpegBytes
import `in`.firm.consultancy.bayaan.cardfit.data.render.mmToPxF
import `in`.firm.consultancy.bayaan.cardfit.data.render.planLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.targetCombinedSize
import `in`.firm.consultancy.bayaan.cardfit.domain.task.DocumentEntry
import `in`.firm.consultancy.bayaan.cardfit.domain.task.EntryKind
import `in`.firm.consultancy.bayaan.cardfit.domain.task.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Combined multi-page PDF for a task (CLAUDE.md Phase 14, item 5): one page per entry, each laid out
 * with its existing layout — PRINT = actual-size pages, UPLOAD = fit-to-width pages. When an upload
 * cap is set, a GLOBAL size loop ([targetCombinedSize]) picks ONE shared JPEG quality for every
 * embedded image (stepping DPI down ~15 % when needed, never below the legibility floor, with the
 * same best-effort warning as single documents).
 *
 * Memory-conscious (CLAUDE.md §12): only one page bitmap is held at a time — the size probes and the
 * final pass each (re)compose entry pages one by one and recycle immediately.
 */
class CombinedPdfRenderer(private val context: Context) {

    private val paper = PaperSize.A4
    private val marginMm = Defaults.UPLOAD_MARGIN_MM

    suspend fun render(task: Task, mode: OutputMode, maxFileSizeKb: Int?): RenderedOutput =
        withContext(Dispatchers.Default) {
            val entries = task.documents.filter { it.frontImage != null }
            require(entries.isNotEmpty()) { "Task has no documents to export" }

            val dpi: Int
            val quality: Int
            var warning: String? = null

            val capBytes = if (mode == OutputMode.UPLOAD) maxFileSizeKb?.let { it * 1024 } else null
            if (capBytes != null) {
                // Reserve structural overhead, then choose one shared quality for all images.
                val overhead = (capBytes * 0.05).toInt().coerceIn(2048, 16384)
                val target = targetCombinedSize(
                    maxBytes = capBytes,
                    startDpi = Defaults.UPLOAD_DPI,
                    overheadBytes = overhead,
                    compressTotal = { d, q -> totalBytes(task.id, entries, mode, d, q) },
                )
                dpi = target.dpi
                quality = target.quality
                if (target.belowFloor) {
                    warning = "Couldn't reach the $maxFileSizeKb KB cap for the combined PDF; saved the " +
                        "smallest legible version (~${target.dpi} dpi)."
                }
            } else {
                dpi = if (mode == OutputMode.PRINT) Defaults.PRINT_DPI else Defaults.UPLOAD_DPI
                quality = PRINT_JPEG_QUALITY
            }

            val bytes = buildPdf(task.id, entries, mode, dpi, quality)
            RenderedOutput(bytes, warning)
        }

    /** Sum of the JPEG-compressed sizes of every entry's page bitmap at [dpi]/[quality]. */
    private fun totalBytes(taskId: String, entries: List<DocumentEntry>, mode: OutputMode, dpi: Int, quality: Int): Int {
        var total = 0
        for (entry in entries) {
            val page = composeEntryPage(taskId, entry, mode, dpi) ?: continue
            try {
                total += jpegByteCount(page.bitmap, quality)
            } finally {
                page.bitmap.recycle()
            }
        }
        return total
    }

    private fun buildPdf(taskId: String, entries: List<DocumentEntry>, mode: OutputMode, dpi: Int, quality: Int): ByteArray {
        val document = PdfDocument()
        var pageNumber = 1
        for (entry in entries) {
            val page = composeEntryPage(taskId, entry, mode, dpi) ?: continue
            val jpeg = jpegBytes(page.bitmap, quality)
            page.bitmap.recycle()
            val baked = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            try {
                val wPt = Units.mmToPoints(page.widthMm).roundToInt().coerceAtLeast(1)
                val hPt = Units.mmToPoints(page.heightMm).roundToInt().coerceAtLeast(1)
                val info = PdfDocument.PageInfo.Builder(wPt, hPt, pageNumber++).create()
                val pdfPage = document.startPage(info)
                pdfPage.canvas.drawColor(Color.WHITE)
                if (baked != null) {
                    pdfPage.canvas.drawBitmap(baked, null, RectF(0f, 0f, wPt.toFloat(), hPt.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                }
                document.finishPage(pdfPage)
            } finally {
                baked?.recycle()
            }
        }
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    /** One composed page: its physical size (mm) and the rendered bitmap (caller recycles). */
    private class EntryPage(val widthMm: Double, val heightMm: Double, val bitmap: Bitmap)

    private fun composeEntryPage(taskId: String, entry: DocumentEntry, mode: OutputMode, dpi: Int): EntryPage? =
        when (entry.kind) {
            EntryKind.DOCUMENT -> composeDocumentPage(taskId, entry, mode, dpi)
            EntryKind.PHOTO -> composePhotoPage(taskId, entry, mode, dpi)
        }

    private fun composeDocumentPage(taskId: String, entry: DocumentEntry, mode: OutputMode, dpi: Int): EntryPage? {
        val store = AndroidTaskStore(context)
        val session = store.documentSession(taskId, entry)
        val front = session.front?.let { decodeSampledBitmap(context, it.imageUri) } ?: return null
        val back = session.back?.let { decodeSampledBitmap(context, it.imageUri) }
        val sides = listOfNotNull(front, back)
        return try {
            val config = RenderConfig(
                mode = mode,
                paper = paper,
                format = OutputFormat.PDF,
                dpi = dpi,
                grayscale = false,
                cropMarks = false,
                maxFileSizeKb = null,
                searchableText = false,
                sizeOverride = entry.sizeOverride,
            )
            val layout = planLayout(session, config, sides)
            val bitmap = composePageBitmap(layout, sides, dpi, cardPaint(false))
            EntryPage(layout.pageWidthMm, layout.pageHeightMm, bitmap)
        } finally {
            front.recycle()
            back?.recycle()
        }
    }

    private fun composePhotoPage(taskId: String, entry: DocumentEntry, mode: OutputMode, dpi: Int): EntryPage? {
        val store = AndroidTaskStore(context)
        val uri = entry.frontImage?.let { store.imageUri(taskId, it) } ?: return null
        val photo = decodeSampledBitmap(context, uri) ?: return null
        val w = entry.photoWidthMm ?: 35.0
        val h = entry.photoHeightMm ?: 45.0
        return try {
            if (mode == OutputMode.PRINT) {
                // Actual-size page: the photo at its physical size.
                val bmp = drawPhoto(photo, w, h, dpi)
                EntryPage(w, h, bmp)
            } else {
                // Fit-to-width page: scale the photo to the usable width on A4, crop to content height.
                val contentW = paper.widthMm - 2 * marginMm
                val aspect = w / h
                val scaledH = contentW / aspect
                val pageH = scaledH + 2 * marginMm
                val bmp = drawPhotoFitWidth(photo, paper.widthMm, pageH, contentW, scaledH, marginMm, dpi)
                EntryPage(paper.widthMm, pageH, bmp)
            }
        } finally {
            photo.recycle()
        }
    }

    /** Draw a single photo, center-cropped, filling an exact-size page (print). */
    private fun drawPhoto(photo: Bitmap, widthMm: Double, heightMm: Double, dpi: Int): Bitmap {
        val wPx = Units.mmToPixels(widthMm, dpi).coerceAtLeast(1)
        val hPx = Units.mmToPixels(heightMm, dpi).coerceAtLeast(1)
        val bmp = createBitmap(wPx, hPx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val dst = RectF(0f, 0f, wPx.toFloat(), hPx.toFloat())
        canvas.drawBitmap(photo, centerCropSrcRect(photo, dst.width(), dst.height()), dst, Paint(Paint.FILTER_BITMAP_FLAG))
        return bmp
    }

    /** Draw a single photo centered within a fit-to-width page (upload). */
    private fun drawPhotoFitWidth(
        photo: Bitmap,
        pageWidthMm: Double,
        pageHeightMm: Double,
        contentWidthMm: Double,
        contentHeightMm: Double,
        marginMm: Double,
        dpi: Int,
    ): Bitmap {
        val wPx = Units.mmToPixels(pageWidthMm, dpi).coerceAtLeast(1)
        val hPx = Units.mmToPixels(pageHeightMm, dpi).coerceAtLeast(1)
        val bmp = createBitmap(wPx, hPx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val dst = RectF(
            mmToPxF(marginMm, dpi),
            mmToPxF(marginMm, dpi),
            mmToPxF(marginMm + contentWidthMm, dpi),
            mmToPxF(marginMm + contentHeightMm, dpi),
        )
        canvas.drawBitmap(photo, centerCropSrcRect(photo, dst.width(), dst.height()), dst, Paint(Paint.FILTER_BITMAP_FLAG))
        return bmp
    }

    private companion object {
        const val PRINT_JPEG_QUALITY = 92
    }
}
