package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType

/**
 * Card-type-specific holder-name heuristics (CLAUDE.md section 10). Pure functions over OCR text
 * lines, so they are JVM-testable and carry no Android dependency.
 *
 * SAFETY: a candidate is rejected outright if it contains ANY digit, so identity numbers (which are
 * digit-bearing) can never be returned or stored. The result is only ever a *suggestion* for an
 * editable field — nothing here finalizes a filename.
 */
object NameParser {

    /** Document/label words that mark a line as NOT a holder name (matched as whole words). */
    private val WORD_BLOCKLIST = setOf(
        "income", "tax", "department", "govt", "government", "india", "republic",
        "permanent", "account", "number", "signature", "photo", "address",
        "name", "father", "mother", "husband", "wife", "elector", "voter",
        "date", "birth", "dob", "year", "sex", "gender", "male", "female",
        "nationality", "unique", "identification", "identity", "authority",
        "election", "commission", "card", "of",
    )

    private val DOB_DATE = Regex("""\d{2}[/\-.]\d{2}[/\-.]\d{4}""")
    private val NON_LETTER = Regex("""[^\p{L}]+""")

    fun parse(cardType: CardType, lines: List<String>): String? {
        val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
        return when (cardType) {
            CardType.PAN -> valueAfterLabel(cleaned) { lower ->
                (lower.contains("name") && !lower.contains("father")) || lower.contains("नाम")
            }
            CardType.EPIC ->
                // Prefer the specific "Elector's Name" label so the card's title
                // ("ELECTOR PHOTO IDENTITY CARD" — has "elector" but no "name") is never matched;
                // fall back to a plain "Name" label.
                valueAfterLabel(cleaned) { lower -> lower.contains("elector") && lower.contains("name") }
                    ?: valueAfterLabel(cleaned) { lower ->
                        lower.contains("name") && !lower.contains("father")
                    }
            CardType.AADHAAR -> valueBeforeDob(cleaned)
            CardType.ADMIT_CARD, CardType.CUSTOM, CardType.FREE -> null
        }
    }

    /**
     * Value on the same line after a ':' on the label line, else the next name-like line. Digit
     * noise between the label and the value is skipped, but the scan stops at the next field's label
     * so a "Name" label never bleeds into the following "Father's Name" value.
     */
    private fun valueAfterLabel(lines: List<String>, isLabel: (lowerLine: String) -> Boolean): String? {
        for (i in lines.indices) {
            if (!isLabel(lines[i].lowercase())) continue
            cleanName(lines[i].substringAfter(':', ""))?.let { return it }
            for (j in i + 1 until lines.size) {
                cleanName(lines[j])?.let { return it }
                if (isLabelLine(lines[j])) break
            }
        }
        return null
    }

    /** A line that reads like a document field label rather than a value. */
    private fun isLabelLine(line: String): Boolean {
        val words = line.lowercase().split(NON_LETTER).filter { it.isNotEmpty() }
        return words.any { it in WORD_BLOCKLIST }
    }

    /** The nearest name-like line above the first date-of-birth line (Aadhaar layout). */
    private fun valueBeforeDob(lines: List<String>): String? {
        for (i in lines.indices) {
            if (!isDobLine(lines[i])) continue
            for (j in i - 1 downTo 0) {
                cleanName(lines[j])?.let { return it }
            }
        }
        return null
    }

    private fun isDobLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("dob") ||
            lower.contains("birth") ||
            lower.contains("जन्म") ||
            DOB_DATE.containsMatchIn(line)
    }

    /** Trim label punctuation and return the value if it looks like a holder name, else null. */
    private fun cleanName(raw: String): String? {
        val s = raw.trim().trim(':', '-', '.', ',', '/', '|').trim()
        return if (isLikelyName(s)) s else null
    }

    private fun isLikelyName(s: String): Boolean {
        if (s.isBlank() || s.length > 50) return false
        if (s.any { it.isDigit() }) return false // never surface ID numbers
        if (s.count { it.isLetter() } < 2) return false
        val words = s.lowercase().split(NON_LETTER).filter { it.isNotEmpty() }
        if (words.isEmpty()) return false
        return words.none { it in WORD_BLOCKLIST }
    }
}
