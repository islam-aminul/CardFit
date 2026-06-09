package `in`.firm.consultancy.bayaan.cardfit.ui

import androidx.lifecycle.ViewModel
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the whole capture -> configure -> export flow.
 *
 * The [session] is the single source of truth for captured sides (CLAUDE.md section 4) and is kept
 * independent of the render settings, so the user can re-export to another mode without re-scanning.
 */
data class AppState(
    val session: ScanSession? = null,
    // Render-settings draft (independent of [session]):
    val selectedModes: Set<OutputMode> = emptySet(),
    val paper: PaperSize = PaperSize.A4,
    val format: OutputFormat = OutputFormat.PDF,
    val grayscale: Boolean = false,
    val cropMarks: Boolean = false,
    val maxFileSizeKb: Int? = null,
    val searchableText: Boolean = false, // PDF only (Phase 11)
    val name: String = "",
)

/**
 * Activity-scoped ViewModel holding the [ScanSession] and render-settings draft across navigation
 * and configuration changes. State mutation is synchronous (no coroutines), keeping it trivially
 * JVM-testable. Persisting captured files across process death (section 12) comes with the real
 * scanner integration.
 */
class AppViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun selectCardType(
        type: CardType,
        customWidthMm: Double? = null,
        customHeightMm: Double? = null,
    ) {
        _state.update { current ->
            val previous = current.session
            // Preserve already-captured sides only when the card type is unchanged.
            val keepSides = previous != null && previous.cardType == type
            current.copy(
                session = ScanSession(
                    cardType = type,
                    front = if (keepSides) previous.front else null,
                    back = if (keepSides) previous.back else null,
                    customWidthMm = customWidthMm,
                    customHeightMm = customHeightMm,
                ),
            )
        }
    }

    fun setFront(side: ScannedSide?) = updateSession { it.copy(front = side) }

    fun setBack(side: ScannedSide?) = updateSession { it.copy(back = side) }

    /** Set the selected output modes directly (used by the Print / Upload / Both purpose tiles). */
    fun setModes(modes: Set<OutputMode>) = _state.update { it.copy(selectedModes = modes) }

    fun toggleMode(mode: OutputMode) {
        _state.update { current ->
            val modes = if (mode in current.selectedModes) {
                current.selectedModes - mode
            } else {
                current.selectedModes + mode
            }
            current.copy(selectedModes = modes)
        }
    }

    fun setPaper(paper: PaperSize) = _state.update { it.copy(paper = paper) }
    fun setFormat(format: OutputFormat) = _state.update { it.copy(format = format) }
    fun setGrayscale(value: Boolean) = _state.update { it.copy(grayscale = value) }
    fun setCropMarks(value: Boolean) = _state.update { it.copy(cropMarks = value) }
    fun setMaxFileSizeKb(value: Int?) = _state.update { it.copy(maxFileSizeKb = value) }
    fun setSearchableText(value: Boolean) = _state.update { it.copy(searchableText = value) }
    fun setName(name: String) = _state.update { it.copy(name = name) }

    fun reset() = _state.update { AppState() }

    /**
     * Build one [RenderConfig] per selected output mode. Encodes the spec rules: default DPI is 300
     * for print and 200 for upload; crop marks apply to print only; max file size to upload only.
     */
    fun renderConfigs(): List<RenderConfig> {
        val s = _state.value
        return s.selectedModes.map { mode ->
            RenderConfig(
                mode = mode,
                paper = s.paper,
                format = s.format,
                dpi = if (mode == OutputMode.PRINT) Defaults.PRINT_DPI else Defaults.UPLOAD_DPI,
                grayscale = s.grayscale,
                cropMarks = mode == OutputMode.PRINT && s.cropMarks,
                maxFileSizeKb = if (mode == OutputMode.UPLOAD) s.maxFileSizeKb else null,
                // Text layer is a PDF-only feature.
                searchableText = s.searchableText && s.format == OutputFormat.PDF,
            )
        }
    }

    private inline fun updateSession(crossinline transform: (ScanSession) -> ScanSession) {
        _state.update { current ->
            val session = current.session ?: return@update current
            current.copy(session = transform(session))
        }
    }
}
