package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidFileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.data.photo.AndroidPhotoProcessor
import `in`.firm.consultancy.bayaan.cardfit.data.photo.MlKitBackgroundSegmenter
import `in`.firm.consultancy.bayaan.cardfit.data.photo.PhotoExporter
import `in`.firm.consultancy.bayaan.cardfit.data.render.decodeSampledBitmap
import `in`.firm.consultancy.bayaan.cardfit.domain.CopiesResult
import `in`.firm.consultancy.bayaan.cardfit.domain.CropRect
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoGrid
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoPaper
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoSize
import `in`.firm.consultancy.bayaan.cardfit.domain.ResolvedPhotoSize
import `in`.firm.consultancy.bayaan.cardfit.domain.gridLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.resolveCopies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/** A crop rectangle in fractions of the (rotated) source, 0..1. Null = full frame (free, no crop). */
data class NormCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * UI state for the Phase 13 photo flow: source image, non-destructive edit params, the selected
 * size, and the upload/print export settings. The original image URI is never overwritten — every
 * preview/export re-derives from it, so edits are always revertible.
 */
data class PhotoState(
    val sourceUri: String? = null,
    val sourceWidthPx: Int = 0,
    val sourceHeightPx: Int = 0,
    // Edit params (crop held separately as a normalized rect for the overlay).
    val rotationDegrees: Int = 0,
    val cropNorm: NormCrop? = null,
    val aspectLocked: Boolean = false,
    val brightnessPercent: Int = 0,
    val contrastPercent: Int = 0,
    val saturationPercent: Int = 0,
    val autoEnhance: Boolean = false,
    val removeBackground: Boolean = false,
    // Size + export.
    val size: PhotoSize = PhotoSize.PASSPORT_INDIA,
    val customWidthMm: Double? = null,
    val customHeightMm: Double? = null,
    val name: String = "",
    val modes: Set<OutputMode> = setOf(OutputMode.UPLOAD),
    val uploadDpi: Int = 300,
    val uploadMaxKb: Int? = null,
    val printPaper: PhotoPaper = PhotoPaper.A4,
    val requestedCopies: Int = 4,
    val cutMarks: Boolean = true,
) {
    val resolvedSize: ResolvedPhotoSize?
        get() = runCatching { ResolvedPhotoSize.of(size, customWidthMm, customHeightMm) }.getOrNull()

    /** Displayed aspect (w/h) of the rotated source, for sizing the crop-overlay container. */
    fun previewAspect(): Float {
        if (sourceWidthPx <= 0 || sourceHeightPx <= 0) return 1f
        val swap = rotationDegrees % 180 != 0
        val w = if (swap) sourceHeightPx else sourceWidthPx
        val h = if (swap) sourceWidthPx else sourceHeightPx
        return w.toFloat() / h.toFloat()
    }

    /** Build the pure edit params; rotation/crop applied first, then colour/segmentation. */
    fun editParams(): PhotoEditParams = PhotoEditParams(
        rotationDegrees = rotationDegrees,
        crop = cropPx(),
        brightnessPercent = brightnessPercent,
        contrastPercent = contrastPercent,
        saturationPercent = saturationPercent,
        autoEnhance = autoEnhance,
        removeBackground = removeBackground,
    )

    /** Convert the normalized crop to pixels in the rotated source's space. */
    fun cropPx(): CropRect? {
        val c = cropNorm ?: return null
        if (sourceWidthPx <= 0 || sourceHeightPx <= 0) return null
        val swap = rotationDegrees % 180 != 0
        val rW = if (swap) sourceHeightPx else sourceWidthPx
        val rH = if (swap) sourceWidthPx else sourceHeightPx
        val x = (c.left * rW).toInt().coerceIn(0, rW - 1)
        val y = (c.top * rH).toInt().coerceIn(0, rH - 1)
        val w = (c.width * rW).toInt().coerceIn(1, rW - x)
        val h = (c.height * rH).toInt().coerceIn(1, rH - y)
        return CropRect(x, y, w, h)
    }

    fun grid(): PhotoGrid? {
        val s = resolvedSize ?: return null
        return gridLayout(s.widthMm, s.heightMm, printPaper.widthMm, printPaper.heightMm)
    }

    fun copies(): CopiesResult? {
        val g = grid() ?: return null
        return resolveCopies(requestedCopies, g, printPaper.label)
    }
}

sealed interface PhotoExportState {
    data object Idle : PhotoExportState
    data object Working : PhotoExportState
    data class Saved(val files: List<ExportedFile>) : PhotoExportState
    data class Failed(val message: String) : PhotoExportState
}

/**
 * Drives the photo flow: source selection, live edit preview (including opt-in background removal),
 * and upload/print export from a single edited image. The Android-backed processor/segmenter/exporter
 * live here so the rest of the UI stays declarative.
 */
