package com.lenyaplay.simple.timer

enum class TimerState {
    Idle,
    Paused,
    Running,
    Finished,
}

data class TimerUiState(
    val remainingDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val state: TimerState = TimerState.Idle,
)