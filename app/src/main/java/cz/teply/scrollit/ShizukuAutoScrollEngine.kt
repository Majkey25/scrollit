package cz.teply.scrollit

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArraySet

enum class ShizukuState {
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    CONNECTING,
    READY,
    UNSUPPORTED,
}

sealed interface ShizukuStartResult {
    data object Started : ShizukuStartResult
    data class Unavailable(val state: ShizukuState) : ShizukuStartResult
    data object InjectionFailed : ShizukuStartResult
}

object ShizukuAutoScrollEngine {
    private const val REQUEST_CODE = 41

    private val mainHandler = Handler(android.os.Looper.getMainLooper())
    private val workerThread = HandlerThread("ScrollItShizuku").apply { start() }
    private val workerHandler = Handler(workerThread.looper)
    private val touchLock = Any()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private lateinit var userServiceArgs: Shizuku.UserServiceArgs
    private var initialized = false

    @Volatile
    private var service: IShizukuInputService? = null

    @Volatile
    private var binding = false

    @Volatile
    private var running = false

    @Volatile
    private var speedLevel = ScrollSpeed.DEFAULT_LEVEL

    private var touchX = 0f
    private var touchStartY = 0f
    private var touchPath: ContinuousTouchPath? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        bindIfAllowed()
        notifyStateChanged()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        binding = false
        stop()
        notifyStateChanged()
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
        if (requestCode == REQUEST_CODE && result == PackageManager.PERMISSION_GRANTED) {
            bindIfAllowed()
        }
        notifyStateChanged()
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            service = binder?.takeIf(IBinder::pingBinder)?.let(IShizukuInputService.Stub::asInterface)
            binding = false
            notifyStateChanged()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            binding = false
            stop()
            notifyStateChanged()
        }
    }
    private val touchTick = object : Runnable {
        override fun run() {
            synchronized(touchLock) {
                if (!running) {
                    return
                }
                val currentService = service
                val path = touchPath
                val profile = TouchScrollProfileFactory.create(speedLevel)
                val nextY = path?.nextY(profile.distancePerTickPx)
                val injected = try {
                    when {
                        currentService == null || path == null -> false
                        nextY != null -> currentService.moveTouch(touchX, nextY)
                        !currentService.finishTouch() -> false
                        else -> {
                            path.restart()
                            currentService.startTouch(touchX, touchStartY)
                        }
                    }
                } catch (_: Exception) {
                    false
                }
                if (!injected) {
                    runCatching { currentService?.finishTouch() }
                    service = currentService?.takeIf { it.asBinder().isBinderAlive }
                    running = false
                    touchPath = null
                    notifyStateChanged()
                    return
                }
                workerHandler.postDelayed(this, profile.frameIntervalMs)
            }
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) {
            return
        }
        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuInputUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("input")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        initialized = true
    }

    fun addStateListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeStateListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun prepare(context: Context): ShizukuState {
        initialize(context.applicationContext)
        bindIfAllowed()
        return state()
    }

    fun state(): ShizukuState {
        if (!initialized || !Shizuku.pingBinder()) {
            return ShizukuState.NOT_RUNNING
        }
        return try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 10) {
                ShizukuState.UNSUPPORTED
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    ShizukuState.PERMISSION_DENIED
                } else {
                    ShizukuState.PERMISSION_REQUIRED
                }
            } else if (service?.asBinder()?.isBinderAlive == true) {
                ShizukuState.READY
            } else {
                ShizukuState.CONNECTING
            }
        } catch (_: Exception) {
            ShizukuState.NOT_RUNNING
        }
    }

    fun requestPermission(context: Context): Boolean {
        return when (prepare(context)) {
            ShizukuState.PERMISSION_REQUIRED -> runCatching {
                Shizuku.requestPermission(REQUEST_CODE)
            }.isSuccess

            ShizukuState.CONNECTING -> {
                bindIfAllowed()
                true
            }

            ShizukuState.READY -> true
            else -> false
        }
    }

    fun start(
        context: Context,
        level: Int,
        x: Float,
        startY: Float,
        endY: Float,
    ): ShizukuStartResult {
        val currentState = prepare(context)
        if (currentState != ShizukuState.READY) {
            return ShizukuStartResult.Unavailable(currentState)
        }
        val currentService = service ?: return ShizukuStartResult.Unavailable(ShizukuState.CONNECTING)
        synchronized(touchLock) {
            val path = ContinuousTouchPath(startY, endY)
            val injected = try {
                currentService.startTouch(x, startY)
            } catch (_: Exception) {
                false
            }
            if (!injected) {
                return ShizukuStartResult.InjectionFailed
            }

            speedLevel = ScrollSpeed.clamp(level)
            touchX = x
            touchStartY = startY
            touchPath = path
            running = true
            workerHandler.removeCallbacks(touchTick)
            workerHandler.postDelayed(
                touchTick,
                TouchScrollProfileFactory.create(speedLevel).frameIntervalMs,
            )
            notifyStateChanged()
            return ShizukuStartResult.Started
        }
    }

    fun updateSpeedLevel(level: Int) {
        speedLevel = ScrollSpeed.clamp(level)
    }

    fun isRunning(): Boolean = running

    fun stop() {
        synchronized(touchLock) {
            val wasRunning = running
            running = false
            workerHandler.removeCallbacks(touchTick)
            if (wasRunning) {
                runCatching { service?.finishTouch() }
                touchPath = null
                notifyStateChanged()
            }
        }
    }

    private fun bindIfAllowed() {
        if (!initialized || !Shizuku.pingBinder()) {
            return
        }
        val canBind = runCatching {
            !Shizuku.isPreV11() &&
                Shizuku.getVersion() >= 10 &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!canBind || service?.asBinder()?.isBinderAlive == true || binding) {
            return
        }

        binding = true
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Exception) {
            binding = false
        }
    }

    private fun notifyStateChanged() {
        mainHandler.post {
            listeners.forEach { it() }
        }
    }
}
