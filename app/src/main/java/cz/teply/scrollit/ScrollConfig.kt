package cz.teply.scrollit

object ScrollConfig {
    const val bubbleHeightDp = 48
    const val bubbleWidthDp = 48
    const val expandedEstimatedHeightDp = 224
    const val expandedWidthDp = 248
    const val controlAnimationDurationMs = 120L
    const val overlayAnimationDurationMs = 160L
    const val gestureXFraction = 0.5f
    const val gestureStartYFraction = 0.72f
    const val gestureMinEndYFraction = 0.28f
    const val gesturesPerBatch = 8
    const val initialOverlayYFraction = 0.18f
    const val maxGestureDistanceFraction = 0.16f
    const val maxGestureBatchDurationMs = 2000L
    const val maxGestureDurationMs = 2000L
    const val maxGestureIntervalMs = 50L
    const val minGestureDistanceFraction = 0.009f
    const val minGestureDurationMs = 300L
    const val minGestureIntervalMs = 4L
    const val notificationChannelId = "scrollit_overlay_channel"
    const val notificationId = 1001
    const val overlayMarginDp = 12
}
