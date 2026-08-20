package com.lenyaplay.simple.timer

enum class TimerState {
    Paused,
    Running,
    Finished,
}

data class TimerUiState(
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val state: TimerState = TimerState.Running,
)