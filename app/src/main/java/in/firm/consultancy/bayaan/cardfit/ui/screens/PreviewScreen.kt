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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportSettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportUiState
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.PersistCardSettingsEffect
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.OutputChip
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ExportResultSheet
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.launchShare
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
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
    exportSettingsViewModel: ExportSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val previewBytes by exportViewModel.previewBytes.collectAsStateWithLifecycle()
    val previewFailed by exportViewModel.previewFailed.collectAsStateWithLifecycle()
    val uiState by exportViewModel.uiState.collectAsStateWithLifecycle()

    val session = state.session
    val configs = viewModel.renderConfigs()

    val scrollState = rememberScrollState()
    // Reported by an Open/Print launcher that found no handling app. Kept out of ExportUiState so a
    // missing viewer can't wipe the result the user is still looking at.
    var actionError by remember { mutableStateOf<String?>(null) }

    // Persist setting changes made on this screen (orientation/size) to the type's blob.
    PersistCardSettingsEffect(viewModel, exportSettingsViewModel)

    // (Re)generate the preview whenever the session or render settings change.
    LaunchedEffect(
        session,
        state.selectedModes,
        state.selectedPapers,
        state.selectedFormats,
        state.grayscale,
        state.maxFileSizeKb,
        state.sizeOverride,
        state.pageOrientation,
        state.contentScalePercent,
    ) {
        if (session != null && configs.isNotEmpty()) {
            exportViewModel.generatePreview(session, configs)
        }
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

    // The result and its next-step actions live in a sheet, so the bar stays one button tall.
    (uiState as? ExportUiState.Saved)?.let { saved ->
        ExportResultSheet(
            files = saved.files,
            onDismiss = { exportViewModel.clearResult() },
            onActionError = { actionError = it },
        ) {
            PrimaryButton(onClick = { newScan() }, modifier = Modifier.fillMaxWidth()) { Text("New scan") }
            GhostButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }

    ScreenScaffold(
        title = "Preview & export",
        scrollState = scrollState,
        bottomBar = {
            ScaffoldBottomBar {
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        if (session == null) {
            Text("No card scanned yet.")
            return@ScreenScaffold
        }

        val documentPages = session.documentPages
        if (documentPages.size > 1) {
            // Multiple pages: show only the page thumbnails with a count (no large preview).
            SectionLabel("${documentPages.size} pages")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                documentPages.forEach { page ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 100.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    ) {
                        AsyncImage(
                            model = page.side.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .rotate(page.edit.rotationDegrees.toFloat()),
                        )
                    }
                }
            }
        } else {
            // Single page or card: show the large rendered preview inside a bordered "sheet" card.
            val bytes = previewBytes
            when {
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
        }

        // Full document: choose the page orientation; the preview above re-renders live.
        if (session.cardType == CardType.FULL_PAGE_DOCUMENT) {
            SectionLabel("Page orientation")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableCard(
                    label = "Portrait",
                    selected = state.pageOrientation == PageOrientation.PORTRAIT,
                    onClick = { viewModel.setPageOrientation(PageOrientation.PORTRAIT) },
                )
                SelectableCard(
                    label = "Landscape",
                    selected = state.pageOrientation == PageOrientation.LANDSCAPE,
                    onClick = { viewModel.setPageOrientation(PageOrientation.LANDSCAPE) },
                )
            }
        }

        Text("Type: ${session.cardType.label}", style = MaterialTheme.typography.bodyMedium)
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

        // One primary action. Export always writes to Downloads; opening, printing and sharing then
        // act on the real saved file, from the result sheet.
        val exportEnabled = configs.isNotEmpty() && uiState !is ExportUiState.Working
        PrimaryButton(
            onClick = ::onSaveClick,
            enabled = exportEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export") }
        if (configs.isNotEmpty()) {
            val count = if (configs.size == 1) "1 file" else "${configs.size} files"
            HelpText("$count → Downloads/CardFit")
        }

        GhostButton(onClick = onEditConfig, modifier = Modifier.fillMaxWidth()) {
            Text("Change output settings")
        }

        ExportProgress(uiState)
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** In-page progress/error only — the success case is the [ExportResultSheet]. */
@Composable
private fun ExportProgress(uiState: ExportUiState) {
    when (uiState) {
        ExportUiState.Working -> {
            CircularProgressIndicator()
            Text("Working…")
        }
        is ExportUiState.Failed -> Text(
            text = uiState.message,
            color = MaterialTheme.colorScheme.error,
        )
        else -> Unit
    }
}

