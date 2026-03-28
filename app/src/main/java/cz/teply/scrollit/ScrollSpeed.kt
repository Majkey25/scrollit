package cz.teply.scrollit

enum class ScrollSpeed(
    val title: String,
    val intervalMs: Long,
    val gestureDurationMs: Long,
    val startYFraction: Float,
    val endYFraction: Float,
) {
    VERY_SLOW(
        title = "Very slow",
        intervalMs = 1800L,
        gestureDurationMs = 420L,
        startYFraction = 0.80f,
        endYFraction = 0.68f,
    ),
    SLOW(
        title = "Slow",
        intervalMs = 1300L,
        gestureDurationMs = 360L,
        startYFraction = 0.82f,
        endYFraction = 0.64f,
    ),
    MEDIUM(
        title = "Medium",
        intervalMs = 950L,
        gestureDurationMs = 320L,
        startYFraction = 0.84f,
        endYFraction = 0.58f,
    ),
    FAST(
        title = "Fast",
        intervalMs = 700L,
        gestureDurationMs = 260L,
        startYFraction = 0.86f,
        endYFraction = 0.52f,
    ),
    ;

    companion object {
        fun fromProgress(progress: Int): ScrollSpeed {
            val safeIndex = progress.coerceIn(0, entries.size - 1)
            return entries[safeIndex]
        }
    }
}
