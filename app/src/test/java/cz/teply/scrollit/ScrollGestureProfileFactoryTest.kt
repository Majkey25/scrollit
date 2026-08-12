package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGestureProfileFactoryTest {
    @Test
    fun levelOne_addsAUsefulRangeBelowThePreviousMinimum() {
        val profile = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val distance = profile.startYFraction - profile.endYFraction

        assertTrue(profile.gestureDurationMs in 1900L..2000L)
        assertTrue(profile.intervalMs in 40L..50L)
        assertEquals(0.009f, distance, 0.0001f)
    }

    @Test
    fun levelFifteen_matchesThePreviousLevelOnePace() {
        val profile = ScrollGestureProfileFactory.create(
            ScrollSettings.defaults,
            ScrollSpeed.DEFAULT_LEVEL,
        )
        val distance = profile.startYFraction - profile.endYFraction

        assertEquals(1440L, profile.gestureDurationMs)
        assertEquals(31L, profile.intervalMs)
        assertEquals(0.035f, distance, 0.0001f)
    }

    @Test
    fun higherSpeed_levelsIncreaseMovementAndReduceDelay() {
        val slow = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val fast = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 30)

        assertTrue(fast.gestureDurationMs < slow.gestureDurationMs)
        assertTrue(fast.intervalMs < slow.intervalMs)
        assertTrue((fast.startYFraction - fast.endYFraction) > (slow.startYFraction - slow.endYFraction))
    }

    @Test
    fun profileStaysInsideSafeReadableBounds() {
        val profile = ScrollGestureProfileFactory.create(
            ScrollSettings(
                distancePercent = ScrollSettings.MAX_DISTANCE_PERCENT,
                intervalMs = ScrollSettings.MIN_INTERVAL_MS,
                gestureDurationMs = ScrollSettings.MIN_GESTURE_DURATION_MS,
            ),
            30,
        )

        assertTrue(profile.intervalMs >= ScrollConfig.minGestureIntervalMs)
        assertTrue(profile.gestureDurationMs >= ScrollConfig.minGestureDurationMs)
        assertTrue(profile.endYFraction >= ScrollConfig.gestureMinEndYFraction)
        assertTrue((profile.startYFraction - profile.endYFraction) <= ScrollConfig.maxGestureDistanceFraction)
    }
}
