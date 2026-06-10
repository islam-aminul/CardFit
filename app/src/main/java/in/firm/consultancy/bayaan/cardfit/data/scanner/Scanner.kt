package `in`.firm.consultancy.bayaan.cardfit.data.scanner

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.tasks.Task
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide

/** Which side a scan is for. */
enum class ScanSlot { FRONT, BACK }

/**
 * Seam over the ML Kit Document Scanner (CLAUDE.md sections 4 and 11.2). The interactive launch is
 * driven from Compose via an `ActivityResultLauncher<IntentSenderRequest>`; this seam supplies the
 * start IntentSender and turns a scan result into a persisted image file.
 *
 * Signatures use Android `Intent`/`Activity` and URI strings (not `android.net.Uri`) so a JVM fake
 * can satisfy the contract for tests without instantiating Android classes.
 */
interface Scanner {
    /** Build the IntentSender that launches the scanner UI for one page. */
    fun startScanIntent(activity: Activity): Task<IntentSender>

    /**
     * Extract the first cropped page from a scanner result and copy it into stable app storage for
     * the given [slot]; returns the persisted [ScannedSide] (URI + pixel dimensions for aspect-ratio
     * classification), or `null` if there was no page.
     */
    suspend fun persistFirstPage(resultIntent: Intent?, slot: ScanSlot): ScannedSide?
}
