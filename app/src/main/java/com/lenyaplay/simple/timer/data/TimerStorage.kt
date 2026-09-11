package com.lenyaplay.simple.timer.data

data class TimerSnapshot(
    val startElapsedMs: Long,
    val totalDurationMs: Long,
    val remainingDurationMs: Long,
    val state: TimerState,
)

interface TimerStorage {
    fun save(snapshot: TimerSnapshot)
    fun load(): TimerSnapshot
    fun clear()

    var overlayPermissionDeclined: Boolean
}
