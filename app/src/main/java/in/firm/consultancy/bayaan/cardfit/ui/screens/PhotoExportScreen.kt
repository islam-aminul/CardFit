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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import `in`.firm.consultancy.bayaan.cardfit.ui.components.IllustratedTile
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PaperArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoPrintArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoUploadArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

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
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val pendingShare by viewModel.pendingShare.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var finishVisible by remember { mutableStateOf(false) }

    // On a successful save: reveal the finish actions first, let the bottom bar grow/relayout (two
    // frames), then scroll the saved-file info into view.
    LaunchedEffect(exportState) {
        if (exportState is PhotoExportState.Saved) {
            finishVisible = true
            withFrameNanos {}
            withFrameNanos {}
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
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

    fun newPhoto() {
        viewModel.reset()
        onNewPhoto()
    }

    ScreenScaffold(
        title = "Export photo",
        scrollState = scrollState,
        bottomBar = {
            ScaffoldBottomBar {
                if (finishVisible) {
                    Button(onClick = { newPhoto() }, modifier = Modifier.fillMaxWidth()) { Text("New photo") }
                    OutlinedButton(onClick = { startFresh() }, modifier = Modifier.fillMaxWidth()) { Text("Home") }
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        // --- purpose ---
        Text("Purpose", style = MaterialTheme.typography.titleSmall)
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
            CopiesStepper(
                value = state.requestedCopies,
                perRow = grid?.perRow ?: 0,
                perPage = grid?.perPage ?: 0,
                onChange = viewModel::setRequestedCopies,
            )
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

        // Emphasized (filled) until a successful save/share, then de-emphasized (outlined).
        ActionButton(
            filled = !finishVisible,
            onClick = ::onSaveClick,
            enabled = canExport,
            text = "Save to Downloads",
        )
        ActionButton(
            filled = !finishVisible,
            onClick = { viewModel.share() },
            enabled = canExport,
            text = "Share",
        )

        PhotoExportStatus(exportState)
    }
}

/** A full-width primary action that is filled (emphasized) or outlined (de-emphasized) per [filled]. */
@Composable
private fun ActionButton(filled: Boolean, onClick: () -> Unit, enabled: Boolean, text: String) {
    if (filled) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
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

/**
 * Copies control as a −/value/+ stepper. The grid rounds copies up to whole rows anyway, so each tap
 * adds or removes one full row ([perRow] photos) and the value is snapped to a row multiple; it is
 * clamped to one row at the bottom and to one full page ([perPage]) at the top. When the photo size
 * doesn't fit the page ([perRow] == 0) it falls back to ±1 (the [CopiesNotice] explains the problem).
 */
@Composable
private fun CopiesStepper(value: Int, perRow: Int, perPage: Int, onChange: (Int) -> Unit) {
    val step = perRow.coerceAtLeast(1)
    val rows = Math.ceil(value.toDouble() / step).toInt().coerceAtLeast(1)
    val decreased = ((rows - 1) * step).coerceAtLeast(step)
    val increasedRaw = (rows + 1) * step
    val increased = if (perPage > 0) increasedRaw.coerceAtMost(perPage) else increasedRaw
    val canDecrease = value > step
    val canIncrease = perPage <= 0 || value < perPage
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Copies", style = MaterialTheme.typography.bodyLarge)
            if (perRow > 0) {
                Text(
                    "$perRow per row",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            ) { Icon(Icons.Filled.Remove, contentDescription = "Fewer copies") }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            FilledTonalIconButton(
                onClick = { onChange(increased) },
                enabled = canIncrease,
            ) { Icon(Icons.Filled.Add, contentDescription = "More copies") }
        }
    }
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
                    file.detail?.let {
                        Text(
                            "  $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
