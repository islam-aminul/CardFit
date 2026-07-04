package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.graphics.Bitmap

/**
 * A [BackgroundSegmenter] that never removes a background. Used by flows that reuse the photo edit
 * pipeline WITHOUT background removal (document editing/rendering), so no ML Kit segmentation model is
 * ever loaded. The interface methods are effectively no-ops.
 */
object NoBackgroundSegmenter : BackgroundSegmenter {
    override suspend fun whiteOutBackground(bitmap: Bitmap): Bitmap? = null
    override fun close() = Unit
}
