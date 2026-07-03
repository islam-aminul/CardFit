package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.OutputChip
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.TealCallout
import `in`.firm.consultancy.bayaan.cardfit.ui.components.outputChipLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal700

/**
 * Step 5 (CLAUDE.md section 11.5): preview the page(s), then Save (MediaStore) or Share
 * (FileProvider + ACTION_SEND). Generates one or two files from the same [ScanSession] per the
 * selection. "Change output settings" returns to Configure to re-export without re-scanning.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEditConfig: () -> Unit,
    onNewScan: () -> Unit,
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

    val scrollState = rememberScrollState()
    // Becomes true after a successful Save or Share, to reveal + scroll to New Scan / Home.
    var finishActionsVisible by remember { mutableStateOf(false) }
    // On a successful save: reveal the finish actions first, let the bottom bar grow/relayout (two
    // frames), then scroll the saved-file info into view.
    LaunchedEffect(uiState) {
        if (uiState is ExportUiState.Saved) {
            finishActionsVisible = true
            withFrameNanos {}
            withFrameNanos {}
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // (Re)generate the preview whenever the session or render settings change.
    LaunchedEffect(
        session,
        state.selectedModes,
        state.selectedPapers,
        state.selectedFormats,
        state.grayscale,
        state.maxFileSizeKb,
        state.sizeOverride,
    ) {
        if (session != null && configs.isNotEmpty()) {
            exportViewModel.generatePreview(session, configs)
        }
    }

    // Launch the share sheet once files are prepared, then surface the finish actions.
    LaunchedEffect(pendingShare) {
        val items = pendingShare ?: return@LaunchedEffect
        launchShare(context, items)
        exportViewModel.shareHandled()
        finishActionsVisible = true
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

    // Fully reset the document (cached images, OCR name, render settings).
    fun resetAll() {
        exportViewModel.resetForNewSession()
        exportViewModel.discardScans()
        viewModel.reset()
    }
    fun startFresh() { resetAll(); onStartFresh() }
    fun newScan() { resetAll(); onNewScan() }

    ScreenScaffold(
        title = "Preview & export",
        scrollState = scrollState,
        bottomBar = {
            ScaffoldBottomBar {
                if (finishActionsVisible) {
                    PrimaryButton(onClick = { newScan() }, modifier = Modifier.fillMaxWidth()) { Text("New Scan") }
                    GhostButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("Home") }
                }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        if (session == null) {
            Text("No card scanned yet.")
            return@ScreenScaffold
        }

        val bytes = previewBytes
        when {
            // The rendered page sits inside a white bordered "sheet" card, per the UI kit.
            bytes != null -> BayaanCard(
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    model = bytes,
                    contentDescription = "Output preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            previewFailed -> Text(
                "Couldn't render a preview from this scan. Try re-scanning the card.",
                color = MaterialTheme.colorScheme.error,
            )
            else -> Text("Generating preview…")
        }

        Text("Card: ${session.cardType.name}", style = MaterialTheme.typography.bodyMedium)
        Text("Name: ${state.name.ifBlank { "(document)" }}", style = MaterialTheme.typography.bodyMedium)

        if (configs.isNotEmpty()) {
            SectionLabel("Files (${configs.size})")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                configs.forEach { config ->
                    OutputChip(outputChipLabel(config))
                }
            }
        }

        val exportEnabled = configs.isNotEmpty() && uiState !is ExportUiState.Working
        // Emphasized (filled) until a successful save/share, then de-emphasized (outlined).
        ActionButton(
            filled = !finishActionsVisible,
            onClick = ::onSaveClick,
            enabled = exportEnabled,
            text = "Save to Downloads",
        )
        ActionButton(
            filled = !finishActionsVisible,
            onClick = { exportViewModel.share(session, state.name, configs) },
            enabled = exportEnabled,
            text = "Share",
        )

        GhostButton(onClick = onEditConfig, modifier = Modifier.fillMaxWidth()) {
            Text("Change output settings")
        }

        ExportStatus(uiState)
    }
}

/** A full-width primary action that is filled (emphasized) or outlined (de-emphasized) per [filled]. */
@Composable
private fun ActionButton(filled: Boolean, onClick: () -> Unit, enabled: Boolean, text: String) {
    if (filled) {
        PrimaryButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
    } else {
        GhostButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
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
        is ExportUiState.Saved -> TealCallout(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Saved ${uiState.files.size} file(s):",
                style = MaterialTheme.typography.labelLarge,
                color = Teal700,
            )
            uiState.files.forEach { file ->
                Text("• ${file.fileName}", style = MaterialTheme.typography.bodySmall, color = Teal700)
                file.warning?.let {
                    Text("  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
