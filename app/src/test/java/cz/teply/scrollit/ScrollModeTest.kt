package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollModeTest {
    @Test
    fun unknownStoredValue_fallsBackToTouchMode() {
        assertEquals(ScrollMode.TOUCH, ScrollMode.fromStoredValue("unknown"))
    }

    @Test
    fun storedValues_roundTripWithoutDependingOnEnumNames() {
        ScrollMode.entries.forEach { mode ->
            assertEquals(mode, ScrollMode.fromStoredValue(mode.storedValue))
        }
    }

    @Test
    fun settingsDefault_keepsAccessibilityAsSimpleMode() {
        assertEquals(ScrollMode.TOUCH, ScrollSettings.defaults.mode)
    }
}
