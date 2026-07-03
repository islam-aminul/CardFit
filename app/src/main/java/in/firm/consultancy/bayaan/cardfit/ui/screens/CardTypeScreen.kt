package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTopBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CardArtwork
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Step 1 (CLAUDE.md section 11.1): tappable card-type tiles with original stylized illustrations.
 * Selecting a type persists the choice into the [ScanSession] (via [AppViewModel.selectCardType])
 * and advances. Custom collects runtime mm dimensions first; Free needs no dimensions.
 */
@Composable
fun CardTypeScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BayaanTopBar("Choose card type") },
        bottomBar = {
            ScaffoldBottomBar {
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
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
    BayaanCard(
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = TextHeading,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitleFor(type),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
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
