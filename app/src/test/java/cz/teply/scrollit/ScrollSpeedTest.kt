package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollSpeedTest {
    @Test
    fun clamp_limitsLevelInsideOneToTen() {
        assertEquals(1, ScrollSpeed.clamp(-2))
        assertEquals(10, ScrollSpeed.clamp(42))
    }

    @Test
    fun stepUpAndDown_keepBoundaries() {
        assertEquals(10, ScrollSpeed.stepUp(10))
        assertEquals(1, ScrollSpeed.stepDown(1))
        assertEquals(5, ScrollSpeed.stepUp(4))
        assertEquals(4, ScrollSpeed.stepDown(5))
    }

    @Test
    fun levelOne_isMuchSlowerAndGentlerThanLevelTen() {
        assertTrue(ScrollSpeed.intervalFactor(1) > ScrollSpeed.intervalFactor(10))
        assertTrue(ScrollSpeed.durationFactor(1) > ScrollSpeed.durationFactor(10))
        assertTrue(ScrollSpeed.distanceFactor(1) < ScrollSpeed.distanceFactor(10))
        assertEquals(3, ScrollSpeed.DEFAULT_LEVEL)
    }
}
