package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import `in`.firm.consultancy.bayaan.cardfit.ui.NormCrop
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CropOverlay
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Photo flow step 2 (CLAUDE.md Phase 13): edit the photo — rotate, crop (free or locked to the
 * selected size's aspect), brightness/contrast/saturation, deterministic auto-enhance, and opt-in
 * background removal. A live preview shows the result; "Compare" reveals the untouched original, and
 * "Revert" restores it. The original file is never modified.
 */
@Composable
fun PhotoEditScreen(
    viewModel: PhotoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val original by viewModel.original.collectAsStateWithLifecycle()
    val busy by viewModel.previewBusy.collectAsStateWithLifecycle()

    var compare by remember { mutableStateOf(false) }

    val cropEnabled = state.cropNorm != null
    val lockAspect = state.resolvedSize?.aspectRatio?.toFloat()

    ScreenScaffold(title = "Edit photo") {
        // --- preview with optional crop overlay ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.previewAspect())
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val shown = if (compare) original else preview
            shown?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = if (compare) "Original photo" else "Edited preview",
                    contentScale = if (compare) ContentScale.Fit else ContentScale.FillBounds,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!compare && cropEnabled) {
                CropOverlay(
                    crop = state.cropNorm!!,
                    lockAspect = if (state.aspectLocked) lockAspect else null,
                    onCropChange = viewModel::setCropNorm,
                )
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.rotateClockwise() }, modifier = Modifier.weight(1f)) {
                Text("Rotate")
            }
            OutlinedButton(onClick = { compare = !compare }, modifier = Modifier.weight(1f)) {
                Text(if (compare) "Show edited" else "Compare")
            }
            TextButton(onClick = { viewModel.revertEdits() }, modifier = Modifier.weight(1f)) {
                Text("Revert")
            }
        }

        HorizontalDivider()

        // --- crop ---
        ToggleRow(
            label = "Crop",
            checked = cropEnabled,
            onChange = { on ->
                if (on) viewModel.setCropNorm(DEFAULT_CROP) else {
                    viewModel.setCropNorm(null)
                    viewModel.setAspectLocked(false)
                }
            },
        )
        if (cropEnabled) {
            ToggleRow(
                label = "Lock to ${state.size.label} ratio",
                checked = state.aspectLocked,
                onChange = viewModel::setAspectLocked,
            )
        }

        HorizontalDivider()

        // --- adjustments ---
        AdjustSlider("Brightness", state.brightnessPercent, viewModel::setBrightness)
        AdjustSlider("Contrast", state.contrastPercent, viewModel::setContrast)
        AdjustSlider("Saturation", state.saturationPercent, viewModel::setSaturation)
        ToggleRow("Auto-enhance", state.autoEnhance, viewModel::setAutoEnhance)

        HorizontalDivider()

        // --- background ---
        ToggleRow("Remove background (white)", state.removeBackground, viewModel::setRemoveBackground)
        if (state.removeBackground) {
            Text(
                "On-device only. Quality varies on hair and edges — turn off to revert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * A -100..100 adjustment slider that updates a smooth local value while dragging and commits to the
 * ViewModel only when the gesture ends (so the edit pipeline re-runs once per change, not per tick).
 */
@Composable
private fun AdjustSlider(label: String, value: Int, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(local.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.toInt()) },
            valueRange = -100f..100f,
            modifier = Modifier.heightIn(min = 32.dp),
        )
    }
}

private val DEFAULT_CROP = NormCrop(0.1f, 0.1f, 0.9f, 0.9f)
