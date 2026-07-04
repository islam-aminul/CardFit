package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import `in`.firm.consultancy.bayaan.cardfit.domain.ReceiptWidth
import `in`.firm.consultancy.bayaan.cardfit.domain.receiptRealSizeMm
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTextField
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.BorderSubtle
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading

/**
 * Receipt width picker for the page at [pageIndex]: choose the receipt's real-world WIDTH (presets or
 * custom), in cm or inch; the height follows the scan's aspect (rotation-aware). A live A4-sheet
 * preview shows the actual scan placed true-size on the sheet. First time nothing is preselected;
 * afterwards the last-used width is preselected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptWidthScreen(
    viewModel: AppViewModel,
    pageIndex: Int,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val page = state.session?.documentPages?.getOrNull(pageIndex)

    // Preselect: this page's own width if set, else the last-used width; first receipt → none.
    var selectedWidthMm by remember(pageIndex) {
        mutableStateOf(page?.widthMm ?: state.lastReceiptWidthMm)
    }
    var unit by remember { mutableStateOf(DimensionUnit.CM) }
    var customMode by remember(pageIndex) {
        mutableStateOf(selectedWidthMm != null && ReceiptWidth.entries.none { it.widthMm == selectedWidthMm })
    }
    var customText by remember(pageIndex) {
        mutableStateOf(if (customMode && selectedWidthMm != null) fmt(unit.fromMm(selectedWidthMm!!)) else "")
    }

    // Rotation-aware pixel dims: a 90°/270° edit swaps width and height.
    val rot = ((page?.edit?.rotationDegrees ?: 0) % 360 + 360) % 360
    val swap = rot % 180 != 0
    val scanWpx = if (swap) (page?.side?.heightPx ?: 0) else (page?.side?.widthPx ?: 0)
    val scanHpx = if (swap) (page?.side?.widthPx ?: 0) else (page?.side?.heightPx ?: 0)

    val effectiveWidthMm = if (customMode) customText.toDoubleOrNull()?.let { unit.toMm(it) } else selectedWidthMm
    val valid = effectiveWidthMm != null && effectiveWidthMm in 20.0..300.0

    ScreenScaffold(
        title = "Receipt size",
        bottomBar = {
            ScaffoldBottomBar {
                PrimaryButton(
                    onClick = {
                        val w = effectiveWidthMm ?: return@PrimaryButton
                        viewModel.setPageWidthMm(pageIndex, w)
                        onDone()
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Use size") }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        if (page == null) {
            Text("No receipt to size.")
            return@ScreenScaffold
        }

        // Unit toggle (cm / inch).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Width")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DimensionUnit.entries.forEach { u ->
                    SelectableCard(
                        label = u.label,
                        selected = unit == u,
                        onClick = {
                            if (u != unit) {
                                // Re-express the custom text in the new unit.
                                customText.toDoubleOrNull()?.let { customText = fmt(u.fromMm(unit.toMm(it))) }
                                unit = u
                            }
                        },
                    )
                }
            }
        }
        HelpText("The receipt prints at this width; its height follows the scan.")

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReceiptWidth.entries.forEach { preset ->
                val presetW = preset.widthMm
                if (presetW != null) {
                    SelectableCard(
                        label = presetLabel(preset, unit),
                        selected = !customMode && selectedWidthMm == presetW,
                        onClick = { customMode = false; selectedWidthMm = presetW },
                    )
                } else {
                    SelectableCard(
                        label = "Custom",
                        selected = customMode,
                        onClick = { customMode = true },
                    )
                }
            }
        }

        if (customMode) {
            BayaanTextField(
                value = customText,
                onValueChange = { customText = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Custom width (${unit.label})",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                help = "Allowed: ${fmt(unit.fromMm(20.0))}–${fmt(unit.fromMm(300.0))} ${unit.label}.",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Live A4-sheet preview with the real size shown on the same line.
        val previewWidth = effectiveWidthMm
        if (previewWidth != null && previewWidth > 0) {
            val (realW, realH) = receiptRealSizeMm(previewWidth, scanWpx, scanHpx)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("On an A4 sheet")
                Text(
                    "${fmt(unit.fromMm(realW))} × ${fmt(unit.fromMm(realH))} ${unit.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHeading,
                )
            }
            A4SheetPreview(
                imageUri = page.side.imageUri,
                rotationDegrees = rot,
                realWmm = realW,
                realHmm = realH,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            HelpText("Pick a width to preview the receipt on the sheet.")
        }
    }
}

/** A portrait A4 rectangle with the actual scan placed centred at its true relative size. */
@Composable
private fun A4SheetPreview(
    imageUri: String,
    rotationDegrees: Int,
    realWmm: Double,
    realHmm: Double,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
            .aspectRatio(210f / 297f)
            .border(1.dp, BorderSubtle),
        contentAlignment = Alignment.Center,
    ) {
        val wFrac = (realWmm / 210.0).coerceIn(0.02, 1.0).toFloat()
        val hFrac = (realHmm / 297.0).coerceIn(0.02, 1.0).toFloat()
        val rw = maxWidth * wFrac
        val rh = maxHeight * hFrac
        AsyncImage(
            model = imageUri,
            contentDescription = "Receipt on sheet",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = rw, height = rh)
                .rotate(rotationDegrees.toFloat()),
        )
    }
}

/** e.g. "Thermal 5.7 cm" / "A5 5.83 in" — the preset's kind plus its width in the current unit. */
private fun presetLabel(preset: ReceiptWidth, unit: DimensionUnit): String {
    val w = preset.widthMm ?: return "Custom"
    val kind = when (preset) {
        ReceiptWidth.THERMAL_57, ReceiptWidth.THERMAL_80 -> "Thermal"
        ReceiptWidth.TWO_INCH -> "Roll"
        ReceiptWidth.POSTCARD_102 -> "Postcard"
        ReceiptWidth.A5_148 -> "A5"
        ReceiptWidth.LETTER_216 -> "Letter"
        ReceiptWidth.CUSTOM -> "Custom"
    }
    return "$kind ${fmt(unit.fromMm(w))} ${unit.label}"
}

private fun fmt(value: Double): String {
    val r = Math.round(value * 100.0) / 100.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}
