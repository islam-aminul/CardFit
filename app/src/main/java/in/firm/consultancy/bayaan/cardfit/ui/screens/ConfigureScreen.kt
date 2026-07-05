package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.domain.Orientation
import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.SizingMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.papersDisabledForReceipts
import `in`.firm.consultancy.bayaan.cardfit.ui.AppState
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportSettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.PersistCardSettingsEffect
import `in`.firm.consultancy.bayaan.cardfit.ui.SettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTextField
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CardPrintArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CardUploadArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.IllustratedTile
import `in`.firm.consultancy.bayaan.cardfit.ui.components.JpegArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.OutputChip
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PaperArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PdfArt
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.bayaanSwitchColors
import `in`.firm.consultancy.bayaan.cardfit.ui.components.outputChipLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.formatNumber
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

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
    exportSettingsViewModel: ExportSettingsViewModel = viewModel(),
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

    // Seed this card type's remembered export settings once per type (the in-state flag survives
    // Preview -> "Edit config" revisits, so in-session edits are never clobbered), then persist
    // every subsequent change back to the type's blob.
    val cardType = state.session?.cardType
    LaunchedEffect(cardType) {
        val type = cardType ?: return@LaunchedEffect
        if (viewModel.state.value.settingsSeededFor != type) {
            viewModel.applyPersistedSettings(type, exportSettingsViewModel.loadCardSettings(type))
        }
    }
    // The size-cap field mirrors state through local text; re-sync it once the seed lands.
    LaunchedEffect(state.settingsSeededFor) {
        if (state.settingsSeededFor != null) {
            maxSizeText = viewModel.state.value.maxFileSizeKb?.toString().orEmpty()
        }
    }
    PersistCardSettingsEffect(viewModel, exportSettingsViewModel)

    ScreenScaffold(
        title = "Configure output",
        bottomBar = {
            ScaffoldBottomBar {
                PrimaryButton(
                    onClick = onNext,
                    enabled = state.hasCompleteSelection,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Next") }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        // Document-specific gating: documents have no card-size section; receipts constrain paper by
        // fit; multi-page documents can't export per-page JPEGs.
        val isDocument = cardType?.fitMode != null && cardType.fitMode != FitMode.ACTUAL_SIZE
        val isReceipt = cardType == CardType.RECEIPT
        val documentPages = state.session?.documentPages ?: emptyList()
        val multiPage = documentPages.size > 1
        val disabledPapers = if (isReceipt) {
            papersDisabledForReceipts(documentPages, PaperSize.entries)
        } else {
            emptySet()
        }
        var hint by remember { mutableStateOf<String?>(null) }

        // If a chosen paper became invalid (e.g. a receipt width changed), drop it.
        LaunchedEffect(disabledPapers) {
            val clash = state.selectedPapers.intersect(disabledPapers)
            clash.forEach { viewModel.togglePaper(it) }
        }

        // Multi-page → PDF only is enforced in AppViewModel (addDocumentPage / applyPersistedSettings /
        // toggleFormat), so the format set here is already valid — no screen-level migration needed.

        hint?.let { HelpText(it) }

        // --- Purpose (multi) ---
        SectionLabel("Purpose")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutputMode.entries.forEach { mode ->
                IllustratedTile(
                    label = mode.label(),
                    subtitle = mode.subtitle(),
                    selected = mode in state.selectedModes,
                    onClick = { viewModel.toggleMode(mode) },
                    artwork = { accent ->
                        when (mode) {
                            OutputMode.PRINT -> CardPrintArt(accent, Modifier.fillMaxSize())
                            OutputMode.UPLOAD -> CardUploadArt(accent, Modifier.fillMaxSize())
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // --- Paper (multi, up to 2) ---
        SectionLabel("Paper (up to ${AppViewModel.MAX_PAPERS})")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaperSize.entries.forEach { paper ->
                val selected = paper in state.selectedPapers
                val fits = paper !in disabledPapers
                val underCap = selected || state.selectedPapers.size < AppViewModel.MAX_PAPERS
                IllustratedTile(
                    label = paper.name,
                    selected = selected,
                    enabled = underCap && fits,
                    onClick = { hint = null; viewModel.togglePaper(paper) },
                    onDisabledClick = if (!fits) {
                        { hint = "This receipt doesn't fit ${paper.name} at its true size — pick larger paper." }
                    } else {
                        null
                    },
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

        // --- Format (multi) ---
        SectionLabel("Format")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutputFormat.entries.forEach { format ->
                // A multi-page document can only export as one multi-page PDF; JPEG would need a file
                // per page, which is what Application sets are for.
                val jpegBlocked = multiPage && format == OutputFormat.JPEG
                IllustratedTile(
                    label = format.name,
                    subtitle = format.subtitle(),
                    // A blocked JPEG can never read as selected, even if a stale set still held it.
                    selected = format in state.selectedFormats && !jpegBlocked,
                    enabled = !jpegBlocked,
                    onClick = { hint = null; viewModel.toggleFormat(format) },
                    onDisabledClick = if (jpegBlocked) {
                        { hint = "JPEG is one image per file. Use Application sets to export per-page JPEGs." }
                    } else {
                        null
                    },
                    artwork = { accent ->
                        when (format) {
                            OutputFormat.PDF -> PdfArt(accent, Modifier.fillMaxSize())
                            OutputFormat.JPEG -> JpegArt(accent, Modifier.fillMaxSize())
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // --- Grayscale ---
        ToggleRow("Grayscale", state.grayscale, viewModel::setGrayscale)

        // --- Trim rounded corners: actual-size cards only (PVC cards have rounded corners) ---
        if (state.session?.cardType?.fitMode == FitMode.ACTUAL_SIZE) {
            ToggleRow("Trim rounded corners", state.roundCorners, viewModel::setRoundCorners)
            HelpText(
                "Rounds the corners and removes off-colour corner spots from PVC cards like PAN / " +
                    "Aadhaar / Voter ID. Turn off for square-corner paper cards.",
            )
        }

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
            HelpText("Embeds the recognized text into the PDF as a hidden, selectable layer.")
        }

        // --- Max upload size: when Upload is selected ---
        if (uploadSelected) {
            SectionLabel("Max upload size")
            BayaanTextField(
                value = maxSizeText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(7)
                    maxSizeText = digits
                    viewModel.setMaxFileSizeKb(digits.toIntOrNull()?.takeIf { it > 0 })
                },
                label = "Size cap in KB (blank = no cap)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val configs = viewModel.renderConfigs()
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

        // --- Card size (just above Next) — cards only; documents have no card-size override ---
        if (!isDocument) {
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
}

private fun OutputMode.label(): String = when (this) {
    OutputMode.PRINT -> "Print"
    OutputMode.UPLOAD -> "Upload"
}

private fun OutputMode.subtitle(): String = when (this) {
    OutputMode.PRINT -> "Both sides, true size"
    OutputMode.UPLOAD -> "Compressed to a cap"
}

private fun OutputFormat.subtitle(): String = when (this) {
    OutputFormat.PDF -> "Vector, searchable"
    OutputFormat.JPEG -> "Single flat image"
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
            color = if (enabled) Ink else TextMuted,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = bayaanSwitchColors(),
        )
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
        session?.cardType == CardType.CUSTOM -> HelpText(
            "Custom: ${formatNumber(session.customWidthMm ?: 85.6)} × " +
                "${formatNumber(session.customHeightMm ?: 54.0)} mm.",
        )
        classification != null && session != null && front != null -> {
            val resolved = CardClassifier.resolveSizingMode(
                cardType = session.cardType,
                frontWidthPx = front.widthPx,
                frontHeightPx = front.heightPx,
                override = state.sizeOverride,
            )
            HelpText(
                "Detected: ${if (classification.format == CardFormat.CR80) "CR-80" else "Non-standard"}, " +
                    "${classification.orientation.name.lowercase()} (ratio ${formatNumber(classification.ratio)})",
            )
            HelpText(
                "Sizing: ${sizingSummary(resolved, classification.orientation, session.customWidthMm, session.customHeightMm)}",
            )
        }
        else -> HelpText("Scan the front to detect the card size.")
    }

    // Offer Force CR-80 only when the detection is a near-CR-80 miss (or it's already selected).
    val nearMiss = classification != null &&
        classification.format == CardFormat.NON_STANDARD &&
        CardClassifier.isNearCr80(classification.ratio)
    val showForceCr80 = nearMiss || state.sizeOverride == SizeOverride.FORCE_CR80
    if (nearMiss) {
        HelpText("Looks close to a CR-80 card — tap Force CR-80 if it is one.")
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
