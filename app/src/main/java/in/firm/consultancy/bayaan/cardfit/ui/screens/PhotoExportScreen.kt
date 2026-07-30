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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.firm.consultancy.bayaan.cardfit.data.export.ShareItem
import `in`.firm.consultancy.bayaan.cardfit.domain.CopiesResult
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoPaper
import `in`.firm.consultancy.bayaan.cardfit.domain.formatSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoExportState
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.SettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTextField
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.IllustratedTile
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PaperArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoPrintArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoUploadArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ExportResultSheet
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoSheetPreview
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SinglePhotoPreview
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.launchShare
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.TealCallout
import `in`.firm.consultancy.bayaan.cardfit.ui.components.bayaanSwitchColors
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight800
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal700
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Photo flow step 4 (CLAUDE.md Phase 13): choose Upload and/or Print, set the name and per-mode
 * options (upload max-KB cap; print paper, copies and cut marks), then Save or Share. Upload yields a
 * single exact-pixel JPEG; Print yields a single-page PDF grid with the copy-count adjustment rules.
 */
@Composable
fun PhotoExportScreen(
    viewModel: PhotoViewModel,
    onBack: () -> Unit,
    onNewPhoto: () -> Unit,
    onStartFresh: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val unit by settingsViewModel.unit.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    // The edited bitmap, reused to paint the sheet/photo previews below.
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val previewImage = preview?.asImageBitmap()

    val scrollState = rememberScrollState()
    // Reported by an Open/Print launcher that found no handling app.
    var actionError by remember { mutableStateOf<String?>(null) }

    val uploadOn = OutputMode.UPLOAD in state.modes
    val printOn = OutputMode.PRINT in state.modes
    val copies = state.copies()
    val printBlocked = printOn && copies !is CopiesResult.Ok
    val canExport = state.modes.isNotEmpty() && !printBlocked && exportState !is PhotoExportState.Working

    fun doSave() = viewModel.save()

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            doSave()
        } else {
            actionError = "Storage permission is required to save to Downloads on this Android version."
        }
    }

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

    fun newPhoto() {
        viewModel.reset()
        onNewPhoto()
    }

    (exportState as? PhotoExportState.Saved)?.let { saved ->
        ExportResultSheet(
            files = saved.files,
            onDismiss = { viewModel.clearExportResult() },
            onActionError = { actionError = it },
        ) {
            PrimaryButton(onClick = { newPhoto() }, modifier = Modifier.fillMaxWidth()) { Text("New photo") }
            GhostButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }

    ScreenScaffold(
        title = "Export photo",
        scrollState = scrollState,
        bottomBar = {
            ScaffoldBottomBar {
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        // --- purpose ---
        SectionLabel("Purpose")
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IllustratedTile(
                label = "Upload",
                subtitle = "One image file",
                selected = uploadOn,
                onClick = { viewModel.toggleMode(OutputMode.UPLOAD) },
                artwork = { accent -> PhotoUploadArt(accent, Modifier.fillMaxSize()) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            IllustratedTile(
                label = "Print",
                subtitle = "A sheet of photos",
                selected = printOn,
                onClick = { viewModel.toggleMode(OutputMode.PRINT) },
                artwork = { accent -> PhotoPrintArt(accent, Modifier.fillMaxSize()) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        BayaanTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = "Name (optional)",
            modifier = Modifier.fillMaxWidth(),
        )

        // --- upload options ---
        if (uploadOn) {
            HorizontalDivider()
            SectionLabel("Upload (JPEG)")
            state.resolvedSize?.let { rs ->
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    SinglePhotoPreview(
                        widthMm = rs.widthMm,
                        heightMm = rs.heightMm,
                        photo = previewImage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            MaxKbField(state.uploadMaxKb, viewModel::setUploadMaxKb)
            state.resolvedSize?.let { rs ->
                HelpText(
                    "Single photo, ${formatSize(rs.widthMm, rs.heightMm, unit)} at ${state.uploadDpi} dpi.",
                )
            }
        }

        // --- print options ---
        if (printOn) {
            HorizontalDivider()
            SectionLabel("Print (single-page PDF)")
            // The actual sheet, so paper / copies / cut marks are visible choices rather than guesses.
            val sheetGrid = state.grid()
            if (sheetGrid != null && sheetGrid.fits) {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    PhotoSheetPreview(
                        grid = sheetGrid,
                        count = (copies as? CopiesResult.Ok)?.finalCount ?: state.requestedCopies,
                        cutMarks = state.cutMarks,
                        photo = previewImage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text("Paper", style = MaterialTheme.typography.bodyMedium, color = Ink)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PhotoPaper.entries.forEach { paper ->
                    IllustratedTile(
                        label = paper.label,
                        selected = state.printPaper == paper,
                        onClick = { viewModel.setPrintPaper(paper) },
                        artwork = { accent ->
                            PaperArt(
                                ratio = (paper.widthMm / paper.heightMm).toFloat(),
                                accent = accent,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val grid = state.grid()
            // Copies are always printed a full row at a time, so snap the requested count to a
            // per-row multiple (at least one row, at most one full page) once the grid is known.
            // This makes the counter start at "one row" (e.g. 5) instead of a stray default.
            val perRow = grid?.perRow ?: 0
            val perPage = grid?.perPage ?: 0
            LaunchedEffect(perRow, perPage) {
                if (perRow > 0) {
                    val rows = ((state.requestedCopies + perRow - 1) / perRow).coerceAtLeast(1)
                    var snapped = rows * perRow
                    if (perPage > 0) snapped = snapped.coerceAtMost(perPage)
                    if (snapped != state.requestedCopies) viewModel.setRequestedCopies(snapped)
                }
            }
            CopiesStepper(
                value = state.requestedCopies,
                perRow = perRow,
                perPage = perPage,
                onChange = viewModel::setRequestedCopies,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cut marks", style = MaterialTheme.typography.bodyLarge, color = Ink)
                Switch(checked = state.cutMarks, onCheckedChange = viewModel::setCutMarks, colors = bayaanSwitchColors())
            }
            CopiesNotice(copies)
        }

        HorizontalDivider()

        // One primary action. Export always writes to Downloads; open/print/share then act on the
        // real saved file from the result sheet.
        PrimaryButton(
            onClick = ::onSaveClick,
            enabled = canExport,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export") }
        if (state.modes.isNotEmpty()) {
            val count = if (state.modes.size == 1) "1 file" else "${state.modes.size} files"
            HelpText("$count → Downloads/CardFit")
        }

        PhotoExportProgress(exportState)
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun MaxKbField(value: Int?, onChange: (Int?) -> Unit) {
    var text by remember { mutableStateOf(value?.toString() ?: "") }
    BayaanTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.trim().toIntOrNull()?.takeIf { n -> n > 0 })
        },
        label = "Max size (KB) — optional",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Copies control as a −/value/+ stepper. The grid rounds copies up to whole rows anyway, so each tap
 * adds or removes one full row ([perRow] photos) and the value is snapped to a row multiple; it is
 * clamped to one row at the bottom and to one full page ([perPage]) at the top. When the photo size
 * doesn't fit the page ([perRow] == 0) it falls back to ±1 (the [CopiesNotice] explains the problem).
 */
@Composable
private fun CopiesStepper(value: Int, perRow: Int, perPage: Int, onChange: (Int) -> Unit) {
    val step = perRow.coerceAtLeast(1)
    // Step to the next per-row multiple strictly above/below the current value, so +/- adds or
    // removes exactly one row (e.g. 5 → 10 → 15, not 4 → 10). Bottom clamps to one row, top to a
    // full page. Math.floorDiv keeps this correct even if [value] isn't yet a clean multiple.
    val increasedRaw = (Math.floorDiv(value, step) + 1) * step
    val increased = if (perPage > 0) increasedRaw.coerceAtMost(perPage) else increasedRaw
    val decreased = (Math.floorDiv(value - 1, step) * step).coerceAtLeast(step)
    val canDecrease = value > step
    val canIncrease = perPage <= 0 || value < perPage
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Copies", style = MaterialTheme.typography.bodyLarge, color = Ink)
            if (perRow > 0) {
                Text(
                    "$perRow per row",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalIconButton(
                onClick = { onChange(decreased) },
                enabled = canDecrease,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Midnight50,
                    contentColor = Midnight800,
                ),
            ) { Icon(Icons.Filled.Remove, contentDescription = "Fewer copies") }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                color = TextHeading,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            FilledTonalIconButton(
                onClick = { onChange(increased) },
                enabled = canIncrease,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Midnight50,
                    contentColor = Midnight800,
                ),
            ) { Icon(Icons.Filled.Add, contentDescription = "More copies") }
        }
    }
}

@Composable
private fun CopiesNotice(copies: CopiesResult?) {
    when (copies) {
        is CopiesResult.Ok -> copies.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Teal600)
        }
        is CopiesResult.DoesNotFit -> Text(copies.message, color = MaterialTheme.colorScheme.error)
        is CopiesResult.Invalid -> Text(copies.message, color = MaterialTheme.colorScheme.error)
        null -> Unit
    }
}

/** In-page progress/error only — the success case is the [ExportResultSheet]. */
@Composable
private fun PhotoExportProgress(state: PhotoExportState) {
    when (state) {
        PhotoExportState.Working -> {
            CircularProgressIndicator()
            Text("Working…")
        }
        is PhotoExportState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
        else -> Unit
    }
}
