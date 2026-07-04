package `in`.firm.consultancy.bayaan.cardfit.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.ExportSettingsJson
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [ExportSettingsStore]: one JSON blob per option under a slug-keyed preference
 * (`card_settings_<slug>` / `photo_settings_<slug>`). Corrupt or missing blobs read as null.
 * Purely local, no network.
 */
class AndroidExportSettingsStore(private val context: Context) : ExportSettingsStore {

    private fun cardKey(type: CardType) = stringPreferencesKey("card_settings_${type.slug}")
    private fun photoKey(size: PhotoSize) = stringPreferencesKey("photo_settings_${size.slug}")

    override fun cardSettings(type: CardType): Flow<CardExportSettings?> =
        context.cardFitDataStore.data.map { stored ->
            stored[cardKey(type)]?.let(ExportSettingsJson::decodeCard)
        }

    override suspend fun saveCardSettings(type: CardType, settings: CardExportSettings) {
        context.cardFitDataStore.edit { stored ->
            stored[cardKey(type)] = ExportSettingsJson.encodeCard(settings)
        }
    }

    override fun photoSettings(size: PhotoSize): Flow<PhotoExportSettings?> =
        context.cardFitDataStore.data.map { stored ->
            stored[photoKey(size)]?.let(ExportSettingsJson::decodePhoto)
        }

    override suspend fun savePhotoSettings(size: PhotoSize, settings: PhotoExportSettings) {
        context.cardFitDataStore.edit { stored ->
            stored[photoKey(size)] = ExportSettingsJson.encodePhoto(settings)
        }
    }
}
