package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoSize
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SelectableCard

/**
 * Photo flow step 3 (CLAUDE.md Phase 13): choose the photo size — Passport (India) 35×45 mm, Visa
 * 2×2 in (51×51 mm), Stamp 20×25 mm, or Custom (cm/inch via the shared converter; stored in mm).
 */
@Composable
fun PhotoSizeScreen(
    viewModel: PhotoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCustom by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Photo size") {
        Text("Pick a standard size or enter your own.")

        PhotoSize.entries.forEach { size ->
            val selected = state.size == size
            SelectableCard(
                label = labelFor(size, state.customWidthMm, state.customHeightMm),
                selected = selected,
                onClick = {
                    if (size == PhotoSize.CUSTOM) showCustom = true else viewModel.selectSize(size)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(onClick = onNext, enabled = state.resolvedSize != null, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }

    if (showCustom) {
        CustomSizeDialog(
            onDismiss = { showCustom = false },
            onConfirm = { widthMm, heightMm ->
                viewModel.setCustomSizeMm(widthMm, heightMm)
                showCustom = false
            },
            initialWidthMm = state.customWidthMm ?: 35.0,
            initialHeightMm = state.customHeightMm ?: 45.0,
        )
    }
}

private fun labelFor(size: PhotoSize, customW: Double?, customH: Double?): String = when (size) {
    PhotoSize.PASSPORT_INDIA -> "Passport (India) — 35 × 45 mm"
    PhotoSize.VISA -> "Visa — 51 × 51 mm (2 × 2 in)"
    PhotoSize.STAMP -> "Stamp — 20 × 25 mm"
    PhotoSize.CUSTOM -> if (customW != null && customH != null) {
        "Custom — ${trim(customW)} × ${trim(customH)} mm"
    } else {
        "Custom…"
    }
}

private fun trim(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
