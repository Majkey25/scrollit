package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureBatchTimingTest {
    @Test
    fun strokeStartTimes_boundBatchDurationForResponsiveStop() {
        val profile = GestureProfile(
            intervalMs = 8L,
            gestureDurationMs = 500L,
            startYFraction = 0.72f,
            endYFraction = 0.64f,
        )

        assertEquals(
            listOf(0L, 508L, 1016L),
            GestureBatchTiming.strokeStartTimes(profile, platformMaxStrokeCount = 10),
        )
    }

    @Test
    fun strokeStartTimes_respectSmallerPlatformLimit() {
        val profile = GestureProfile(
            intervalMs = 4L,
            gestureDurationMs = 320L,
            startYFraction = 0.72f,
            endYFraction = 0.60f,
        )

        assertEquals(
            listOf(0L, 324L, 648L),
            GestureBatchTiming.strokeStartTimes(profile, platformMaxStrokeCount = 3),
        )
    }

    @Test
    fun strokeStartTimes_useOneSlowStrokeWhenTwoWouldExceedBatchLimit() {
        val profile = GestureProfile(
            intervalMs = 31L,
            gestureDurationMs = 1440L,
            startYFraction = 0.72f,
            endYFraction = 0.685f,
        )

        assertEquals(
            listOf(0L),
            GestureBatchTiming.strokeStartTimes(profile, platformMaxStrokeCount = 10),
        )
    }
}
