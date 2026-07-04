package `in`.firm.consultancy.bayaan.cardfit.data

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.tasks.Task
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.ScanSlot
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.Scanner
import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fakes of the data-layer interfaces for JVM tests. They avoid invoking Android APIs, so
 * the contracts can be exercised without a device or Robolectric. (Types like Intent/Activity are
 * only referenced in signatures; their methods are never called.)
 */

class FakeScanner : Scanner {
    val persisted = mutableListOf<ScanSlot>()

    override fun startScanIntent(activity: Activity): Task<IntentSender> =
        throw UnsupportedOperationException("Launching is not exercised in JVM tests")

    override suspend fun persistFirstPage(
        resultIntent: Intent?,
        slot: ScanSlot,
    ): `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide {
        persisted += slot
        return `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide(
            imageUri = "content://fake/${slot.name.lowercase()}",
            widthPx = 856,
            heightPx = 540,
        )
    }

    val persistedPrefixes = mutableListOf<String>()

    override suspend fun persistPage(
        resultIntent: Intent?,
        prefix: String,
    ): `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide {
        persistedPrefixes += prefix
        return `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide(
            imageUri = "content://fake/$prefix",
            widthPx = 856,
            heightPx = 540,
        )
    }
}

class FakeOcr(
    var lines: List<String> = emptyList(),
    var layer: `in`.firm.consultancy.bayaan.cardfit.domain.OcrTextLayer =
        `in`.firm.consultancy.bayaan.cardfit.domain.OcrTextLayer(0, 0, emptyList()),
) : Ocr {
    override suspend fun recognize(imageUri: String): List<String> = lines
    override suspend fun recognizeLayer(imageUri: String) = layer
}

class FakePdfRenderer(var output: ByteArray = ByteArray(4)) : PdfRenderer {
    val rendered = mutableListOf<Pair<ScanSession, RenderConfig>>()

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput {
        rendered += session to config
        return RenderedOutput(output)
    }
}

class FakeJpegRenderer(var output: ByteArray = ByteArray(3)) : JpegRenderer {
    val rendered = mutableListOf<Pair<ScanSession, RenderConfig>>()

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput {
        rendered += session to config
        return RenderedOutput(output)
    }
}

class FakeFileSaver : FileSaver {
    data class Saved(val fileName: String, val mimeType: String, val size: Int)

    val saved = mutableListOf<Saved>()
    val shared = mutableListOf<Saved>()
    private val names = mutableSetOf<String>()

    /** Seed pre-existing names to exercise collision suffixing. */
    fun seedExisting(vararg fileNames: String) { names += fileNames }

    override fun exists(fileName: String): Boolean = fileName in names

    override suspend fun save(fileName: String, mimeType: String, bytes: ByteArray): String {
        saved += Saved(fileName, mimeType, bytes.size)
        names += fileName
        return "content://saved/$fileName"
    }

    override suspend fun cacheForShare(fileName: String, mimeType: String, bytes: ByteArray): String {
        shared += Saved(fileName, mimeType, bytes.size)
        return "content://share/$fileName"
    }
}

class FakePrefs(initial: UserPrefs = UserPrefs()) : Prefs {
    private val _prefs = MutableStateFlow(initial)
    override val prefs: Flow<UserPrefs> = _prefs

    override suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        _prefs.value = transform(_prefs.value)
    }

    fun current(): UserPrefs = _prefs.value
}

class FakeExportSettingsStore : ExportSettingsStore {
    private val cards = MutableStateFlow<Map<CardType, CardExportSettings>>(emptyMap())
    private val photos = MutableStateFlow<Map<PhotoSize, PhotoExportSettings>>(emptyMap())

    override fun cardSettings(type: CardType): Flow<CardExportSettings?> = cards.map { it[type] }

    override suspend fun saveCardSettings(type: CardType, settings: CardExportSettings) {
        cards.value = cards.value + (type to settings)
    }

    override fun photoSettings(size: PhotoSize): Flow<PhotoExportSettings?> = photos.map { it[size] }

    override suspend fun savePhotoSettings(size: PhotoSize, settings: PhotoExportSettings) {
        photos.value = photos.value + (size to settings)
    }

    fun currentCard(type: CardType): CardExportSettings? = cards.value[type]
    fun currentPhoto(size: PhotoSize): PhotoExportSettings? = photos.value[size]
}
