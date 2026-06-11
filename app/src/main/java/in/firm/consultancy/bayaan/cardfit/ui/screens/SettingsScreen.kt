package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * About: the firm's name + contact details (from bayaan.consultancy.firm.in), the on-device privacy
 * statement, and open-source licenses. Back is anchored at the bottom.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    fun open(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri())) }
    }

    ScreenScaffold(title = "About", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Bayaan Consultancy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Contact",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ContactRow(Icons.Filled.Email, "bayaan@consultancy.firm.in") {
                    open("mailto:bayaan@consultancy.firm.in")
                }
                ContactRow(Icons.Filled.Phone, "+91 6399009003") { open("tel:+916399009003") }
                ContactRow(Icons.Filled.Language, "bayaan.consultancy.firm.in") {
                    open("https://bayaan.consultancy.firm.in/")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "CardFit works entirely on your device. Scanning, text recognition, and file " +
                        "generation all happen offline.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "• No internet permission — nothing is uploaded or sent anywhere.\n" +
                        "• No accounts, analytics, ads, or tracking.\n" +
                        "• Your card images and any ID numbers never leave the device, and ID " +
                        "numbers are never put in filenames or logs.\n" +
                        "• Files you save go to your Downloads folder; sharing is always your choice.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(
            onClick = {
                OssLicensesMenuActivity.setActivityTitle("Open source licenses")
                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open source licenses") }
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
