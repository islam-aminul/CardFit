package `in`.firm.consultancy.bayaan.cardfit.ui

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.PaperSize
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelTest {

    private fun vm() = AppViewModel()

    @Test
    fun initialState_isEmpty() {
        val s = vm().state.value
        assertNull(s.session)
        assertTrue(s.selectedModes.isEmpty())
        assertEquals("", s.name)
    }

    @Test
    fun selectCardType_createsSession() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        val session = vm.state.value.session
        assertEquals(CardType.PAN, session?.cardType)
        assertNull(session?.front)
        assertNull(session?.back)
    }

    @Test
    fun customCardType_storesRuntimeDimensions() {
        val vm = vm()
        vm.selectCardType(CardType.CUSTOM, customWidthMm = 100.0, customHeightMm = 70.0)
        val session = vm.state.value.session
        assertEquals(100.0, session?.customWidthMm)
        assertEquals(70.0, session?.customHeightMm)
    }

    @Test
    fun setFrontAndBack_populateSession() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        vm.setFront(ScannedSide("uri://front"))
        vm.setBack(ScannedSide("uri://back"))
        val session = vm.state.value.session
        assertEquals("uri://front", session?.front?.imageUri)
        assertEquals("uri://back", session?.back?.imageUri)
    }

    @Test
    fun setFront_withoutSession_isNoOp() {
        val vm = vm()
        vm.setFront(ScannedSide("uri://front"))
        assertNull(vm.state.value.session)
    }

    @Test
    fun reselectingSameType_keepsSides_differentType_resets() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        vm.setFront(ScannedSide("uri://front"))

        vm.selectCardType(CardType.PAN) // same type -> keep
        assertEquals("uri://front", vm.state.value.session?.front?.imageUri)

        vm.selectCardType(CardType.EPIC) // different -> reset sides
        assertNull(vm.state.value.session?.front)
        assertEquals(CardType.EPIC, vm.state.value.session?.cardType)
    }

    @Test
    fun toggleMode_addsAndRemoves() {
        val vm = vm()
        vm.toggleMode(OutputMode.PRINT)
        assertTrue(OutputMode.PRINT in vm.state.value.selectedModes)
        vm.toggleMode(OutputMode.PRINT)
        assertTrue(vm.state.value.selectedModes.isEmpty())
    }

    @Test
    fun renderConfigs_encodesPrintAndUploadRules() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        vm.toggleMode(OutputMode.PRINT)
        vm.toggleMode(OutputMode.UPLOAD)
        vm.toggleFormat(OutputFormat.PDF) // no format is selected by default
        vm.setGrayscale(true)
        vm.setCropMarks(true)
        vm.setMaxFileSizeKb(200)

        val configs = vm.renderConfigs()
        assertEquals(2, configs.size)

        val print = configs.first { it.mode == OutputMode.PRINT }
        assertEquals(300, print.dpi)
        assertTrue(print.cropMarks)
        assertNull(print.maxFileSizeKb)
        assertTrue(print.grayscale)

        val upload = configs.first { it.mode == OutputMode.UPLOAD }
        assertEquals(200, upload.dpi)
        assertEquals(false, upload.cropMarks) // crop marks are print-only
        assertEquals(200, upload.maxFileSizeKb)
    }

    @Test
    fun multiSelect_producesCartesianProduct() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        vm.toggleMode(OutputMode.PRINT)
        vm.toggleMode(OutputMode.UPLOAD)
        vm.togglePaper(PaperSize.A5) // A4 (default) + A5
        vm.toggleFormat(OutputFormat.PDF)
        vm.toggleFormat(OutputFormat.JPEG) // PDF + JPEG
        // 2 modes x 2 papers x 2 formats = 8 files
        assertEquals(8, vm.renderConfigs().size)
    }

    @Test
    fun togglePaper_cappedAtMax() {
        val vm = vm() // default A4
        vm.togglePaper(PaperSize.A5)
        vm.togglePaper(PaperSize.LETTER) // ignored: already at max 2
        assertEquals(setOf(PaperSize.A4, PaperSize.A5), vm.state.value.selectedPapers)
        vm.togglePaper(PaperSize.A4) // remove one, frees a slot
        vm.togglePaper(PaperSize.LETTER)
        assertEquals(setOf(PaperSize.A5, PaperSize.LETTER), vm.state.value.selectedPapers)
    }

    @Test
    fun toggleFormat_addsAndRemoves() {
        val vm = vm() // no format selected by default
        vm.toggleFormat(OutputFormat.PDF)
        vm.toggleFormat(OutputFormat.JPEG)
        assertEquals(setOf(OutputFormat.PDF, OutputFormat.JPEG), vm.state.value.selectedFormats)
        vm.toggleFormat(OutputFormat.PDF)
        assertEquals(setOf(OutputFormat.JPEG), vm.state.value.selectedFormats)
    }

    @Test
    fun renderConfigs_emptyWhenNoModeSelected() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        assertTrue(vm.renderConfigs().isEmpty())
    }

    @Test
    fun applyNameSuggestion_replacesAutoFilled_clearsOnNone_keepsManual() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)

        // First scan detects a name -> fills the blank field.
        vm.applyNameSuggestion("John")
        assertEquals("John", vm.state.value.name)

        // Second scan detects nothing -> clears the stale auto-filled name (the bug fix).
        vm.applyNameSuggestion(null)
        assertEquals("", vm.state.value.name)

        // A manual edit is never overwritten by a later suggestion.
        vm.setName("Mike")
        vm.applyNameSuggestion("Alex")
        assertEquals("Mike", vm.state.value.name)
    }

    @Test
    fun reset_clearsEverything() {
        val vm = vm()
        vm.selectCardType(CardType.PAN)
        vm.setName("Sam")
        vm.toggleMode(OutputMode.PRINT)
        vm.reset()
        val s = vm.state.value
        assertNull(s.session)
        assertEquals("", s.name)
        assertTrue(s.selectedModes.isEmpty())
    }
}
