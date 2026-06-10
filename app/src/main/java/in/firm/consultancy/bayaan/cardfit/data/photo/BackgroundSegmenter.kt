package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.graphics.Bitmap

/**
 * Opt-in background removal (CLAUDE.md Phase 13). Wraps on-device subject segmentation behind an
 * interface so the photo pipeline / ViewModel stay testable with a fake. The real implementation uses
 * ML Kit Subject Segmentation, whose model is served by Google Play services (like the document
 * scanner) — the app still declares no INTERNET permission.
 *
 * Segmentation quality varies on hair and fine edges, so this is never applied silently: the caller
 * exposes it as a toggle with live preview and always keeps the original to revert to.
 */
interface BackgroundSegmenter {
    /**
     * Return a copy of [bitmap] with the detected background replaced by solid white, or `null` if
     * the subject could not be segmented (so the caller can fall back to the untouched image and warn).
     * Heavy work runs off the main thread.
     */
    suspend fun whiteOutBackground(bitmap: Bitmap): Bitmap?

    /** Release native resources. */
    fun close()
}
