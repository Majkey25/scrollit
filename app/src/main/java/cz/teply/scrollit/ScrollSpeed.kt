package cz.teply.scrollit

object ScrollSpeed {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 30
    const val DEFAULT_LEVEL = 15

    private const val SLOWEST_INTERVAL_FACTOR = 0.35f
    private const val FASTEST_INTERVAL_FACTOR = 0.05f
    private const val SLOWEST_DISTANCE_FACTOR = 0.35f
    private const val FASTEST_DISTANCE_FACTOR = 1.25f
    private const val SLOWEST_DURATION_FACTOR = 1.6f
    private const val FASTEST_DURATION_FACTOR = 0.45f

    fun clamp(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun stepUp(level: Int): Int = clamp(level + 1)

    fun stepDown(level: Int): Int = clamp(level - 1)

    fun intervalFactor(level: Int): Float = factor(
        SLOWEST_INTERVAL_FACTOR,
        FASTEST_INTERVAL_FACTOR,
        level,
    )

    fun distanceFactor(level: Int): Float = factor(
        SLOWEST_DISTANCE_FACTOR,
        FASTEST_DISTANCE_FACTOR,
        level,
    )

    fun durationFactor(level: Int): Float = factor(
        SLOWEST_DURATION_FACTOR,
        FASTEST_DURATION_FACTOR,
        level,
    )

    private fun factor(start: Float, end: Float, level: Int): Float {
        val progress = (clamp(level) - MIN_LEVEL).toFloat() / (MAX_LEVEL - MIN_LEVEL)
        return start + ((end - start) * progress)
    }
}
