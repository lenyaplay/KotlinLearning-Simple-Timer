package com.lenyaplay.simple.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerRestoreTest {

    @Test
    fun idleRestoresNothing() {
        val restored = restoredTimerUiState(
            persistedState = TimerState.Idle,
            remainingDurationMs = 0,
            totalDurationMs = 0,
            startElapsedMs = 0,
            nowElapsedMs = 1_000,
        )

        assertNull(restored)
    }

    @Test
    fun pausedRestoresExactSavedValue() {
        val restored = restoredTimerUiState(
            persistedState = TimerState.Paused,
            remainingDurationMs = 42_000,
            totalDurationMs = 90_000,
            startElapsedMs = 500,
            // На паузе время не идет, nowElapsedMs не должен влиять на результат
            nowElapsedMs = 999_999,
        )

        assertEquals(
            TimerUiState(
                remainingDurationMs = 42_000,
                totalDurationMs = 90_000,
                state = TimerState.Paused,
            ),
            restored,
        )
    }

    @Test
    fun runningRestoresWithElapsedTimeSubtracted() {
        val restored = restoredTimerUiState(
            persistedState = TimerState.Running,
            remainingDurationMs = 60_000,
            totalDurationMs = 60_000,
            startElapsedMs = 1_000,
            nowElapsedMs = 21_000,
        )

        assertEquals(
            TimerUiState(
                remainingDurationMs = 40_000,
                totalDurationMs = 60_000,
                state = TimerState.Running,
            ),
            restored,
        )
    }

    @Test
    fun runningAlreadyFinishedRestoresNothing() {
        val exactlyFinished = restoredTimerUiState(
            persistedState = TimerState.Running,
            remainingDurationMs = 10_000,
            totalDurationMs = 10_000,
            startElapsedMs = 0,
            // Прошло ровно столько же, сколько было остатка
            nowElapsedMs = 10_000,
        )
        val longAfterFinish = restoredTimerUiState(
            persistedState = TimerState.Running,
            remainingDurationMs = 10_000,
            totalDurationMs = 10_000,
            startElapsedMs = 0,
            nowElapsedMs = 999_999,
        )

        assertNull(exactlyFinished)
        assertNull(longAfterFinish)
    }
}
