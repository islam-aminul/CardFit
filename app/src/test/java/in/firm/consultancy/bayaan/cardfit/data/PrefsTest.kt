package `in`.firm.consultancy.bayaan.cardfit.data

import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract-level cover for the shared preferences. `AndroidPrefs` itself needs a Context, so these
 * pin the [UserPrefs] shape and the update semantics the real store must honour — notably that an
 * update touching one field leaves the others intact (the bug the old single-key `update` had).
 */
class PrefsTest {

    @Test
    fun unit_defaultsToCm() {
        assertEquals(DimensionUnit.CM, UserPrefs().unit)
    }

    @Test
    fun update_persistsUnit() = runTest {
        val prefs = FakePrefs()
        prefs.update { it.copy(unit = DimensionUnit.INCH) }
        assertEquals(DimensionUnit.INCH, prefs.current().unit)
    }

    @Test
    fun update_ofOneField_preservesTheOthers() = runTest {
        val prefs = FakePrefs(UserPrefs(searchableText = true, unit = DimensionUnit.INCH))

        prefs.update { it.copy(searchableText = false) }
        assertEquals(DimensionUnit.INCH, prefs.current().unit) // not reset by an unrelated write

        prefs.update { it.copy(unit = DimensionUnit.CM) }
        assertEquals(false, prefs.current().searchableText)
        assertEquals(DimensionUnit.CM, prefs.current().unit)
    }
}
