package cz.teply.scrollit

data class WheelScrollProfile(
    val frameIntervalMs: Long,
    val verticalAxisValue: Float,
)

object WheelScrollProfileFactory {
    private const val frameIntervalMs = 16L
    private const val slowestAxisValue = 0.005f
    private const val fastestAxisValue = 0.020f

    fun create(speedLevel: Int): WheelScrollProfile {
        val level = ScrollSpeed.clamp(speedLevel)
        val progress = (level - ScrollSpeed.MIN_LEVEL).toFloat() /
            (ScrollSpeed.MAX_LEVEL - ScrollSpeed.MIN_LEVEL)
        return WheelScrollProfile(
            frameIntervalMs = frameIntervalMs,
            verticalAxisValue = -(slowestAxisValue + (fastestAxisValue - slowestAxisValue) * progress),
        )
    }
}
