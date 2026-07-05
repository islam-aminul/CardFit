package `in`.firm.consultancy.bayaan.cardfit.ui

import `in`.firm.consultancy.bayaan.cardfit.domain.CardExportSettings
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelDocumentTest {

    private fun vm() = AppViewModel()
    private fun page(uri: String, w: Int = 800, h: Int = 1200) = DocumentPage(ScannedSide(uri, w, h))

    @Test
    fun addDocumentPage_appendsInOrder() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("uri://1"))
        vm.addDocumentPage(page("uri://2"))
        val pages = vm.state.value.session?.documentPages.orEmpty()
        assertEquals(2, pages.size)
        assertEquals("uri://1", pages[0].side.imageUri)
        assertEquals("uri://2", pages[1].side.imageUri)
    }

    @Test
    fun replaceRemoveReorder_behave() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.addDocumentPage(page("b"))
        vm.addDocumentPage(page("c"))

        vm.replaceDocumentPage(1, page("b2"))
        assertEquals("b2", vm.state.value.session?.documentPages?.get(1)?.side?.imageUri)

        vm.reorderDocumentPage(0, 2) // a → end: [b2, c, a]
        assertEquals(listOf("b2", "c", "a"), vm.state.value.session?.documentPages?.map { it.side.imageUri })

        vm.removeDocumentPage(1) // remove c → [b2, a]
        assertEquals(listOf("b2", "a"), vm.state.value.session?.documentPages?.map { it.side.imageUri })

        // Out-of-range ops are no-ops.
        vm.removeDocumentPage(9)
        vm.replaceDocumentPage(9, page("x"))
        vm.reorderDocumentPage(0, 9)
        assertEquals(2, vm.state.value.session?.documentPages?.size)
    }

    @Test
    fun setPageWidthMm_setsReceiptWidth() {
        val vm = vm()
        vm.selectCardType(CardType.RECEIPT)
        vm.addDocumentPage(page("r1"))
        vm.setPageWidthMm(0, 57.0)
        assertEquals(57.0, vm.state.value.session?.documentPages?.get(0)?.widthMm)
    }

    @Test
    fun selectCardType_change_clearsDocumentPages_sameType_keeps() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        // Same type re-selected: pages preserved.
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        assertEquals(1, vm.state.value.session?.documentPages?.size)
        // Different type: cleared.
        vm.selectCardType(CardType.RECEIPT)
        assertTrue(vm.state.value.session?.documentPages.isNullOrEmpty())
    }

    @Test
    fun renderConfigs_omitsJpeg_whenMultiPage() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.addDocumentPage(page("b")) // 2 pages → multi-page (PDF auto-forced, JPEG blocked)
        vm.toggleMode(OutputMode.UPLOAD)
        vm.toggleFormat(OutputFormat.JPEG) // ignored — can't add JPEG to a multi-page document
        val formats = vm.renderConfigs().map { it.format }.toSet()
        assertEquals(setOf(OutputFormat.PDF), formats)
    }

    @Test
    fun addingSecondPage_stripsJpeg_andForcesPdf() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.toggleMode(OutputMode.UPLOAD)
        vm.toggleFormat(OutputFormat.JPEG) // JPEG selected while single-page
        assertEquals(setOf(OutputFormat.JPEG), vm.state.value.selectedFormats)

        vm.addDocumentPage(page("b")) // now multi-page
        assertEquals(setOf(OutputFormat.PDF), vm.state.value.selectedFormats)
    }

    @Test
    fun toggleFormat_jpeg_isNoOp_whenMultiPage() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.addDocumentPage(page("b"))
        vm.toggleFormat(OutputFormat.JPEG) // ignored
        assertTrue(OutputFormat.JPEG !in vm.state.value.selectedFormats)
    }

    @Test
    fun applyPersistedSettings_multiPage_dropsStoredJpeg_forcesPdf() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.addDocumentPage(page("b"))
        vm.applyPersistedSettings(
            CardType.FULL_PAGE_DOCUMENT,
            CardExportSettings(formats = setOf(OutputFormat.JPEG)),
        )
        assertEquals(setOf(OutputFormat.PDF), vm.state.value.selectedFormats)
    }

    @Test
    fun renderConfigs_keepsJpeg_whenSinglePage() {
        val vm = vm()
        vm.selectCardType(CardType.RECEIPT)
        vm.addDocumentPage(page("only"))
        vm.toggleMode(OutputMode.UPLOAD)
        vm.toggleFormat(OutputFormat.JPEG)
        assertEquals(1, vm.renderConfigs().size)
        assertEquals(OutputFormat.JPEG, vm.renderConfigs().first().format)
    }

    @Test
    fun reset_clearsDocumentPages() {
        val vm = vm()
        vm.selectCardType(CardType.FULL_PAGE_DOCUMENT)
        vm.addDocumentPage(page("a"))
        vm.reset()
        assertNull(vm.state.value.session)
    }
}
