package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchScrollProfileFactoryTest {
    @Test
    fun levelOne_movesAtFrameCadenceWithSubpixelSteps() {
        val profile = TouchScrollProfileFactory.create(1)

        assertEquals(16L, profile.frameIntervalMs)
        assertEquals(0.2f, profile.distancePerTickPx, 0.0001f)
    }

    @Test
    fun allThirtyLevels_increaseDistanceMonotonically() {
        for (level in ScrollSpeed.MIN_LEVEL until ScrollSpeed.MAX_LEVEL) {
            val current = TouchScrollProfileFactory.create(level)
            val next = TouchScrollProfileFactory.create(level + 1)

            assertEquals(16L, current.frameIntervalMs)
            assertTrue(next.distancePerTickPx > current.distancePerTickPx)
        }
    }

    @Test
    fun levelThirty_staysWithinSlowContinuousRange() {
        val profile = TouchScrollProfileFactory.create(ScrollSpeed.MAX_LEVEL)

        assertEquals(16L, profile.frameIntervalMs)
        assertEquals(1.6f, profile.distancePerTickPx, 0.0001f)
    }

    @Test
    fun outOfRangeLevels_areClamped() {
        assertEquals(
            TouchScrollProfileFactory.create(ScrollSpeed.MIN_LEVEL),
            TouchScrollProfileFactory.create(Int.MIN_VALUE),
        )
        assertEquals(
            TouchScrollProfileFactory.create(ScrollSpeed.MAX_LEVEL),
            TouchScrollProfileFactory.create(Int.MAX_VALUE),
        )
    }
}
