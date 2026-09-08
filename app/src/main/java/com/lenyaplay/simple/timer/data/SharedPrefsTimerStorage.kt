package com.lenyaplay.simple.timer.data

import android.content.Context

internal class SharedPrefsTimerStorage(context: Context) : TimerStorage {
    private val prefs = context.timerSettings()

    override fun save(snapshot: TimerSnapshot) = prefs.write(snapshot)
    
    override fun load(): TimerSnapshot = prefs.read()

    override fun clear() {
        val snapshot = TimerSnapshot(0L, 0L, 0L, TimerState.Idle)
        prefs.write(snapshot)
    }

    override var overlayPermissionDeclined: Boolean
        get() = prefs.overlayPermissionDeclined
        set(value) {
            prefs.overlayPermissionDeclined = value
        }
}
