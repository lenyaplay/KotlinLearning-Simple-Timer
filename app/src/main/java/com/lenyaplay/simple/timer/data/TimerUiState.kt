package com.lenyaplay.simple.timer.data

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

/**
 * Состояние счетчика при открытии приложения, по данным из TimerSettings.
 * null - восстанавливать нечего: было Idle, либо таймер уже сработал, пока
 * приложение было закрыто (Alarm показал уведомление сам)
 */
fun restoredTimerUiState(
    persistedState: TimerState,
    remainingDurationMs: Long,
    totalDurationMs: Long,
    startElapsedMs: Long,
    nowElapsedMs: Long,
): TimerUiState? = when (persistedState) {
    TimerState.Idle -> null
    TimerState.Paused -> TimerUiState(remainingDurationMs, totalDurationMs, TimerState.Paused)
    TimerState.Running -> {
        val actualRemaining = remainingDurationMs - (nowElapsedMs - startElapsedMs)
        if (actualRemaining > 0) {
            TimerUiState(actualRemaining, totalDurationMs, TimerState.Running)
        } else {
            null
        }
    }
}