package cz.teply.scrollit

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.InputEvent
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.Keep
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

class WheelInputUserService : IWheelInputService.Stub {
    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    private val injector by lazy(::InputEventInjector)

    override fun scroll(verticalAxisValue: Float, x: Float, y: Float): Boolean =
        runCatching { injector.injectScroll(verticalAxisValue, x, y) }.getOrDefault(false)

    override fun destroy() {
        exitProcess(0)
    }
}

private class InputEventInjector {
    private val inputManager: Any
    private val injectInputEvent: java.lang.reflect.Method

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/hardware/input/")
        }
        val className = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "android.hardware.input.InputManagerGlobal"
        } else {
            "android.hardware.input.InputManager"
        }
        val inputManagerClass = Class.forName(className)
        inputManager = requireNotNull(
            inputManagerClass.getDeclaredMethod("getInstance").invoke(null),
        )
        injectInputEvent = inputManagerClass.getDeclaredMethod(
            "injectInputEvent",
            InputEvent::class.java,
            Int::class.javaPrimitiveType,
        )
    }

    fun injectScroll(verticalAxisValue: Float, x: Float, y: Float): Boolean {
        val pointerProperties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val pointerCoords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, verticalAxisValue)
        }
        val eventTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(
            eventTime,
            eventTime,
            MotionEvent.ACTION_SCROLL,
            1,
            arrayOf(pointerProperties),
            arrayOf(pointerCoords),
            0,
            0,
            1f,
            1f,
            -1,
            0,
            InputDevice.SOURCE_MOUSE,
            0,
        )

        return try {
            injectInputEvent.invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
        } finally {
            event.recycle()
        }
    }

    private companion object {
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    }
}
