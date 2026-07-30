package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import `in`.firm.consultancy.bayaan.cardfit.domain.formatInUnit
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.ErrorRed
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Custom card-size entry with a cm/inch unit selector. All math is in millimetres; the UI converts
 * at the boundary (1 cm = 10 mm, 1 inch = 25.4 mm). Validated against [Defaults.CUSTOM_MIN_MM]..
 * [Defaults.CUSTOM_MAX_MM]. Shared by the card-type picker and the Configure size override.
 *
 * [unit] is hoisted rather than owned here: the dialog is composed conditionally, so local state
 * died on every dismiss and the choice reset to cm. The caller supplies the persisted preference and
 * [onUnitChange] writes it back, so the selection is shared with every other size input and survives
 * both dismissal and process death.
 */
@Composable
fun CustomSizeDialog(
    unit: DimensionUnit,
    onUnitChange: (DimensionUnit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (widthMm: Double, heightMm: Double) -> Unit,
    initialWidthMm: Double = 85.6,
    initialHeightMm: Double = 54.0,
) {
    var widthText by remember { mutableStateOf(formatInUnit(unit.fromMm(initialWidthMm))) }
    var heightText by remember { mutableStateOf(formatInUnit(unit.fromMm(initialHeightMm))) }

    // Re-express what the user already typed when the unit changes. The previous unit has to be
    // tracked explicitly now that [unit] arrives from outside — the effect fires after the change.
    var previousUnit by remember { mutableStateOf(unit) }
    LaunchedEffect(unit) {
        if (unit != previousUnit) {
            widthText.toDoubleOrNull()?.let { widthText = formatInUnit(unit.fromMm(previousUnit.toMm(it))) }
            heightText.toDoubleOrNull()?.let { heightText = formatInUnit(unit.fromMm(previousUnit.toMm(it))) }
            previousUnit = unit
        }
    }

    val widthMm = widthText.toDoubleOrNull()?.let { unit.toMm(it) }
    val heightMm = heightText.toDoubleOrNull()?.let { unit.toMm(it) }

    fun inBounds(mm: Double?): Boolean =
        mm != null && mm >= Defaults.CUSTOM_MIN_MM && mm <= Defaults.CUSTOM_MAX_MM
    val valid = inBounds(widthMm) && inBounds(heightMm)
    val anyOutOfBounds = (widthText.isNotBlank() && !inBounds(widthMm)) ||
        (heightText.isNotBlank() && !inBounds(heightMm))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        titleContentColor = TextHeading,
        textContentColor = Ink,
        title = { Text("Custom card size", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text("Enter the physical size of your card.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DimensionUnit.entries.forEach { u ->
                        SelectableCard(
                            label = u.label,
                            selected = unit == u,
                            onClick = { onUnitChange(u) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                BayaanTextField(
                    value = widthText,
                    onValueChange = { widthText = it },
                    label = "Width (${unit.label})",
                    isError = widthText.isNotBlank() && !inBounds(widthMm),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                BayaanTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = "Height (${unit.label})",
                    isError = heightText.isNotBlank() && !inBounds(heightMm),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allowed: ${formatInUnit(unit.fromMm(Defaults.CUSTOM_MIN_MM))}–" +
                        "${formatInUnit(unit.fromMm(Defaults.CUSTOM_MAX_MM))} ${unit.label}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (anyOutOfBounds) ErrorRed else TextMuted,
                )
            }
        },
        confirmButton = {
            PrimaryButton(onClick = { if (valid) onConfirm(widthMm!!, heightMm!!) }, enabled = valid) {
                Text("Use size")
            }
        },
        dismissButton = { GhostButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
