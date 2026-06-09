package `in`.firm.consultancy.bayaan.cardfit.data.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real [Scanner] backed by the on-device ML Kit Document Scanner. The scanner provides its own
 * capture + crop/perspective-correction UI (no CameraX), and its models/UI are served by Google
 * Play services — the app itself needs no INTERNET permission.
 *
 * SCANNER_MODE_FULL gives the full editing UI: after the (auto- or manual-) capture the user can
 * drag the detected corners/boundary to correct a bad auto-detect, crop, rotate, and clean the
 * image before confirming. We always consume the corrected/cropped result page
 * (`result.pages[0].imageUri`), never the raw camera frame. Page limit is 1 (front and back are
 * captured separately), and the result is JPEG.
 *
 * Cropped pages are copied out of ML Kit's temporary location into `filesDir/scans/<slot>-<ts>.jpg`.
 * Unique per-capture filenames mean a fresh scan never collides with a previous image's cache (so
 * thumbnails/OCR re-resolve correctly), and the captured sides survive process death for re-export
 * without re-scanning (CLAUDE.md section 12). Older files for the same slot are pruned on retake.
 */
class MlKitDocumentScanner(private val appContext: Context) : Scanner {

    private val options: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // one page per side; we capture front and back separately
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // full edit UI incl. manual crop
        .build()

    private val client = GmsDocumentScanning.getClient(options)

    override fun startScanIntent(activity: Activity): Task<IntentSender> =
        client.getStartScanIntent(activity)

    override suspend fun persistFirstPage(resultIntent: Intent?, slot: ScanSlot): String? =
        withContext(Dispatchers.IO) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(resultIntent)
                ?: return@withContext null
            val pageUri = result.pages?.firstOrNull()?.imageUri ?: return@withContext null

            val dir = File(appContext.filesDir, SCAN_DIR).apply { mkdirs() }
            val prefix = "${slot.name.lowercase()}-"
            // Prune any previous file for this slot (e.g. a retake) before writing the new one.
            dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
            val dest = File(dir, "$prefix${System.currentTimeMillis()}.jpg")

            val copied = appContext.contentResolver.openInputStream(pageUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false

            // Treat an empty copy as a failure so the user retakes instead of hitting a broken export.
            if (copied && dest.length() > 0L) {
                Uri.fromFile(dest).toString()
            } else {
                dest.delete()
                null
            }
        }

    private companion object {
        const val SCAN_DIR = "scans"
    }
}
