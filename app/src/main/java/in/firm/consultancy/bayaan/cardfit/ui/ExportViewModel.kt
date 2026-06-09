package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidFileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.Exporter
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.data.ocr.MlKitOcr
import `in`.firm.consultancy.bayaan.cardfit.data.render.AndroidJpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.render.AndroidPdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.PassThroughTextFilter
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Working : ExportUiState
    data class Saved(val files: List<ExportedFile>) : ExportUiState
    data class Failed(val message: String) : ExportUiState
}

/**
 * Drives preview generation and export/share for the Preview screen. Holds the Android-backed
 * renderers + saver (so [AppViewModel] stays pure). Re-running [save]/[share] re-exports from the
 * same [ScanSession] — supporting "return to Configure and re-export without re-scanning".
 */
class ExportViewModel(application: Application) : AndroidViewModel(application) {

    // Used by the PDF renderer for the optional searchable text layer; closed in onCleared.
    private val ocr = MlKitOcr(application)

    private val exporter = Exporter(
        pdfRenderer = AndroidPdfRenderer(application, ocr, PassThroughTextFilter),
        jpegRenderer = AndroidJpegRenderer(application),
        fileSaver = AndroidFileSaver(application),
        clock = ::now,
    )

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _previewBytes = MutableStateFlow<ByteArray?>(null)
    val previewBytes: StateFlow<ByteArray?> = _previewBytes.asStateFlow()

    /** True once a preview attempt finished without producing an image. */
    private val _previewFailed = MutableStateFlow(false)
    val previewFailed: StateFlow<Boolean> = _previewFailed.asStateFlow()

    private val _pendingShare = MutableStateFlow<List<ShareItem>?>(null)
    val pendingShare: StateFlow<List<ShareItem>?> = _pendingShare.asStateFlow()

    fun generatePreview(session: ScanSession, configs: List<RenderConfig>) {
        val config = configs.firstOrNull() ?: return
        viewModelScope.launch {
            _previewFailed.value = false
            _previewBytes.value = null
            val bytes = runCatching { exporter.preview(session, config) }.getOrNull()
            _previewBytes.value = bytes
            _previewFailed.value = bytes == null
        }
    }

    fun save(session: ScanSession, name: String, configs: List<RenderConfig>) {
        if (configs.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ExportUiState.Working
            _uiState.value = try {
                ExportUiState.Saved(exporter.export(session, name, configs))
            } catch (e: Exception) {
                ExportUiState.Failed(e.message ?: "Couldn't save the file(s).")
            }
        }
    }

    fun share(session: ScanSession, name: String, configs: List<RenderConfig>) {
        if (configs.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ExportUiState.Working
            try {
                _pendingShare.value = exporter.prepareShare(session, name, configs)
                _uiState.value = ExportUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ExportUiState.Failed(e.message ?: "Couldn't prepare the file(s) to share.")
            }
        }
    }

    fun shareHandled() {
        _pendingShare.value = null
    }

    fun clearResult() {
        _uiState.value = ExportUiState.Idle
    }

    /** Surface an error raised outside the export coroutine (e.g. a denied storage permission). */
    fun reportError(message: String) {
        _uiState.value = ExportUiState.Failed(message)
    }

    /** Clear all export/preview/share UI state for a fresh document. */
    fun resetForNewSession() {
        _uiState.value = ExportUiState.Idle
        _previewBytes.value = null
        _previewFailed.value = false
        _pendingShare.value = null
    }

    /** Delete the cached scanned-side images so nothing leaks into the next document. */
    fun discardScans() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().filesDir, "scans")
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    override fun onCleared() {
        ocr.close()
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
}
