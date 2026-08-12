package cz.teply.scrollit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButtonToggleGroup
import rikka.shizuku.ShizukuProvider

class MainActivity : AppCompatActivity() {
    private lateinit var mainStateTitle: TextView
    private lateinit var mainStateDescription: TextView
    private lateinit var overlayStatusValue: TextView
    private lateinit var accessibilityStatusValue: TextView
    private lateinit var modeDependencyLabel: TextView
    private lateinit var openOverlaySettingsButton: Button
    private lateinit var openAccessibilitySettingsButton: Button
    private lateinit var permissionHelpText: TextView
    private lateinit var speedSeekBar: SeekBar
    private lateinit var distanceSeekBar: SeekBar
    private lateinit var intervalSeekBar: SeekBar
    private lateinit var durationSeekBar: SeekBar
    private lateinit var speedValueText: TextView
    private lateinit var distanceValueText: TextView
    private lateinit var intervalValueText: TextView
    private lateinit var durationValueText: TextView
    private lateinit var advancedSettingsButton: Button
    private lateinit var advancedSettingsContent: View
    private lateinit var modeToggleGroup: MaterialButtonToggleGroup
    private var syncingSettingsControls = false
    private val shizukuStateListener: () -> Unit = { refreshPermissionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainStateTitle = findViewById(R.id.mainStateTitle)
        mainStateDescription = findViewById(R.id.mainStateDescription)
        overlayStatusValue = findViewById(R.id.overlayStatusValue)
        accessibilityStatusValue = findViewById(R.id.accessibilityStatusValue)
        modeDependencyLabel = findViewById(R.id.modeDependencyLabel)
        openOverlaySettingsButton = findViewById(R.id.openOverlaySettingsButton)
        openAccessibilitySettingsButton = findViewById(R.id.openAccessibilitySettingsButton)
        permissionHelpText = findViewById(R.id.permissionHelpText)
        speedSeekBar = findViewById(R.id.speedSeekBar)
        distanceSeekBar = findViewById(R.id.distanceSeekBar)
        intervalSeekBar = findViewById(R.id.intervalSeekBar)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        speedValueText = findViewById(R.id.speedValueText)
        distanceValueText = findViewById(R.id.distanceValueText)
        intervalValueText = findViewById(R.id.intervalValueText)
        durationValueText = findViewById(R.id.durationValueText)
        advancedSettingsButton = findViewById(R.id.advancedSettingsButton)
        advancedSettingsContent = findViewById(R.id.advancedSettingsContent)
        modeToggleGroup = findViewById(R.id.modeToggleGroup)

        ShizukuAutoScrollEngine.initialize(applicationContext)
        bindModeControls()
        bindSettingsControls()

        openOverlaySettingsButton.setOnClickListener { openOverlaySettings() }
        openAccessibilitySettingsButton.setOnClickListener { openModeDependencySetup() }
        findViewById<Button>(R.id.launchOverlayButton).setOnClickListener { launchOverlay() }
        advancedSettingsButton.setOnClickListener { toggleAdvancedSettings() }
    }

    override fun onStart() {
        super.onStart()
        ShizukuAutoScrollEngine.addStateListener(shizukuStateListener)
    }

    override fun onResume() {
        super.onResume()
        syncSettingsControls()
        refreshPermissionStatus()
        OverlayService.refreshIfRunning()
    }

    override fun onStop() {
        ShizukuAutoScrollEngine.removeStateListener(shizukuStateListener)
        super.onStop()
    }

