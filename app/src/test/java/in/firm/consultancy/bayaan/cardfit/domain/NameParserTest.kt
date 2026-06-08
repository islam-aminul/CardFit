package `in`.firm.consultancy.bayaan.cardfit.domain

import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameParserTest {

    // ---------- PAN ----------

    @Test
    fun pan_nameAfterLabel_aboveFather() {
        val lines = listOf(
            "INCOME TAX DEPARTMENT",
            "GOVT. OF INDIA",
            "Permanent Account Number",
            "ABCDE1234F",
            "Name",
            "JOHN MICHAEL DOE",
            "Father's Name",
            "RICHARD DOE",
            "Date of Birth",
            "01/01/1990",
        )
        assertEquals("JOHN MICHAEL DOE", NameParser.parse(CardType.PAN, lines))
    }

    @Test
    fun pan_inlineNameAfterColon() {
        val lines = listOf("Name: JOHN DOE", "Father's Name: RICHARD DOE")
        assertEquals("JOHN DOE", NameParser.parse(CardType.PAN, lines))
    }

    @Test
    fun pan_noNameLabel_returnsNull() {
        val lines = listOf("INCOME TAX DEPARTMENT", "JOHN DOE")
        assertNull(NameParser.parse(CardType.PAN, lines))
    }

    @Test
    fun pan_neverReturnsDigitBearingLine() {
        val lines = listOf("Name", "JOHN123", "Father's Name", "RICHARD")
        // "JOHN123" has a digit -> rejected; next candidate "Father's Name" is a label -> rejected.
        assertNull(NameParser.parse(CardType.PAN, lines))
    }

    // ---------- Aadhaar ----------

    @Test
    fun aadhaar_nameAboveDob() {
        val lines = listOf(
            "Government of India",
            "JOHN MICHAEL DOE",
            "DOB: 01/01/1990",
            "MALE",
            "1234 5678 9012",
        )
        assertEquals("JOHN MICHAEL DOE", NameParser.parse(CardType.AADHAAR, lines))
    }

    @Test
    fun aadhaar_yearOfBirthVariant() {
        val lines = listOf("Govt of India", "JANE DOE", "Year of Birth: 1985", "Female", "9876 5432 1098")
        assertEquals("JANE DOE", NameParser.parse(CardType.AADHAAR, lines))
    }

    @Test
    fun aadhaar_idNumberAboveDob_isNotReturned() {
        // If the only line above DOB is the number, the parser must not return it.
        val lines = listOf("1234 5678 9012", "DOB 01/01/1990")
        assertNull(NameParser.parse(CardType.AADHAAR, lines))
    }

    @Test
    fun aadhaar_noDobLine_returnsNull() {
        val lines = listOf("JOHN DOE", "MALE")
        assertNull(NameParser.parse(CardType.AADHAAR, lines))
    }

    // ---------- EPIC ----------

    @Test
    fun epic_electorsNameLabel() {
        val lines = listOf(
            "ELECTION COMMISSION OF INDIA",
            "Elector's Name",
            "JOHN DOE",
            "Father's Name",
            "RICHARD",
        )
        assertEquals("JOHN DOE", NameParser.parse(CardType.EPIC, lines))
    }

    @Test
    fun epic_inlineElectorsName() {
        val lines = listOf("Elector's Name: JOHN DOE")
        assertEquals("JOHN DOE", NameParser.parse(CardType.EPIC, lines))
    }

    @Test
    fun epic_plainNameLabel() {
        val lines = listOf("Name", "JOHN DOE")
        assertEquals("JOHN DOE", NameParser.parse(CardType.EPIC, lines))
    }

    // ---------- card types without a parser ----------

    @Test
    fun admitCustomFree_returnNull() {
        val lines = listOf("Name", "JOHN DOE")
        assertNull(NameParser.parse(CardType.ADMIT_CARD, lines))
        assertNull(NameParser.parse(CardType.CUSTOM, lines))
        assertNull(NameParser.parse(CardType.FREE, lines))
    }

    @Test
    fun emptyLines_returnNull() {
        assertNull(NameParser.parse(CardType.PAN, emptyList()))
        assertNull(NameParser.parse(CardType.AADHAAR, listOf("", "   ")))
    }
}
