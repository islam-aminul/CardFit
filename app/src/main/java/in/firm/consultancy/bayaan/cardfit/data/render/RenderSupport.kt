package `in`.firm.consultancy.bayaan.cardfit.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.LayoutCalculator
import `in`.firm.consultancy.bayaan.cardfit.domain.LayoutPlanner
import `in`.firm.consultancy.bayaan.cardfit.domain.PageLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.SideInfo
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shared Android rendering helpers for the PDF/JPEG renderers. Memory-conscious per CLAUDE.md
 * section 12: source images are decoded with `inSampleSize`, the page canvas is RGB_565, size probes
 * use a counting stream (no byte buffers), and callers process one page bitmap at a time and recycle.
 */

internal const val SOURCE_MAX_DIM = 2000
internal const val PRINT_JPEG_QUALITY = 92

/**
 * Decode a source image, downsampled so its largest side is <= [maxDim] px to bound memory.
 *
 * Reads the bytes once and decodes with [BitmapFactory.decodeByteArray]. This is deliberately NOT
 * `openInputStream` + `decodeStream`: that combination can return null over some ContentResolver
 * streams even for valid images (a long-standing `decodeStream` quirk). Decoding from a byte array
 * is reliable for both `file://` and `content://` sources.
 */
internal fun decodeSampledBitmap(context: Context, uriString: String, maxDim: Int = SOURCE_MAX_DIM): Bitmap? {
    val bytes = readAllBytes(context, uriString) ?: return null
    if (bytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null

    var sample = 1
    while (max(w, h) / sample > maxDim) sample *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    return applyExifOrientation(decoded, bytes)
}

/**
 * Rotate/flip [bmp] so it is displayed upright, honouring the JPEG's EXIF orientation tag. Camera
 * captures commonly store the photo sideways with an orientation flag rather than rotating the pixels;
 * without this the preview would come up rotated (e.g. facing "east" instead of "north"). Decoding
 * from a byte array bypasses BitmapFactory's EXIF handling, so we apply it explicitly here.
 */
private fun applyExifOrientation(bmp: Bitmap, bytes: ByteArray): Bitmap {
    val orientation = try {
        ExifInterface(java.io.ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bmp
    }
    return try {
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        if (out !== bmp) bmp.recycle()
        out
    } catch (e: OutOfMemoryError) {
        bmp
    }
}

private fun readAllBytes(context: Context, uriString: String): ByteArray? = try {
    context.contentResolver.openInputStream(uriString.toUri())?.use { it.readBytes() }
} catch (e: Exception) {
    null
}

/** Paint for drawing card images; applies a saturation-0 ColorMatrix when [grayscale]. */
internal fun cardPaint(grayscale: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isFilterBitmap = true
    isDither = true
    if (grayscale) {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }
}

/**
 * Center-crop source rectangle so [src] fills a destination of the given aspect without stretching
 * (CLAUDE.md section 6 aspect reconciliation: crop the minimal amount, never stretch).
 */
internal fun centerCropSrcRect(src: Bitmap, dstWidth: Float, dstHeight: Float): Rect {
    val dstAspect = dstWidth / dstHeight
    val srcAspect = src.width.toFloat() / src.height.toFloat()
    return if (srcAspect > dstAspect) {
        val cropW = (src.height * dstAspect).roundToInt().coerceIn(1, src.width)
        val x = ((src.width - cropW) / 2f).roundToInt().coerceAtLeast(0)
        Rect(x, 0, x + cropW, src.height)
    } else {
        val cropH = (src.width / dstAspect).roundToInt().coerceIn(1, src.height)
        val y = ((src.height - cropH) / 2f).roundToInt().coerceAtLeast(0)
        Rect(0, y, src.width, y + cropH)
    }
}

/** Plan + calculate the page layout for a session/config given the decoded sides. */
internal fun planLayout(session: ScanSession, config: RenderConfig, sides: List<Bitmap>): PageLayout {
    // Prefer the cropped sides' recorded pixel dimensions (used for aspect classification); fall back
    // to the decoded bitmap dimensions (same aspect) if unknown.
    val scanned = listOfNotNull(session.front, session.back)
    val sideInfos = sides.mapIndexed { i, bmp ->
        val s = scanned.getOrNull(i)
        if (s != null && s.widthPx > 0 && s.heightPx > 0) {
            SideInfo(s.widthPx, s.heightPx)
        } else {
            SideInfo(bmp.width, bmp.height)
        }
    }
    val input = LayoutPlanner.plan(
        cardType = session.cardType,
        mode = config.mode,
        paper = config.paper,
        sides = sideInfos,
        sizeOverride = config.sizeOverride,
        customWidthMm = session.customWidthMm,
        customHeightMm = session.customHeightMm,
    )
    return LayoutCalculator.calculate(input)
}

/**
 * Compose the whole page as one RGB_565 bitmap at [dpi] (white background), drawing each side
 * center-cropped into its layout rect. Caller owns + recycles the returned bitmap.
 */
internal fun composePageBitmap(
    layout: PageLayout,
    sides: List<Bitmap>,
    dpi: Int,
    paint: Paint,
    cornerRadiusMm: Double = 0.0,
): Bitmap {
    val wPx = Units.mmToPixels(layout.pageWidthMm, dpi).coerceAtLeast(1)
    val hPx = Units.mmToPixels(layout.pageHeightMm, dpi).coerceAtLeast(1)
    val bmp = createBitmap(wPx, hPx, Bitmap.Config.RGB_565)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)
    val cornerPx = if (cornerRadiusMm > 0.0) mmToPxF(cornerRadiusMm, dpi) else 0f
    layout.cards.forEachIndexed { i, rect ->
        val src = sides[i]
        val dst = RectF(
            mmToPxF(rect.xMm, dpi),
            mmToPxF(rect.yMm, dpi),
            mmToPxF(rect.xMm + rect.widthMm, dpi),
            mmToPxF(rect.yMm + rect.heightMm, dpi),
        )
        canvas.drawBitmap(src, centerCropSrcRect(src, dst.width(), dst.height()), dst, paint)
        drawRoundedCardCorners(canvas, dst, cornerPx)
    }
    return bmp
}

/**
 * Trim a card's four corners to a rounded-rectangle of [radius] by painting the corner "nubs" white
 * (the page background) with anti-aliasing. Used for PVC cards (PAN/Aadhaar/EPIC) whose true corners
 * are rounded — the rectangular scan crop leaves off-colour background in those corners. Unit-agnostic
 * ([radius] in the canvas's own units), so it serves both the px page bitmap and the PDF point canvas.
 * No-op when [radius] <= 0.
 */
internal fun drawRoundedCardCorners(canvas: Canvas, rect: RectF, radius: Float) {
    val r = radius.coerceAtMost(minOf(rect.width(), rect.height()) / 2f)
    if (r <= 0f) return
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val l = rect.left
    val t = rect.top
    val rr = rect.right
    val b = rect.bottom
    val path = Path()

    fun wedge(moveX: Float, moveY: Float, lineX: Float, lineY: Float, oval: RectF, start: Float) {
        path.reset()
        path.moveTo(moveX, moveY)
        path.lineTo(lineX, lineY)
        path.arcTo(oval, start, -90f, false)
        path.close()
        canvas.drawPath(path, white)
    }
    // Top-left, top-right, bottom-right, bottom-left.
    wedge(l, t, l + r, t, RectF(l, t, l + 2 * r, t + 2 * r), 270f)
    wedge(rr, t, rr, t + r, RectF(rr - 2 * r, t, rr, t + 2 * r), 0f)
    wedge(rr, b, rr - r, b, RectF(rr - 2 * r, b - 2 * r, rr, b), 90f)
    wedge(l, b, l, b - r, RectF(l, b - 2 * r, l + 2 * r, b), 180f)
}

internal fun mmToPxF(mm: Double, dpi: Int): Float = (mm * dpi / Units.MM_PER_INCH).toFloat()

internal fun mmToPtF(mm: Double): Float = Units.mmToPoints(mm).toFloat()

/** Measure JPEG size at [quality] without allocating the bytes (counting stream). */
internal fun jpegByteCount(bitmap: Bitmap, quality: Int): Int {
    val counter = object : OutputStream() {
        var count = 0
        override fun write(b: Int) { count++ }
        override fun write(b: ByteArray) { count += b.size }
        override fun write(b: ByteArray, off: Int, len: Int) { count += len }
    }
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, counter)
    return counter.count
}

internal fun jpegBytes(bitmap: Bitmap, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

/**
 * Write JPEG DPI density into [bytes] via ExifInterface (TAG_X/Y_RESOLUTION = dpi, RESOLUTION_UNIT =
 * inches) and return the tagged bytes. Uses a short-lived temp file in the cache dir.
 */
internal fun writeJpegDpi(context: Context, bytes: ByteArray, dpi: Int): ByteArray {
    val tmp = File.createTempFile("cardfit_", ".jpg", context.cacheDir)
    return try {
        tmp.writeBytes(bytes)
        ExifInterface(tmp.absolutePath).apply {
            setAttribute(ExifInterface.TAG_X_RESOLUTION, "$dpi/1")
            setAttribute(ExifInterface.TAG_Y_RESOLUTION, "$dpi/1")
            setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, RESOLUTION_UNIT_INCHES)
            saveAttributes()
        }
        tmp.readBytes()
    } finally {
        tmp.delete()
    }
}

// ExifInterface RESOLUTION_UNIT value for inches.
private const val RESOLUTION_UNIT_INCHES = "2"
