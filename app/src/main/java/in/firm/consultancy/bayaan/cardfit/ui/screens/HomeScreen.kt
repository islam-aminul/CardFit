package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.AccentSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Paper
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.SageSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Sage700
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal700
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Top-level chooser. Bordered white flow tiles route into the three flows; About is anchored at the
 * very bottom as a fourth tile on the paper background (no action-bar chrome).
 */
@Composable
fun HomeScreen(
    onChooseDocument: () -> Unit,
    onChoosePhoto: () -> Unit,
    onChooseTasks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenScaffold(
        title = "CardFit",
        bottomBar = {
            // The About tile sits directly on the page background — an anchor, not an action bar.
            Surface(color = Paper) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    HomeTile(
                        title = "About",
                        subtitle = "Privacy, version, and open-source licenses.",
                        icon = Icons.Filled.Info,
                        iconBg = Midnight50,
                        iconTint = Midnight600,
                        onClick = onOpenSettings,
                    )
                }
            }
        },
    ) {
        HelpText("Everything stays on your device.")
        Spacer(Modifier.height(4.dp))

        HomeTile(
            title = "Documents & cards",
            subtitle = "Scan any document or ID — both sides laid out on one page.",
            icon = Icons.Filled.DocumentScanner,
            iconBg = AccentSoft,
            iconTint = Teal700,
            onClick = onChooseDocument,
        )
        HomeTile(
            title = "Photo",
            subtitle = "Crop, enhance, and size a passport / visa / stamp photo.",
            icon = Icons.Filled.PhotoCamera,
            iconBg = SageSoft,
            iconTint = Sage700,
            onClick = onChoosePhoto,
        )
        HomeTile(
            title = "Tasks",
            subtitle = "Collect several people's documents into one application set.",
            icon = Icons.Filled.FolderCopy,
            iconBg = Midnight50,
            iconTint = Midnight600,
            onClick = onChooseTasks,
        )
    }
}

/** Flow tile (.bv-home-tile): white bordered card, 52dp circular icon, title/subtitle, chevron. */
@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    BayaanCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = iconBg, shape = CircleShape) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextHeading)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
            )
        }
    }
}
