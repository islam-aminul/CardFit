package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.AndroidPrefs
import `in`.firm.consultancy.bayaan.cardfit.data.UserPrefs
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes persisted preferences (DataStore) for the UI: the searchable-PDF toggle and the cm/inch
 * unit shared by every size input and dimension readout.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AndroidPrefs(application)

    val searchableText: StateFlow<Boolean> = prefs.prefs
        .map { it.searchableText }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Seeded with the same default as [UserPrefs.unit] so the first frame doesn't show a stale unit. */
    val unit: StateFlow<DimensionUnit> = prefs.prefs
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DimensionUnit.CM)

    fun setSearchableText(value: Boolean) {
        viewModelScope.launch {
            prefs.update { it.copy(searchableText = value) }
        }
    }

    fun setUnit(value: DimensionUnit) {
        viewModelScope.launch {
            prefs.update { it.copy(unit = value) }
        }
    }
}
