package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContinuousTouchPathTest {
    @Test
    fun nextY_movesUpByConfiguredDistance() {
        val path = ContinuousTouchPath(startY = 100f, endY = 50f)

        assertEquals(99.5f, path.nextY(0.5f))
        assertEquals(99f, path.nextY(0.5f))
    }

    @Test
    fun nextY_stopsAtEndUntilRestarted() {
        val path = ContinuousTouchPath(startY = 10f, endY = 6f)

        assertEquals(8f, path.nextY(2f))
        assertEquals(6f, path.nextY(2f))
        assertNull(path.nextY(2f))

        path.restart()

        assertEquals(8f, path.nextY(2f))
    }

    @Test
    fun constructor_rejectsReversedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            ContinuousTouchPath(startY = 50f, endY = 100f)
        }
    }

    @Test
    fun nextY_rejectsNonPositiveDistance() {
        val path = ContinuousTouchPath(startY = 100f, endY = 50f)

        assertThrows(IllegalArgumentException::class.java) {
            path.nextY(0f)
        }
    }
}
