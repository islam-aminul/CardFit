package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Step 6 (CLAUDE.md section 11.6): an "Open source licenses" entry (Google OSS Licenses plugin) and
 * a short on-device-only privacy statement.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    ScreenScaffold(title = "Settings & About") {
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

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
