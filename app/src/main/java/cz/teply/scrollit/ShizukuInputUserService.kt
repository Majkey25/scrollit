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

class ShizukuInputUserService : IShizukuInputService.Stub {
    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    private val injector by lazy(::InputEventInjector)

    override fun startTouch(x: Float, y: Float): Boolean =
        runCatching { injector.startTouch(x, y) }.getOrDefault(false)

    override fun moveTouch(x: Float, y: Float): Boolean =
        runCatching { injector.moveTouch(x, y) }.getOrDefault(false)

    override fun finishTouch(): Boolean =
        runCatching { injector.finishTouch() }.getOrDefault(false)

    override fun destroy() {
        runCatching { injector.finishTouch() }
        exitProcess(0)
    }
}

private class InputEventInjector {
    private val inputManager: Any
    private val injectInputEvent: java.lang.reflect.Method
    private var touchDown = false
    private var touchDownTime = 0L
    private var touchX = 0f
    private var touchY = 0f

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

    @Synchronized
    fun startTouch(x: Float, y: Float): Boolean {
        if (touchDown && !finishTouch()) {
            return false
        }
        touchDownTime = SystemClock.uptimeMillis()
        val injected = injectTouch(MotionEvent.ACTION_DOWN, x, y)
        if (injected) {
            touchDown = true
            touchX = x
            touchY = y
        }
        return injected
    }

    @Synchronized
    fun moveTouch(x: Float, y: Float): Boolean {
        if (!touchDown) {
            return false
        }
        val injected = injectTouch(MotionEvent.ACTION_MOVE, x, y)
        if (injected) {
            touchX = x
            touchY = y
        }
        return injected
    }

    @Synchronized
    fun finishTouch(): Boolean {
        if (!touchDown) {
            return true
        }
        val injected = injectTouch(MotionEvent.ACTION_UP, touchX, touchY)
        touchDown = false
        return injected
    }

    private fun injectTouch(action: Int, x: Float, y: Float): Boolean {
        val pointerProperties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val pointerCoords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (action == MotionEvent.ACTION_UP) 0f else 1f
            size = 1f
        }
        val event = MotionEvent.obtain(
            touchDownTime,
            SystemClock.uptimeMillis(),
            action,
            1,
            arrayOf(pointerProperties),
            arrayOf(pointerCoords),
            0,
            0,
            1f,
            1f,
            -1,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )

        return try {
            injectInputEvent.invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT) as Boolean
        } finally {
            event.recycle()
        }
    }

    private companion object {
        const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1
    }
}
