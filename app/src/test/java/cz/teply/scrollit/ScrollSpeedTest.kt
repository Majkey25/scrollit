package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollSpeedTest {
    @Test
    fun fromProgress_clampsBelowRangeToVerySlow() {
        assertEquals(ScrollSpeed.VERY_SLOW, ScrollSpeed.fromProgress(-1))
    }

    @Test
    fun fromProgress_returnsMediumForMiddleValue() {
        assertEquals(ScrollSpeed.MEDIUM, ScrollSpeed.fromProgress(2))
    }

    @Test
    fun fromProgress_clampsAboveRangeToFast() {
        assertEquals(ScrollSpeed.FAST, ScrollSpeed.fromProgress(9))
    }
}
