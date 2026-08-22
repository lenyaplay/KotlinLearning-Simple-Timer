package com.lenyaplay.simple.timer;

import android.content.SharedPreferences

internal class TimerSettings(prefs: SharedPreferences) {
    // Время которое прошло с запуска устройства при установке таймера
    var startElapsedMs: Long by prefs.long("start_elapsed_ms", -1L)

    // На сколько поставли таймер в мс
    var totalDurationMs: Long by prefs.long("total_duration_millis_ms", -1L)

    // Время которое осталось если таймер на паузе
    var remainingDurationMs: Long by prefs.long("remaining_duration_millis_ms", -1L)

    // Таймер запущен/на паузе/остановлен
    var state: TimerState by prefs.enum("state", TimerState.Idle, TimerState::class.java)
}
