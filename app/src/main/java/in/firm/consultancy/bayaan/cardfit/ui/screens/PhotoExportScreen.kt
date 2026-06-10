package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.domain.CopiesResult
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoPaper
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoExportState
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard

/**
 * Photo flow step 4 (CLAUDE.md Phase 13): choose Upload and/or Print, set the name and per-mode
 * options (upload max-KB cap; print paper, copies and cut marks), then Save or Share. Upload yields a
 * single exact-pixel JPEG; Print yields a single-page PDF grid with the copy-count adjustment rules.
 */
@Composable
fun PhotoExportScreen(
    viewModel: PhotoViewModel,
    onBack: () -> Unit,
    onStartFresh: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pendingShare by viewModel.pendingShare.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var finishVisible by remember { mutableStateOf(false) }

    LaunchedEffect(exportState) { if (exportState is PhotoExportState.Saved) finishVisible = true }
    LaunchedEffect(finishVisible) { if (finishVisible) scrollState.animateScrollTo(scrollState.maxValue) }
    LaunchedEffect(pendingShare) {
        val items = pendingShare ?: return@LaunchedEffect
        launchPhotoShare(context, items)
        viewModel.shareHandled()
        finishVisible = true
    }

    val uploadOn = OutputMode.UPLOAD in state.modes
    val printOn = OutputMode.PRINT in state.modes
    val copies = state.copies()
    val printBlocked = printOn && copies !is CopiesResult.Ok
    val canExport = state.modes.isNotEmpty() && !printBlocked && exportState !is PhotoExportState.Working

    fun doSave() = viewModel.save()

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) doSave() }

    fun onSaveClick() {
        viewModel.clearExportResult()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) doSave() else storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doSave()
        }
    }

    fun startFresh() {
        viewModel.reset()
        onStartFresh()
    }

    ScreenScaffold(title = "Export photo", scrollState = scrollState) {
        // --- purpose ---
        Text("Purpose", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableCard("Upload", uploadOn, { viewModel.toggleMode(OutputMode.UPLOAD) })
            SelectableCard("Print", printOn, { viewModel.toggleMode(OutputMode.PRINT) })
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text("Name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // --- upload options ---
        if (uploadOn) {
            HorizontalDivider()
            Text("Upload (JPEG)", style = MaterialTheme.typography.titleSmall)
            MaxKbField(state.uploadMaxKb, viewModel::setUploadMaxKb)
            state.resolvedSize?.let { rs ->
                Text(
                    "Single photo, ${trim(rs.widthMm)}×${trim(rs.heightMm)} mm at ${state.uploadDpi} dpi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- print options ---
        if (printOn) {
            HorizontalDivider()
            Text("Print (single-page PDF)", style = MaterialTheme.typography.titleSmall)
            Text("Paper", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoPaper.entries.forEach { paper ->
                    SelectableCard(paper.label, state.printPaper == paper, { viewModel.setPrintPaper(paper) })
                }
            }
            CopiesField(state.requestedCopies, viewModel::setRequestedCopies)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cut marks", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.cutMarks, onCheckedChange = viewModel::setCutMarks)
            }
            CopiesNotice(copies)
        }

        HorizontalDivider()

        Button(onClick = ::onSaveClick, enabled = canExport, modifier = Modifier.fillMaxWidth()) {
            Text("Save to Downloads")
        }
        Button(onClick = { viewModel.share() }, enabled = canExport, modifier = Modifier.fillMaxWidth()) {
            Text("Share")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }

        PhotoExportStatus(exportState)

        if (finishVisible) {
            Button(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("New photo") }
            OutlinedButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }
}

@Composable
private fun MaxKbField(value: Int?, onChange: (Int?) -> Unit) {
    var text by remember { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.trim().toIntOrNull()?.takeIf { n -> n > 0 })
        },
        label = { Text("Max size (KB) — optional") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CopiesField(value: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.trim().toIntOrNull()?.let(onChange)
        },
        label = { Text("Copies") },
        singleLine = true,
        isError = text.trim().toIntOrNull()?.let { it < 1 } ?: text.isNotBlank(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CopiesNotice(copies: CopiesResult?) {
    when (copies) {
        is CopiesResult.Ok -> copies.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        is CopiesResult.DoesNotFit -> Text(copies.message, color = MaterialTheme.colorScheme.error)
        is CopiesResult.Invalid -> Text(copies.message, color = MaterialTheme.colorScheme.error)
        null -> Unit
    }
}

@Composable
private fun PhotoExportStatus(state: PhotoExportState) {
    when (state) {
        PhotoExportState.Idle -> Unit
        PhotoExportState.Working -> {
            CircularProgressIndicator()
            Text("Working…")
        }
        is PhotoExportState.Saved -> Card(modifier = Modifier.fillMaxWidth()) {
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
        is PhotoExportState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
    }
}

private fun trim(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

private fun launchPhotoShare(context: Context, items: List<ShareItem>) {
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
