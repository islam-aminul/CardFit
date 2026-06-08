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
 * Cropped pages are copied out of ML Kit's temporary location into `filesDir/scans/<slot>.jpg` so
 * the captured sides survive process death and remain available for re-export without re-scanning
 * (CLAUDE.md section 12).
 */
class MlKitDocumentScanner(private val appContext: Context) : Scanner {

    private val options: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // one page per side; we capture front and back separately
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
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
            val dest = File(dir, "${slot.name.lowercase()}.jpg")

            val copied = appContext.contentResolver.openInputStream(pageUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false

            if (copied) Uri.fromFile(dest).toString() else null
        }

    private companion object {
        const val SCAN_DIR = "scans"
    }
}
