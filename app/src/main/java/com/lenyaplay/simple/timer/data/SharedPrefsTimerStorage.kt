package com.lenyaplay.simple.timer.data

import android.content.Context

internal class SharedPrefsTimerStorage(context: Context) : TimerStorage {
    private val prefs = context.timerSettings()

    override fun save(snapshot: TimerSnapshot) {
        prefs.startElapsedMs = snapshot.startElapsedMs
        prefs.totalDurationMs = snapshot.totalDurationMs
        prefs.remainingDurationMs = snapshot.remainingDurationMs
        prefs.state = snapshot.state
    }

    override fun load(): TimerSnapshot = TimerSnapshot(
        startElapsedMs = prefs.startElapsedMs,
        totalDurationMs = prefs.totalDurationMs,
        remainingDurationMs = prefs.remainingDurationMs,
        state = prefs.state,
    )

    override fun clear() {
        save(TimerSnapshot(0L, 0L, 0L, TimerState.Idle))
    }

    override var overlayPermissionDeclined: Boolean
        get() = prefs.overlayPermissionDeclined
        set(value) {
            prefs.overlayPermissionDeclined = value
        }
}