class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val segmenter = MlKitBackgroundSegmenter()
    private val processor = AndroidPhotoProcessor(application, segmenter)
    private val exporter = PhotoExporter(application, AndroidFileSaver(application), ::now)

    private val _state = MutableStateFlow(PhotoState())
    val state: StateFlow<PhotoState> = _state.asStateFlow()

    /** Edited preview bitmap (after all edits) and the untouched original, for before/after. */
    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()

    private val _original = MutableStateFlow<Bitmap?>(null)
    val original: StateFlow<Bitmap?> = _original.asStateFlow()

    private val _previewBusy = MutableStateFlow(false)
    val previewBusy: StateFlow<Boolean> = _previewBusy.asStateFlow()

    private val _exportState = MutableStateFlow<PhotoExportState>(PhotoExportState.Idle)
    val exportState: StateFlow<PhotoExportState> = _exportState.asStateFlow()

    private val _pendingShare = MutableStateFlow<List<ShareItem>?>(null)
    val pendingShare: StateFlow<List<ShareItem>?> = _pendingShare.asStateFlow()

    private var previewJob: Job? = null

    fun setSource(uri: String) {
        viewModelScope.launch {
            val size = processor.sourceSize(uri)
            _state.value = PhotoState(
                sourceUri = uri,
                sourceWidthPx = size?.first ?: 0,
                sourceHeightPx = size?.second ?: 0,
            )
            loadOriginal(uri)
            refreshPreview()
        }
    }

    private suspend fun loadOriginal(uri: String) {
        _original.value?.recycle()
        _original.value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(getApplication(), uri, PREVIEW_MAX_DIM)
        }
    }

    // --- edit mutations ---

    fun rotateClockwise() = mutate { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    fun setBrightness(v: Int) = mutate { it.copy(brightnessPercent = v) }
    fun setContrast(v: Int) = mutate { it.copy(contrastPercent = v) }
    fun setSaturation(v: Int) = mutate { it.copy(saturationPercent = v) }
    fun setAutoEnhance(v: Boolean) = mutate { it.copy(autoEnhance = v) }
    fun setRemoveBackground(v: Boolean) = mutate { it.copy(removeBackground = v) }
    fun setCropNorm(crop: NormCrop?) = mutate { it.copy(cropNorm = crop) }

    fun setAspectLocked(locked: Boolean) {
        val s = _state.value
        val crop = if (locked) lockedCropFor(s) else null
        _state.update { it.copy(aspectLocked = locked, cropNorm = crop) }
        refreshPreview()
    }

    /** Reset all edits to the untouched original. */
    fun revertEdits() {
        _state.update {
            it.copy(
                rotationDegrees = 0, cropNorm = null, aspectLocked = false,
                brightnessPercent = 0, contrastPercent = 0, saturationPercent = 0,
                autoEnhance = false, removeBackground = false,
            )
        }
        refreshPreview()
    }

    /** A centred, aspect-correct max crop for the selected size, in normalized rotated-source space. */
    private fun lockedCropFor(s: PhotoState): NormCrop? {
        val size = s.resolvedSize ?: return null
        val swap = s.rotationDegrees % 180 != 0
        val rW = (if (swap) s.sourceHeightPx else s.sourceWidthPx).toDouble()
        val rH = (if (swap) s.sourceWidthPx else s.sourceHeightPx).toDouble()
        if (rW <= 0 || rH <= 0) return null
        val target = size.aspectRatio // w/h
        val srcAspect = rW / rH
        return if (srcAspect > target) {
            val wFrac = (target / srcAspect).toFloat()
            val left = (1f - wFrac) / 2f
            NormCrop(left, 0f, left + wFrac, 1f)
        } else {
            val hFrac = (srcAspect / target).toFloat()
            val top = (1f - hFrac) / 2f
            NormCrop(0f, top, 1f, top + hFrac)
        }
    }

    private inline fun mutate(crossinline transform: (PhotoState) -> PhotoState) {
        _state.update(transform)
        refreshPreview()
    }

    /** Re-run the edit pipeline for the live preview, cancelling any in-flight render. */
    private fun refreshPreview() {
        val s = _state.value
        val uri = s.sourceUri ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            _previewBusy.value = true
            // The crop is shown as an overlay rectangle, so the preview renders the full (uncropped)
            // frame; the crop is applied only at export time.
            val params = s.editParams().copy(crop = null)
            val result = processor.process(uri, params, PREVIEW_MAX_DIM)
            val old = _preview.value
            _preview.value = result
            if (old !== result) old?.recycle()
            _previewBusy.value = false
        }
    }

    // --- size + export config ---

    fun selectSize(size: PhotoSize) {
        _state.update {
            val next = it.copy(size = size)
            // Re-lock the crop to the new aspect if the lock is on.
            if (it.aspectLocked) next.copy(cropNorm = lockedCropFor(next)) else next
        }
        if (_state.value.aspectLocked) refreshPreview()
    }

    fun setCustomSizeMm(widthMm: Double, heightMm: Double) = _state.update {
        it.copy(size = PhotoSize.CUSTOM, customWidthMm = widthMm, customHeightMm = heightMm)
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun toggleMode(mode: OutputMode) = _state.update {
        val modes = if (mode in it.modes) it.modes - mode else it.modes + mode
        it.copy(modes = modes)
    }
    fun setUploadMaxKb(kb: Int?) = _state.update { it.copy(uploadMaxKb = kb) }
    fun setPrintPaper(paper: PhotoPaper) = _state.update { it.copy(printPaper = paper) }
    fun setRequestedCopies(n: Int) = _state.update { it.copy(requestedCopies = n) }
    fun setCutMarks(v: Boolean) = _state.update { it.copy(cutMarks = v) }

    // --- save / share ---

    fun save() = runExport { editedUri, s, size ->
        val files = mutableListOf<ExportedFile>()
        if (OutputMode.UPLOAD in s.modes) {
            files += exporter.saveUpload(editedUri, size, s.name, s.uploadDpi, s.uploadMaxKb)
        }
        if (OutputMode.PRINT in s.modes) {
            val grid = s.grid()
            val copies = s.copies()
            if (grid != null && copies is CopiesResult.Ok) {
                files += exporter.savePrint(editedUri, size, s.name, grid, copies.finalCount, s.cutMarks)
            }
        }
        _exportState.value = PhotoExportState.Saved(files)
    }

    fun share() = runExport { editedUri, s, size ->
        val items = mutableListOf<ShareItem>()
        if (OutputMode.UPLOAD in s.modes) {
            items += exporter.shareUpload(editedUri, size, s.name, s.uploadDpi, s.uploadMaxKb)
        }
        if (OutputMode.PRINT in s.modes) {
            val grid = s.grid()
            val copies = s.copies()
            if (grid != null && copies is CopiesResult.Ok) {
                items += exporter.sharePrint(editedUri, size, s.name, grid, copies.finalCount, s.cutMarks)
            }
        }
        _pendingShare.value = items
        _exportState.value = PhotoExportState.Idle
    }

    private fun runExport(block: suspend (editedUri: String, state: PhotoState, size: ResolvedPhotoSize) -> Unit) {
        val s = _state.value
        val uri = s.sourceUri ?: return
        val size = s.resolvedSize ?: return
        if (s.modes.isEmpty()) return
        viewModelScope.launch {
            _exportState.value = PhotoExportState.Working
            try {
                // Process once at full resolution, persist, then export both selected modes from it.
                val edited = processor.process(uri, s.editParams(), EXPORT_MAX_DIM)
                    ?: error("Could not process the photo.")
                val editedUri = exporter.persistEdited(edited)
                edited.recycle()
                block(editedUri, s, size)
            } catch (e: Exception) {
                _exportState.value = PhotoExportState.Failed(e.message ?: "Couldn't export the photo.")
            }
        }
    }

    /**
     * Process the current source at full resolution, persist the edited image, and return its URI —
     * used by task mode to capture this photo as a task entry. Returns null if there's nothing to do.
     */
    suspend fun produceEditedImage(): String? {
        val s = _state.value
        val uri = s.sourceUri ?: return null
        val edited = processor.process(uri, s.editParams(), EXPORT_MAX_DIM) ?: return null
        val out = exporter.persistEdited(edited)
        edited.recycle()
        return out
    }

    fun shareHandled() { _pendingShare.value = null }
    fun clearExportResult() { _exportState.value = PhotoExportState.Idle }

    /** Clear everything for a fresh photo and delete cached edited images. */
    fun reset() {
        previewJob?.cancel()
        _preview.value?.recycle(); _preview.value = null
        _original.value?.recycle(); _original.value = null
        _state.value = PhotoState()
        _exportState.value = PhotoExportState.Idle
        _pendingShare.value = null
        viewModelScope.launch(Dispatchers.IO) {
            File(getApplication<Application>().filesDir, "photo").deleteRecursively()
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
        _preview.value?.recycle()
        _original.value?.recycle()
        segmenter.close()
    }

    private fun now(): FileTimestamp {
        val c = Calendar.getInstance()
        return FileTimestamp(
            year = c.get(Calendar.YEAR),
            month = c.get(Calendar.MONTH) + 1,
            day = c.get(Calendar.DAY_OF_MONTH),
            hour = c.get(Calendar.HOUR_OF_DAY),
            minute = c.get(Calendar.MINUTE),
            second = c.get(Calendar.SECOND),
        )
    }

    private companion object {
        const val PREVIEW_MAX_DIM = 1200
        const val EXPORT_MAX_DIM = 2400
    }
}
