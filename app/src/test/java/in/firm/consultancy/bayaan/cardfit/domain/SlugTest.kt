package `in`.firm.consultancy.bayaan.cardfit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SlugTest {

    @Test
    fun simpleName_lowercasedAndHyphenated() {
        assertEquals("aminul-islam", Slug.slugify("Aminul Islam"))
    }

    @Test
    fun diacritics_areDropped() {
        assertEquals("aminul", Slug.slugify("Ámïnúl"))
        assertEquals("munoz", Slug.slugify("Muñoz"))
        assertEquals("francois", Slug.slugify("François"))
    }

    @Test
    fun runsOfNonAlnum_collapseToSingleHyphen() {
        assertEquals("pan-card-1", Slug.slugify("PAN_Card   #1"))
        assertEquals("a-b", Slug.slugify("a___---   b"))
    }

    @Test
    fun leadingAndTrailingSeparators_areTrimmed() {
        assertEquals("aminul", Slug.slugify("  --Aminul!!  "))
        assertEquals("name", Slug.slugify("...name..."))
    }

    @Test
    fun emptyOrSymbolOnly_becomesDocument() {
        assertEquals("document", Slug.slugify(""))
        assertEquals("document", Slug.slugify("   "))
        assertEquals("document", Slug.slugify("!!!"))
        assertEquals("document", Slug.slugify("----"))
    }

    @Test
    fun digitsArePreserved() {
        assertEquals("john-2024", Slug.slugify("John 2024"))
    }

    @Test
    fun alreadySlug_isStable() {
        assertEquals("aminul-islam", Slug.slugify("aminul-islam"))
    }
}
