package `in`.firm.consultancy.bayaan.cardfit.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import `in`.firm.consultancy.bayaan.cardfit.data.JpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.PdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.data.photo.AndroidPhotoProcessor
import `in`.firm.consultancy.bayaan.cardfit.data.photo.NoBackgroundSegmenter
import `in`.firm.consultancy.bayaan.cardfit.data.photo.PhotoProcessor
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import `in`.firm.consultancy.bayaan.cardfit.domain.receiptRealSizeMm
import `in`.firm.consultancy.bayaan.cardfit.domain.targetCombinedSize
import `in`.firm.consultancy.bayaan.cardfit.domain.targetJpegSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/**
 * Renders a standalone multi-page document ([ScanSession.documentPages]) — one output page per
 * scanned page. FULL_PAGE_DOCUMENT reuses the existing single-side layout maths (FIT_PAGE for print,
 * FIT_WIDTH for upload, orientation honoured) via [planLayout]/[composePageBitmap]; RECEIPT places
 * the scan at its true real-world size (chosen width × derived height) centred on the selected paper.
 *
 * A multi-page document exports as one multi-page PDF; JPEG is only used when there is exactly one
 * page (Configure disables JPEG otherwise). Card sessions never reach here.
 *
 * Memory-conscious (CLAUDE.md §12): each page's non-destructive [DocumentPage.edit] is baked ONCE into
 * a temporary JPEG (one bitmap held at a time), then pages are re-decoded and composed one-at-a-time
 * during the size-cap search and the final pass — the same disk-backed strategy as the combined-PDF
 * renderer, so the synchronous size-targeting closures never touch the suspend edit pipeline.
 */
