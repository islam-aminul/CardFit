package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.data.OssLicenseEntry
import `in`.firm.consultancy.bayaan.cardfit.data.loadOssLicenses
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline open-source licenses screen. Reads the bundled attribution data (see [loadOssLicenses]) and
 * lists each library; tapping one expands its full license text. Back is anchored at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<OssLicenseEntry>?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) { loadOssLicenses(context) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Open-source licenses") }) },
        bottomBar = {
            ScaffoldBottomBar {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) { padding ->
        val list = entries
        if (list == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(list, key = { it.name }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = if (expanded == entry.name) null else entry.name }
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall)
                        if (expanded == entry.name) {
                            SelectionContainer {
                                Text(
                                    entry.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
