package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollSpeedTest {
    @Test
    fun clamp_limitsLevelInsideOneToThirty() {
        assertEquals(1, ScrollSpeed.clamp(-2))
        assertEquals(30, ScrollSpeed.clamp(42))
    }

    @Test
    fun stepUpAndDown_keepBoundaries() {
        assertEquals(30, ScrollSpeed.stepUp(30))
        assertEquals(1, ScrollSpeed.stepDown(1))
        assertEquals(10, ScrollSpeed.stepUp(9))
        assertEquals(9, ScrollSpeed.stepDown(10))
    }

    @Test
    fun allThirtyLevels_increaseSpeedMonotonically() {
        for (level in ScrollSpeed.MIN_LEVEL until ScrollSpeed.MAX_LEVEL) {
            assertTrue(ScrollSpeed.intervalFactor(level) > ScrollSpeed.intervalFactor(level + 1))
            assertTrue(ScrollSpeed.durationFactor(level) > ScrollSpeed.durationFactor(level + 1))
            assertTrue(ScrollSpeed.distanceFactor(level) < ScrollSpeed.distanceFactor(level + 1))
        }
        assertEquals(15, ScrollSpeed.DEFAULT_LEVEL)
    }

    @Test
    fun highestLevel_isStillFastest() {
        assertTrue(ScrollSpeed.intervalFactor(1) > ScrollSpeed.intervalFactor(30))
        assertTrue(ScrollSpeed.durationFactor(1) > ScrollSpeed.durationFactor(30))
        assertTrue(ScrollSpeed.distanceFactor(1) < ScrollSpeed.distanceFactor(30))
    }

    @Test
    fun settingsDefault_usesTheMiddleSpeed() {
        assertEquals(ScrollSpeed.DEFAULT_LEVEL, ScrollSettings.defaults.speedLevel)
    }
}
