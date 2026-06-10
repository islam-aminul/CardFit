package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Top-level chooser (CLAUDE.md Phase 13): the photo flow lives alongside the existing ID-card
 * document flow. Two tappable tiles route into each.
 */
@Composable
fun HomeScreen(
    onChooseDocument: () -> Unit,
    onChoosePhoto: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenScaffold(title = "CardFit") {
        Text(
            "Everything stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        HomeTile(
            title = "ID card",
            subtitle = "Scan both sides and lay them out for print or upload.",
            onClick = onChooseDocument,
        )
        HomeTile(
            title = "Photo",
            subtitle = "Crop, enhance, and size a passport / visa / stamp photo.",
            onClick = onChoosePhoto,
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "About & licenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings)
                .padding(8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeTile(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
