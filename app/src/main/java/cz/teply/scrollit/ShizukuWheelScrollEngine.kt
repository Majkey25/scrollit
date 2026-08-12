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

sealed interface WheelStartResult {
    data object Started : WheelStartResult
    data class Unavailable(val state: ShizukuState) : WheelStartResult
    data object InjectionFailed : WheelStartResult
}

object ShizukuWheelScrollEngine {
    private const val REQUEST_CODE = 41

    private val mainHandler = Handler(android.os.Looper.getMainLooper())
    private val workerThread = HandlerThread("ScrollItWheel").apply { start() }
    private val workerHandler = Handler(workerThread.looper)
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private lateinit var userServiceArgs: Shizuku.UserServiceArgs
    private var initialized = false

    @Volatile
    private var service: IWheelInputService? = null

    @Volatile
    private var binding = false

    @Volatile
    private var running = false

    @Volatile
    private var speedLevel = ScrollSpeed.DEFAULT_LEVEL

    @Volatile
    private var scrollX = 0f

    @Volatile
    private var scrollY = 0f

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
            service = binder?.takeIf(IBinder::pingBinder)?.let(IWheelInputService.Stub::asInterface)
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
    private val scrollTick = object : Runnable {
        override fun run() {
            if (!running) {
                return
            }
            val currentService = service
            val profile = WheelScrollProfileFactory.create(speedLevel)
            val injected = try {
                currentService?.scroll(profile.verticalAxisValue, scrollX, scrollY) == true
            } catch (_: Exception) {
                false
            }
            if (!injected) {
                service = currentService?.takeIf { it.asBinder().isBinderAlive }
                running = false
                notifyStateChanged()
                return
            }
            workerHandler.postDelayed(this, profile.frameIntervalMs)
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) {
            return
        }
        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, WheelInputUserService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("wheel")
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

    fun start(context: Context, level: Int, x: Float, y: Float): WheelStartResult {
        val currentState = prepare(context)
        if (currentState != ShizukuState.READY) {
            return WheelStartResult.Unavailable(currentState)
        }
        val currentService = service ?: return WheelStartResult.Unavailable(ShizukuState.CONNECTING)
        val profile = WheelScrollProfileFactory.create(level)
        val injected = try {
            currentService.scroll(profile.verticalAxisValue, x, y)
        } catch (_: Exception) {
            false
        }
        if (!injected) {
            return WheelStartResult.InjectionFailed
        }

        speedLevel = ScrollSpeed.clamp(level)
        scrollX = x
        scrollY = y
        running = true
        workerHandler.removeCallbacks(scrollTick)
        workerHandler.postDelayed(scrollTick, profile.frameIntervalMs)
        notifyStateChanged()
        return WheelStartResult.Started
    }

    fun updateSpeedLevel(level: Int) {
        speedLevel = ScrollSpeed.clamp(level)
    }

    fun isRunning(): Boolean = running

    fun stop() {
        val wasRunning = running
        running = false
        workerHandler.removeCallbacks(scrollTick)
        if (wasRunning) {
            notifyStateChanged()
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
