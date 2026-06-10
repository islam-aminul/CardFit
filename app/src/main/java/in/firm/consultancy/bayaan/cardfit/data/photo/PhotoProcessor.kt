package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import `in`.firm.consultancy.bayaan.cardfit.data.render.decodeSampledBitmap
import `in`.firm.consultancy.bayaan.cardfit.domain.LevelsAdjustment
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams
import `in`.firm.consultancy.bayaan.cardfit.domain.autoLevels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies [PhotoEditParams] to a source image and returns the edited bitmap (CLAUDE.md Phase 13). The
 * original file is never modified: every call decodes a fresh copy of the source and produces a new
 * bitmap, so the edit is fully revertible. Heavy work runs off the main thread.
 *
 * Pipeline order: rotate → crop → (optional) background→white → colour adjustments (brightness,
 * contrast, saturation) → (optional) deterministic auto-levels. Auto-enhance is computed from the
 * post-adjustment luminance histogram via the pure [autoLevels].
 */
interface PhotoProcessor {
    /**
     * Decode [sourceUri] (downsampled to [maxDim]), apply [params], and return the edited bitmap.
     * Returns `null` if the source can't be read. Caller owns + recycles the result.
     */
    suspend fun process(sourceUri: String, params: PhotoEditParams, maxDim: Int): Bitmap?

    /** Source pixel dimensions (width, height) without a full decode; null if unreadable. */
    suspend fun sourceSize(sourceUri: String): Pair<Int, Int>?
}

class AndroidPhotoProcessor(
    private val context: Context,
    private val segmenter: BackgroundSegmenter,
) : PhotoProcessor {

    override suspend fun process(sourceUri: String, params: PhotoEditParams, maxDim: Int): Bitmap? =
        withContext(Dispatchers.Default) {
            var current = decodeSampledBitmap(context, sourceUri, maxDim) ?: return@withContext null

            current = applyRotation(current, params.rotationDegrees)
            current = applyCrop(current, params)

            if (params.removeBackground) {
                val whited = segmenter.whiteOutBackground(current)
                if (whited != null) {
                    if (whited !== current) current.recycle()
                    current = whited
                }
                // On failure keep the un-segmented image (revert-to-original behaviour).
            }

            current = applyColorAdjustments(current, params)
            current
        }

    override suspend fun sourceSize(sourceUri: String): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            val bmp = decodeSampledBitmap(context, sourceUri, Int.MAX_VALUE / 2) ?: return@withContext null
            val size = bmp.width to bmp.height
            bmp.recycle()
            size
        }

    private fun applyRotation(src: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return src
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (rotated !== src) src.recycle()
        return rotated
    }

    private fun applyCrop(src: Bitmap, params: PhotoEditParams): Bitmap {
        val crop = params.crop ?: return src
        val x = crop.xPx.coerceIn(0, src.width - 1)
        val y = crop.yPx.coerceIn(0, src.height - 1)
        val w = crop.widthPx.coerceIn(1, src.width - x)
        val h = crop.heightPx.coerceIn(1, src.height - y)
        if (x == 0 && y == 0 && w == src.width && h == src.height) return src
        val cropped = Bitmap.createBitmap(src, x, y, w, h)
        if (cropped !== src) src.recycle()
        return cropped
    }

    /**
     * Apply the brightness/contrast/saturation [ColorMatrix] and, when enabled, a deterministic
     * auto-levels stretch derived from the adjusted image's luminance histogram. Returns a new bitmap.
     */
    private fun applyColorAdjustments(src: Bitmap, params: PhotoEditParams): Bitmap {
        val base = colorMatrix(params)
        val hasBase = !base.isIdentityApprox()
        if (!hasBase && !params.autoEnhance) return src

        // First pass: brightness/contrast/saturation (applyMatrix always returns a new bitmap).
        var working = src
        if (hasBase) {
            working = applyMatrix(src, base)
            src.recycle()
        }

        if (params.autoEnhance) {
            val levels = autoLevels(luminanceHistogram(working))
            if (levels != LevelsAdjustment.IDENTITY) {
                val enhanced = applyMatrix(working, levelsMatrix(levels))
                if (enhanced !== working) working.recycle()
                working = enhanced
            }
        }
        return working
    }

    private fun applyMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Build a brightness/contrast/saturation matrix from the slider percentages (-100..100). */
    private fun colorMatrix(params: PhotoEditParams): ColorMatrix {
        val matrix = ColorMatrix()

        // Saturation: 0 -> grayscale, 1 -> unchanged, 2 -> doubled.
        val saturation = (1f + params.saturationPercent / 100f).coerceIn(0f, 2f)
        matrix.setSaturation(saturation)

        // Contrast around mid-grey (128): out = in*c + 128*(1-c), c in [0.2, 2].
        val c = (1f + params.contrastPercent / 100f).coerceIn(0.2f, 2f)
        val t = 128f * (1f - c)
        val contrast = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        matrix.postConcat(contrast)

        // Brightness: additive offset, +-100 -> +-100/255 of the range.
        val b = params.brightnessPercent.toFloat()
        if (b != 0f) {
            val brightness = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            matrix.postConcat(brightness)
        }
        return matrix
    }

    private fun levelsMatrix(levels: LevelsAdjustment): ColorMatrix = ColorMatrix(
        floatArrayOf(
            levels.scale, 0f, 0f, 0f, levels.offset,
            0f, levels.scale, 0f, 0f, levels.offset,
            0f, 0f, levels.scale, 0f, levels.offset,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    /** Build a 256-bin luminance histogram (Rec. 601 weights) from [bitmap]. */
    private fun luminanceHistogram(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val hist = IntArray(256)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            hist[lum]++
        }
        return hist
    }

    private fun ColorMatrix.isIdentityApprox(): Boolean {
        val a = this.array
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        for (i in a.indices) if (kotlin.math.abs(a[i] - identity[i]) > 1e-3f) return false
        return true
    }
}
