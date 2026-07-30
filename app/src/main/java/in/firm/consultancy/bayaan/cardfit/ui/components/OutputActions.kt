package `in`.firm.consultancy.bayaan.cardfit.ui.components

import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.print.PrintHelper
import `in`.firm.consultancy.bayaan.cardfit.data.MimeTypes
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem

/**
 * The three things a user can do with an exported file. Previously each export screen carried its
 * own byte-identical copy of the share intent; they all route through here now, and Open/Print sit
 * alongside it so the post-save actions behave the same everywhere.
 *
 * Every launcher is guarded: a device with no PDF viewer, no share target, or no print service must
 * leave the user on the export screen rather than crash with ActivityNotFoundException.
 */

/** Share one or more prepared cache copies via the system chooser. */
fun launchShare(context: Context, items: List<ShareItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList(items.map { it.uri.toUri() })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items.first().mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share via")) }
}

/**
 * View a saved file in whichever app handles [mimeType]. Returns false when nothing can open it, so
 * the caller can surface a message instead of appearing to do nothing.
 */
fun launchOpen(context: Context, uri: String, mimeType: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri.toUri(), mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(intent) }.isSuccess
}

/**
 * Send a saved file to the system print dialog. Images go through [PrintHelper]; PDFs stream
 * straight to the printer via [PdfPrintAdapter] (the framework has no built-in PDF adapter).
 * Returns false when printing couldn't be started.
 */
fun launchPrint(context: Context, uri: String, mimeType: String?, jobName: String): Boolean =
    runCatching {
        if (mimeType == MimeTypes.JPEG) {
            PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
                // The print grid is already laid out for the sheet — don't let the helper re-fit it.
                colorMode = PrintHelper.COLOR_MODE_COLOR
            }.printBitmap(jobName, uri.toUri())
        } else {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                jobName,
                PdfPrintAdapter(context, uri, jobName),
                PrintAttributes.Builder().build(),
            )
        }
        true
    }.getOrDefault(false)

/**
 * The three things you can do with a saved file, at equal weight. All three apply to every output:
 * [launchPrint] routes images through [PrintHelper] and PDFs through [PdfPrintAdapter], so neither
 * format has an action that "doesn't apply" — an earlier two-per-type split left Open unreachable
 * for PDFs and Print unreachable for JPEGs.
 *
 * Renders nothing when the saver couldn't produce a URI — better no affordance than a dead one.
 * [onError] reports a launcher that found no handling app.
 */
@Composable
fun SavedFileActions(
    file: ExportedFile,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uri = file.uri ?: return
    val context = LocalContext.current
    val mimeType = file.mimeType ?: MimeTypes.PDF

    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionCell(
            icon = Icons.Filled.OpenInNew,
            label = "Open",
            modifier = Modifier.weight(1f),
        ) {
            if (!launchOpen(context, uri, file.mimeType)) {
                onError("No app on this device can open ${file.fileName}.")
            }
        }
        ActionCell(
            icon = Icons.Filled.Print,
            label = "Print",
            modifier = Modifier.weight(1f),
        ) {
            if (!launchPrint(context, uri, file.mimeType, file.fileName)) {
                onError("No print service is available on this device.")
            }
        }
        ActionCell(
            icon = Icons.Filled.Share,
            label = "Share",
            modifier = Modifier.weight(1f),
        ) {
            launchShare(context, listOf(ShareItem(uri, mimeType)))
        }
    }
}

/** One compact icon-over-label action pill; the three peers divide the row evenly. */
@Composable
private fun ActionCell(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GhostButtonSmall(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

/**
 * Streams an already-rendered PDF to the print spooler unchanged. The document is laid out for its
 * paper size at export time, so this reports a single unknown-length document and copies bytes
 * verbatim rather than re-paginating.
 */
private class PdfPrintAdapter(
    private val context: Context,
    private val uri: String,
    private val jobName: String,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (destination == null) {
            callback?.onWriteFailed("No destination for the print job.")
            return
        }
        runCatching {
            context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Couldn't read the saved file.")
        }.onSuccess {
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }.onFailure {
            callback?.onWriteFailed(it.message)
        }
    }
}
