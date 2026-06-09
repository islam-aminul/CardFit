package `in`.firm.consultancy.bayaan.cardfit.data.ocr

import android.content.Context
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import `in`.firm.consultancy.bayaan.cardfit.data.Ocr
import `in`.firm.consultancy.bayaan.cardfit.domain.BoxPx
import `in`.firm.consultancy.bayaan.cardfit.domain.OcrElement
import `in`.firm.consultancy.bayaan.cardfit.domain.OcrTextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text recognition using ML Kit's BUNDLED Latin model (no runtime download, no network —
 * reinforces the offline guarantee). Returns recognized lines top-to-bottom in reading order.
 *
 * Recognized text is transient: it is read, handed to [NameParser], and discarded. Nothing here logs
 * or persists the OCR output, and no identity numbers are extracted (the parser rejects digit lines).
 */
class MlKitOcr(private val appContext: Context) : Ocr, Closeable {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imageUri: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(appContext, imageUri.toUri())
            val text = recognizer.processAwait(image)
            text.textBlocks
                .flatMap { it.lines }
                .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                .map { it.text }
        } catch (e: Exception) {
            // Stay fully usable on any OCR failure: no suggestion, manual entry still works.
            emptyList()
        }
    }

    override suspend fun recognizeLayer(imageUri: String): OcrTextLayer = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(appContext, imageUri.toUri())
            val text = recognizer.processAwait(image)
            // The InputImage is upright; its width/height are the pixel space the boxes use.
            val width = image.width
            val height = image.height
            val elements = buildList {
                for (block in text.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val b = element.boundingBox ?: continue
                            if (element.text.isBlank()) continue
                            add(OcrElement(element.text, BoxPx(b.left, b.top, b.right, b.bottom)))
                        }
                    }
                }
            }
            OcrTextLayer(imageWidthPx = width, imageHeightPx = height, elements = elements)
        } catch (e: Exception) {
            OcrTextLayer(imageWidthPx = 0, imageHeightPx = 0, elements = emptyList())
        }
    }

    override fun close() {
        recognizer.close()
    }

    private suspend fun com.google.mlkit.vision.text.TextRecognizer.processAwait(image: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            process(image)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { error -> cont.resumeWithException(error) }
        }
}
