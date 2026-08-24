package com.lenyaplay.simple.timer

/**
 * Период обновления счетчика. Дуга прогресса анимируется ровно на это же время: иначе
 * анимация не успевает завершиться до следующего обновления и движение идет рывками
 */
const val TICK_INTERVAL_MS = 200

enum class TimerState {
    Idle,
    Paused,
    Running,
}

data class TimerUiState(
    val remainingDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val state: TimerState = TimerState.Idle,
)