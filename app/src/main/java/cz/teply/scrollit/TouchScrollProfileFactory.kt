package cz.teply.scrollit

data class TouchScrollProfile(
    val frameIntervalMs: Long,
    val distancePerTickPx: Float,
)

object TouchScrollProfileFactory {
    private const val frameIntervalMs = 16L
    private const val slowestDistancePerTickPx = 0.2f
    private const val fastestDistancePerTickPx = 1.6f

    fun create(speedLevel: Int): TouchScrollProfile {
        val level = ScrollSpeed.clamp(speedLevel)
        val progress = (level - ScrollSpeed.MIN_LEVEL).toFloat() /
            (ScrollSpeed.MAX_LEVEL - ScrollSpeed.MIN_LEVEL)
        return TouchScrollProfile(
            frameIntervalMs = frameIntervalMs,
            distancePerTickPx = slowestDistancePerTickPx +
                (fastestDistancePerTickPx - slowestDistancePerTickPx) * progress,
        )
    }
}

class ContinuousTouchPath(
    private val startY: Float,
    private val endY: Float,
) {
    private var currentY = startY

    init {
        require(startY > endY) { "startY must be greater than endY" }
    }

    fun nextY(distancePx: Float): Float? {
        require(distancePx > 0f) { "distancePx must be positive" }
        val nextY = currentY - distancePx
        if (nextY < endY) {
            return null
        }
        currentY = nextY
        return currentY
    }

    fun restart() {
        currentY = startY
    }
}
