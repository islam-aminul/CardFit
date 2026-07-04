package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.NameSuggestion
import `in`.firm.consultancy.bayaan.cardfit.ui.NameViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTextField
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
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
    // Documents (full page / receipt) aren't identity cards: no holder-name OCR, a neutral file label.
    val isDocument = session?.cardType?.fitMode != null &&
        session.cardType.fitMode != `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode.ACTUAL_SIZE

    LaunchedEffect(session?.front?.imageUri) {
        if (session != null && !isDocument) nameViewModel.suggestFrom(session)
    }

    // Apply each new OCR suggestion: it replaces a blank or previously auto-filled name (so a new
    // scan that detects nothing clears the stale name), but never overwrites text the user typed.
    LaunchedEffect(suggestion) {
        val ready = suggestion as? NameSuggestion.Ready ?: return@LaunchedEffect
        viewModel.applyNameSuggestion(ready.name)
    }

    ScreenScaffold(
        title = "Name on file",
        bottomBar = {
            ScaffoldBottomBar {
                PrimaryButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        Text("Used only for the filename. Edit freely; nothing is auto-finalized.")

        BayaanTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = if (isDocument) "File name (optional)" else "Holder name (optional)",
            modifier = Modifier.fillMaxWidth(),
        )

        if (isDocument) {
            HelpText("Name the exported file — the person's name isn't needed for documents.")
        } else {
            when (val s = suggestion) {
                NameSuggestion.Loading -> HelpText("Reading the name from your scan…")
                is NameSuggestion.Ready -> {
                    val message = if (s.name != null) {
                        "Suggested from the scan — edit if it's not quite right."
                    } else {
                        "No name detected — type it in if you'd like."
                    }
                    HelpText(message)
                }
                NameSuggestion.Idle -> Unit
            }
        }
    }
}
