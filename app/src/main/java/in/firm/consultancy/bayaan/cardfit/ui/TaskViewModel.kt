package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidFileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.data.ocr.MlKitOcr
import `in`.firm.consultancy.bayaan.cardfit.data.task.AndroidTaskStore
import `in`.firm.consultancy.bayaan.cardfit.data.task.TaskExporter
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.NameParser
import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.task.DocumentEntry
import `in`.firm.consultancy.bayaan.cardfit.domain.task.EntryKind
import `in`.firm.consultancy.bayaan.cardfit.domain.task.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed interface TaskExportState {
    data object Idle : TaskExportState
    data object Working : TaskExportState
    data class Saved(val files: List<ExportedFile>) : TaskExportState
    data class Failed(val message: String) : TaskExportState
}

/**
 * Drives task mode (CLAUDE.md Phase 14): the list of saved tasks, the currently open task, adding
 * document/photo entries (reusing the scan and photo flows), per-entry editing/reorder/delete, and
 * the individual/combined exports. All state is persisted via [AndroidTaskStore] so tasks survive
 * restarts; deleting a task removes its image files.
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AndroidTaskStore(application)
    private val ocr = MlKitOcr(application)
    private val exporter = TaskExporter(application, store, AndroidFileSaver(application), ::now)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskId: StateFlow<String?> = _currentTaskId.asStateFlow()

    private val _exportState = MutableStateFlow<TaskExportState>(TaskExportState.Idle)
    val exportState: StateFlow<TaskExportState> = _exportState.asStateFlow()

    private val _pendingShare = MutableStateFlow<List<ShareItem>?>(null)
    val pendingShare: StateFlow<List<ShareItem>?> = _pendingShare.asStateFlow()

    init {
        refresh()
    }

    fun current(): Task? = _tasks.value.find { it.id == _currentTaskId.value }

    fun refresh() {
        viewModelScope.launch { _tasks.value = store.loadAll() }
    }

    fun openTask(id: String) { _currentTaskId.value = id }

    /** File URI of an entry's front image (for thumbnails), or null if it has no image yet. */
    fun entryThumbUri(entry: DocumentEntry): String? {
        val taskId = _currentTaskId.value ?: return null
        val front = entry.frontImage ?: return null
        return store.imageUri(taskId, front)
    }

    /** Create a new task, persist it, open it, and return its id. */
    fun createTask(name: String, onCreated: (String) -> Unit) {
        val task = Task(id = UUID.randomUUID().toString(), name = name.trim(), createdAt = System.currentTimeMillis())
        viewModelScope.launch {
            store.save(task)
            _tasks.value = listOf(task) + _tasks.value
            _currentTaskId.value = task.id
            onCreated(task.id)
        }
    }

    fun renameTask(name: String) = updateCurrent { it.copy(name = name) }

    fun setDefaultMaxKb(kb: Int?) = updateCurrent { it.copy(defaultMaxFileSizeKb = kb) }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            store.delete(id)
            if (_currentTaskId.value == id) _currentTaskId.value = null
            _tasks.value = _tasks.value.filterNot { it.id == id }
        }
    }

    /** Add a scanned document to the current task, copying its images and OCR-suggesting a person name. */
    suspend fun addDocumentEntry(session: ScanSession, sizeOverride: SizeOverride) {
        val task = current() ?: return
        val entryId = shortId()
        val frontUri = session.front?.imageUri ?: return
        val frontName = "$entryId-front.jpg"
        val frontDims = store.importImage(task.id, frontUri, frontName) ?: return

        var backName: String? = null
        var backDims: Pair<Int, Int>? = null
        session.back?.imageUri?.let { backUri ->
            backName = "$entryId-back.jpg"
            backDims = store.importImage(task.id, backUri, backName!!)
        }

        val suggested = runCatching {
            NameParser.parse(session.cardType, ocr.recognize(frontUri))?.takeIf { it.isNotBlank() }
        }.getOrNull()

        val entry = DocumentEntry(
            id = entryId,
            personName = suggested.orEmpty(),
            kind = EntryKind.DOCUMENT,
            cardType = session.cardType,
            frontImage = frontName,
            frontWidthPx = frontDims.first,
            frontHeightPx = frontDims.second,
            backImage = backName,
            backWidthPx = backDims?.first ?: 0,
            backHeightPx = backDims?.second ?: 0,
            customWidthMm = session.customWidthMm,
            customHeightMm = session.customHeightMm,
            sizeOverride = sizeOverride,
        )
        persist(task.withEntry(entry))
    }

    /** Add an edited Phase 13 photo to the current task at the given physical size. */
    suspend fun addPhotoEntry(editedUri: String, widthMm: Double, heightMm: Double) {
        val task = current() ?: return
        val entryId = shortId()
        val name = "$entryId-photo.jpg"
        val dims = store.importImage(task.id, editedUri, name) ?: return
        val entry = DocumentEntry(
            id = entryId,
            personName = "",
            kind = EntryKind.PHOTO,
            frontImage = name,
            frontWidthPx = dims.first,
            frontHeightPx = dims.second,
            photoWidthMm = widthMm,
            photoHeightMm = heightMm,
        )
        persist(task.withEntry(entry))
    }

    fun renameEntry(entryId: String, name: String) =
        updateCurrent { task -> task.mapEntry(entryId) { it.copy(personName = name) } }

    fun setEntryMaxKb(entryId: String, kb: Int?) =
        updateCurrent { task -> task.mapEntry(entryId) { it.copy(maxFileSizeKbOverride = kb) } }

    fun deleteEntry(entryId: String) {
        val task = current() ?: return
        val entry = task.documents.find { it.id == entryId } ?: return
        viewModelScope.launch {
            store.deleteImages(task.id, entry.imageFiles())
            persist(task.copy(documents = task.documents.filterNot { it.id == entryId }))
        }
    }

    fun moveEntry(entryId: String, delta: Int) = updateCurrent { task ->
        val list = task.documents.toMutableList()
        val i = list.indexOfFirst { it.id == entryId }
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return@updateCurrent task
        list.add(j, list.removeAt(i))
        task.copy(documents = list)
    }

    // --- export ---

    fun exportIndividual() = runExport { task -> _exportState.value = TaskExportState.Saved(exporter.saveIndividual(task)) }

    fun exportCombined(mode: OutputMode) = runExport { task ->
        _exportState.value = TaskExportState.Saved(listOf(exporter.saveCombined(task, mode)))
    }

    fun shareIndividual() = runShare { task -> exporter.shareIndividual(task) }
    fun shareCombined(mode: OutputMode) = runShare { task -> listOf(exporter.shareCombined(task, mode)) }

    fun shareHandled() { _pendingShare.value = null }
    fun clearExportResult() { _exportState.value = TaskExportState.Idle }

    private fun runExport(block: suspend (Task) -> Unit) {
        val task = current() ?: return
        if (task.documents.none { it.frontImage != null }) {
            _exportState.value = TaskExportState.Failed("Add at least one document first.")
            return
        }
        viewModelScope.launch {
            _exportState.value = TaskExportState.Working
            try {
                block(task)
            } catch (e: Exception) {
                _exportState.value = TaskExportState.Failed(e.message ?: "Couldn't export the task.")
            }
        }
    }

    private fun runShare(block: suspend (Task) -> List<ShareItem>) {
        val task = current() ?: return
        if (task.documents.none { it.frontImage != null }) {
            _exportState.value = TaskExportState.Failed("Add at least one document first.")
            return
        }
        viewModelScope.launch {
            _exportState.value = TaskExportState.Working
            try {
                _pendingShare.value = block(task)
                _exportState.value = TaskExportState.Idle
            } catch (e: Exception) {
                _exportState.value = TaskExportState.Failed(e.message ?: "Couldn't prepare the task to share.")
            }
        }
    }

    private fun updateCurrent(transform: (Task) -> Task) {
        val task = current() ?: return
        persist(transform(task))
    }

    /** Replace the task in memory and persist it to disk. */
    private fun persist(task: Task) {
        _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
        viewModelScope.launch { store.save(task) }
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

    private fun shortId(): String = UUID.randomUUID().toString().take(8)
}

private fun Task.withEntry(entry: DocumentEntry): Task = copy(documents = documents + entry)

private fun Task.mapEntry(entryId: String, transform: (DocumentEntry) -> DocumentEntry): Task =
    copy(documents = documents.map { if (it.id == entryId) transform(it) else it })
