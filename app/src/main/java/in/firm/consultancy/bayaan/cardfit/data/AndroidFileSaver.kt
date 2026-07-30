package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saves outputs to the public Downloads collection and prepares share copies (CLAUDE.md sections 8,
 * 11). Uses MediaStore on API 29+ (no storage permission needed); on API 24–28 it writes to the
 * public Downloads directory, which requires WRITE_EXTERNAL_STORAGE (requested at save time).
 *
 * Sharing copies go to a private cache directory exposed through a FileProvider, so identity numbers
 * never sit in a world-readable location.
 */
class AndroidFileSaver(private val context: Context) : FileSaver {

    override fun exists(fileName: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            context.contentResolver.query(collection, projection, selection, arrayOf(fileName), null)
                ?.use { cursor -> cursor.count > 0 } ?: false
        } else {
            File(File(legacyDownloadsDir(), APP_SUBDIR), fileName).exists()
        }
    }

    override suspend fun save(fileName: String, mimeType: String, bytes: ByteArray): SavedFile =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(fileName, mimeType, bytes)
            } else {
                saveLegacy(fileName, mimeType, bytes)
            }
        }

    override suspend fun cacheForShare(fileName: String, mimeType: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            val file = File(dir, fileName)
            file.outputStream().use { it.write(bytes) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(fileName: String, mimeType: String, bytes: ByteArray): SavedFile {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$APP_SUBDIR")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: error("Couldn't create a Downloads entry")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Couldn't open the Downloads file for writing")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        // Keep the MediaStore URI: it is what lets the UI open or print the file afterwards.
        return SavedFile("${Environment.DIRECTORY_DOWNLOADS}/$APP_SUBDIR/$fileName", uri.toString())
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(fileName: String, mimeType: String, bytes: ByteArray): SavedFile {
        val dir = File(legacyDownloadsDir(), APP_SUBDIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { it.write(bytes) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
        // A raw file:// URI would trip FileUriExposedException on ACTION_VIEW, so hand out a
        // FileProvider URI instead; if the path isn't covered by file_paths.xml, drop the actions.
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
        }.getOrNull()
        return SavedFile(file.absolutePath, uri)
    }

    @Suppress("DEPRECATION")
    private fun legacyDownloadsDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private companion object {
        const val APP_SUBDIR = "CardFit"
        const val SHARE_DIR = "shared"
    }
}
