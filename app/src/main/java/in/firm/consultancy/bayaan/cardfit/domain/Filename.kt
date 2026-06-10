package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputFormat
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode

/**
 * Filename construction from CLAUDE.md section 9.
 *
 * Template: `{nameSlug}-{cardTypeSlug}-{paperSlug}-{purpose}-{yyMMdd}-{HHmm}.{ext}`
 * Examples: `aminul-islam-pan-a4-upload-260608-1430.jpeg`, `aminul-islam-pan-a5-print-260608-1430.pdf`.
 *
 * (The paper slug was added in Phase 13 so multi-paper exports produce distinct, readable names.)
 */

/**
 * The wall-clock instant a file is generated, decomposed into local components. Passed in so the
 * builder stays pure (no clock dependency). [year] is the full year (e.g. 2026).
 */
data class FileTimestamp(
    val year: Int,
    val month: Int, // 1..12
    val day: Int, // 1..31
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val second: Int, // 0..59
)

object FilenameBuilder {

    fun purposeOf(mode: OutputMode): String = when (mode) {
        OutputMode.PRINT -> "print"
        OutputMode.UPLOAD -> "upload"
    }

    fun extOf(format: OutputFormat): String = when (format) {
        OutputFormat.PDF -> "pdf"
        OutputFormat.JPEG -> "jpeg"
    }

    /**
     * Build a unique filename.
     *
     * @param name raw holder name; slugified internally (empty -> `document`).
     * @param exists predicate that reports whether a candidate filename already exists; used for
     *   collision suffixing. Defaults to "nothing exists".
     *
     * Collision strategy (section 9): if `base.ext` exists, append `-{ss}` (the seconds); if that
     * still exists, append an incrementing `-2`, `-3`, ... to the seconds-qualified name.
     */
    fun build(
        name: String,
        cardTypeSlug: String,
        paperSlug: String,
        mode: OutputMode,
        format: OutputFormat,
        timestamp: FileTimestamp,
        exists: (String) -> Boolean = { false },
    ): String {
        val nameSlug = Slug.slugify(name)
        val purpose = purposeOf(mode)
        val ext = extOf(format)

        val datePart = pad2(timestamp.year % 100) + pad2(timestamp.month) + pad2(timestamp.day)
        val timePart = pad2(timestamp.hour) + pad2(timestamp.minute)
        val base = "$nameSlug-$cardTypeSlug-$paperSlug-$purpose-$datePart-$timePart"

        val first = "$base.$ext"
        if (!exists(first)) return first

        val ss = pad2(timestamp.second)
        val withSeconds = "$base-$ss.$ext"
        if (!exists(withSeconds)) return withSeconds

        var n = 2
        while (true) {
            val candidate = "$base-$ss-$n.$ext"
            if (!exists(candidate)) return candidate
            n++
        }
    }

    /**
     * Build a unique filename for the Phase 13 photo flow.
     *
     * Template: `{nameSlug}-photo-{purpose}-{yyMMdd}-{HHmm}.{ext}` where purpose ∈ {print, upload}
     * and ext ∈ {pdf, jpeg}. The name is optional: an empty/blank name slugifies to `photo`
     * (e.g. `photo-photo-upload-260608-1430.jpeg`). Collision suffixing matches [build].
     */
    fun buildPhoto(
        name: String,
        mode: OutputMode,
        format: OutputFormat,
        timestamp: FileTimestamp,
        exists: (String) -> Boolean = { false },
    ): String {
        val nameSlug = Slug.slugify(name, emptyFallback = "photo")
        val purpose = purposeOf(mode)
        val ext = extOf(format)

        val datePart = pad2(timestamp.year % 100) + pad2(timestamp.month) + pad2(timestamp.day)
        val timePart = pad2(timestamp.hour) + pad2(timestamp.minute)
        val base = "$nameSlug-photo-$purpose-$datePart-$timePart"

        val first = "$base.$ext"
        if (!exists(first)) return first

        val ss = pad2(timestamp.second)
        val withSeconds = "$base-$ss.$ext"
        if (!exists(withSeconds)) return withSeconds

        var n = 2
        while (true) {
            val candidate = "$base-$ss-$n.$ext"
            if (!exists(candidate)) return candidate
            n++
        }
    }

    /**
     * Build a unique filename for a task INDIVIDUAL upload export (CLAUDE.md Phase 14, item 4).
     *
     * Template: `{personSlug}-{docSlug}-{taskSlug}-upload-{yyMMdd}-{HHmm}.{ext}`. The person name and
     * task name are slugified (empty person -> `document`). Collision suffixing matches [build].
     */
    fun buildTaskIndividual(
        personName: String,
        docSlug: String,
        taskName: String,
        format: OutputFormat,
        timestamp: FileTimestamp,
        exists: (String) -> Boolean = { false },
    ): String {
        val personSlug = Slug.slugify(personName)
        val taskSlug = Slug.slugify(taskName, emptyFallback = "task")
        val ext = extOf(format)
        val datePart = pad2(timestamp.year % 100) + pad2(timestamp.month) + pad2(timestamp.day)
        val timePart = pad2(timestamp.hour) + pad2(timestamp.minute)
        val base = "$personSlug-$docSlug-$taskSlug-upload-$datePart-$timePart"
        return uniquify(base, ext, timestamp, exists)
    }

    /**
     * Build a unique filename for a task COMBINED multi-page PDF (CLAUDE.md Phase 14, item 5).
     *
     * Template: `{taskSlug}-combined-{purpose}-{yyMMdd}-{HHmm}.pdf` (purpose ∈ {print, upload}).
     */
    fun buildTaskCombined(
        taskName: String,
        mode: OutputMode,
        timestamp: FileTimestamp,
        exists: (String) -> Boolean = { false },
    ): String {
        val taskSlug = Slug.slugify(taskName, emptyFallback = "task")
        val purpose = purposeOf(mode)
        val datePart = pad2(timestamp.year % 100) + pad2(timestamp.month) + pad2(timestamp.day)
        val timePart = pad2(timestamp.hour) + pad2(timestamp.minute)
        val base = "$taskSlug-combined-$purpose-$datePart-$timePart"
        return uniquify(base, "pdf", timestamp, exists)
    }

    /** Shared collision strategy: `base.ext`, then `-{ss}`, then `-{ss}-2`, `-3`, … */
    private fun uniquify(
        base: String,
        ext: String,
        timestamp: FileTimestamp,
        exists: (String) -> Boolean,
    ): String {
        val first = "$base.$ext"
        if (!exists(first)) return first
        val ss = pad2(timestamp.second)
        val withSeconds = "$base-$ss.$ext"
        if (!exists(withSeconds)) return withSeconds
        var n = 2
        while (true) {
            val candidate = "$base-$ss-$n.$ext"
            if (!exists(candidate)) return candidate
            n++
        }
    }

    private fun pad2(value: Int): String = if (value < 10) "0$value" else value.toString()
}
