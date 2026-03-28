package cz.teply.scrollit

data class ScrollSettings(
    val distancePercent: Int,
    val intervalMs: Int,
    val gestureDurationMs: Int,
) {
    companion object {
        const val MIN_DISTANCE_PERCENT = 6
        const val MAX_DISTANCE_PERCENT = 18
        const val MIN_INTERVAL_MS = 40
        const val MAX_INTERVAL_MS = 260
        const val MIN_GESTURE_DURATION_MS = 420
        const val MAX_GESTURE_DURATION_MS = 1400

        val defaults = ScrollSettings(
            distancePercent = 9,
            intervalMs = 90,
            gestureDurationMs = 900,
        )
    }
}
