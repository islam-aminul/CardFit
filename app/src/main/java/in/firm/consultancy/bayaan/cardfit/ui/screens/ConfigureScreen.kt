package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

private enum class Purpose(val label: String, val modes: Set<OutputMode>) {
    PRINT("Print", setOf(OutputMode.PRINT)),
    UPLOAD("Upload", setOf(OutputMode.UPLOAD)),
    BOTH("Both", setOf(OutputMode.PRINT, OutputMode.UPLOAD)),
}

/**
 * Step 3 (CLAUDE.md section 11.3): purpose tiles (Print / Upload / Both), paper-size tiles, format,
 * grayscale; the max-size field appears only when Upload is part of the selection and crop-marks
 * only when Print is. The selection drives [AppViewModel.renderConfigs], shown in the summary.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigureScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uploadSelected = OutputMode.UPLOAD in state.selectedModes
    val printSelected = OutputMode.PRINT in state.selectedModes

    var maxSizeText by remember { mutableStateOf(state.maxFileSizeKb?.toString().orEmpty()) }

    val currentPurpose = Purpose.entries.firstOrNull { it.modes == state.selectedModes }

    ScreenScaffold(title = "Configure output") {
        // --- Purpose ---
        SectionLabel("Purpose")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Purpose.entries.forEach { purpose ->
                FilterChip(
                    selected = currentPurpose == purpose,
                    onClick = { viewModel.setModes(purpose.modes) },
                    label = { Text(purpose.label) },
                )
            }
        }

        // --- Paper size ---
        SectionLabel("Paper size")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperSize.entries.forEach { paper ->
                FilterChip(
                    selected = state.paper == paper,
                    onClick = { viewModel.setPaper(paper) },
                    label = { Text(paper.name) },
                )
            }
        }

        // --- Format ---
        SectionLabel("Format")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputFormat.entries.forEach { format ->
                FilterChip(
                    selected = state.format == format,
                    onClick = { viewModel.setFormat(format) },
                    label = { Text(format.name) },
                )
            }
        }

        // --- Grayscale (always available) ---
        ToggleRow(
            label = "Grayscale",
            checked = state.grayscale,
            onCheckedChange = viewModel::setGrayscale,
        )

        // --- Crop marks: PRINT only ---
        if (printSelected) {
            ToggleRow(
                label = "Crop marks (print)",
                checked = state.cropMarks,
                onCheckedChange = viewModel::setCropMarks,
            )
        }

        // --- Max upload size: UPLOAD only ---
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

        ConfigSummary(viewModel)

        Button(
            onClick = onNext,
            enabled = state.selectedModes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Next") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfigSummary(viewModel: AppViewModel) {
    val configs = viewModel.renderConfigs()
    if (configs.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Will generate ${configs.size} file(s):", style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            configs.forEach { config ->
                val capSuffix = config.maxFileSizeKb?.let { " · ≤ $it KB" }.orEmpty()
                val cropSuffix = if (config.cropMarks) " · crop marks" else ""
                val graySuffix = if (config.grayscale) " · grayscale" else ""
                Text(
                    "• ${config.mode.name}: ${config.format.name} @ ${config.dpi} dpi, " +
                        "${config.paper.name}$capSuffix$cropSuffix$graySuffix",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
