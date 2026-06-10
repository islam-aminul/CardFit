package `in`.firm.consultancy.bayaan.cardfit.ui

import androidx.lifecycle.ViewModel
import `in`.firm.consultancy.bayaan.cardfit.domain.CardClassifier
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
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
    // Render-settings draft (independent of [session]). Purpose, paper, and format are all
    // multi-select; an export produces every combination (mode x paper x format).
    val selectedModes: Set<OutputMode> = emptySet(),
    val selectedPapers: Set<PaperSize> = setOf(PaperSize.A4),
    val selectedFormats: Set<OutputFormat> = setOf(OutputFormat.PDF),
    val grayscale: Boolean = false,
    val cropMarks: Boolean = false,
    val maxFileSizeKb: Int? = null,
    val searchableText: Boolean = true, // PDF only (Phase 11); default ON per product decision
    val sizeOverride: SizeOverride = SizeOverride.AUTOMATIC, // Phase 12 sizing override
    val name: String = "",
    // True when [name] came from an OCR suggestion (not a manual edit), so a new scan may replace it.
    val nameAutoFilled: Boolean = false,
) {
    /** Whether at least one of each axis is chosen (export is possible). */
    val hasCompleteSelection: Boolean
        get() = selectedModes.isNotEmpty() && selectedPapers.isNotEmpty() && selectedFormats.isNotEmpty()
}

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
                // Reset the sizing override to this card type's default (Phase 12).
                sizeOverride = CardClassifier.defaultOverride(type),
            )
        }
    }

    fun setFront(side: ScannedSide?) = updateSession { it.copy(front = side) }

    fun setBack(side: ScannedSide?) = updateSession { it.copy(back = side) }

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

    /** Toggle a paper size. Capped at [MAX_PAPERS]: tapping an unselected size when full is a no-op. */
    fun togglePaper(paper: PaperSize) {
        _state.update { current ->
            val papers = when {
                paper in current.selectedPapers -> current.selectedPapers - paper
                current.selectedPapers.size < MAX_PAPERS -> current.selectedPapers + paper
                else -> current.selectedPapers
            }
            current.copy(selectedPapers = papers)
        }
    }

    fun toggleFormat(format: OutputFormat) {
        _state.update { current ->
            val formats = if (format in current.selectedFormats) {
                current.selectedFormats - format
            } else {
                current.selectedFormats + format
            }
            current.copy(selectedFormats = formats)
        }
    }

    fun setGrayscale(value: Boolean) = _state.update { it.copy(grayscale = value) }
    fun setCropMarks(value: Boolean) = _state.update { it.copy(cropMarks = value) }
    fun setMaxFileSizeKb(value: Int?) = _state.update { it.copy(maxFileSizeKb = value) }
    fun setSearchableText(value: Boolean) = _state.update { it.copy(searchableText = value) }
    fun setSizeOverride(value: SizeOverride) = _state.update { it.copy(sizeOverride = value) }

    /** Set custom card dimensions (mm) without changing the card type (Phase 12 Custom override). */
    fun setCustomSize(widthMm: Double, heightMm: Double) =
        updateSession { it.copy(customWidthMm = widthMm, customHeightMm = heightMm) }

    /** A manual edit to the name field; clears the auto-filled flag so OCR won't overwrite it. */
    fun setName(name: String) = _state.update { it.copy(name = name, nameAutoFilled = false) }

    /**
     * Apply an OCR name suggestion ([suggestion] is null when nothing was detected). Only replaces
     * the field when it's blank or still holds a previous auto-filled value — never overwrites text
     * the user typed. A new scan that detects nothing therefore clears a stale auto-filled name.
     */
    fun applyNameSuggestion(suggestion: String?) {
        _state.update { s ->
            if (s.nameAutoFilled || s.name.isBlank()) {
                s.copy(name = suggestion.orEmpty(), nameAutoFilled = suggestion != null)
            } else {
                s
            }
        }
    }

    fun reset() = _state.update { AppState() }

    /**
     * Build one [RenderConfig] for every combination of selected mode x paper x format. Encodes the
     * spec rules: default DPI is 300 for print and 200 for upload; crop marks apply to print only;
     * max file size to upload only; the searchable text layer to PDF only.
     */
    fun renderConfigs(): List<RenderConfig> {
        val s = _state.value
        return buildList {
            for (mode in s.selectedModes) {
                for (paper in s.selectedPapers) {
                    for (format in s.selectedFormats) {
                        add(
                            RenderConfig(
                                mode = mode,
                                paper = paper,
                                format = format,
                                dpi = if (mode == OutputMode.PRINT) Defaults.PRINT_DPI else Defaults.UPLOAD_DPI,
                                grayscale = s.grayscale,
                                cropMarks = mode == OutputMode.PRINT && s.cropMarks,
                                maxFileSizeKb = if (mode == OutputMode.UPLOAD) s.maxFileSizeKb else null,
                                searchableText = s.searchableText && format == OutputFormat.PDF,
                                sizeOverride = s.sizeOverride,
                            ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val MAX_PAPERS = 2
    }

    private inline fun updateSession(crossinline transform: (ScanSession) -> ScanSession) {
        _state.update { current ->
            val session = current.session ?: return@update current
            current.copy(session = transform(session))
        }
    }
}
