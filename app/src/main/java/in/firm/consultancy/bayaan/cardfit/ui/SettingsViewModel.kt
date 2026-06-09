package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes persisted preferences (DataStore) for the UI. Currently the searchable-PDF toggle. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AndroidPrefs(application)

    val searchableText: StateFlow<Boolean> = prefs.prefs
        .map { it.searchableText }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSearchableText(value: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(searchableText = value) }
        }
    }
}
