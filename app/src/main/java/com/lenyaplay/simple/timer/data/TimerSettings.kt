package com.lenyaplay.simple.timer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Timer

private const val PREFS_NAME = "app_prefs"

// Время, которое прошло с запуска устройства при установке таймера
private const val KEY_START_ELAPSED_MS = "start_elapsed_ms"

// На сколько поставли таймер в мс
private const val KEY_TOTAL_DURATION_MS = "total_duration_millis_ms"

// Время, которое осталось если таймер на паузе
private const val KEY_REMAINING_DURATION_MS = "remaining_duration_millis_ms"

// Таймер запущен/на паузе/остановлен
private const val KEY_STATE = "state"
internal fun Context.timerSettings(): TimerSettings =
    TimerSettings(getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

internal class TimerSettings(private val prefs: SharedPreferences) {
    // Пользователь отказался выдавать разрешение "поверх других приложений"
    var overlayPermissionDeclined: Boolean
        get() = prefs.getBoolean("overlay_permission_declined", false)
        set(value) = prefs.edit { putBoolean("overlay_permission_declined", value) }

    fun read(): TimerSnapshot = TimerSnapshot(
        startElapsedMs = prefs.getLong(KEY_START_ELAPSED_MS, -1L),
        totalDurationMs = prefs.getLong(KEY_TOTAL_DURATION_MS, -1L),
        remainingDurationMs = prefs.getLong(KEY_REMAINING_DURATION_MS, -1L),
        state = prefs.getString(KEY_STATE, null)?.let {
            try {
                TimerState.valueOf(it)
            } catch (e: IllegalArgumentException) {
                TimerState.Idle
            }
        } ?: TimerState.Idle,
    )

    fun write(snapshot: TimerSnapshot) = prefs.edit {
        putLong(KEY_START_ELAPSED_MS, snapshot.startElapsedMs)
        putLong(KEY_TOTAL_DURATION_MS, snapshot.totalDurationMs)
        putLong(KEY_REMAINING_DURATION_MS, snapshot.remainingDurationMs)
        putString(KEY_STATE, snapshot.state.name)
    }
}
