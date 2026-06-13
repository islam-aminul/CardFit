package `in`.firm.consultancy.bayaan.cardfit.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import `in`.firm.consultancy.bayaan.cardfit.data.JpegRenderer
import `in`.firm.consultancy.bayaan.cardfit.data.RenderedOutput
import `in`.firm.consultancy.bayaan.cardfit.domain.PageLayout
import `in`.firm.consultancy.bayaan.cardfit.domain.Units
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScanSession
import `in`.firm.consultancy.bayaan.cardfit.domain.targetJpegSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JPEG renderer (CLAUDE.md sections 6–8): composes the layout to a white-background bitmap at the
 * target pixel size, compresses to JPEG, and writes DPI density via ExifInterface.
 *
 * For uploads with a size cap, runs the full [targetJpegSize] search + DPI downscale-retry flow,
 * re-rendering the page bitmap only when the DPI changes (one page bitmap in memory at a time).
 */
class AndroidJpegRenderer(private val context: Context) : JpegRenderer {

    override suspend fun render(session: ScanSession, config: RenderConfig): RenderedOutput =
        withContext(Dispatchers.Default) {
            val front = decodeSampledBitmap(context, requireNotNull(session.front).imageUri)
                ?: error("Could not read the front image")
            val back = session.back?.let { decodeSampledBitmap(context, it.imageUri) }
            val sides = listOfNotNull(front, back)
            try {
                val layout = planLayout(session, config, sides)
                val paint = cardPaint(config.grayscale)
                val capBytes = config.maxFileSizeKb?.let { it * 1024 }

                val cornerMm = if (config.roundCorners) Units.ID1_CORNER_RADIUS_MM else 0.0
                if (capBytes == null) {
                    // No size cap (print, or upload without a cap): single high-quality pass.
                    val page = composePageBitmap(layout, sides, config.dpi, paint, cornerMm)
                    try {
                        val bytes = jpegBytes(page, PRINT_JPEG_QUALITY)
                        RenderedOutput(writeJpegDpi(context, bytes, config.dpi))
                    } finally {
                        page.recycle()
                    }
                } else {
                    renderWithCap(layout, sides, paint, config, capBytes)
                }
            } finally {
                front.recycle()
                back?.recycle()
            }
        }

    private fun renderWithCap(
        layout: PageLayout,
        sides: List<Bitmap>,
        paint: Paint,
        config: RenderConfig,
        capBytes: Int,
    ): RenderedOutput {
        var cachedDpi = -1
        var cached: Bitmap? = null
        val cornerMm = if (config.roundCorners) Units.ID1_CORNER_RADIUS_MM else 0.0

        fun pageAt(dpi: Int): Bitmap {
            if (dpi != cachedDpi || cached == null) {
                cached?.recycle()
                cached = composePageBitmap(layout, sides, dpi, paint, cornerMm)
                cachedDpi = dpi
            }
            return cached!!
        }

        try {
            val target = targetJpegSize(
                maxBytes = capBytes,
                startDpi = config.dpi,
                compress = { dpi, quality -> jpegByteCount(pageAt(dpi), quality) },
            )
            val finalPage = pageAt(target.dpi)
            val bytes = jpegBytes(finalPage, target.quality)
            val tagged = writeJpegDpi(context, bytes, target.dpi)
            val warning = if (target.belowFloor) {
                "Couldn't reach the ${config.maxFileSizeKb} KB cap; saved the smallest legible " +
                    "version (~${target.dpi} dpi)."
            } else {
                null
            }
            return RenderedOutput(tagged, warning)
        } finally {
            cached?.recycle()
        }
    }
}
