package cz.teply.scrollit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var expandedView: View? = null
    private var bubbleView: View? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var selectedSpeed = ScrollSpeed.MEDIUM
    private var lastExpandedX: Int? = null
    private var lastExpandedY: Int? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
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

        if (expandedView == null && bubbleView == null) {
            showExpandedOverlay()
        } else {
            refreshPermissionStatus()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
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

        val params = expandedParams ?: createExpandedParams().also {
            expandedParams = it
        }
        params.x = OverlayPositioning.clampX(
            lastExpandedX ?: params.x,
            screenSize().x,
            params.width,
            dp(ScrollConfig.overlayMarginDp),
        )
        params.y = OverlayPositioning.clampY(
            lastExpandedY ?: params.y,
            screenSize().y,
            dp(ScrollConfig.expandedEstimatedHeightDp),
            dp(ScrollConfig.overlayMarginDp),
        )

        expandedView?.let { view ->
            if (!view.isAttachedToWindow) {
                windowManager.addView(view, params)
            }
        }

        refreshPermissionStatus()
        updateActionStatus(
            if (ScrollAccessibilityService.instance?.isAutoScrollRunning() == true) {
                getString(R.string.overlay_running, selectedSpeed.title)
            } else {
                getString(R.string.overlay_idle)
            },
            isError = false,
        )
    }

    private fun bindExpandedOverlay(view: View) {
        val dragHandle = view.findViewById<TextView>(R.id.overlayDragHandle)
        val speedLabel = view.findViewById<TextView>(R.id.speedValueText)
        val speedSeekBar = view.findViewById<SeekBar>(R.id.speedSeekBar)
        val startButton = view.findViewById<Button>(R.id.startButton)
        val stopButton = view.findViewById<Button>(R.id.stopButton)
        val hideButton = view.findViewById<Button>(R.id.hideButton)
        val exitButton = view.findViewById<Button>(R.id.exitButton)

        speedLabel.text = selectedSpeed.title
        speedSeekBar.max = ScrollSpeed.entries.size - 1
        speedSeekBar.progress = selectedSpeed.ordinal
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedSpeed = ScrollSpeed.fromProgress(progress)
                speedLabel.text = selectedSpeed.title
                if (ScrollAccessibilityService.instance?.isAutoScrollRunning() == true) {
                    startAutoScroll()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        dragHandle.setOnClickListener {
            refreshPermissionStatus()
        }
        dragHandle.setOnTouchListener(createDragTouchListener(isBubble = false))

        startButton.setOnClickListener {
            startAutoScroll()
        }
        stopButton.setOnClickListener {
            stopAutoScroll()
            updateActionStatus(getString(R.string.overlay_stopped), isError = false)
        }
        hideButton.setOnClickListener {
            collapseToBubble()
        }
        exitButton.setOnClickListener {
            shutdownOverlay()
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
                setOnClickListener {
                    showExpandedOverlay()
                }
                setOnTouchListener(createDragTouchListener(isBubble = true))
            }
        }

        val bubble = bubbleView ?: return
        val bubbleLayoutParams = bubbleParams ?: createBubbleParams().also {
            bubbleParams = it
        }
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
        }
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
                    val height = if (isBubble) params.height else dp(ScrollConfig.expandedEstimatedHeightDp)
                    params.x = OverlayPositioning.clampX(
                        startX + deltaX,
                        screen.x,
                        width,
                        dp(ScrollConfig.overlayMarginDp),
                    )
                    params.y = OverlayPositioning.clampY(
                        startY + deltaY,
                        screen.y,
                        height,
                        dp(ScrollConfig.overlayMarginDp),
                    )
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
        refreshPermissionStatus()
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

        when (val result = service.startAutoScroll(selectedSpeed)) {
            AutoScrollResult.Started -> {
                updateActionStatus(getString(R.string.overlay_running, selectedSpeed.title), isError = false)
            }

            is AutoScrollResult.Failed -> {
                updateActionStatus(result.message, isError = true)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopAutoScroll() {
        ScrollAccessibilityService.instance?.stopAutoScroll()
    }

    private fun refreshPermissionStatus() {
        val view = expandedView ?: return
        val permissionStatus = view.findViewById<TextView>(R.id.permissionStatusText)
        val isEnabled = PermissionState.isAccessibilityEnabled(this)
        permissionStatus.text = if (isEnabled) {
            getString(R.string.overlay_accessibility_ready)
        } else {
            getString(R.string.overlay_accessibility_missing)
        }
        permissionStatus.setTextColor(getColor(if (isEnabled) R.color.status_ok else R.color.status_error))
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
        return baseLayoutParams(
            width = width,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
        ).apply {
            x = (screen.x - width - margin).coerceAtLeast(margin)
            y = (screen.y * ScrollConfig.initialOverlayYFraction).toInt()
        }
    }

    private fun createBubbleParams(): WindowManager.LayoutParams {
        return baseLayoutParams(
            width = dp(ScrollConfig.bubbleWidthDp),
            height = dp(ScrollConfig.bubbleHeightDp),
        ).apply {
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
        val parent = FrameLayout(this)
        return LayoutInflater.from(this).inflate(layoutResId, parent, false)
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val ACTION_EXIT = "cz.teply.scrollit.action.EXIT"
        const val ACTION_SHOW_OVERLAY = "cz.teply.scrollit.action.SHOW_OVERLAY"
    }
}
