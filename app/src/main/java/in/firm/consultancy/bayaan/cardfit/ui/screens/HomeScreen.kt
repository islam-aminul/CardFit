package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Top-level chooser. Vibrant, icon-led tiles route into the three flows; About & licenses is anchored
 * at the very bottom.
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
            ScaffoldBottomBar {
                TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("About & open-source licenses")
                }
            }
        },
    ) {
        Text(
            "Everything stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        HomeTile(
            title = "Documents & cards",
            subtitle = "Scan any document or ID — both sides laid out on one page.",
            icon = Icons.Filled.DocumentScanner,
            iconBg = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onChooseDocument,
        )
        HomeTile(
            title = "Photo",
            subtitle = "Crop, enhance, and size a passport / visa / stamp photo.",
            icon = Icons.Filled.PhotoCamera,
            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onChoosePhoto,
        )
        HomeTile(
            title = "Tasks",
            subtitle = "Collect several people's documents into one application set.",
            icon = Icons.Filled.FolderCopy,
            iconBg = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onChooseTasks,
        )
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = iconBg, shape = CircleShape) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
