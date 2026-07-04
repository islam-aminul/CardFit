package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [Prefs]. Persists the user preferences that survive across sessions. Currently
 * only the Phase-11 searchable-PDF toggle is stored; other [UserPrefs] fields keep their defaults.
 * No network, no analytics — purely local on-device storage.
 */
class AndroidPrefs(private val context: Context) : Prefs {

    private val searchableKey = booleanPreferencesKey("searchable_text")

    override val prefs: Flow<UserPrefs> = context.cardFitDataStore.data.map { stored ->
        UserPrefs(searchableText = stored[searchableKey] ?: false)
    }

    override suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        context.cardFitDataStore.edit { stored ->
            val current = UserPrefs(searchableText = stored[searchableKey] ?: false)
            val updated = transform(current)
            stored[searchableKey] = updated.searchableText
        }
    }
}
