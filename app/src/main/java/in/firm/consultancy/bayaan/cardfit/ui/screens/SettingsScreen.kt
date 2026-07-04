package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import `in`.firm.consultancy.bayaan.cardfit.R
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTopBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * About (design-system CfAbout): app identity card, the privacy facts with teal dots, clickable
 * Website and Open-source-licenses rows, and the "Built by" Bayaan lockup pinned at the bottom.
 * Fully offline — the version comes from PackageManager, everything else is static.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: "1.0"
    }

    fun open(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri())) }
    }

    Scaffold(
        topBar = { BayaanTopBar("About") },
        bottomBar = {
            ScaffoldBottomBar {
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // --- app identity ---
                BayaanCard(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "CardFit",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextHeading,
                        )
                        Text(
                            "Version $versionName · fully offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                        Text(
                            "Scan IDs and documents, prepare ID/passport photos, and export " +
                                "print-ready pages — all on your device.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                    }
                }

                // --- privacy ---
                SectionLabel("Privacy")
                BayaanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrivacyLine("Everything stays on your device.")
                        PrivacyLine("No accounts, no tracking, no network access.")
                        PrivacyLine("Files are saved only where you choose.")
                    }
                }

                // --- links ---
                SectionLabel("More")
                BayaanCard(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LinkRow(
                        icon = Icons.Filled.Language,
                        label = "Website",
                        value = "bayaan.consultancy.firm.in",
                        onClick = { open("https://bayaan.consultancy.firm.in/") },
                    )
                    HorizontalDivider()
                    LinkRow(
                        icon = Icons.AutoMirrored.Outlined.Article,
                        label = "Open-source licenses",
                        value = null,
                        onClick = onOpenLicenses,
                    )
                }
            }

            // --- "Built by" lockup, pinned above the bottom bar ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Built by",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMuted,
                )
                Image(
                    painter = painterResource(R.drawable.ic_bayaan_logo),
                    contentDescription = "Bayaan Consultancy",
                    modifier = Modifier.height(32.dp),
                )
            }
        }
    }
}

/** One privacy fact with the teal status dot the brand uses in place of bullets. */
@Composable
private fun PrivacyLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(Teal500, CircleShape),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

/** A clickable design-system row: teal leading icon, label (+ optional muted value), chevron. */
@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Teal600, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextMuted,
        )
    }
}
