package `in`.firm.consultancy.bayaan.cardfit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.firm.consultancy.bayaan.cardfit.data.ocr.MlKitOcr
import `in`.firm.consultancy.bayaan.cardfit.domain.NameParser
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** OCR-derived name suggestion state for the Name screen. */
sealed interface NameSuggestion {
    data object Idle : NameSuggestion
    data object Loading : NameSuggestion

    /** [name] is null when nothing usable was detected (the field stays empty for manual entry). */
    data class Ready(val name: String?) : NameSuggestion
}

/**
 * Runs OCR on the FRONT side and produces a name *suggestion* via [NameParser] (CLAUDE.md section
 * 10). The suggestion only ever pre-fills an editable field; it never finalizes anything, and ID
 * numbers are never extracted (the parser rejects digit-bearing lines).
 */
class NameViewModel(application: Application) : AndroidViewModel(application) {

    private val ocr = MlKitOcr(application)

    private val _suggestion = MutableStateFlow<NameSuggestion>(NameSuggestion.Idle)
    val suggestion: StateFlow<NameSuggestion> = _suggestion.asStateFlow()

    private var processedUri: String? = null

    /** Recognize + parse the front side once per distinct image. Safe to call repeatedly. */
    fun suggestFrom(session: ScanSession) {
        val frontUri = session.front?.imageUri
        if (frontUri == null) {
            _suggestion.value = NameSuggestion.Ready(null)
            return
        }
        if (frontUri == processedUri) return
        processedUri = frontUri

        viewModelScope.launch {
            _suggestion.value = NameSuggestion.Loading
            val lines = ocr.recognize(frontUri)
            val name = NameParser.parse(session.cardType, lines)?.takeIf { it.isNotBlank() }
            _suggestion.value = NameSuggestion.Ready(name)
        }
    }

    override fun onCleared() {
        ocr.close()
    }
}
