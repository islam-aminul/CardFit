package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The single process-wide DataStore for CardFit preferences. Shared by [AndroidPrefs] and
 * [AndroidExportSettingsStore] — `preferencesDataStore` requires exactly one delegate per file name,
 * so every consumer must go through this property.
 */
internal val Context.cardFitDataStore: DataStore<Preferences> by preferencesDataStore(name = "cardfit_prefs")
