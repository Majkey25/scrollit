package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGestureProfileFactoryTest {
    @Test
    fun levelOne_profileStaysSlowButAvoidsVisiblePauses() {
        val profile = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val distance = profile.startYFraction - profile.endYFraction

        assertTrue(profile.gestureDurationMs >= 1400L)
        assertTrue(profile.intervalMs <= 32L)
        assertEquals(ScrollConfig.minGestureDistanceFraction, distance, 0.0001f)
    }

    @Test
    fun defaultProfile_usesANearContinuousGap() {
        val profile = ScrollGestureProfileFactory.create(
            ScrollSettings.defaults,
            ScrollSpeed.DEFAULT_LEVEL,
        )

        assertTrue(profile.intervalMs in 4L..24L)
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
