package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.NameSuggestion
import `in`.firm.consultancy.bayaan.cardfit.ui.NameViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

/**
 * Step 4 (CLAUDE.md sections 11.4 and 10): an editable name field pre-filled by an OCR *suggestion*.
 * The suggestion only pre-fills when the field is empty (never overwrites the user's edits and never
 * auto-finalizes); an empty/failed OCR result leaves the field empty for manual entry.
 */
@Composable
fun NameScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    nameViewModel: NameViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestion by nameViewModel.suggestion.collectAsStateWithLifecycle()
    val session = state.session

    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(session?.front?.imageUri) {
        if (session != null) nameViewModel.suggestFrom(session)
    }

    // Pre-fill ONCE, and only into a still-empty field — keep it a suggestion, never a finalization.
    LaunchedEffect(suggestion) {
        val ready = suggestion as? NameSuggestion.Ready ?: return@LaunchedEffect
        val name = ready.name
        if (!prefilled && name != null && state.name.isBlank()) {
            viewModel.setName(name)
            prefilled = true
        }
    }

    ScreenScaffold(title = "Name on file") {
        Text("Used only for the filename. Edit freely; nothing is auto-finalized.")

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text("Holder name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when (val s = suggestion) {
            NameSuggestion.Loading -> Text(
                "Reading the name from your scan…",
                style = MaterialTheme.typography.bodySmall,
            )
            is NameSuggestion.Ready -> {
                val message = if (s.name != null) {
                    "Suggested from the scan — edit if it's not quite right."
                } else {
                    "No name detected — type it in if you'd like."
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            NameSuggestion.Idle -> Unit
        }

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
