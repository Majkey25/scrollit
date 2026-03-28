package cz.teply.scrollit

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings

object PermissionState {
    fun hasOverlayPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        if (enabledServices.isBlank()) {
            return false
        }

        val expectedService = ComponentName(
            context,
            ScrollAccessibilityService::class.java,
        ).flattenToString()

        return enabledServices
            .split(':')
            .any { it.equals(expectedService, ignoreCase = true) }
    }
}
