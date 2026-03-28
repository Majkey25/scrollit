package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollSpeedTest {
    @Test
    fun clamp_limitsLevelInsideOneToFifteen() {
        assertEquals(1, ScrollSpeed.clamp(-2))
        assertEquals(15, ScrollSpeed.clamp(42))
    }

    @Test
    fun stepUpAndDown_keepBoundaries() {
        assertEquals(15, ScrollSpeed.stepUp(15))
        assertEquals(1, ScrollSpeed.stepDown(1))
        assertEquals(10, ScrollSpeed.stepUp(9))
        assertEquals(9, ScrollSpeed.stepDown(10))
    }

    @Test
    fun firstFiveLevels_areSlowerThanCurrentBaselineRange() {
        assertTrue(ScrollSpeed.intervalFactor(1) > ScrollSpeed.intervalFactor(6))
        assertTrue(ScrollSpeed.durationFactor(1) > ScrollSpeed.durationFactor(6))
        assertTrue(ScrollSpeed.distanceFactor(1) < ScrollSpeed.distanceFactor(6))
        assertEquals(8, ScrollSpeed.DEFAULT_LEVEL)
    }

    @Test
    fun highestLevel_isStillFastest() {
        assertTrue(ScrollSpeed.intervalFactor(6) > ScrollSpeed.intervalFactor(15))
        assertTrue(ScrollSpeed.durationFactor(6) > ScrollSpeed.durationFactor(15))
        assertTrue(ScrollSpeed.distanceFactor(6) < ScrollSpeed.distanceFactor(15))
    }
}
