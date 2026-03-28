package cz.teply.scrollit

object ScrollSpeed {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 15
    const val DEFAULT_LEVEL = 8

    private val intervalFactors = floatArrayOf(
        5.4f,
        4.8f,
        4.2f,
        3.6f,
        3.0f,
        2.4f,
        2.2f,
        1.95f,
        1.7f,
        1.48f,
        1.28f,
        1.1f,
        0.94f,
        0.79f,
        0.65f,
    )

    private val distanceFactors = floatArrayOf(
        0.12f,
        0.16f,
        0.21f,
        0.27f,
        0.35f,
        0.45f,
        0.54f,
        0.63f,
        0.72f,
        0.81f,
        0.89f,
        0.96f,
        1.02f,
        1.07f,
        1.1f,
    )

    private val durationFactors = floatArrayOf(
        2.8f,
        2.45f,
        2.15f,
        1.9f,
        1.68f,
        1.5f,
        1.4f,
        1.31f,
        1.22f,
        1.14f,
        1.07f,
        1.0f,
        0.94f,
        0.89f,
        0.85f,
    )

    fun clamp(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun stepUp(level: Int): Int = clamp(level + 1)

    fun stepDown(level: Int): Int = clamp(level - 1)

    fun intervalFactor(level: Int): Float = intervalFactors[index(level)]

    fun distanceFactor(level: Int): Float = distanceFactors[index(level)]

    fun durationFactor(level: Int): Float = durationFactors[index(level)]

    private fun index(level: Int): Int = clamp(level) - MIN_LEVEL
}
