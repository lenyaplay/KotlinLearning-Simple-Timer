package com.lenyaplay.simple.timer

data class TimerUiState(
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val isRunning: Boolean = false,
)