    private fun bindModeControls() {
        modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || syncingSettingsControls) {
                return@addOnButtonCheckedListener
            }
            val mode = if (checkedId == R.id.modeAutoScrollButton) {
                ScrollMode.AUTO_SCROLL
            } else {
                ScrollMode.TOUCH
            }
            val settings = ScrollSettingsStore.load(this).copy(mode = mode)
            ScrollSettingsStore.save(this, settings)
            ScrollAccessibilityService.instance?.stopAutoScroll()
            ShizukuAutoScrollEngine.stop()
            OverlayService.refreshIfRunning()
            refreshPermissionStatus()
        }
        findViewById<Button>(R.id.modeTouchButton).setOnClickListener {
            Toast.makeText(this, R.string.mode_touch_explanation, Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.modeAutoScrollButton).setOnClickListener {
            Toast.makeText(this, R.string.mode_auto_scroll_explanation, Toast.LENGTH_LONG).show()
        }
    }

    private fun bindSettingsControls() {
        speedSeekBar.max = ScrollSpeed.MAX_LEVEL - ScrollSpeed.MIN_LEVEL
        distanceSeekBar.max = ScrollSettings.MAX_DISTANCE_PERCENT - ScrollSettings.MIN_DISTANCE_PERCENT
        intervalSeekBar.max = ScrollSettings.MAX_INTERVAL_MS - ScrollSettings.MIN_INTERVAL_MS
        durationSeekBar.max = ScrollSettings.MAX_GESTURE_DURATION_MS - ScrollSettings.MIN_GESTURE_DURATION_MS

        syncSettingsControls()

        val listener = SimpleSeekBarListener {
            if (!syncingSettingsControls) {
                saveSettingsFromControls()
            }
        }
        speedSeekBar.setOnSeekBarChangeListener(listener)
        distanceSeekBar.setOnSeekBarChangeListener(listener)
        intervalSeekBar.setOnSeekBarChangeListener(listener)
        durationSeekBar.setOnSeekBarChangeListener(listener)
    }

    private fun syncSettingsControls() {
        val settings = ScrollSettingsStore.load(this)
        syncingSettingsControls = true
        modeToggleGroup.check(
            if (settings.mode == ScrollMode.AUTO_SCROLL) {
                R.id.modeAutoScrollButton
            } else {
                R.id.modeTouchButton
            },
        )
        speedSeekBar.progress = settings.speedLevel - ScrollSpeed.MIN_LEVEL
        distanceSeekBar.progress = settings.distancePercent - ScrollSettings.MIN_DISTANCE_PERCENT
        intervalSeekBar.progress = settings.intervalMs - ScrollSettings.MIN_INTERVAL_MS
        durationSeekBar.progress = settings.gestureDurationMs - ScrollSettings.MIN_GESTURE_DURATION_MS
        updateSettingsValueLabels(settings)
        syncingSettingsControls = false
    }

    private fun saveSettingsFromControls() {
        val settings = ScrollSettingsStore.load(this).copy(
            distancePercent = ScrollSettings.MIN_DISTANCE_PERCENT + distanceSeekBar.progress,
            intervalMs = ScrollSettings.MIN_INTERVAL_MS + intervalSeekBar.progress,
            gestureDurationMs = ScrollSettings.MIN_GESTURE_DURATION_MS + durationSeekBar.progress,
            speedLevel = ScrollSpeed.MIN_LEVEL + speedSeekBar.progress,
        )
        ScrollSettingsStore.save(this, settings)
        updateSettingsValueLabels(settings)
        ScrollAccessibilityService.instance?.updateSettings(settings)
        ScrollAccessibilityService.instance?.updateSpeedLevel(settings.speedLevel)
        ShizukuAutoScrollEngine.updateSpeedLevel(settings.speedLevel)
        OverlayService.refreshIfRunning()
    }

    private fun updateSettingsValueLabels(settings: ScrollSettings) {
        speedValueText.text = getString(
            R.string.speed_value,
            settings.speedLevel,
            ScrollSpeed.MAX_LEVEL,
        )
        distanceValueText.text = getString(R.string.distance_value, settings.distancePercent)
        intervalValueText.text = getString(R.string.interval_value, settings.intervalMs)
        durationValueText.text = getString(R.string.duration_value, settings.gestureDurationMs)
    }

    private fun refreshPermissionStatus() {
        if (!::modeToggleGroup.isInitialized) {
            return
        }
        val mode = ScrollSettingsStore.load(this).mode
        val overlayEnabled = PermissionState.hasOverlayPermission(this)
        val dependencyEnabled = when (mode) {
            ScrollMode.TOUCH -> PermissionState.isAccessibilityEnabled(this)
            ScrollMode.AUTO_SCROLL -> ShizukuAutoScrollEngine.prepare(this) == ShizukuState.READY
        }
        val ready = overlayEnabled && dependencyEnabled

        updateStatus(overlayStatusValue, overlayEnabled)
        updateStatus(accessibilityStatusValue, dependencyEnabled)
        modeDependencyLabel.setText(
            if (mode == ScrollMode.TOUCH) {
                R.string.accessibility_permission_label
            } else {
                R.string.shizuku_permission_label
            },
        )
        openOverlaySettingsButton.isVisible = !overlayEnabled
        openAccessibilitySettingsButton.isVisible = !dependencyEnabled
        openAccessibilitySettingsButton.setText(
            if (mode == ScrollMode.TOUCH) {
                R.string.open_accessibility_settings
            } else {
                R.string.open_shizuku
            },
        )
        permissionHelpText.isVisible = !ready
        permissionHelpText.setText(
            if (mode == ScrollMode.TOUCH) {
                R.string.permission_help_accessibility
            } else {
                R.string.permission_help_shizuku
            },
        )
        mainStateTitle.setText(if (ready) R.string.main_state_ready else R.string.main_state_setup)
        mainStateDescription.setText(
            if (ready) R.string.main_state_ready_description else R.string.main_state_setup_description,
        )
    }

    private fun toggleAdvancedSettings() {
        advancedSettingsContent.isVisible = !advancedSettingsContent.isVisible
        advancedSettingsButton.setText(
            if (advancedSettingsContent.isVisible) {
                R.string.motion_details_hide
            } else {
                R.string.motion_details_show
            },
        )
    }

    private fun updateStatus(view: TextView, enabled: Boolean) {
        view.text = getString(if (enabled) R.string.permission_enabled else R.string.permission_missing)
        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (enabled) R.color.status_ok else R.color.status_error,
            ),
        )
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openModeDependencySetup() {
        if (ScrollSettingsStore.load(this).mode == ScrollMode.TOUCH) {
            openAccessibilitySettings()
            return
        }

        when (val state = ShizukuAutoScrollEngine.prepare(this)) {
            ShizukuState.PERMISSION_REQUIRED -> {
                if (!ShizukuAutoScrollEngine.requestPermission(this)) {
                    openShizukuManager()
                }
            }

            ShizukuState.CONNECTING -> Toast.makeText(
                this,
                R.string.shizuku_connecting_message,
                Toast.LENGTH_LONG,
            ).show()

            ShizukuState.READY -> Toast.makeText(
                this,
                R.string.shizuku_ready_message,
                Toast.LENGTH_SHORT,
            ).show()

            else -> {
                Toast.makeText(this, shizukuMessage(state), Toast.LENGTH_LONG).show()
                openShizukuManager()
            }
        }
    }

    private fun openShizukuManager() {
        val managerIntent = packageManager.getLaunchIntentForPackage(
            ShizukuProvider.MANAGER_APPLICATION_ID,
        ) ?: Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/RikkaApps/Shizuku/releases/latest"),
        )
        startActivity(managerIntent)
    }

    private fun shizukuMessage(state: ShizukuState): Int = when (state) {
        ShizukuState.NOT_RUNNING -> R.string.shizuku_missing_message
        ShizukuState.PERMISSION_REQUIRED -> R.string.shizuku_permission_needed_message
        ShizukuState.PERMISSION_DENIED -> R.string.shizuku_permission_denied_message
        ShizukuState.CONNECTING -> R.string.shizuku_connecting_message
        ShizukuState.READY -> R.string.shizuku_ready_message
        ShizukuState.UNSUPPORTED -> R.string.shizuku_too_old_message
    }

    private fun launchOverlay() {
        if (!PermissionState.hasOverlayPermission(this)) {
            Toast.makeText(this, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        val mode = ScrollSettingsStore.load(this).mode
        if (mode == ScrollMode.TOUCH && !PermissionState.isAccessibilityEnabled(this)) {
            Toast.makeText(this, R.string.accessibility_permission_needed, Toast.LENGTH_LONG).show()
        } else if (mode == ScrollMode.AUTO_SCROLL) {
            val shizukuState = ShizukuAutoScrollEngine.prepare(this)
            if (shizukuState != ShizukuState.READY) {
                Toast.makeText(this, shizukuMessage(shizukuState), Toast.LENGTH_LONG).show()
            }
        }

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_OVERLAY
        }
        ContextCompat.startForegroundService(this, intent)
    }
}
