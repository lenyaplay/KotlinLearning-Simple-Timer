package com.lenyaplay.simple.timer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFS_NAME = "app_prefs"

internal fun Context.timerSettings(): TimerSettings =
    TimerSettings(getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

internal class TimerSettings(private val prefs: SharedPreferences) {
    // Время которое прошло с запуска устройства при установке таймера
    var startElapsedMs: Long
        get() = prefs.getLong("start_elapsed_ms", -1L)
        set(value) = prefs.edit { putLong("start_elapsed_ms", value) }

    // На сколько поставли таймер в мс
    var totalDurationMs: Long
        get() = prefs.getLong("total_duration_millis_ms", -1L)
        set(value) = prefs.edit { putLong("total_duration_millis_ms", value) }

    // Время которое осталось если таймер на паузе
    var remainingDurationMs: Long
        get() = prefs.getLong("remaining_duration_millis_ms", -1L)
        set(value) = prefs.edit { putLong("remaining_duration_millis_ms", value) }

    // Таймер запущен/на паузе/остановлен
    var state: TimerState
        get() = prefs.getString("state", null)?.let {
            try {
                TimerState.valueOf(it)
            } catch (e: IllegalArgumentException) {
                TimerState.Idle
            }
        } ?: TimerState.Idle
        set(value) = prefs.edit { putString("state", value.name) }

    // Пользователь отказался выдавать разрешение "поверх других приложений"
    var overlayPermissionDeclined: Boolean
        get() = prefs.getBoolean("overlay_permission_declined", false)
        set(value) = prefs.edit { putBoolean("overlay_permission_declined", value) }
}
