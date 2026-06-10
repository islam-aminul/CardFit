package `in`.firm.consultancy.bayaan.cardfit.data.task

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import `in`.firm.consultancy.bayaan.cardfit.domain.task.DocumentEntry
import `in`.firm.consultancy.bayaan.cardfit.domain.task.Task
import `in`.firm.consultancy.bayaan.cardfit.domain.task.TaskJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device task persistence (CLAUDE.md Phase 14, item 3). Each task lives in its own private
 * directory `filesDir/tasks/<taskId>/` holding `task.json` (kotlinx-serialization) plus the captured
 * image files. Nothing leaves the device; no new permissions; deleting a task removes its directory
 * (and therefore every image it owns). No identity numbers are stored — only image bytes + metadata.
 */
class AndroidTaskStore(private val context: Context) {

    private val root: File get() = File(context.filesDir, TASKS_DIR)

    fun taskDir(taskId: String): File = File(root, taskId)

    /** Load all saved tasks (newest first), skipping any directory without a readable task.json. */
    suspend fun loadAll(): List<Task> = withContext(Dispatchers.IO) {
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: return@withContext emptyList()
        dirs.mapNotNull { dir ->
            val json = File(dir, TASK_FILE).takeIf { it.exists() }?.readText() ?: return@mapNotNull null
            runCatching { TaskJson.decode(json) }.getOrNull()
        }.sortedByDescending { it.createdAt }
    }

    suspend fun load(taskId: String): Task? = withContext(Dispatchers.IO) {
        val file = File(taskDir(taskId), TASK_FILE)
        if (!file.exists()) return@withContext null
        runCatching { TaskJson.decode(file.readText()) }.getOrNull()
    }

    suspend fun save(task: Task) = withContext(Dispatchers.IO) {
        val dir = taskDir(task.id).apply { mkdirs() }
        File(dir, TASK_FILE).writeText(TaskJson.encode(task))
    }

    /** Delete the whole task directory, including every image file it owns. */
    suspend fun delete(taskId: String) = withContext(Dispatchers.IO) {
        taskDir(taskId).deleteRecursively()
        Unit
    }

    /** Delete specific image files from a task directory (e.g. after an entry is removed/re-scanned). */
    suspend fun deleteImages(taskId: String, fileNames: List<String>) = withContext(Dispatchers.IO) {
        val dir = taskDir(taskId)
        fileNames.forEach { File(dir, it).delete() }
    }

    /**
     * Copy a captured image (from a `file://`/`content://` source URI) into the task directory under
     * [destName], returning its pixel dimensions (width, height) for aspect classification. Returns
     * null if the source can't be read.
     */
    suspend fun importImage(taskId: String, sourceUri: String, destName: String): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            val dir = taskDir(taskId).apply { mkdirs() }
            val dest = File(dir, destName)
            val copied = context.contentResolver.openInputStream(sourceUri.toUri())?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied || dest.length() <= 0L) {
                dest.delete()
                return@withContext null
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, bounds)
            bounds.outWidth.coerceAtLeast(0) to bounds.outHeight.coerceAtLeast(0)
        }

    /** Absolute `file://` URI string for an image filename inside a task directory. */
    fun imageUri(taskId: String, fileName: String): String =
        File(taskDir(taskId), fileName).toUri().toString()

    /** Rebuild a [ScanSession] for a DOCUMENT entry, resolving its image filenames to file URIs. */
    fun documentSession(taskId: String, entry: DocumentEntry): ScanSession {
        val front = entry.frontImage?.let {
            ScannedSide(imageUri(taskId, it), entry.frontWidthPx, entry.frontHeightPx)
        }
        val back = entry.backImage?.let {
            ScannedSide(imageUri(taskId, it), entry.backWidthPx, entry.backHeightPx)
        }
        return ScanSession(
            cardType = requireNotNull(entry.cardType) { "Document entry missing card type" },
            front = front,
            back = back,
            customWidthMm = entry.customWidthMm,
            customHeightMm = entry.customHeightMm,
        )
    }

    private companion object {
        const val TASKS_DIR = "tasks"
        const val TASK_FILE = "task.json"
    }
}
