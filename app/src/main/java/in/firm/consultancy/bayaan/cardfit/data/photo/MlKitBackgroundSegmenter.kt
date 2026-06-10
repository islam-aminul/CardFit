package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Real [BackgroundSegmenter] backed by ML Kit Subject Segmentation
 * (`play-services-mlkit-subject-segmentation`). Inference runs on-device through Google Play services
 * (the optional model is declared for install-time download in the manifest), so no INTERNET
 * permission is required, consistent with the document scanner.
 *
 * The segmenter is configured with `enableForegroundBitmap()`, which yields the subject pixels on a
 * transparent background; we composite that over solid white to produce the ID-photo background.
 */
class MlKitBackgroundSegmenter : BackgroundSegmenter {

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build(),
    )

    override suspend fun whiteOutBackground(bitmap: Bitmap): Bitmap? {
        val foreground = runSegmentation(bitmap) ?: return null
        // Composite the segmented subject over a solid white background, matching the source size.
        val out = createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        // The foreground bitmap matches the input dimensions; draw it at the origin.
        canvas.drawBitmap(foreground, 0f, 0f, null)
        if (foreground !== bitmap) foreground.recycle()
        return out
    }

    private suspend fun runSegmentation(bitmap: Bitmap): Bitmap? =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(input)
                .addOnSuccessListener { result -> cont.resume(result.foregroundBitmap) }
                .addOnFailureListener { cont.resume(null) }
        }

    override fun close() {
        segmenter.close()
    }
}
