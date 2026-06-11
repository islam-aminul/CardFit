package `in`.firm.consultancy.bayaan.cardfit.data.photo

import android.content.Context
import android.graphics.Bitmap
import `in`.firm.consultancy.bayaan.cardfit.data.FileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.MimeTypes
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.FilenameBuilder
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoGrid
import `in`.firm.consultancy.bayaan.cardfit.domain.ResolvedPhotoSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Orchestrates the photo flow's UPLOAD (JPEG) and PRINT (single-page PDF grid) exports from one
 * edited image (CLAUDE.md Phase 13). The edited bitmap is persisted once to app storage; both
 * exports decode from that file, so the same edit re-exports to either mode without reprocessing.
 * Naming uses the Phase 13 photo template; saving/sharing/collision-checks go through [FileSaver].
 */
class PhotoExporter(
    private val context: Context,
    private val fileSaver: FileSaver,
    private val clock: () -> FileTimestamp,
) {
    /** Persist an edited bitmap to private app storage as a JPEG and return its `file://` URI string. */
    suspend fun persistEdited(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
        // One edited file at a time — prune older edits before writing the new one.
        dir.listFiles()?.forEach { it.delete() }
        val dest = File(dir, "edited-${System.currentTimeMillis()}.jpg")
        dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        dest.toUri().toString()
    }

    suspend fun saveUpload(
        editedUri: String,
        size: ResolvedPhotoSize,
        name: String,
        dpi: Int,
        maxFileSizeKb: Int?,
    ): ExportedFile = withContext(Dispatchers.Default) {
        val output = PhotoRenderers.renderUploadJpeg(
            context, editedUri, size.widthMm, size.heightMm, dpi, maxFileSizeKb,
        )
        val fileName = photoName(name, OutputMode.UPLOAD, OutputFormat.JPEG)
        val location = fileSaver.save(fileName, MimeTypes.JPEG, output.bytes)
        ExportedFile(fileName, location, output.sizeWarning)
    }

    suspend fun savePrint(
        editedUri: String,
        size: ResolvedPhotoSize,
        name: String,
        grid: PhotoGrid,
        finalCount: Int,
        cutMarks: Boolean,
    ): ExportedFile = withContext(Dispatchers.Default) {
        val output = PhotoRenderers.renderPrintPdf(
            context, editedUri, size.widthMm, size.heightMm, grid, finalCount, cutMarks,
        )
        val fileName = photoName(name, OutputMode.PRINT, OutputFormat.PDF)
        val location = fileSaver.save(fileName, MimeTypes.PDF, output.bytes)
        val photos = if (finalCount == 1) "1 photo" else "$finalCount photos"
        ExportedFile(fileName, location, output.sizeWarning, detail = "$photos on this sheet")
    }

    suspend fun shareUpload(
        editedUri: String,
        size: ResolvedPhotoSize,
        name: String,
        dpi: Int,
        maxFileSizeKb: Int?,
    ): ShareItem = withContext(Dispatchers.Default) {
        val output = PhotoRenderers.renderUploadJpeg(
            context, editedUri, size.widthMm, size.heightMm, dpi, maxFileSizeKb,
        )
        val fileName = photoName(name, OutputMode.UPLOAD, OutputFormat.JPEG)
        ShareItem(fileSaver.cacheForShare(fileName, MimeTypes.JPEG, output.bytes), MimeTypes.JPEG)
    }

    suspend fun sharePrint(
        editedUri: String,
        size: ResolvedPhotoSize,
        name: String,
        grid: PhotoGrid,
        finalCount: Int,
        cutMarks: Boolean,
    ): ShareItem = withContext(Dispatchers.Default) {
        val output = PhotoRenderers.renderPrintPdf(
            context, editedUri, size.widthMm, size.heightMm, grid, finalCount, cutMarks,
        )
        val fileName = photoName(name, OutputMode.PRINT, OutputFormat.PDF)
        ShareItem(fileSaver.cacheForShare(fileName, MimeTypes.PDF, output.bytes), MimeTypes.PDF)
    }

    private fun photoName(name: String, mode: OutputMode, format: OutputFormat): String =
        FilenameBuilder.buildPhoto(
            name = name,
            mode = mode,
            format = format,
            timestamp = clock(),
            exists = fileSaver::exists,
        )

    private companion object {
        const val PHOTO_DIR = "photo"
    }
}
