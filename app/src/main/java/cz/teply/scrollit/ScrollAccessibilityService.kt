package cz.teply.scrollit

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var activeSpeed: ScrollSpeed? = null
    private var running = false
    private val repeatScroll = Runnable {
        dispatchNextGesture()
    }

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

    fun startAutoScroll(speed: ScrollSpeed): AutoScrollResult {
        stopAutoScroll()
        activeSpeed = speed
        running = true
        return dispatchNextGesture()
    }

    fun stopAutoScroll() {
        running = false
        handler.removeCallbacks(repeatScroll)
    }

    fun isAutoScrollRunning(): Boolean = running

    private fun dispatchNextGesture(): AutoScrollResult {
        val speed = activeSpeed ?: return AutoScrollResult.Failed(getString(R.string.overlay_idle))
        val path = Path()
        val width = resources.displayMetrics.widthPixels.toFloat()
        val height = resources.displayMetrics.heightPixels.toFloat()
        path.moveTo(width * ScrollConfig.gestureXFraction, height * speed.startYFraction)
        path.lineTo(width * ScrollConfig.gestureXFraction, height * speed.endYFraction)

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    speed.gestureDurationMs,
                ),
            )
            .build()

        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (running) {
                        handler.postDelayed(repeatScroll, speed.intervalMs)
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

    companion object {
        @Volatile
        var instance: ScrollAccessibilityService? = null
            private set
    }
}
