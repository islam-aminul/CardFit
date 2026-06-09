package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportUiState
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Step 5 (CLAUDE.md section 11.5): preview the page(s), then Save (MediaStore) or Share
 * (FileProvider + ACTION_SEND). Generates one or two files from the same [ScanSession] per the
 * selection. "Change output settings" returns to Configure to re-export without re-scanning.
 */
@Composable
fun PreviewScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEditConfig: () -> Unit,
    onStartFresh: () -> Unit,
    exportViewModel: ExportViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val previewBytes by exportViewModel.previewBytes.collectAsStateWithLifecycle()
    val previewFailed by exportViewModel.previewFailed.collectAsStateWithLifecycle()
    val uiState by exportViewModel.uiState.collectAsStateWithLifecycle()
    val pendingShare by exportViewModel.pendingShare.collectAsStateWithLifecycle()

    val session = state.session
    val configs = viewModel.renderConfigs()

    // (Re)generate the preview whenever the session or render settings change.
    LaunchedEffect(
        session,
        state.selectedModes,
        state.paper,
        state.format,
        state.grayscale,
        state.maxFileSizeKb,
    ) {
        if (session != null && configs.isNotEmpty()) {
            exportViewModel.generatePreview(session, configs)
        }
    }

    // Launch the share sheet once files are prepared.
    LaunchedEffect(pendingShare) {
        val items = pendingShare ?: return@LaunchedEffect
        launchShare(context, items)
        exportViewModel.shareHandled()
    }

    fun doSave() {
        if (session != null) exportViewModel.save(session, state.name, configs)
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            doSave()
        } else {
            exportViewModel.reportError("Storage permission is required to save to Downloads on this Android version.")
        }
    }

    fun onSaveClick() {
        exportViewModel.clearResult()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) doSave() else storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doSave()
        }
    }

    // Fully reset the document (cached images, OCR name, render settings) and return to the start.
    fun startFresh() {
        exportViewModel.resetForNewSession()
        exportViewModel.discardScans()
        viewModel.reset()
        onStartFresh()
    }

    ScreenScaffold(title = "Preview & export") {
        if (session == null) {
            Text("No card scanned yet.")
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            return@ScreenScaffold
        }

        val bytes = previewBytes
        when {
            bytes != null -> AsyncImage(
                model = bytes,
                contentDescription = "Output preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            previewFailed -> Text(
                "Couldn't render a preview from this scan. Try re-scanning the card.",
                color = MaterialTheme.colorScheme.error,
            )
            else -> Text("Generating preview…")
        }

        Text("Card: ${session.cardType.name}", style = MaterialTheme.typography.bodyMedium)
        Text("Name: ${state.name.ifBlank { "(document)" }}", style = MaterialTheme.typography.bodyMedium)
        Text("Will generate ${configs.size} file(s).", style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = ::onSaveClick,
            enabled = configs.isNotEmpty() && uiState !is ExportUiState.Working,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save to Downloads") }

        Button(
            onClick = { exportViewModel.share(session, state.name, configs) },
            enabled = configs.isNotEmpty() && uiState !is ExportUiState.Working,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share") }

        OutlinedButton(onClick = onEditConfig, modifier = Modifier.fillMaxWidth()) {
            Text("Change output settings")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }

        ExportStatus(uiState)

        // After a successful save, make sure the user is never stranded.
        if (uiState is ExportUiState.Saved) {
            Button(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) {
                Text("New Scan")
            }
            OutlinedButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) {
                Text("Home")
            }
        }
    }
}

@Composable
private fun ExportStatus(uiState: ExportUiState) {
    when (uiState) {
        ExportUiState.Idle -> Unit
        ExportUiState.Working -> {
            CircularProgressIndicator()
            Text("Working…")
        }
        is ExportUiState.Saved -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Saved ${uiState.files.size} file(s):", style = MaterialTheme.typography.titleSmall)
                uiState.files.forEach { file ->
                    Text("• ${file.fileName}", style = MaterialTheme.typography.bodySmall)
                    file.warning?.let {
                        Text("  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        is ExportUiState.Failed -> Text(
            text = uiState.message,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun launchShare(context: android.content.Context, items: List<ShareItem>) {
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
