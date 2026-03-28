package cz.teply.scrollit

import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGestureProfileFactoryTest {
    @Test
    fun levelOne_profileIsVerySlowAndSmall() {
        val profile = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)

        assertTrue(profile.gestureDurationMs >= 1200L)
        assertTrue(profile.intervalMs >= 180L)
        assertTrue(profile.startYFraction - profile.endYFraction <= 0.05f)
    }

    @Test
    fun higherSpeed_levelsIncreaseMovementAndReduceDelay() {
        val slow = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val fast = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 10)

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
            10,
        )

        assertTrue(profile.intervalMs >= ScrollConfig.minGestureIntervalMs)
        assertTrue(profile.gestureDurationMs >= ScrollConfig.minGestureDurationMs)
        assertTrue(profile.endYFraction >= ScrollConfig.gestureMinEndYFraction)
        assertTrue((profile.startYFraction - profile.endYFraction) <= ScrollConfig.maxGestureDistanceFraction)
    }
}