class DocumentPdfRenderer(
    private val context: Context,
    private val processor: PhotoProcessor = AndroidPhotoProcessor(context, NoBackgroundSegmenter),
) : PdfRenderer {

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput =
        withContext(Dispatchers.Default) {
            require(session.documentPages.isNotEmpty()) { "Document has no pages to export" }
            val prepared = bakePages(context, processor, session, config)
            try {
                val dpi: Int
                val quality: Int
                var warning: String? = null

                val capBytes = if (config.mode == OutputMode.UPLOAD) config.maxFileSizeKb?.let { it * 1024 } else null
                if (capBytes != null) {
                    val overhead = (capBytes * 0.05).toInt().coerceIn(2048, 16384)
                    val target = targetCombinedSize(
                        maxBytes = capBytes,
                        startDpi = Defaults.UPLOAD_DPI,
                        overheadBytes = overhead,
                        compressTotal = { d, q -> totalBytes(prepared, config, d, q) },
                    )
                    dpi = target.dpi
                    quality = target.quality
                    if (target.belowFloor) {
                        warning = "Couldn't reach the ${config.maxFileSizeKb} KB cap for the document; saved " +
                            "the smallest legible version (~${target.dpi} dpi)."
                    }
                } else {
                    dpi = if (config.mode == OutputMode.PRINT) Defaults.PRINT_DPI else Defaults.UPLOAD_DPI
                    quality = PRINT_JPEG_QUALITY
                }
                RenderedOutput(buildPdf(prepared, config, dpi, quality), warning)
            } finally {
                prepared.forEach { it.file.delete() }
            }
        }

    private fun totalBytes(prepared: List<PreparedPage>, config: RenderConfig, dpi: Int, quality: Int): Int {
        var total = 0
        for (page in prepared) {
            val composed = composePreparedPage(page, config, dpi) ?: continue
            try {
                total += jpegByteCount(composed.bitmap, quality)
            } finally {
                composed.bitmap.recycle()
            }
        }
        return total
    }

    private fun buildPdf(prepared: List<PreparedPage>, config: RenderConfig, dpi: Int, quality: Int): ByteArray {
        val document = PdfDocument()
        var pageNumber = 1
        for (page in prepared) {
            val composed = composePreparedPage(page, config, dpi) ?: continue
            val jpeg = jpegBytes(composed.bitmap, quality)
            composed.bitmap.recycle()
            val baked = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
            try {
                val wPt = Units.mmToPoints(composed.widthMm).roundToInt().coerceAtLeast(1)
                val hPt = Units.mmToPoints(composed.heightMm).roundToInt().coerceAtLeast(1)
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

    private companion object {
        const val PRINT_JPEG_QUALITY = 92
    }
}

/**
 * JPEG variant for a document — only reached when the document has exactly one page (Configure
 * disables JPEG for multi-page). Renders that single page and honours the upload size cap.
 */
class DocumentJpegRenderer(
    private val context: Context,
    private val processor: PhotoProcessor = AndroidPhotoProcessor(context, NoBackgroundSegmenter),
) : JpegRenderer {

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput =
        withContext(Dispatchers.Default) {
            require(session.documentPages.isNotEmpty()) { "Document has no page to export" }
            val prepared = bakePages(context, processor, session, config.copy())
            try {
                val page = prepared.first()
                val capBytes = if (config.mode == OutputMode.UPLOAD) config.maxFileSizeKb?.let { it * 1024 } else null
                if (capBytes == null) {
                    val composed = composePreparedPage(page, config, config.dpi)
                        ?: error("Could not render the document page")
                    try {
                        val bytes = jpegBytes(composed.bitmap, PRINT_JPEG_QUALITY)
                        RenderedOutput(writeJpegDpi(context, bytes, config.dpi))
                    } finally {
                        composed.bitmap.recycle()
                    }
                } else {
                    renderWithCap(page, config, capBytes)
                }
            } finally {
                prepared.forEach { it.file.delete() }
            }
        }

    private fun renderWithCap(page: PreparedPage, config: RenderConfig, capBytes: Int): RenderedOutput {
        var cachedDpi = -1
        var cached: Bitmap? = null
        fun pageAt(dpi: Int): Bitmap {
            if (dpi != cachedDpi || cached == null) {
                cached?.recycle()
                cached = composePreparedPage(page, config, dpi)?.bitmap
                    ?: error("Could not render the document page")
                cachedDpi = dpi
            }
            return cached!!
        }
        try {
            val target = targetJpegSize(
                maxBytes = capBytes,
                startDpi = config.dpi,
                compress = { dpi, quality -> jpegByteCount(pageAt(dpi), quality) },
            )
            val bytes = jpegBytes(pageAt(target.dpi), target.quality)
            val tagged = writeJpegDpi(context, bytes, target.dpi)
            val warning = if (target.belowFloor) {
                "Couldn't reach the ${config.maxFileSizeKb} KB cap; saved the smallest legible " +
                    "version (~${target.dpi} dpi)."
            } else {
                null
            }
            return RenderedOutput(tagged, warning)
        } finally {
            cached?.recycle()
        }
    }

    private companion object {
        const val PRINT_JPEG_QUALITY = 92
    }
}

// ---- shared prepare/compose helpers (package-internal) ----

/** A page whose edits have been baked to a temp JPEG on disk, plus the metadata to lay it out. */
internal class PreparedPage(val file: File, val cardType: CardType, val widthMm: Double?)

/** One composed document page: its physical size (mm) and the rendered bitmap (caller recycles). */
internal class ComposedDocumentPage(val widthMm: Double, val heightMm: Double, val bitmap: Bitmap)

/**
 * Bake every page's non-destructive edits into a temporary JPEG (one bitmap in memory at a time),
 * returning the temp files + layout metadata. Caller deletes the files when done.
 */
internal suspend fun bakePages(
    context: Context,
    processor: PhotoProcessor,
    session: ScanSession,
    config: RenderConfig,
): List<PreparedPage> = withContext(Dispatchers.Default) {
    val dir = File(context.cacheDir, "doc-export").apply { mkdirs() }
    session.documentPages.mapIndexedNotNull { index, page ->
        val edited = processor.process(page.side.imageUri, page.edit, SOURCE_MAX_DIM) ?: return@mapIndexedNotNull null
        try {
            val file = File(dir, "page-$index-${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(jpegBytes(edited, BAKE_QUALITY)) }
            PreparedPage(file, session.cardType, page.widthMm)
        } finally {
            edited.recycle()
        }
    }
}

/**
 * Compose one prepared page to a bitmap at [dpi] (synchronous; re-decodes the temp JPEG each call).
 *  - FULL_PAGE_DOCUMENT: fed through the existing [planLayout] so the page inherits FIT_PAGE/FIT_WIDTH
 *    + orientation maths; the decoded bitmap's own pixel dimensions drive the aspect classification.
 *  - RECEIPT: placed at its true real-world size (chosen width × derived height) centred on the paper.
 */
internal fun composePreparedPage(page: PreparedPage, config: RenderConfig, dpi: Int): ComposedDocumentPage? {
    val bmp = decodeSampledBitmap(page.file.toURIString(), page.file) ?: return null
    return try {
        if (page.cardType == CardType.RECEIPT) {
            composeReceiptPage(bmp, page.widthMm, config, dpi)
        } else {
            composeFullPage(bmp, config, dpi)
        }
    } finally {
        bmp.recycle()
    }
}

private fun File.toURIString(): String = toUri().toString()

/** Decode a temp file to a bitmap, bounding the largest side (matches the export pipeline elsewhere). */
private fun decodeSampledBitmap(uri: String, file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > SOURCE_MAX_DIM) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}

private fun composeFullPage(edited: Bitmap, config: RenderConfig, dpi: Int): ComposedDocumentPage {
    // A per-page single-side session; the edited bitmap's own dims drive aspect classification.
    val pageSession = ScanSession(
        cardType = CardType.FULL_PAGE_DOCUMENT,
        front = ScannedSide("", edited.width, edited.height),
        back = null,
    )
    val layout = planLayout(pageSession, config, listOf(edited))
    val bitmap = composePageBitmap(layout, listOf(edited), dpi, cardPaint(config.grayscale))
    return ComposedDocumentPage(layout.pageWidthMm, layout.pageHeightMm, bitmap)
}

private fun composeReceiptPage(edited: Bitmap, widthMm: Double?, config: RenderConfig, dpi: Int): ComposedDocumentPage {
    val paperWmm = config.paper.widthMm
    val paperHmm = config.paper.heightMm
    val chosenWmm = widthMm ?: paperWmm * 0.5 // UI requires a width before Next; fallback keeps rendering safe
    val (realWmm, realHmm) = receiptRealSizeMm(chosenWmm, edited.width, edited.height)

    val pageWpx = Units.mmToPixels(paperWmm, dpi).coerceAtLeast(1)
    val pageHpx = Units.mmToPixels(paperHmm, dpi).coerceAtLeast(1)
    val bmp = createBitmap(pageWpx, pageHpx, Bitmap.Config.RGB_565)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)

    // Centre the receipt at its true physical size on the sheet (clamped to the sheet if oversize).
    val drawWmm = realWmm.coerceAtMost(paperWmm)
    val drawHmm = realHmm.coerceAtMost(paperHmm)
    val left = mmToPxF((paperWmm - drawWmm) / 2.0, dpi)
    val top = mmToPxF((paperHmm - drawHmm) / 2.0, dpi)
    val dst = RectF(left, top, left + mmToPxF(drawWmm, dpi), top + mmToPxF(drawHmm, dpi))
    canvas.drawBitmap(edited, centerCropSrcRect(edited, dst.width(), dst.height()), dst, cardPaint(config.grayscale))
    return ComposedDocumentPage(paperWmm, paperHmm, bmp)
}

// SOURCE_MAX_DIM (the shared downsample bound) is reused from RenderSupport.kt (same package).
private const val BAKE_QUALITY = 95
