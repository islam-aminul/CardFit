package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit

/**
 * Custom card-size entry with a cm/inch unit selector. All math is in millimetres; the UI converts
 * at the boundary (1 cm = 10 mm, 1 inch = 25.4 mm). Validated against [Defaults.CUSTOM_MIN_MM]..
 * [Defaults.CUSTOM_MAX_MM]. Shared by the card-type picker and the Configure size override.
 */
@Composable
fun CustomSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (widthMm: Double, heightMm: Double) -> Unit,
    initialWidthMm: Double = 85.6,
    initialHeightMm: Double = 54.0,
) {
    var unit by remember { mutableStateOf(DimensionUnit.CM) }
    var widthText by remember { mutableStateOf(formatNumber(unit.fromMm(initialWidthMm))) }
    var heightText by remember { mutableStateOf(formatNumber(unit.fromMm(initialHeightMm))) }

    val width = widthText.toDoubleOrNull()
    val height = heightText.toDoubleOrNull()
    val widthMm = width?.let { unit.toMm(it) }
    val heightMm = height?.let { unit.toMm(it) }

    fun inBounds(mm: Double?): Boolean =
        mm != null && mm >= Defaults.CUSTOM_MIN_MM && mm <= Defaults.CUSTOM_MAX_MM
    val valid = inBounds(widthMm) && inBounds(heightMm)

    fun switchUnit(target: DimensionUnit) {
        if (target == unit) return
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
            TextButton(onClick = { if (valid) onConfirm(widthMm!!, heightMm!!) }, enabled = valid) {
                Text("Use size")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Round to 2 decimals and drop a trailing ".0" for tidy display. */
internal fun formatNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
