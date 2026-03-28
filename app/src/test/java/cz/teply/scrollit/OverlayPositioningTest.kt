package cz.teply.scrollit

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPositioningTest {
    @Test
    fun clampX_keepsOverlayInsideLeftEdge() {
        val result = OverlayPositioning.clampX(
            x = -40,
            screenWidth = 1080,
            overlayWidth = 240,
            margin = 12,
        )

        assertEquals(12, result)
    }

    @Test
    fun clampY_keepsOverlayInsideBottomEdge() {
        val result = OverlayPositioning.clampY(
            y = 2200,
            screenHeight = 1920,
            overlayHeight = 180,
            margin = 12,
        )

        assertEquals(1728, result)
    }

    @Test
    fun snapBubbleToEdge_movesBubbleToNearestRightEdge() {
        val result = OverlayPositioning.snapBubbleToEdge(
            x = 700,
            screenWidth = 1080,
            bubbleWidth = 52,
        )

        assertEquals(1028, result)
    }
}
