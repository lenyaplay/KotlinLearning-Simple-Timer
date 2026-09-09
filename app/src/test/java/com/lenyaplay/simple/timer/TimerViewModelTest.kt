package com.lenyaplay.simple.timer

import com.lenyaplay.simple.timer.data.AlarmScheduler
import com.lenyaplay.simple.timer.data.TICK_INTERVAL_MS
import com.lenyaplay.simple.timer.data.TimerSnapshot
import com.lenyaplay.simple.timer.data.TimerState
import com.lenyaplay.simple.timer.data.TimerStorage
import com.lenyaplay.simple.timer.data.TimerUiState
import com.lenyaplay.simple.timer.ui.TimerInputValues
import com.lenyaplay.simple.timer.ui.TimerViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeAlarmScheduler : AlarmScheduler {
    var scheduledAfterMs: Long? = null
    var cancelCallCount = 0

    override fun schedule(afterMs: Long) {
        scheduledAfterMs = afterMs
    }

    override fun cancel() {
        cancelCallCount++
    }
}

private class FakeTimerStorage(
    initial: TimerSnapshot = TimerSnapshot(0L, 0L, 0L, TimerState.Idle),
) : TimerStorage {
    var snapshot = initial
    var clearCallCount = 0
    override var overlayPermissionDeclined: Boolean = false

    override fun save(snapshot: TimerSnapshot) {
        this.snapshot = snapshot
    }

    override fun load(): TimerSnapshot = snapshot

    override fun clear() {
        clearCallCount++
        snapshot = TimerSnapshot(0L, 0L, 0L, TimerState.Idle)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startClickSchedulesAlarmAndSavesRunningSnapshot() = runTest(dispatcher) {
        val alarms = FakeAlarmScheduler()
        val storage = FakeTimerStorage()
        val vm = TimerViewModel(alarms, storage, clock = { 0L })
        try {
            vm.inputValues = TimerInputValues(hours = 0, minutes = 1, seconds = 0)

            vm.onStartClick()

            assertEquals(60_000L, alarms.scheduledAfterMs)
            assertEquals(
                TimerSnapshot(
                    startElapsedMs = 0L,
                    totalDurationMs = 60_000L,
                    remainingDurationMs = 60_000L,
                    state = TimerState.Running,
                ),
                storage.snapshot,
            )
            assertEquals(TimerState.Running, vm.uiState.value.state)
        } finally {
            vm.onStopClick()
        }
    }

    /**
     * На паузе должно сохраняться уже уменьшенное время, а не исходную длительность,
     * заданную при старте
     */
    @Test
    fun pauseClickCancelsAlarmAndSavesRemainingTime() = runTest(dispatcher) {
        var fakeNow = 0L
        val alarms = FakeAlarmScheduler()
        val storage = FakeTimerStorage()
        val vm = TimerViewModel(alarms, storage, clock = { fakeNow })
        try {
            vm.inputValues = TimerInputValues(hours = 0, minutes = 1, seconds = 0)
            vm.onStartClick()

            fakeNow = 20_000L
            dispatcher.scheduler.advanceTimeBy(TICK_INTERVAL_MS.toLong() + 10)
            runCurrent()
            vm.onPauseClick()

            assertEquals(1, alarms.cancelCallCount)
            assertEquals(TimerState.Paused, storage.snapshot.state)
            assertEquals(40_000L, storage.snapshot.remainingDurationMs)
        } finally {
            vm.onStopClick()
        }
    }

    @Test
    fun resumeClickReschedulesAlarmForRemainingTime() = runTest(dispatcher) {
        var fakeNow = 0L
        val alarms = FakeAlarmScheduler()
        val storage = FakeTimerStorage()
        val vm = TimerViewModel(alarms, storage, clock = { fakeNow })
        try {
            vm.inputValues = TimerInputValues(hours = 0, minutes = 1, seconds = 0)
            vm.onStartClick()
            fakeNow = 20_000L
            dispatcher.scheduler.advanceTimeBy(TICK_INTERVAL_MS.toLong() + 10)
            runCurrent()
            vm.onPauseClick()
            alarms.scheduledAfterMs = null

            vm.onResumeClick()

            assertEquals(40_000L, alarms.scheduledAfterMs)
            assertEquals(TimerState.Running, vm.uiState.value.state)
        } finally {
            vm.onStopClick()
        }
    }

    @Test
    fun tickerFinishesTimerWhenTimeIsUp() = runTest(dispatcher) {
        var fakeNow = 0L
        val alarms = FakeAlarmScheduler()
        val storage = FakeTimerStorage()
        val vm = TimerViewModel(alarms, storage, clock = { fakeNow })
        try {
            vm.inputValues = TimerInputValues(hours = 0, minutes = 0, seconds = 1)
            var finishedEventsReceived = 0
            backgroundScope.launch { vm.timerFinishedEvents.collect { finishedEventsReceived++ } }

            vm.onStartClick()
            fakeNow = 1_000L
            dispatcher.scheduler.advanceTimeBy(TICK_INTERVAL_MS.toLong() + 10)
            runCurrent()

            assertEquals(TimerUiState(0L, 1_000L, TimerState.Idle), vm.uiState.value)
            assertEquals(1, storage.clearCallCount)
            assertEquals(1, finishedEventsReceived)
        } finally {
            vm.onStopClick()
        }
    }

    /**
     * Регрессия на баг: раньше `startTicker` проверял на "конец таймера" ещё не пересчитанное
     * (старое) значение remainingDurationMs, поэтому переход в Idle происходил на тик позже,
     * чем время реально истекло
     */
    @Test
    fun tickerDoesNotLingerOneExtraTickAfterTimeIsUp() = runTest(dispatcher) {
        var fakeNow = 0L
        val alarms = FakeAlarmScheduler()
        val storage = FakeTimerStorage()
        val vm = TimerViewModel(alarms, storage, clock = { fakeNow })
        try {
            vm.inputValues = TimerInputValues(hours = 0, minutes = 0, seconds = 1)

            vm.onStartClick()
            // Время истекло уже к моменту первой же проверки внутри цикла тикера. Намеренно
            // без advanceTimeBy: должна выполниться РОВНО одна итерация цикла - тест на баг,
            // из-за которого переход в Idle происходил только на СЛЕДУЮЩЕЙ итерации
            fakeNow = 5_000L
            runCurrent()

            assertEquals(TimerState.Idle, vm.uiState.value.state)
        } finally {
            vm.onStopClick()
        }
    }

    @Test
    fun restoreRunningReducesRemainingByElapsedTime() = runTest(dispatcher) {
        val storage = FakeTimerStorage(
            initial = TimerSnapshot(
                startElapsedMs = 1_000L,
                totalDurationMs = 60_000L,
                remainingDurationMs = 60_000L,
                state = TimerState.Running,
            )
        )
        val vm = TimerViewModel(FakeAlarmScheduler(), storage, clock = { 21_000L })
        try {
            assertEquals(TimerState.Running, vm.uiState.value.state)
            assertEquals(40_000L, vm.uiState.value.remainingDurationMs)
        } finally {
            vm.onStopClick()
        }
    }

    @Test
    fun restoreExpiredRunningClearsStorage() = runTest(dispatcher) {
        val storage = FakeTimerStorage(
            initial = TimerSnapshot(
                startElapsedMs = 0L,
                totalDurationMs = 10_000L,
                remainingDurationMs = 10_000L,
                state = TimerState.Running,
            )
        )
        val vm = TimerViewModel(FakeAlarmScheduler(), storage, clock = { 999_999L })
        try {
            // Когда восстанавливать нечего, uiState остаётся дефолтным значением ViewModel
            // (TimerUiState(remainingDurationMs = 60_000L)), а не TimerUiState() с нулями
            assertEquals(TimerUiState(remainingDurationMs = 60_000L), vm.uiState.value)
            assertEquals(1, storage.clearCallCount)
        } finally {
            vm.onStopClick()
        }
    }
}
