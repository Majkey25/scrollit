package cz.teply.scrollit

object ScrollGestureProfileFactory {
    fun create(settings: ScrollSettings, speedLevel: Int): GestureProfile {
        val intervalMs = (settings.intervalMs * ScrollSpeed.intervalFactor(speedLevel))
            .toLong()
            .coerceIn(ScrollConfig.minGestureIntervalMs, ScrollConfig.maxGestureIntervalMs)

        val gestureDurationMs = (settings.gestureDurationMs * ScrollSpeed.durationFactor(speedLevel))
            .toLong()
            .coerceIn(ScrollConfig.minGestureDurationMs, ScrollConfig.maxGestureDurationMs)

        val distanceFraction = (settings.distancePercent / 100f * ScrollSpeed.distanceFactor(speedLevel))
            .coerceIn(ScrollConfig.minGestureDistanceFraction, ScrollConfig.maxGestureDistanceFraction)

        val startYFraction = ScrollConfig.gestureStartYFraction
        val endYFraction = (startYFraction - distanceFraction).coerceAtLeast(ScrollConfig.gestureMinEndYFraction)

        return GestureProfile(
            intervalMs = intervalMs,
            gestureDurationMs = gestureDurationMs,
            startYFraction = startYFraction,
            endYFraction = endYFraction,
        )
    }
}
