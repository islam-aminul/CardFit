package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidExportSettingsStore
import `in`.firm.consultancy.bayaan.cardfit.data.ExportSettingsStore
import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Screen-layer bridge to the per-option [ExportSettingsStore], keeping [AppViewModel] free of
 * Android dependencies (the same pattern as [SettingsViewModel] for the global searchable flag).
 */
class ExportSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store: ExportSettingsStore = AndroidExportSettingsStore(application)

    fun cardSettings(type: CardType): Flow<CardExportSettings?> = store.cardSettings(type)

    /** One-shot read of the persisted settings for [type] (null when nothing is stored yet). */
    suspend fun loadCardSettings(type: CardType): CardExportSettings? = store.cardSettings(type).first()

    fun saveCardSettings(type: CardType, settings: CardExportSettings) {
        viewModelScope.launch { store.saveCardSettings(type, settings) }
    }
}

/**
 * Persist the render-settings draft to the current card type's blob whenever it changes. Saves are
 * gated on [AppState.settingsSeededFor] matching the session's type, so nothing is written before
 * that type's persisted settings have been applied (the seed/save handshake — otherwise the defaults
 * would race the seed and wipe the stored blob). Debounced so slider drags don't spam DataStore.
 *
 * Installed on every screen where the draft can change (Configure and Preview).
 */
@OptIn(FlowPreview::class)
@Composable
fun PersistCardSettingsEffect(
    appViewModel: AppViewModel,
    settingsViewModel: ExportSettingsViewModel,
) {
    LaunchedEffect(appViewModel, settingsViewModel) {
        appViewModel.state
            .filter { it.settingsSeededFor != null && it.settingsSeededFor == it.session?.cardType }
            .map { it.settingsSeededFor!! to it.toCardExportSettings() }
            .distinctUntilChanged()
            .debounce(300)
            .collect { (type, settings) -> settingsViewModel.saveCardSettings(type, settings) }
    }
}
