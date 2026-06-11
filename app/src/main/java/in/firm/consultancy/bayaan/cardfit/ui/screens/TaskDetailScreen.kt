package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.task.DocumentEntry
import `in`.firm.consultancy.bayaan.cardfit.domain.task.EntryKind
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskExportState
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Task detail (CLAUDE.md Phase 14): name the task, set a default upload cap, add documents/photos,
 * manage the entry list (rename, reorder, delete, per-entry cap), and export individually or as one
 * combined multi-page PDF.
 */
@Composable
fun TaskDetailScreen(
    viewModel: TaskViewModel,
    onAddDocument: () -> Unit,
    onAddPhoto: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val currentId by viewModel.currentTaskId.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pendingShare by viewModel.pendingShare.collectAsStateWithLifecycle()
    val task = tasks.find { it.id == currentId }

    LaunchedEffect(pendingShare) {
        val items = pendingShare ?: return@LaunchedEffect
        launchTaskShare(context, items)
        viewModel.shareHandled()
    }

    var editingEntry by remember { mutableStateOf<DocumentEntry?>(null) }

    ScreenScaffold(
        title = "Task",
        scrollState = rememberScrollState(),
        bottomBar = {
            ScaffoldBottomBar {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to tasks") }
            }
        },
    ) {
        if (task == null) {
            Text("Task not found.")
            return@ScreenScaffold
        }

        OutlinedTextField(
            value = task.name,
            onValueChange = viewModel::renameTask,
            label = { Text("Task name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DefaultMaxKbField(task.defaultMaxFileSizeKb, viewModel::setDefaultMaxKb)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAddDocument, modifier = Modifier.weight(1f)) { Text("Add document") }
            Button(onClick = onAddPhoto, modifier = Modifier.weight(1f)) { Text("Add photo") }
        }

        HorizontalDivider()

        if (task.documents.isEmpty()) {
            Text(
                "No documents yet. Add a scanned document or a photo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        task.documents.forEachIndexed { index, entry ->
            EntryRow(
                viewModel = viewModel,
                entry = entry,
                isFirst = index == 0,
                isLast = index == task.documents.size - 1,
                onEdit = { editingEntry = entry },
            )
        }

        HorizontalDivider()

        Text("Export", style = MaterialTheme.typography.titleSmall)
        val canExport = task.documents.any { it.frontImage != null } && exportState !is TaskExportState.Working
        Button(onClick = { viewModel.exportIndividual() }, enabled = canExport, modifier = Modifier.fillMaxWidth()) {
            Text("Individual upload files")
        }
        Button(onClick = { viewModel.exportCombined(OutputMode.PRINT) }, enabled = canExport, modifier = Modifier.fillMaxWidth()) {
            Text("Combined PDF — print (actual size)")
        }
        Button(onClick = { viewModel.exportCombined(OutputMode.UPLOAD) }, enabled = canExport, modifier = Modifier.fillMaxWidth()) {
            Text("Combined PDF — upload (fit width)")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.shareIndividual() }, enabled = canExport, modifier = Modifier.weight(1f)) {
                Text("Share individual")
            }
            OutlinedButton(onClick = { viewModel.shareCombined(OutputMode.UPLOAD) }, enabled = canExport, modifier = Modifier.weight(1f)) {
                Text("Share combined")
            }
        }

        TaskExportStatus(exportState)
    }

    editingEntry?.let { entry ->
        EntryEditDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { name, maxKb ->
                viewModel.renameEntry(entry.id, name)
                viewModel.setEntryMaxKb(entry.id, maxKb)
                editingEntry = null
            },
        )
    }
}

@Composable
private fun EntryRow(
    viewModel: TaskViewModel,
    entry: DocumentEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp))) {
                AsyncImage(
                    model = viewModel.entryThumbUri(entry),
                    contentDescription = "Document thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.personName.ifBlank { "(no name)" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    typeLabel(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { viewModel.moveEntry(entry.id, -1) }, enabled = !isFirst) { Text("↑") }
            TextButton(onClick = { viewModel.moveEntry(entry.id, +1) }, enabled = !isLast) { Text("↓") }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = { viewModel.deleteEntry(entry.id) }) { Text("✕") }
        }
    }
}

private fun typeLabel(entry: DocumentEntry): String = when (entry.kind) {
    EntryKind.PHOTO -> "Photo"
    EntryKind.DOCUMENT -> entry.cardType?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Document"
}

@Composable
private fun DefaultMaxKbField(value: Int?, onChange: (Int?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.trim().toIntOrNull()?.takeIf { n -> n > 0 })
        },
        label = { Text("Default upload max size (KB) — optional") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EntryEditDialog(
    entry: DocumentEntry,
    onDismiss: () -> Unit,
    onSave: (name: String, maxKb: Int?) -> Unit,
) {
    var name by remember { mutableStateOf(entry.personName) }
    var kbText by remember { mutableStateOf(entry.maxFileSizeKbOverride?.toString() ?: "") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit document") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kbText,
                    onValueChange = { kbText = it },
                    label = { Text("Max size override (KB)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name.trim(), kbText.trim().toIntOrNull()?.takeIf { it > 0 })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TaskExportStatus(state: TaskExportState) {
    when (state) {
        TaskExportState.Idle -> Unit
        TaskExportState.Working -> {
            CircularProgressIndicator()
            Text("Working…")
        }
        is TaskExportState.Saved -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Saved ${state.files.size} file(s):", style = MaterialTheme.typography.titleSmall)
                state.files.forEach { file ->
                    Text("• ${file.fileName}", style = MaterialTheme.typography.bodySmall)
                    file.warning?.let {
                        Text("  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        is TaskExportState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
    }
}

private fun launchTaskShare(context: Context, items: List<ShareItem>) {
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
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
