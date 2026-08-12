package cz.teply.scrollit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var expandedView: View? = null
    private var bubbleView: View? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var selectedSpeedLevel = ScrollSpeed.DEFAULT_LEVEL
    private var selectedMode = ScrollMode.TOUCH
    private var activeMode: ScrollMode? = null
    private var lastExpandedX: Int? = null
    private var lastExpandedY: Int? = null
    private val shizukuStateListener: () -> Unit = {
        if (activeMode == ScrollMode.AUTO_SCROLL && !ShizukuWheelScrollEngine.isRunning()) {
            activeMode = null
            updateActionStatus(getString(R.string.overlay_injection_failed), isError = true)
        }
        refreshPermissionStatus()
        renderRunningState()
    }

    override fun onCreate() {
        super.onCreate()
        runningInstance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ShizukuWheelScrollEngine.initialize(applicationContext)
        ShizukuWheelScrollEngine.addStateListener(shizukuStateListener)
        createNotificationChannel()
        startForeground(ScrollConfig.notificationId, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionState.hasOverlayPermission(this)) {
            Toast.makeText(this, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_EXIT) {
            shutdownOverlay()
            return START_NOT_STICKY
        }

        refreshStoredSettings()
        if (expandedView == null && bubbleView == null) {
            showExpandedOverlay()
        }

        return START_NOT_STICKY
    }

    private fun refreshStoredSettings() {
        val settings = ScrollSettingsStore.load(this)
        if (activeMode != null && activeMode != settings.mode) {
            stopAutoScroll()
        }
        selectedSpeedLevel = settings.speedLevel
        selectedMode = settings.mode
        ScrollAccessibilityService.instance?.updateSpeedLevel(selectedSpeedLevel)
        ShizukuWheelScrollEngine.updateSpeedLevel(selectedSpeedLevel)
        if (expandedView == null && bubbleView == null) {
            return
        }
        refreshPermissionStatus()
        renderSpeedLevel()
        renderRunningState()
    }

    override fun onDestroy() {
        if (runningInstance === this) {
            runningInstance = null
        }
        ShizukuWheelScrollEngine.removeStateListener(shizukuStateListener)
        stopAutoScroll()
        removeOverlay(expandedView)
        removeOverlay(bubbleView)
        expandedView = null
        bubbleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showExpandedOverlay() {
        removeOverlay(bubbleView)
        bubbleView = null

        if (expandedView == null) {
            expandedView = inflateOverlayLayout(R.layout.overlay_controls).also(::bindExpandedOverlay)
        }

        val view = expandedView ?: return
        val params = expandedParams ?: createExpandedParams().also { expandedParams = it }
        params.x = OverlayPositioning.clampX(
            lastExpandedX ?: params.x,
            screenSize().x,
            params.width,
            dp(ScrollConfig.overlayMarginDp),
        )
        params.y = OverlayPositioning.clampY(
            lastExpandedY ?: params.y,
            screenSize().y,
            expandedOverlayHeight(view),
            dp(ScrollConfig.overlayMarginDp),
        )

        if (!view.isAttachedToWindow) {
            windowManager.addView(view, params)
            animateOverlayIn(view)
        }

        refreshPermissionStatus()
        renderSpeedLevel()
        renderRunningState()
    }

    private fun bindExpandedOverlay(view: View) {
        view.addOnLayoutChangeListener { target, _, top, _, bottom, _, oldTop, _, oldBottom ->
            val height = bottom - top
            if (height <= 0 || height == oldBottom - oldTop || !target.isAttachedToWindow) {
                return@addOnLayoutChangeListener
            }
            val params = expandedParams ?: return@addOnLayoutChangeListener
            val clampedY = OverlayPositioning.clampY(
                params.y,
                screenSize().y,
                height,
                dp(ScrollConfig.overlayMarginDp),
            )
            if (clampedY != params.y) {
                params.y = clampedY
                windowManager.updateViewLayout(target, params)
            }
        }
        view.findViewById<View>(R.id.overlayDragHandle).setOnTouchListener(createDragTouchListener(isBubble = false))

        view.findViewById<Button>(R.id.speedMinusButton).setOnClickListener {
            updateSelectedSpeedLevel(ScrollSpeed.stepDown(selectedSpeedLevel))
        }

        view.findViewById<Button>(R.id.speedPlusButton).setOnClickListener {
            updateSelectedSpeedLevel(ScrollSpeed.stepUp(selectedSpeedLevel))
        }

        view.findViewById<Button>(R.id.startStopButton).setOnClickListener {
            if (isAutoScrollRunning()) {
                stopAutoScroll()
                updateActionStatus(getString(R.string.overlay_stopped), isError = false)
            } else {
                startAutoScroll()
            }
            renderRunningState(animate = true)
        }

        view.findViewById<Button>(R.id.hideButton).setOnClickListener {
            collapseToBubble()
        }

        view.findViewById<Button>(R.id.exitButton).setOnClickListener {
            shutdownOverlay()
        }
    }

    private fun renderSpeedLevel() {
        val view = expandedView ?: return
        view.findViewById<TextView>(R.id.speedValueText).text = selectedSpeedLevel.toString()
    }

    private fun renderRunningState(animate: Boolean = false) {
        val view = expandedView ?: return
        val running = isAutoScrollRunning()
        val button = view.findViewById<Button>(R.id.startStopButton)
        button.animate().cancel()

        if (animate) {
            val halfDuration = ScrollConfig.controlAnimationDurationMs / 2
            button.animate()
                .alpha(0.55f)
                .setDuration(halfDuration)
                .withEndAction {
                    button.text = getString(if (running) R.string.stop_button else R.string.start_button)
                    button.backgroundTintList = ColorStateList.valueOf(
                        getColor(if (running) R.color.control_on_primary else R.color.control_primary),
                    )
                    button.setTextColor(getColor(if (running) R.color.control_primary else R.color.control_on_primary))
                    button.animate()
                        .alpha(1f)
                        .setDuration(halfDuration)
                        .start()
                }
                .start()
        } else {
            button.alpha = 1f
            button.text = getString(if (running) R.string.stop_button else R.string.start_button)
            button.backgroundTintList = ColorStateList.valueOf(
                getColor(if (running) R.color.control_on_primary else R.color.control_primary),
            )
            button.setTextColor(getColor(if (running) R.color.control_primary else R.color.control_on_primary))
        }

        if (running) {
            updateActionStatus(getString(R.string.overlay_running, selectedSpeedLevel), isError = false)
        }
    }

    private fun collapseToBubble() {
        val view = expandedView ?: return
        val params = expandedParams ?: return
        lastExpandedX = params.x
        lastExpandedY = params.y
        removeOverlay(view)

        if (bubbleView == null) {
            bubbleView = inflateOverlayLayout(R.layout.overlay_bubble).apply {
                findViewById<TextView>(R.id.bubbleLabel).text = getString(R.string.overlay_bubble_label)
                setOnClickListener { showExpandedOverlay() }
                setOnTouchListener(createDragTouchListener(isBubble = true))
            }
        }

        val bubble = bubbleView ?: return
        val bubbleLayoutParams = bubbleParams ?: createBubbleParams().also { bubbleParams = it }
        bubbleLayoutParams.x = OverlayPositioning.snapBubbleToEdge(
            params.x,
            screenSize().x,
            bubbleLayoutParams.width,
        )
        bubbleLayoutParams.y = OverlayPositioning.clampY(
            params.y,
            screenSize().y,
            bubbleLayoutParams.height,
            dp(ScrollConfig.overlayMarginDp),
        )

        if (!bubble.isAttachedToWindow) {
            windowManager.addView(bubble, bubbleLayoutParams)
            animateOverlayIn(bubble)
        }
    }

    private fun updateSelectedSpeedLevel(level: Int) {
        selectedSpeedLevel = ScrollSpeed.clamp(level)
        val settings = ScrollSettingsStore.load(this).copy(speedLevel = selectedSpeedLevel)
        ScrollSettingsStore.save(this, settings)
        ScrollAccessibilityService.instance?.updateSpeedLevel(selectedSpeedLevel)
        ShizukuWheelScrollEngine.updateSpeedLevel(selectedSpeedLevel)
        renderSpeedLevel()
        renderRunningState()
    }

    private fun animateOverlayIn(view: View) {
        view.alpha = 0f
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ScrollConfig.overlayAnimationDurationMs)
            .start()
    }

    private fun createDragTouchListener(isBubble: Boolean): View.OnTouchListener {
        val touchSlop = dp(8)
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false

        return View.OnTouchListener { view, event ->
            val params = if (isBubble) bubbleParams else expandedParams
            val target = if (isBubble) bubbleView else expandedView
            if (params == null || target == null) {
                return@OnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchX).toInt()
                    val deltaY = (event.rawY - touchY).toInt()
                    dragging = dragging || kotlin.math.abs(deltaX) > touchSlop || kotlin.math.abs(deltaY) > touchSlop
                    val screen = screenSize()
                    val width = if (isBubble) params.width else dp(ScrollConfig.expandedWidthDp)
                    val height = if (isBubble) params.height else expandedOverlayHeight(target)
                    params.x = OverlayPositioning.clampX(startX + deltaX, screen.x, width, dp(ScrollConfig.overlayMarginDp))
                    params.y = OverlayPositioning.clampY(startY + deltaY, screen.y, height, dp(ScrollConfig.overlayMarginDp))
                    windowManager.updateViewLayout(target, params)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isBubble) {
                        params.x = OverlayPositioning.snapBubbleToEdge(params.x, screenSize().x, params.width)
                        windowManager.updateViewLayout(target, params)
                    } else {
                        lastExpandedX = params.x
                        lastExpandedY = params.y
                    }

                    if (!dragging) {
                        view.performClick()
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun startAutoScroll() {
        val storedMode = ScrollSettingsStore.load(this).mode
        if (storedMode != selectedMode) {
            stopAutoScroll()
            selectedMode = storedMode
        }
        refreshPermissionStatus()
        if (selectedMode == ScrollMode.AUTO_SCROLL) {
            startWheelAutoScroll()
            return
        }

        if (!PermissionState.isAccessibilityEnabled(this)) {
            val message = getString(R.string.overlay_accessibility_missing)
            updateActionStatus(message, isError = true)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        val service = ScrollAccessibilityService.instance
        if (service == null) {
            val message = getString(R.string.overlay_accessibility_not_connected)
            updateActionStatus(message, isError = true)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        val settings = ScrollSettingsStore.load(this)
        when (val result = service.startAutoScroll(selectedSpeedLevel, settings)) {
            AutoScrollResult.Started -> {
                activeMode = ScrollMode.TOUCH
                updateActionStatus(
                    getString(R.string.overlay_running, selectedSpeedLevel),
                    isError = false,
                )
            }

            is AutoScrollResult.Failed -> {
                updateActionStatus(result.message, isError = true)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startWheelAutoScroll() {
        val screen = screenSize()
        when (
            val result = ShizukuWheelScrollEngine.start(
                this,
                selectedSpeedLevel,
                screen.x / 2f,
                screen.y / 2f,
            )
        ) {
            WheelStartResult.Started -> {
                activeMode = ScrollMode.AUTO_SCROLL
                updateActionStatus(
                    getString(R.string.overlay_running, selectedSpeedLevel),
                    isError = false,
                )
            }

            WheelStartResult.InjectionFailed -> showStartError(
                getString(R.string.overlay_injection_failed),
            )

            is WheelStartResult.Unavailable -> showStartError(
                getString(shizukuStatusMessage(result.state)),
            )
        }
    }

    private fun showStartError(message: String) {
        activeMode = null
        updateActionStatus(message, isError = true)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun stopAutoScroll() {
        ScrollAccessibilityService.instance?.stopAutoScroll()
        ShizukuWheelScrollEngine.stop()
        activeMode = null
    }

    private fun isAutoScrollRunning(): Boolean = when (activeMode) {
        ScrollMode.TOUCH -> ScrollAccessibilityService.instance?.isAutoScrollRunning() == true
        ScrollMode.AUTO_SCROLL -> ShizukuWheelScrollEngine.isRunning()
        null -> false
    }

    private fun refreshPermissionStatus() {
        val view = expandedView ?: return
        val permissionStatus = view.findViewById<TextView>(R.id.permissionStatusText)
        val actionStatus = view.findViewById<TextView>(R.id.actionStatusText)
        view.findViewById<TextView>(R.id.overlayDragHandle).setText(
            if (selectedMode == ScrollMode.TOUCH) {
                R.string.overlay_mode_touch
            } else {
                R.string.overlay_mode_auto_scroll
            },
        )
        val shizukuState = if (selectedMode == ScrollMode.AUTO_SCROLL) {
            ShizukuWheelScrollEngine.prepare(this)
        } else {
            null
        }
        val enabled = when (selectedMode) {
            ScrollMode.TOUCH -> PermissionState.isAccessibilityEnabled(this)
            ScrollMode.AUTO_SCROLL -> shizukuState == ShizukuState.READY
        }
        permissionStatus.isVisible = !enabled
        actionStatus.isVisible = enabled
        permissionStatus.text = if (selectedMode == ScrollMode.TOUCH) {
            getString(R.string.overlay_accessibility_missing)
        } else {
            getString(shizukuStatusMessage(requireNotNull(shizukuState)))
        }
        permissionStatus.setTextColor(getColor(R.color.status_error))
    }

    private fun shizukuStatusMessage(state: ShizukuState): Int = when (state) {
        ShizukuState.NOT_RUNNING -> R.string.shizuku_missing_message
        ShizukuState.PERMISSION_REQUIRED -> R.string.shizuku_permission_needed_message
        ShizukuState.PERMISSION_DENIED -> R.string.shizuku_permission_denied_message
        ShizukuState.CONNECTING -> R.string.shizuku_connecting_message
        ShizukuState.READY -> R.string.shizuku_ready_message
        ShizukuState.UNSUPPORTED -> R.string.shizuku_too_old_message
    }

    private fun updateActionStatus(message: String, isError: Boolean) {
        val view = expandedView ?: return
        val actionStatus = view.findViewById<TextView>(R.id.actionStatusText)
        actionStatus.text = message
        actionStatus.setTextColor(getColor(if (isError) R.color.status_error else R.color.status_ok))
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, ScrollConfig.notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(ScrollConfig.notificationChannelId) != null) {
            return
        }
        val channel = NotificationChannel(
            ScrollConfig.notificationChannelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun createExpandedParams(): WindowManager.LayoutParams {
        val screen = screenSize()
        val width = dp(ScrollConfig.expandedWidthDp)
        val margin = dp(ScrollConfig.overlayMarginDp)
        return baseLayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            x = (screen.x - width - margin).coerceAtLeast(margin)
            y = (screen.y * ScrollConfig.initialOverlayYFraction).toInt()
        }
    }

    private fun createBubbleParams(): WindowManager.LayoutParams {
        return baseLayoutParams(dp(ScrollConfig.bubbleWidthDp), dp(ScrollConfig.bubbleHeightDp)).apply {
            x = screenSize().x - width
            y = (screenSize().y * ScrollConfig.initialOverlayYFraction).toInt()
        }
    }

    private fun baseLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun inflateOverlayLayout(layoutResId: Int): View {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ScrollIt)
        val parent = FrameLayout(themedContext)
        return LayoutInflater.from(themedContext).inflate(layoutResId, parent, false)
    }

    private fun removeOverlay(view: View?) {
        if (view?.isAttachedToWindow == true) {
            windowManager.removeView(view)
        }
    }

    private fun shutdownOverlay() {
        stopAutoScroll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun screenSize(): Point {
        val metrics = resources.displayMetrics
        return Point(metrics.widthPixels, metrics.heightPixels)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun expandedOverlayHeight(view: View): Int =
        view.height.takeIf { it > 0 } ?: dp(ScrollConfig.expandedEstimatedHeightDp)

    companion object {
        const val ACTION_EXIT = "cz.teply.scrollit.action.EXIT"
        const val ACTION_SHOW_OVERLAY = "cz.teply.scrollit.action.SHOW_OVERLAY"

        private var runningInstance: OverlayService? = null

        fun refreshIfRunning() {
            runningInstance?.refreshStoredSettings()
        }
    }
}
