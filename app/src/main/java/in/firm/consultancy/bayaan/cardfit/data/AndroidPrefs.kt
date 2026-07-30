package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [Prefs]. Persists the user preferences that survive across sessions: the
 * searchable-PDF toggle and the cm/inch unit used by every size input and dimension readout. Other
 * [UserPrefs] fields keep their defaults. No network, no analytics — purely local on-device storage.
 *
 * Both [prefs] and [update] read through the same [readPrefs] so a new key can never be added to one
 * half and forgotten in the other (which would make it read back as the default forever).
 */
class AndroidPrefs(private val context: Context) : Prefs {

    private val searchableKey = booleanPreferencesKey("searchable_text")
    private val unitKey = stringPreferencesKey("dimension_unit")

    override val prefs: Flow<UserPrefs> = context.cardFitDataStore.data.map(::readPrefs)

    override suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        context.cardFitDataStore.edit { stored ->
            val updated = transform(readPrefs(stored))
            stored[searchableKey] = updated.searchableText
            stored[unitKey] = updated.unit.name
        }
    }

    private fun readPrefs(stored: Preferences): UserPrefs = UserPrefs(
        searchableText = stored[searchableKey] ?: false,
        // Stored by enum name; an unknown/renamed value falls back to the default rather than throwing.
        unit = stored[unitKey]?.let { runCatching { DimensionUnit.valueOf(it) }.getOrNull() }
            ?: DimensionUnit.CM,
    )
}
