package `in`.firm.consultancy.bayaan.cardfit.domain

import java.text.Normalizer

/**
 * Name slugification from CLAUDE.md section 9.
 *
 * Uses only `java.text.Normalizer` (JVM/standard library, not an Android API), so this stays
 * unit-testable on the plain JVM.
 */
object Slug {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val NON_ALNUM_RUN = Regex("[^a-z0-9]+")

    /**
     * lowercase -> NFD normalize -> drop diacritics -> collapse runs of non `[a-z0-9]` to a single
     * `-` -> trim leading/trailing `-`. An empty result becomes [emptyFallback] (default `document`;
     * the photo flow passes `photo`).
     */
    fun slugify(input: String, emptyFallback: String = "document"): String {
        val lowered = input.lowercase()
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val withoutDiacritics = COMBINING_MARKS.replace(decomposed, "")
        val hyphenated = NON_ALNUM_RUN.replace(withoutDiacritics, "-")
        val trimmed = hyphenated.trim('-')
        return trimmed.ifEmpty { emptyFallback }
    }
}
