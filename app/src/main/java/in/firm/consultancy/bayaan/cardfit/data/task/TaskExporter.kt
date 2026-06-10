package `in`.firm.consultancy.bayaan.cardfit.data.task

import android.content.Context
import `in`.firm.consultancy.bayaan.cardfit.data.FileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.MimeTypes
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.data.photo.PhotoRenderers
import `in`.firm.consultancy.bayaan.cardfit.data.render.AndroidJpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.FilenameBuilder
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.task.DocumentEntry
import `in`.firm.consultancy.bayaan.cardfit.domain.task.EntryKind
import `in`.firm.consultancy.bayaan.cardfit.domain.task.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Task exports (CLAUDE.md Phase 14, items 4–5):
 *  - INDIVIDUAL: each entry as a separate upload file via the existing per-document pipeline (document
 *    -> JPEG renderer; photo -> photo JPEG renderer), capped by the per-entry override or the task
 *    default. Named `{personSlug}-{docSlug}-{taskSlug}-upload-…`.
 *  - COMBINED: one multi-page PDF via [CombinedPdfRenderer] (one page per entry; global upload size
 *    loop). Named `{taskSlug}-combined-{purpose}-…`.
 *
 * Renderers/saver/clock are injected so the orchestration could be faked; Android types stay behind
 * the renderers.
 */
class TaskExporter(
    private val context: Context,
    private val store: AndroidTaskStore,
    private val fileSaver: FileSaver,
    private val clock: () -> FileTimestamp,
) {
    private val jpegRenderer = AndroidJpegRenderer(context)
    private val combinedRenderer = CombinedPdfRenderer(context)

    suspend fun saveIndividual(task: Task): List<ExportedFile> = withContext(Dispatchers.Default) {
        entriesWithImages(task).map { entry ->
            val output = renderEntryUpload(task, entry)
            val fileName = individualName(task, entry)
            val location = fileSaver.save(fileName, MimeTypes.JPEG, output.bytes)
            ExportedFile(fileName, location, output.sizeWarning)
        }
    }

    suspend fun shareIndividual(task: Task): List<ShareItem> = withContext(Dispatchers.Default) {
        entriesWithImages(task).map { entry ->
            val output = renderEntryUpload(task, entry)
            val fileName = individualName(task, entry)
            ShareItem(fileSaver.cacheForShare(fileName, MimeTypes.JPEG, output.bytes), MimeTypes.JPEG)
        }
    }

    suspend fun saveCombined(task: Task, mode: OutputMode): ExportedFile = withContext(Dispatchers.Default) {
        val output = combinedRenderer.render(task, mode, task.defaultMaxFileSizeKb)
        val fileName = combinedName(task, mode)
        val location = fileSaver.save(fileName, MimeTypes.PDF, output.bytes)
        ExportedFile(fileName, location, output.sizeWarning)
    }

    suspend fun shareCombined(task: Task, mode: OutputMode): ShareItem = withContext(Dispatchers.Default) {
        val output = combinedRenderer.render(task, mode, task.defaultMaxFileSizeKb)
        val fileName = combinedName(task, mode)
        ShareItem(fileSaver.cacheForShare(fileName, MimeTypes.PDF, output.bytes), MimeTypes.PDF)
    }

    private fun entriesWithImages(task: Task): List<DocumentEntry> =
        task.documents.filter { it.frontImage != null }

    private suspend fun renderEntryUpload(task: Task, entry: DocumentEntry): RenderedOutput {
        val maxKb = entry.maxFileSizeKbOverride ?: task.defaultMaxFileSizeKb
        return when (entry.kind) {
            EntryKind.DOCUMENT -> {
                val session = store.documentSession(task.id, entry)
                val config = RenderConfig(
                    mode = OutputMode.UPLOAD,
                    paper = PaperSize.A4,
                    format = OutputFormat.JPEG,
                    dpi = Defaults.UPLOAD_DPI,
                    grayscale = false,
                    cropMarks = false,
                    maxFileSizeKb = maxKb,
                    searchableText = false,
                    sizeOverride = entry.sizeOverride,
                )
                jpegRenderer.render(session, config)
            }
            EntryKind.PHOTO -> {
                val uri = store.imageUri(task.id, requireNotNull(entry.frontImage))
                PhotoRenderers.renderUploadJpeg(
                    context = context,
                    editedUri = uri,
                    widthMm = entry.photoWidthMm ?: 35.0,
                    heightMm = entry.photoHeightMm ?: 45.0,
                    dpi = Defaults.PRINT_DPI,
                    maxFileSizeKb = maxKb,
                )
            }
        }
    }

    private fun individualName(task: Task, entry: DocumentEntry): String =
        FilenameBuilder.buildTaskIndividual(
            personName = entry.personName,
            docSlug = entry.docSlug,
            taskName = task.name,
            format = OutputFormat.JPEG,
            timestamp = clock(),
            exists = fileSaver::exists,
        )

    private fun combinedName(task: Task, mode: OutputMode): String =
        FilenameBuilder.buildTaskCombined(
            taskName = task.name,
            mode = mode,
            timestamp = clock(),
            exists = fileSaver::exists,
        )
}
