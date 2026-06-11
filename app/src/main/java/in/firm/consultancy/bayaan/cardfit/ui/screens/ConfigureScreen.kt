package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.firm.consultancy.bayaan.cardfit.domain.CardClassifier
import `in`.firm.consultancy.bayaan.cardfit.domain.CardFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.Orientation
import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.SizingMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.ui.AppState
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.SettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.formatNumber

/**
 * Step 3: configure output. Purpose, paper, and format are all multi-select (small tappable cards);
 * an export generates every combination. Paper is capped at [AppViewModel.MAX_PAPERS]. Conditional
 * controls appear by selection (crop marks for print, size cap for upload, searchable layer for PDF).
 * The Card size detection/override sits just above Next.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigureScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val printSelected = OutputMode.PRINT in state.selectedModes
    val uploadSelected = OutputMode.UPLOAD in state.selectedModes
    val pdfSelected = OutputFormat.PDF in state.selectedFormats

    var maxSizeText by remember { mutableStateOf(state.maxFileSizeKb?.toString().orEmpty()) }
    var showSizeDialog by remember { mutableStateOf(false) }

    // Seed the in-flow searchable flag once from the persisted DataStore preference.
    val persistedSearchable by settingsViewModel.searchableText.collectAsStateWithLifecycle()
    var seededSearchable by remember { mutableStateOf(false) }
    LaunchedEffect(persistedSearchable) {
        if (!seededSearchable) {
            viewModel.setSearchableText(persistedSearchable)
            seededSearchable = true
        }
    }

    ScreenScaffold(
        title = "Configure output",
        bottomBar = {
            ScaffoldBottomBar {
                Button(
                    onClick = onNext,
                    enabled = state.hasCompleteSelection,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Next") }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        // --- Purpose (multi) ---
        SectionLabel("Purpose")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputMode.entries.forEach { mode ->
                SelectableCard(
                    label = mode.label(),
                    selected = mode in state.selectedModes,
                    onClick = { viewModel.toggleMode(mode) },
                )
            }
        }

        // --- Paper (multi, up to 2) ---
        SectionLabel("Paper (up to ${AppViewModel.MAX_PAPERS})")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperSize.entries.forEach { paper ->
                val selected = paper in state.selectedPapers
                SelectableCard(
                    label = paper.name,
                    selected = selected,
                    enabled = selected || state.selectedPapers.size < AppViewModel.MAX_PAPERS,
                    onClick = { viewModel.togglePaper(paper) },
                )
            }
        }

        // --- Format (multi) ---
        SectionLabel("Format")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputFormat.entries.forEach { format ->
                SelectableCard(
                    label = format.name,
                    selected = format in state.selectedFormats,
                    onClick = { viewModel.toggleFormat(format) },
                )
            }
        }

        // --- Grayscale ---
        ToggleRow("Grayscale", state.grayscale, viewModel::setGrayscale)

        // --- Crop marks: when Print is selected ---
        if (printSelected) {
            ToggleRow("Crop marks (print)", state.cropMarks, viewModel::setCropMarks)
        }

        // --- Searchable text: when PDF is selected ---
        if (pdfSelected) {
            ToggleRow(
                label = "Searchable PDF text",
                checked = state.searchableText,
                onCheckedChange = { value ->
                    viewModel.setSearchableText(value)
                    settingsViewModel.setSearchableText(value)
                },
            )
            Text(
                "Embeds the recognized text into the PDF as a hidden, selectable layer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // --- Max upload size: when Upload is selected ---
        if (uploadSelected) {
            SectionLabel("Max upload size")
            OutlinedTextField(
                value = maxSizeText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(7)
                    maxSizeText = digits
                    viewModel.setMaxFileSizeKb(digits.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("Size cap in KB (blank = no cap)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val fileCount = viewModel.renderConfigs().size
        if (fileCount > 0) {
            Text(
                "This will create $fileCount file(s).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // --- Card size (just above Next) ---
        SectionLabel("Card size")
        CardSizeSection(
            state = state,
            onOverride = viewModel::setSizeOverride,
            onCustom = { showSizeDialog = true },
        )

        if (showSizeDialog) {
            val session = state.session
            CustomSizeDialog(
                onDismiss = { showSizeDialog = false },
                onConfirm = { widthMm, heightMm ->
                    viewModel.setCustomSize(widthMm, heightMm)
                    viewModel.setSizeOverride(SizeOverride.CUSTOM)
                    showSizeDialog = false
                },
                initialWidthMm = session?.customWidthMm ?: 85.6,
                initialHeightMm = session?.customHeightMm ?: 54.0,
            )
        }
    }
}

private fun OutputMode.label(): String = when (this) {
    OutputMode.PRINT -> "Print"
    OutputMode.UPLOAD -> "Upload"
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardSizeSection(
    state: AppState,
    onOverride: (SizeOverride) -> Unit,
    onCustom: () -> Unit,
) {
    val session = state.session
    val front = session?.front
    val classification = if (
        front != null && front.widthPx > 0 && front.heightPx > 0 && session.cardType != CardType.CUSTOM
    ) {
        CardClassifier.classify(front.widthPx, front.heightPx)
    } else {
        null
    }

    when {
        session?.cardType == CardType.CUSTOM -> Text(
            "Custom: ${formatNumber(session.customWidthMm ?: 85.6)} × " +
                "${formatNumber(session.customHeightMm ?: 54.0)} mm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        classification != null && session != null && front != null -> {
            val resolved = CardClassifier.resolveSizingMode(
                cardType = session.cardType,
                frontWidthPx = front.widthPx,
                frontHeightPx = front.heightPx,
                override = state.sizeOverride,
            )
            Text(
                "Detected: ${if (classification.format == CardFormat.CR80) "CR-80" else "Non-standard"}, " +
                    "${classification.orientation.name.lowercase()} (ratio ${formatNumber(classification.ratio)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Sizing: ${sizingSummary(resolved, classification.orientation, session.customWidthMm, session.customHeightMm)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> Text(
            "Scan the front to detect the card size.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Offer Force CR-80 only when the detection is a near-CR-80 miss (or it's already selected).
    val nearMiss = classification != null &&
        classification.format == CardFormat.NON_STANDARD &&
        CardClassifier.isNearCr80(classification.ratio)
    val showForceCr80 = nearMiss || state.sizeOverride == SizeOverride.FORCE_CR80
    if (nearMiss) {
        Text(
            "Looks close to a CR-80 card — tap Force CR-80 if it is one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableCard(
            label = "Automatic",
            selected = state.sizeOverride == SizeOverride.AUTOMATIC,
            onClick = { onOverride(SizeOverride.AUTOMATIC) },
        )
        if (showForceCr80) {
            SelectableCard(
                label = "Force CR-80",
                selected = state.sizeOverride == SizeOverride.FORCE_CR80,
                onClick = { onOverride(SizeOverride.FORCE_CR80) },
            )
        }
        SelectableCard(
            label = "Custom",
            selected = state.sizeOverride == SizeOverride.CUSTOM,
            onClick = onCustom,
        )
    }
}

private fun sizingSummary(
    mode: SizingMode,
    orientation: Orientation,
    customWidthMm: Double?,
    customHeightMm: Double?,
): String = when (mode) {
    SizingMode.CR80 -> {
        val (w, h) = CardClassifier.cr80SizeMm(orientation)
        "${formatNumber(w)} × ${formatNumber(h)} mm (CR-80)"
    }
    SizingMode.CUSTOM ->
        "${formatNumber(customWidthMm ?: 85.6)} × ${formatNumber(customHeightMm ?: 54.0)} mm (custom)"
    SizingMode.NON_STANDARD ->
        "scaled to fit — full page for print, cropped to content for upload"
}
