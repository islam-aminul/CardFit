package `in`.firm.consultancy.bayaan.cardfit.ui

import androidx.lifecycle.ViewModel
import `in`.firm.consultancy.bayaan.cardfit.domain.CardClassifier
import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.Defaults
import `in`.firm.consultancy.bayaan.cardfit.domain.SizeOverride
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PageOrientation
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
    val selectedFormats: Set<OutputFormat> = emptySet(),
    val grayscale: Boolean = false,
    val cropMarks: Boolean = false,
    val roundCorners: Boolean = true, // PVC-card corner trim; set per card type on selection
    val maxFileSizeKb: Int? = null,
    val searchableText: Boolean = false, // PDF only (Phase 11); default OFF — privacy by default
    val sizeOverride: SizeOverride = SizeOverride.AUTOMATIC, // Phase 12 sizing override
    // Full-document (FULL_PAGE_DOCUMENT) page orientation; LANDSCAPE swaps the paper's width and height.
    val pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
    // CardType.RECEIPT size on the page: percent of the maximum content size, 25..100.
    val contentScalePercent: Int = 100,
    // The card type whose persisted settings have been applied this session (null = not yet seeded).
    // Guards the seed-once / save-after-seed handshake with the per-type settings store.
    val settingsSeededFor: CardType? = null,
    // Last receipt width (mm) the user chose this session, so the next receipt preselects it. The very
    // first receipt has no preselection.
    val lastReceiptWidthMm: Double? = null,
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
            val base = if (keepSides) {
                current
            } else {
                // Each type owns its render settings: switching types returns the draft to defaults,
                // so the new type's persisted settings (or true defaults) apply — nothing leaks from
                // the previous type. The global searchable flag and the name are kept.
                current.copy(
                    selectedModes = emptySet(),
                    selectedPapers = setOf(PaperSize.A4),
                    selectedFormats = emptySet(),
                    grayscale = false,
                    cropMarks = false,
                    maxFileSizeKb = null,
                    pageOrientation = PageOrientation.PORTRAIT,
                    contentScalePercent = 100,
                    settingsSeededFor = null,
                )
            }
            base.copy(
                session = ScanSession(
                    cardType = type,
                    front = if (keepSides) previous.front else null,
                    back = if (keepSides) previous.back else null,
                    customWidthMm = customWidthMm,
                    customHeightMm = customHeightMm,
                    // Keep captured document pages when re-selecting the same type; clear on a change.
                    documentPages = if (keepSides) previous.documentPages else emptyList(),
                ),
                // Reset the sizing override to this card type's default (Phase 12).
                sizeOverride = CardClassifier.defaultOverride(type),
                // Default corner-trim ON for rounded PVC cards (PAN/Aadhaar/EPIC), off otherwise.
                roundCorners = type.roundedByDefault,
            )
        }
    }

    /**
     * Apply this card type's persisted settings ([settings] null = nothing stored yet) and mark the
     * type as seeded. Idempotent: a repeat call for an already-seeded type is a no-op, so a screen
     * revisit never clobbers in-session edits. The session's custom size is only filled when absent —
     * a value the user just typed in the size dialog always wins over the remembered one.
     */
    fun applyPersistedSettings(type: CardType, settings: CardExportSettings?) {
        _state.update { current ->
            if (current.settingsSeededFor == type) return@update current
            if (settings == null) return@update current.copy(settingsSeededFor = type)
            current.copy(
                selectedModes = settings.modes,
                selectedPapers = settings.papers.ifEmpty { setOf(PaperSize.A4) },
                selectedFormats = settings.formats,
                grayscale = settings.grayscale,
                cropMarks = settings.cropMarks,
                roundCorners = settings.roundCorners ?: type.roundedByDefault,
                maxFileSizeKb = settings.maxFileSizeKb,
                sizeOverride = settings.sizeOverride,
                pageOrientation = settings.pageOrientation,
                contentScalePercent = settings.contentScalePercent,
                session = current.session?.let { s ->
                    if (s.customWidthMm == null && settings.customWidthMm != null) {
                        s.copy(
                            customWidthMm = settings.customWidthMm,
                            customHeightMm = settings.customHeightMm,
                        )
                    } else {
                        s
                    }
                },
                settingsSeededFor = type,
            )
        }
    }

    fun setFront(side: ScannedSide?) = updateSession { it.copy(front = side) }

    fun setBack(side: ScannedSide?) = updateSession { it.copy(back = side) }

    // --- Standalone multi-page document pages (FULL_PAGE_DOCUMENT / RECEIPT) ---
    // All synchronous; only the new document flow touches these. Page 1 is mirrored into [front] by
    // the capture screen so OCR name-suggestion and the preview thumbnail keep working via [front].

    /** Append a captured page to the current document. */
    fun addDocumentPage(page: DocumentPage) =
        updateSession { it.copy(documentPages = it.documentPages + page) }

    /** Replace the page at [index] (e.g. after editing it); no-op if out of range. */
    fun replaceDocumentPage(index: Int, page: DocumentPage) = updateSession { s ->
        if (index !in s.documentPages.indices) s
        else s.copy(documentPages = s.documentPages.toMutableList().also { it[index] = page })
    }

    /** Remove the page at [index]; no-op if out of range. */
    fun removeDocumentPage(index: Int) = updateSession { s ->
        if (index !in s.documentPages.indices) s
        else s.copy(documentPages = s.documentPages.filterIndexed { i, _ -> i != index })
    }

    /** Move a page from [from] to [to]; no-op if either index is out of range. */
    fun reorderDocumentPage(from: Int, to: Int) = updateSession { s ->
        val list = s.documentPages.toMutableList()
        if (from !in list.indices || to !in list.indices) return@updateSession s
        list.add(to, list.removeAt(from))
        s.copy(documentPages = list)
    }

    /** Set the chosen real-world width (mm) for the receipt page at [index]; no-op if out of range.
     *  Also records it as [AppState.lastReceiptWidthMm] so the next receipt preselects the same width. */
    fun setPageWidthMm(index: Int, widthMm: Double) {
        _state.update { current ->
            val session = current.session ?: return@update current
            if (index !in session.documentPages.indices) return@update current
            val pages = session.documentPages.toMutableList()
                .also { it[index] = it[index].copy(widthMm = widthMm) }
            current.copy(session = session.copy(documentPages = pages), lastReceiptWidthMm = widthMm)
        }
    }

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
    fun setRoundCorners(value: Boolean) = _state.update { it.copy(roundCorners = value) }
    fun setMaxFileSizeKb(value: Int?) = _state.update { it.copy(maxFileSizeKb = value) }
    fun setSearchableText(value: Boolean) = _state.update { it.copy(searchableText = value) }
    fun setSizeOverride(value: SizeOverride) = _state.update { it.copy(sizeOverride = value) }
    fun setPageOrientation(value: PageOrientation) = _state.update { it.copy(pageOrientation = value) }
    fun setContentScalePercent(value: Int) =
        _state.update { it.copy(contentScalePercent = value.coerceIn(25, 100)) }

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
     * max file size to upload only; the searchable text layer to PDF only. A multi-page document can
     * only export as a single multi-page PDF, so JPEG is skipped when there is more than one page
     * (the Configure UI also disables the JPEG tile — this is the defensive backstop).
     */
    fun renderConfigs(): List<RenderConfig> {
        val s = _state.value
        val multiPageDocument = (s.session?.documentPages?.size ?: 0) > 1
        return buildList {
            for (mode in s.selectedModes) {
                for (paper in s.selectedPapers) {
                    for (format in s.selectedFormats) {
                        if (multiPageDocument && format == OutputFormat.JPEG) continue
                        add(
                            RenderConfig(
                                mode = mode,
                                paper = paper,
                                format = format,
                                dpi = if (mode == OutputMode.PRINT) Defaults.PRINT_DPI else Defaults.UPLOAD_DPI,
                                grayscale = s.grayscale,
                                cropMarks = mode == OutputMode.PRINT && s.cropMarks,
                                roundCorners = s.roundCorners,
                                maxFileSizeKb = if (mode == OutputMode.UPLOAD) s.maxFileSizeKb else null,
                                searchableText = s.searchableText && format == OutputFormat.PDF,
                                sizeOverride = s.sizeOverride,
                                pageOrientation = s.pageOrientation,
                                contentScalePercent = s.contentScalePercent,
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

/**
 * Snapshot of the render-settings draft as this type's persistable blob. The global searchable-PDF
 * flag is deliberately excluded (it stays an app-wide preference, not a per-type one).
 */
fun AppState.toCardExportSettings(): CardExportSettings = CardExportSettings(
    modes = selectedModes,
    papers = selectedPapers,
    formats = selectedFormats,
    grayscale = grayscale,
    cropMarks = cropMarks,
    roundCorners = roundCorners,
    maxFileSizeKb = maxFileSizeKb,
    sizeOverride = sizeOverride,
    pageOrientation = pageOrientation,
    contentScalePercent = contentScalePercent,
    customWidthMm = session?.customWidthMm,
    customHeightMm = session?.customHeightMm,
)
