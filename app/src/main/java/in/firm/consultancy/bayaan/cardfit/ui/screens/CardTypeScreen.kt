package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import `in`.firm.consultancy.bayaan.cardfit.domain.formatSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.ExportSettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.SettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CardOutputArtwork
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Step 1 (CLAUDE.md section 11.1): full-width tappable rows with original FINAL-OUTPUT
 * illustrations — each shows the page the export produces (both card sides on one sheet / a fitted
 * document). Split into a Cards section (Aadhaar, PAN, Voter ID, Custom — both sides on one page at
 * true size) and a Documents section (Full page document = each page fits the sheet, multi-page;
 * Receipt = smaller than the page, you set its real width). Selecting a card persists the choice into
 * the [ScanSession] (via [AppViewModel.selectCardType]) and advances; documents branch into their own
 * capture flow. Custom collects runtime mm dimensions first.
 */
@Composable
fun CardTypeScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDocumentSelected: (CardType) -> Unit = {},
    exportSettingsViewModel: ExportSettingsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    // Last-used custom dimensions, remembered across sessions (prefills the dialog).
    val persistedCustom by exportSettingsViewModel.cardSettings(CardType.CUSTOM)
        .collectAsStateWithLifecycle(initialValue = null)
    val unit by settingsViewModel.unit.collectAsStateWithLifecycle()

    val pick: (CardType) -> Unit = { type ->
        when (type) {
            CardType.CUSTOM -> showCustomDialog = true
            // Documents get their own capture/edit/size flow (multi-page, receipt widths).
            CardType.FULL_PAGE_DOCUMENT, CardType.RECEIPT -> {
                viewModel.selectCardType(type)
                onDocumentSelected(type)
            }
            else -> {
                viewModel.selectCardType(type)
                onNext()
            }
        }
    }

    ScreenScaffold(title = "Choose a card or document", onBack = onBack) {
        SectionLabel("Cards")
        cardSection.forEach { type -> CardTypeRow(type = type, unit = unit, onClick = { pick(type) }) }
        SectionLabel("Documents", modifier = Modifier.padding(top = 8.dp))
        documentSection.forEach { type -> CardTypeRow(type = type, unit = unit, onClick = { pick(type) }) }
    }

    if (showCustomDialog) {
        CustomSizeDialog(
            unit = unit,
            onUnitChange = settingsViewModel::setUnit,
            onDismiss = { showCustomDialog = false },
            onConfirm = { widthMm, heightMm ->
                viewModel.selectCardType(CardType.CUSTOM, customWidthMm = widthMm, customHeightMm = heightMm)
                // Remember the size immediately (merged into the stored blob): the task flow never
                // reaches Configure, so this is its only persistence point.
                exportSettingsViewModel.saveCardSettings(
                    CardType.CUSTOM,
                    (persistedCustom ?: CardExportSettings())
                        .copy(customWidthMm = widthMm, customHeightMm = heightMm),
                )
                showCustomDialog = false
                onNext()
            },
            initialWidthMm = persistedCustom?.customWidthMm ?: 85.6,
            initialHeightMm = persistedCustom?.customHeightMm ?: 54.0,
        )
    }
}

// Presentation grouping and order for the two screen sections.
private val cardSection = listOf(CardType.AADHAAR, CardType.PAN, CardType.VOTER_ID, CardType.CUSTOM)
private val documentSection = listOf(CardType.FULL_PAGE_DOCUMENT, CardType.RECEIPT)

/**
 * One full-width option row of uniform height: the final-output sheet illustration on the left,
 * the title and subtitle filling the right.
 */
@Composable
private fun CardTypeRow(
    type: CardType,
    unit: DimensionUnit,
    onClick: () -> Unit,
) {
    BayaanCard(
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Min, not fixed: a fixed 50dp left only 28dp under the title, so the subtitle's
                // second line could never render and every longer description was ellipsised.
                .heightIn(min = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CardOutputArtwork(
                type = type,
                // Explicit height, not fillMaxHeight: the row is now min-height inside a scrolling
                // column, so its max height is unbounded and fillMaxHeight would collapse to zero.
                modifier = Modifier.size(width = 44.dp, height = 50.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextHeading,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleFor(type, unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun subtitleFor(type: CardType, unit: DimensionUnit): String = when (type) {
    CardType.PAN, CardType.AADHAAR, CardType.VOTER_ID -> {
        val w = type.widthMm
        val h = type.heightMm
        val size = if (w != null && h != null) formatSize(w, h, unit) else "?"
        "$size · both sides on one page"
    }
    CardType.FULL_PAGE_DOCUMENT -> "Admit card, certificate, deeds — fits the page"
    CardType.CUSTOM -> "Your dimensions, true size"
    CardType.RECEIPT -> "Bills, labels, prescriptions — you set the width"
}
