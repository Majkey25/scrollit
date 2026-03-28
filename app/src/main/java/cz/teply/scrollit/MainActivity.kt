package cz.teply.scrollit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var overlayStatusValue: TextView
    private lateinit var accessibilityStatusValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayStatusValue = findViewById(R.id.overlayStatusValue)
        accessibilityStatusValue = findViewById(R.id.accessibilityStatusValue)

        findViewById<Button>(R.id.openOverlaySettingsButton).setOnClickListener {
            openOverlaySettings()
        }
        findViewById<Button>(R.id.openAccessibilitySettingsButton).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<Button>(R.id.launchOverlayButton).setOnClickListener {
            launchOverlay()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        overlayStatusValue.text = if (PermissionState.hasOverlayPermission(this)) {
            getString(R.string.permission_enabled)
        } else {
            getString(R.string.permission_missing)
        }

        accessibilityStatusValue.text = if (PermissionState.isAccessibilityEnabled(this)) {
            getString(R.string.permission_enabled)
        } else {
            getString(R.string.permission_missing)
        }
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
