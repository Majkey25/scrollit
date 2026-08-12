package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelScrollProfileFactoryTest {
    @Test
    fun levelOne_usesPerFrameMicroScroll() {
        val profile = WheelScrollProfileFactory.create(1)

        assertEquals(16L, profile.frameIntervalMs)
        assertEquals(-0.005f, profile.verticalAxisValue, 0.0001f)
    }

    @Test
    fun allThirtyLevels_increaseWheelVelocityMonotonically() {
        for (level in ScrollSpeed.MIN_LEVEL until ScrollSpeed.MAX_LEVEL) {
            val current = WheelScrollProfileFactory.create(level)
            val next = WheelScrollProfileFactory.create(level + 1)

            assertTrue(next.verticalAxisValue < current.verticalAxisValue)
        }
    }

    @Test
    fun outOfRangeLevels_areClamped() {
        assertEquals(
            WheelScrollProfileFactory.create(ScrollSpeed.MIN_LEVEL),
            WheelScrollProfileFactory.create(Int.MIN_VALUE),
        )
        assertEquals(
            WheelScrollProfileFactory.create(ScrollSpeed.MAX_LEVEL),
            WheelScrollProfileFactory.create(Int.MAX_VALUE),
        )
    }

    @Test
    fun levelThirty_staysWithinSlowWheelRange() {
        assertEquals(
            -0.020f,
            WheelScrollProfileFactory.create(ScrollSpeed.MAX_LEVEL).verticalAxisValue,
            0.0001f,
        )
    }
}
