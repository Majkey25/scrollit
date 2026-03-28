package cz.teply.scrollit

import android.content.Context

object ScrollSettingsStore {
    private const val PREF_NAME = "scrollit_settings"
    private const val KEY_DISTANCE_PERCENT = "distance_percent"
    private const val KEY_INTERVAL_MS = "interval_ms"
    private const val KEY_GESTURE_DURATION_MS = "gesture_duration_ms"

    fun load(context: Context): ScrollSettings {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val defaults = ScrollSettings.defaults
        return ScrollSettings(
            distancePercent = prefs.getInt(KEY_DISTANCE_PERCENT, defaults.distancePercent)
                .coerceIn(ScrollSettings.MIN_DISTANCE_PERCENT, ScrollSettings.MAX_DISTANCE_PERCENT),
            intervalMs = prefs.getInt(KEY_INTERVAL_MS, defaults.intervalMs)
                .coerceIn(ScrollSettings.MIN_INTERVAL_MS, ScrollSettings.MAX_INTERVAL_MS),
            gestureDurationMs = prefs.getInt(KEY_GESTURE_DURATION_MS, defaults.gestureDurationMs)
                .coerceIn(ScrollSettings.MIN_GESTURE_DURATION_MS, ScrollSettings.MAX_GESTURE_DURATION_MS),
        )
    }

    fun save(context: Context, settings: ScrollSettings) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISTANCE_PERCENT, settings.distancePercent)
            .putInt(KEY_INTERVAL_MS, settings.intervalMs)
            .putInt(KEY_GESTURE_DURATION_MS, settings.gestureDurationMs)
            .apply()
    }
}
