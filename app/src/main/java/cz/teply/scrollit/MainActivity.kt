package cz.teply.scrollit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var overlayStatusValue: TextView
    private lateinit var accessibilityStatusValue: TextView
    private lateinit var modeSpinner: Spinner
    private lateinit var distanceSeekBar: SeekBar
    private lateinit var intervalSeekBar: SeekBar
    private lateinit var durationSeekBar: SeekBar
    private lateinit var distanceValueText: TextView
    private lateinit var intervalValueText: TextView
    private lateinit var durationValueText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayStatusValue = findViewById(R.id.overlayStatusValue)
        accessibilityStatusValue = findViewById(R.id.accessibilityStatusValue)
        modeSpinner = findViewById(R.id.modeSpinner)
        distanceSeekBar = findViewById(R.id.distanceSeekBar)
        intervalSeekBar = findViewById(R.id.intervalSeekBar)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        distanceValueText = findViewById(R.id.distanceValueText)
        intervalValueText = findViewById(R.id.intervalValueText)
        durationValueText = findViewById(R.id.durationValueText)

        bindSettingsControls()

        findViewById<Button>(R.id.openOverlaySettingsButton).setOnClickListener { openOverlaySettings() }
        findViewById<Button>(R.id.openAccessibilitySettingsButton).setOnClickListener { openAccessibilitySettings() }
        findViewById<Button>(R.id.launchOverlayButton).setOnClickListener { launchOverlay() }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun bindSettingsControls() {
        val settings = ScrollSettingsStore.load(this)

        val modeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ScrollMode.entries.map { it.title },
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        modeSpinner.adapter = modeAdapter
        modeSpinner.setSelection(settings.mode.ordinal)

        distanceSeekBar.max = ScrollSettings.MAX_DISTANCE_PERCENT - ScrollSettings.MIN_DISTANCE_PERCENT
        intervalSeekBar.max = ScrollSettings.MAX_INTERVAL_MS - ScrollSettings.MIN_INTERVAL_MS
        durationSeekBar.max = ScrollSettings.MAX_GESTURE_DURATION_MS - ScrollSettings.MIN_GESTURE_DURATION_MS

        distanceSeekBar.progress = settings.distancePercent - ScrollSettings.MIN_DISTANCE_PERCENT
        intervalSeekBar.progress = settings.intervalMs - ScrollSettings.MIN_INTERVAL_MS
        durationSeekBar.progress = settings.gestureDurationMs - ScrollSettings.MIN_GESTURE_DURATION_MS

        updateSettingsValueLabels(settings)

        modeSpinner.setOnItemSelectedListener(SimpleItemSelectedListener {
            saveSettingsFromControls()
        })

        distanceSeekBar.setOnSeekBarChangeListener(SimpleSeekBarListener {
            saveSettingsFromControls()
        })
        intervalSeekBar.setOnSeekBarChangeListener(SimpleSeekBarListener {
            saveSettingsFromControls()
        })
        durationSeekBar.setOnSeekBarChangeListener(SimpleSeekBarListener {
            saveSettingsFromControls()
        })
    }

    private fun saveSettingsFromControls() {
        val settings = ScrollSettings(
            mode = ScrollMode.entries[modeSpinner.selectedItemPosition],
            distancePercent = ScrollSettings.MIN_DISTANCE_PERCENT + distanceSeekBar.progress,
            intervalMs = ScrollSettings.MIN_INTERVAL_MS + intervalSeekBar.progress,
            gestureDurationMs = ScrollSettings.MIN_GESTURE_DURATION_MS + durationSeekBar.progress,
        )
        ScrollSettingsStore.save(this, settings)
        updateSettingsValueLabels(settings)
        ScrollAccessibilityService.instance?.updateSettings(settings)
    }

    private fun updateSettingsValueLabels(settings: ScrollSettings) {
        distanceValueText.text = getString(R.string.distance_value, settings.distancePercent)
        intervalValueText.text = getString(R.string.interval_value, settings.intervalMs)
        durationValueText.text = getString(R.string.duration_value, settings.gestureDurationMs)
    }

    private fun refreshPermissionStatus() {
        updateStatus(overlayStatusValue, PermissionState.hasOverlayPermission(this))
        updateStatus(accessibilityStatusValue, PermissionState.isAccessibilityEnabled(this))
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

    private fun launchOverlay() {
        if (!PermissionState.hasOverlayPermission(this)) {
            Toast.makeText(this, R.string.overlay_permission_needed, Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        if (!PermissionState.isAccessibilityEnabled(this)) {
            Toast.makeText(this, R.string.accessibility_permission_needed, Toast.LENGTH_LONG).show()
        }

        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_OVERLAY
        }
        ContextCompat.startForegroundService(this, intent)
    }
}
