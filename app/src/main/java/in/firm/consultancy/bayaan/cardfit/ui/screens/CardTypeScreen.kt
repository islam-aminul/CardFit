package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CardArtwork

/**
 * Step 1 (CLAUDE.md section 11.1): tappable card-type tiles with original stylized illustrations.
 * Selecting a type persists the choice into the [ScanSession] (via [AppViewModel.selectCardType])
 * and advances. Custom collects runtime mm dimensions first; Free needs no dimensions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTypeScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose card type") }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(CardType.entries) { type ->
                CardTypeTile(
                    type = type,
                    onClick = {
                        if (type == CardType.CUSTOM) {
                            showCustomDialog = true
                        } else {
                            viewModel.selectCardType(type)
                            onNext()
                        }
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Settings / About")
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomSizeDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { widthMm, heightMm ->
                viewModel.selectCardType(CardType.CUSTOM, customWidthMm = widthMm, customHeightMm = heightMm)
                showCustomDialog = false
                onNext()
            },
        )
    }
}

@Composable
private fun CardTypeTile(
    type: CardType,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardArtwork(
                type = type,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.585f, matchHeightConstraintsFirst = false)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = labelFor(type),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitleFor(type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CustomSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (widthMm: Double, heightMm: Double) -> Unit,
) {
    // Internal math stays in mm; the UI converts at the boundary. Defaults = CR-80 in cm.
    var unit by remember { mutableStateOf(DimensionUnit.CM) }
    var widthText by remember { mutableStateOf("8.56") }
    var heightText by remember { mutableStateOf("5.4") }

    val width = widthText.toDoubleOrNull()
    val height = heightText.toDoubleOrNull()
    val widthMm = width?.let { unit.toMm(it) }
    val heightMm = height?.let { unit.toMm(it) }

    fun inBounds(mm: Double?): Boolean =
        mm != null && mm >= Defaults.CUSTOM_MIN_MM && mm <= Defaults.CUSTOM_MAX_MM
    val valid = inBounds(widthMm) && inBounds(heightMm)

    fun switchUnit(target: DimensionUnit) {
        if (target == unit) return
        // Preserve the physical size: re-express the current numbers in the new unit.
        width?.let { widthText = formatNumber(target.fromMm(unit.toMm(it))) }
        height?.let { heightText = formatNumber(target.fromMm(unit.toMm(it))) }
        unit = target
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom card size") },
        text = {
            Column {
                Text("Enter the physical size of your card.")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DimensionUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { switchUnit(u) },
                            label = { Text(u.label) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it },
                    label = { Text("Width (${unit.label})") },
                    singleLine = true,
                    isError = widthText.isNotBlank() && !inBounds(widthMm),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (${unit.label})") },
                    singleLine = true,
                    isError = heightText.isNotBlank() && !inBounds(heightMm),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allowed: ${formatNumber(unit.fromMm(Defaults.CUSTOM_MIN_MM))}–" +
                        "${formatNumber(unit.fromMm(Defaults.CUSTOM_MAX_MM))} ${unit.label}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onConfirm(widthMm!!, heightMm!!) },
                enabled = valid,
            ) { Text("Use size") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Round to 2 decimals and drop a trailing ".0" for tidy display. */
private fun formatNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun labelFor(type: CardType): String = when (type) {
    CardType.PAN -> "PAN"
    CardType.AADHAAR -> "Aadhaar"
    CardType.EPIC -> "Voter ID (EPIC)"
    CardType.ADMIT_CARD -> "Admit card"
    CardType.CUSTOM -> "Custom"
    CardType.FREE -> "Free"
}

private fun subtitleFor(type: CardType): String = when (type) {
    CardType.PAN, CardType.AADHAAR, CardType.EPIC ->
        "${trimMm(type.widthMm)} × ${trimMm(type.heightMm)} mm"
    CardType.ADMIT_CARD -> "Fit to page"
    CardType.CUSTOM -> "Your dimensions"
    CardType.FREE -> "Fit to width"
}

private fun trimMm(value: Double?): String {
    if (value == null) return "?"
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
