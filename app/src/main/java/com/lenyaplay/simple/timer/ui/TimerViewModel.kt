package com.lenyaplay.simple.timer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenyaplay.simple.timer.data.AlarmScheduler
import com.lenyaplay.simple.timer.data.SharedPrefsTimerStorage
import com.lenyaplay.simple.timer.data.SystemAlarmScheduler
import com.lenyaplay.simple.timer.data.TimerSnapshot
import com.lenyaplay.simple.timer.data.TimerState
import com.lenyaplay.simple.timer.data.TimerStorage
import com.lenyaplay.simple.timer.data.TimerUiState
import com.lenyaplay.simple.timer.data.TICK_INTERVAL_MS
import com.lenyaplay.simple.timer.data.restoredTimerUiState
import com.lenyaplay.simple.timer.ui.components.parseDurationMs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerInputValues(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 15,
) {
    val durationMs: Long get() = parseDurationMs(hours, minutes, seconds)
}

class TimerViewModel(
    private val alarms: AlarmScheduler,
    private val storage: TimerStorage,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    var inputValues by mutableStateOf(TimerInputValues())

    // Версия команды "встать на значение". Барабаны доворачиваются по ее смене, а не по
    // смене самих чисел: числа они меняют и сами, когда их крутят
    var presetVersion by mutableIntStateOf(0)
        private set

    fun applyPreset(presetMinutes: Int) {
        inputValues = TimerInputValues(hours = 0, minutes = presetMinutes, seconds = 0)
        presetVersion++
    }

    private val _uiState = MutableStateFlow(TimerUiState(remainingDurationMs = 60_000L))
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        restoreState()
    }

    private fun restoreState() {
        val snapshot = storage.load()
        val restored = restoredTimerUiState(
            persistedState = snapshot.state,
            remainingDurationMs = snapshot.remainingDurationMs,
            totalDurationMs = snapshot.totalDurationMs,
            startElapsedMs = snapshot.startElapsedMs,
            nowElapsedMs = clock(),
        )

        if (restored == null) {
            // Таймер шел и уже сработал, пока приложение было закрыто - приводим
            // сохраненное состояние в порядок, само срабатывание Alarm уже обработал
            if (snapshot.state == TimerState.Running) storage.clear()
            return
        }

        _uiState.value = restored
        if (restored.state == TimerState.Running) {
            runJob(restored.remainingDurationMs)
        }
    }

    var overlayAsked: Boolean = false

    var overlayPermissionDeclined: Boolean
        get() = storage.overlayPermissionDeclined
        set(value) {
            storage.overlayPermissionDeclined = value
        }

    @SuppressLint("MissingPermission")
    fun onStartClick() {
        val delayInMs = inputValues.durationMs

        storage.save(
            TimerSnapshot(
                startElapsedMs = clock(),
                totalDurationMs = delayInMs,
                remainingDurationMs = delayInMs,
                state = TimerState.Running,
            )
        )

        alarms.schedule(delayInMs)

        _uiState.update {
            it.copy(
                remainingDurationMs = delayInMs,
                totalDurationMs = delayInMs,
                state = TimerState.Running
            )
        }
        runJob(remainingDurationMs = delayInMs)
    }

    fun runJob(remainingDurationMs: Long) {
        val start = clock()
        val end = start + remainingDurationMs
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                if (uiState.value.remainingDurationMs <= 0) {
                    _uiState.update {
                        it.copy(
                            remainingDurationMs = 0,
                            state = TimerState.Idle
                        )
                    }
                    storage.clear()
                    break
                }
                _uiState.update {
                    val newRemainingDurationMs = end - clock()
                    it.copy(remainingDurationMs = newRemainingDurationMs)
                }
                delay(TICK_INTERVAL_MS.toLong())
            }
        }
    }

    fun onPauseClick() {
        tickerJob?.cancel()
        _uiState.update { it.copy(state = TimerState.Paused) }

        alarms.cancel()

        storage.save(
            storage.load().copy(
                state = TimerState.Paused,
                remainingDurationMs = uiState.value.remainingDurationMs,
            )
        )
    }

    fun onResumeClick() {
        val remainingDurationMs = uiState.value.remainingDurationMs

        alarms.schedule(remainingDurationMs)
        runJob(remainingDurationMs)

        storage.save(
            storage.load().copy(
                startElapsedMs = clock(),
                remainingDurationMs = remainingDurationMs,
                state = TimerState.Running,
            )
        )
        _uiState.update { it.copy(state = TimerState.Running) }
    }

    fun onStopClick() {
        tickerJob?.cancel()
        _uiState.update { it.copy(state = TimerState.Idle) }

        alarms.cancel()
        storage.clear()
    }
}

fun timerViewModelFactory(context: Context): ViewModelProvider.Factory {
    val appContext = context.applicationContext
    return viewModelFactory {
        initializer {
            TimerViewModel(
                alarms = SystemAlarmScheduler(appContext),
                storage = SharedPrefsTimerStorage(appContext),
            )
        }
    }
}
