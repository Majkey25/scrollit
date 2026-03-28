package cz.teply.scrollit

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var speedLevel = ScrollSpeed.DEFAULT_LEVEL
    private var settings = ScrollSettings.defaults
    private var running = false
    private val repeatScroll = Runnable { dispatchNextGesture() }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        stopAutoScroll()
    }

    override fun onDestroy() {
        stopAutoScroll()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun startAutoScroll(level: Int, newSettings: ScrollSettings): AutoScrollResult {
        stopAutoScroll()
        speedLevel = ScrollSpeed.clamp(level)
        settings = newSettings
        running = true
        return dispatchNextGesture()
    }

    fun updateSpeedLevel(level: Int) {
        speedLevel = ScrollSpeed.clamp(level)
    }

    fun updateSettings(newSettings: ScrollSettings) {
        settings = newSettings
    }

    fun stopAutoScroll() {
        running = false
        handler.removeCallbacks(repeatScroll)
    }

    fun isAutoScrollRunning(): Boolean = running

    private fun dispatchNextGesture(): AutoScrollResult {
        val profile = currentProfile()

        val path = Path()
        val width = resources.displayMetrics.widthPixels.toFloat()
        val height = resources.displayMetrics.heightPixels.toFloat()
        path.moveTo(width * ScrollConfig.gestureXFraction, height * profile.startYFraction)
        path.lineTo(width * ScrollConfig.gestureXFraction, height * profile.endYFraction)

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    profile.gestureDurationMs,
                ),
            )
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (running) {
                        handler.postDelayed(repeatScroll, profile.intervalMs)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    running = false
                }
            },
            null,
        )

        if (!accepted) {
            running = false
            return AutoScrollResult.Failed(getString(R.string.overlay_gesture_failed))
        }

        return AutoScrollResult.Started
    }

    private fun currentProfile(): GestureProfile {
        val mode = settings.mode
        val interval = (
            settings.intervalMs *
                mode.intervalScale *
                ScrollSpeed.intervalFactor(speedLevel)
            ).toLong().coerceIn(190L, 2200L)

        val gestureDuration = (
            settings.gestureDurationMs *
                mode.durationScale *
                ScrollSpeed.durationFactor(speedLevel)
            ).toLong().coerceIn(120L, 800L)

        val distanceFraction = (
            settings.distancePercent / 100f *
                mode.distanceScale *
                ScrollSpeed.distanceFactor(speedLevel)
            ).coerceIn(0.11f, 0.45f)

        val startY = 0.82f
        val endY = (startY - distanceFraction).coerceAtLeast(0.20f)

        return GestureProfile(
            intervalMs = interval,
            gestureDurationMs = gestureDuration,
            startYFraction = startY,
            endYFraction = endY,
        )
    }

    companion object {
        @Volatile
        var instance: ScrollAccessibilityService? = null
            private set
    }
}

data class GestureProfile(
    val intervalMs: Long,
    val gestureDurationMs: Long,
    val startYFraction: Float,
    val endYFraction: Float,
)
