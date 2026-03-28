package cz.teply.scrollit

object ScrollSpeed {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 10
    const val DEFAULT_LEVEL = 4

    fun clamp(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun stepUp(level: Int): Int = clamp(level + 1)

    fun stepDown(level: Int): Int = clamp(level - 1)

    fun intervalFactor(level: Int): Float {
        val step = clamp(level) - MIN_LEVEL
        return 1.34f - (step * 0.068f)
    }

    fun distanceFactor(level: Int): Float {
        val step = clamp(level) - MIN_LEVEL
        return 0.86f + (step * 0.03f)
    }

    fun durationFactor(level: Int): Float {
        val step = clamp(level) - MIN_LEVEL
        return 1.16f - (step * 0.038f)
    }
}
