package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.data.MimeTypes
import `in`.firm.consultancy.bayaan.cardfit.data.export.ExportedFile
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * What the user sees after a successful export: where the files went, and what they can do with each
 * one. A floating sheet rather than an inline callout — the previous in-page version grew at the
 * same time as the bottom bar, which forced a two-frame delay and an auto-scroll-to-bottom just to
 * keep it visible.
 *
 * [footer] carries the caller's next-step actions ("New scan" / "Home"), which used to be appended
 * to the screen's bottom bar. Dismissing is the caller's cue to clear its export state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportResultSheet(
    files: List<ExportedFile>,
    onDismiss: () -> Unit,
    onActionError: (String) -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (files.size == 1) "1 file saved" else "${files.size} files saved",
                style = MaterialTheme.typography.titleMedium,
                color = TextHeading,
            )
            // The save location has always been recorded on ExportedFile and never shown; the
            // button promises "Downloads" so the confirmation should say exactly where it landed.
            folderOf(files)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            files.forEach { file -> SavedFileCard(file = file, onActionError = onActionError) }

            if (files.isNotEmpty()) Spacer(Modifier.height(4.dp))
            footer()
        }
    }
}

@Composable
private fun SavedFileCard(file: ExportedFile, onActionError: (String) -> Unit) {
    BayaanCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (file.mimeType == MimeTypes.JPEG) {
                    JpegArt(accent = Teal500, modifier = Modifier.size(28.dp))
                } else {
                    PdfArt(accent = Teal500, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextHeading,
                    )
                    // e.g. "4 photos on this sheet" — only the photo flow sets this today.
                    file.detail?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    file.warning?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            SavedFileActions(file = file, onError = onActionError)
        }
    }
}

/** The directory the files landed in, from the first entry's saved location ("Download/CardFit"). */
private fun folderOf(files: List<ExportedFile>): String? {
    val location = files.firstOrNull()?.savedLocation ?: return null
    val folder = location.substringBeforeLast('/', missingDelimiterValue = "")
    return if (folder.isBlank()) null else folder
}
