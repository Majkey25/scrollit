package cz.teply.scrollit

data class ScrollSettings(
    val mode: ScrollMode,
    val distancePercent: Int,
    val intervalMs: Int,
    val gestureDurationMs: Int,
) {
    companion object {
        const val MIN_DISTANCE_PERCENT = 16
        const val MAX_DISTANCE_PERCENT = 42
        const val MIN_INTERVAL_MS = 200
        const val MAX_INTERVAL_MS = 1200
        const val MIN_GESTURE_DURATION_MS = 140
        const val MAX_GESTURE_DURATION_MS = 700

        val defaults = ScrollSettings(
            mode = ScrollMode.STABLE,
            distancePercent = 28,
            intervalMs = 520,
            gestureDurationMs = 250,
        )
    }
}
