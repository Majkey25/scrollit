package cz.teply.scrollit

object ScrollSpeed {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 10
    const val DEFAULT_LEVEL = 3

    fun clamp(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun stepUp(level: Int): Int = clamp(level + 1)

    fun stepDown(level: Int): Int = clamp(level - 1)

    fun intervalFactor(level: Int): Float = interpolate(level, 2.4f, 0.65f)

    fun distanceFactor(level: Int): Float = interpolate(level, 0.45f, 1.1f)

    fun durationFactor(level: Int): Float = interpolate(level, 1.5f, 0.85f)

    private fun interpolate(level: Int, slowValue: Float, fastValue: Float): Float {
        val progress = (clamp(level) - MIN_LEVEL).toFloat() / (MAX_LEVEL - MIN_LEVEL).toFloat()
        return slowValue + ((fastValue - slowValue) * progress)
    }
}
