package com.lenyaplay.simple.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lenyaplay.simple.timer.ui.components.parseDurationMs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerInputState(application: Application) : AndroidViewModel(application) {
    var hours by mutableIntStateOf(0)
    var minutes by mutableIntStateOf(0)
    var seconds by mutableIntStateOf(15)

    // Версия команды "встать на значение". Барабаны доворачиваются по ее смене, а не по
    // смене самих чисел: числа они меняют и сами, когда их крутят
    var presetVersion by mutableIntStateOf(0)
        private set

    fun applyPreset(presetMinutes: Int) {
        hours = 0
        minutes = presetMinutes
        seconds = 0
        presetVersion++
    }

    private val _uiState = MutableStateFlow(TimerUiState(remainingDurationMs = 60_000L))
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    private val timerSettings by lazy {
        TimerSettings(
            getApplication<Application>()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        )
    }

    init {
        restoreState()
    }

    private fun restoreState() {
        val restored = restoredTimerUiState(
            persistedState = timerSettings.state,
            remainingDurationMs = timerSettings.remainingDurationMs,
            totalDurationMs = timerSettings.totalDurationMs,
            startElapsedMs = timerSettings.startElapsedMs,
            nowElapsedMs = SystemClock.elapsedRealtime(),
        )

        if (restored == null) {
            // Таймер шел и уже сработал, пока приложение было закрыто - приводим
            // сохраненное состояние в порядок, само срабатывание Alarm уже обработал
            if (timerSettings.state == TimerState.Running) resetTimerSettings()
            return
        }

        _uiState.value = restored
        if (restored.state == TimerState.Running) {
            runJob(restored.remainingDurationMs)
        }
    }

    private fun timerPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            AlarmConstants.TIMER_REQUEST_CODE,
            Intent(context, TimerReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setAlarm(context: Context, remainingDurationMs: Long) {
        val pendingIntent = timerPendingIntent(context)
        val triggerAtMillis = SystemClock.elapsedRealtime() + remainingDurationMs

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent
        )
    }

    private fun cancelAlarm(context: Context) {
        val pendingIntent = timerPendingIntent(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    var overlayAsked: Boolean = false

    var overlayPermissionDeclined: Boolean
        get() = timerSettings.overlayPermissionDeclined
        set(value) {
            timerSettings.overlayPermissionDeclined = value
        }

    @SuppressLint("MissingPermission")
    fun onStartClick() {
        // Парсинг данных
        val delayInMs = parseDurationMs(hours, minutes, seconds)

        val context = getApplication<Application>()

        // Сохранение в Shared Preferences
        timerSettings.startElapsedMs = SystemClock.elapsedRealtime()
        timerSettings.remainingDurationMs = delayInMs
        timerSettings.totalDurationMs = delayInMs
        timerSettings.state = TimerState.Running

        setAlarm(context, delayInMs)

        // Работа со счетчиком
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
        val start = SystemClock.elapsedRealtime()
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
                    resetTimerSettings()
                    break
                }
                _uiState.update {
                    val newRemainingDurationMs = end - SystemClock.elapsedRealtime()
                    it.copy(remainingDurationMs = newRemainingDurationMs)
                }
                delay(TICK_INTERVAL_MS.toLong())
            }
        }
    }

    fun onPauseClick() {
        tickerJob?.cancel()
        _uiState.update { it.copy(state = TimerState.Paused) }

        cancelAlarm(getApplication())

        timerSettings.state = TimerState.Paused
        timerSettings.remainingDurationMs = uiState.value.remainingDurationMs
    }

    fun onResumeClick() {
        val remainingDurationMs = uiState.value.remainingDurationMs

        setAlarm(getApplication(), remainingDurationMs)
        runJob(remainingDurationMs)

        timerSettings.startElapsedMs = SystemClock.elapsedRealtime()
        timerSettings.remainingDurationMs = remainingDurationMs
        timerSettings.state = TimerState.Running
        _uiState.update { it.copy(state = TimerState.Running) }
    }

    fun onStopClick() {
        tickerJob?.cancel()
        _uiState.update { it.copy(state = TimerState.Idle) }

        cancelAlarm(getApplication())

        resetTimerSettings()
    }

    private fun resetTimerSettings() {
        timerSettings.state = TimerState.Idle
        timerSettings.remainingDurationMs = 0
        timerSettings.totalDurationMs = 0
        timerSettings.startElapsedMs = 0
    }
}
