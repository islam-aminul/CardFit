package `in`.firm.consultancy.bayaan.cardfit.data.export

import `in`.firm.consultancy.bayaan.cardfit.data.FileSaver
import `in`.firm.consultancy.bayaan.cardfit.data.JpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.MimeTypes
import `in`.firm.consultancy.bayaan.cardfit.data.PdfRenderer
import `in`.firm.consultancy.bayaan.cardfit.domain.FileTimestamp
import `in`.firm.consultancy.bayaan.cardfit.domain.FilenameBuilder
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One saved file in an export. [detail] is an optional human note (e.g. the photo count for a print grid). */
data class ExportedFile(
    val fileName: String,
    val savedLocation: String?,
    val warning: String?,
    val detail: String? = null,
)

/** One file prepared for sharing (FileProvider content-URI string + MIME type). */
data class ShareItem(val uri: String, val mimeType: String)

/**
 * Orchestrates export from a single [ScanSession] (CLAUDE.md sections 8–9, 11.5): renders one file
 * per selected [RenderConfig], names it with the section-9 template (collision-safe via the saver),
 * and saves or share-caches it. Pure of Android types (renderers/saver/clock are injected), so it is
 * unit-testable with fakes; the same [ScanSession] can be re-exported to other modes without
 * re-scanning by calling [export] again with different configs.
 */
class Exporter(
    private val pdfRenderer: PdfRenderer,
    private val jpegRenderer: JpegRenderer,
    private val fileSaver: FileSaver,
    private val clock: () -> FileTimestamp,
) {
    suspend fun export(
        session: ScanSession,
        name: String,
        configs: List<RenderConfig>,
    ): List<ExportedFile> = withContext(Dispatchers.Default) {
        configs.map { config ->
            val output = rendererFor(config).render(session, config)
            val mimeType = MimeTypes.forFormat(config.format)
            val fileName = FilenameBuilder.build(
                name = name,
                cardTypeSlug = session.cardType.slug,
                paperSlug = config.paper.name.lowercase(),
                mode = config.mode,
                format = config.format,
                timestamp = clock(),
                exists = fileSaver::exists,
            )
            val location = fileSaver.save(fileName, mimeType, output.bytes)
            ExportedFile(fileName, location, output.sizeWarning)
        }
    }

    suspend fun prepareShare(
        session: ScanSession,
        name: String,
        configs: List<RenderConfig>,
    ): List<ShareItem> = withContext(Dispatchers.Default) {
        configs.map { config ->
            val output = rendererFor(config).render(session, config)
            val mimeType = MimeTypes.forFormat(config.format)
            val fileName = FilenameBuilder.build(
                name = name,
                cardTypeSlug = session.cardType.slug,
                paperSlug = config.paper.name.lowercase(),
                mode = config.mode,
                format = config.format,
                timestamp = clock(),
            )
            ShareItem(fileSaver.cacheForShare(fileName, mimeType, output.bytes), mimeType)
        }
    }

    /** Render a lightweight JPEG preview of one config for on-screen display. */
    suspend fun preview(session: ScanSession, config: RenderConfig): ByteArray =
        withContext(Dispatchers.Default) {
            val previewConfig = config.copy(
                format = OutputFormat.JPEG,
                dpi = PREVIEW_DPI,
                maxFileSizeKb = null,
            )
            jpegRenderer.render(session, previewConfig).bytes
        }

    private fun rendererFor(config: RenderConfig) =
        if (config.format == OutputFormat.PDF) pdfRenderer else jpegRenderer

    private companion object {
        const val PREVIEW_DPI = 110
    }
}
