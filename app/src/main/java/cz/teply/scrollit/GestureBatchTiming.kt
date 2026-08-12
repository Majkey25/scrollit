package cz.teply.scrollit

object GestureBatchTiming {
    fun strokeStartTimes(profile: GestureProfile, platformMaxStrokeCount: Int): List<Long> {
        val strokeStepMs = profile.gestureDurationMs + profile.intervalMs
        val durationLimit = (
            (ScrollConfig.maxGestureBatchDurationMs + profile.intervalMs) / strokeStepMs
        ).toInt().coerceAtLeast(1)
        val strokeCount = minOf(
            platformMaxStrokeCount.coerceAtLeast(1),
            ScrollConfig.gesturesPerBatch,
            durationLimit,
        )
        return List(strokeCount) { index -> index * strokeStepMs }
    }
}
