package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.SageSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Sage700
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading

/**
 * About: the firm's name + website (bayaan.consultancy.firm.in), the on-device privacy statement, and
 * open-source licenses. Back is anchored at the bottom.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val context = LocalContext.current

    fun open(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri())) }
    }

    ScreenScaffold(title = "About", onBack = onBack) {
        // The quote card carries the sage "calm second voice".
        BayaanCard(
            containerColor = SageSoft,
            border = null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Trusts are returned to those they belong to, and matters between people are " +
                    "settled with fairness and justice.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = Sage700,
            )
        }

        BayaanCard(modifier = Modifier.fillMaxWidth()) {
            Text("Bayaan Consultancy", style = MaterialTheme.typography.titleMedium, color = TextHeading)
            ContactRow(Icons.Filled.Language, "bayaan.consultancy.firm.in") {
                open("https://bayaan.consultancy.firm.in/")
            }
        }

        BayaanCard(modifier = Modifier.fillMaxWidth()) {
            Text("Privacy", style = MaterialTheme.typography.titleMedium, color = TextHeading)
            Text(
                "CardFit works entirely on your device. Scanning, text recognition, photo " +
                    "editing, and file generation all happen offline.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            listOf(
                "No internet permission — nothing is uploaded or sent anywhere.",
                "No accounts, analytics, ads, or tracking.",
                "Your card images, photos, and any ID numbers never leave the device, and " +
                    "ID numbers are never put in filenames or logs.",
                "Photo background removal and enhancement run on-device; your photos are " +
                    "never sent anywhere.",
                "Files you save go to your Downloads folder; sharing is always your choice.",
            ).forEach { line -> PrivacyLine(line) }
        }

        GhostButton(onClick = onOpenLicenses, modifier = Modifier.fillMaxWidth()) {
            Text("Open source licenses")
        }
    }
}

/** One privacy fact with the teal status dot the brand uses in place of bullets. */
@Composable
private fun PrivacyLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(Teal500, CircleShape),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink)
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Teal600,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Teal600)
    }
}
