package cz.teply.scrollit

enum class ScrollMode(
    val title: String,
    val distanceScale: Float,
    val intervalScale: Float,
    val durationScale: Float,
) {
    STABLE(
        title = "Stable",
        distanceScale = 1.0f,
        intervalScale = 1.12f,
        durationScale = 1.06f,
    ),
    SMOOTH(
        title = "Smooth",
        distanceScale = 0.82f,
        intervalScale = 0.76f,
        durationScale = 0.9f,
    ),
}
