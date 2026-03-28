package cz.teply.scrollit

import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGestureProfileFactoryTest {
    @Test
    fun levelOne_profileIsVerySlowAndSmall() {
        val profile = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)

        assertTrue(profile.gestureDurationMs >= 1800L)
        assertTrue(profile.intervalMs >= 450L)
        assertTrue(profile.startYFraction - profile.endYFraction <= 0.02f)
    }

    @Test
    fun firstFiveLevels_areSlowerThanOldLevelOneBaseline() {
        val extraSlow = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val baseline = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 6)

        assertTrue(extraSlow.gestureDurationMs > baseline.gestureDurationMs)
        assertTrue(extraSlow.intervalMs > baseline.intervalMs)
        assertTrue((extraSlow.startYFraction - extraSlow.endYFraction) < (baseline.startYFraction - baseline.endYFraction))
    }

    @Test
    fun higherSpeed_levelsIncreaseMovementAndReduceDelay() {
        val slow = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 1)
        val fast = ScrollGestureProfileFactory.create(ScrollSettings.defaults, 15)

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
            15,
        )

        assertTrue(profile.intervalMs >= ScrollConfig.minGestureIntervalMs)
        assertTrue(profile.gestureDurationMs >= ScrollConfig.minGestureDurationMs)
        assertTrue(profile.endYFraction >= ScrollConfig.gestureMinEndYFraction)
        assertTrue((profile.startYFraction - profile.endYFraction) <= ScrollConfig.maxGestureDistanceFraction)
    }
}
