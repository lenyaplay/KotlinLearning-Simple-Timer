package com.lenyaplay.simple.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerInputState(application: Application) : AndroidViewModel(application) {
    val hours = TextFieldState(initialText = "00")
    val minutes = TextFieldState(initialText = "00")
    val seconds = TextFieldState(initialText = "15")

    private val _uiState = MutableStateFlow(TimerUiState(remainingMs = 60_000L))
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    private fun setAlarm(context: Context, delayInMs: Long) {
        val intent = Intent(context, TimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + delayInMs

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent
        )
    }

    @SuppressLint("MissingPermission")
    fun onStartClick() {
        // Парсинг данных
        val h = hours.text.toString().toLongOrNull() ?: 0L
        val m = minutes.text.toString().toLongOrNull() ?: 0L
        val s = seconds.text.toString().toLongOrNull() ?: 0L
        val delayInMs = (h.hours + m.minutes + s.seconds).inWholeMilliseconds

        val context = getApplication<Application>()

        // Сохранение в Shared Preferences
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val timerSettings = TimerSettings(sharedPreferences)
        timerSettings.startTimeUtcInMs = System.currentTimeMillis()
        timerSettings.delayInMs = delayInMs

        val endTimeInMs = System.currentTimeMillis() + delayInMs

        setAlarm(context, delayInMs)

        // Работа со счетчиком
        _uiState.update {
            it.copy(
                remainingMs = delayInMs,
                totalMs = delayInMs,
                state = TimerState.Running
            )
        }
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = endTimeInMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.update { it.copy(remainingMs = 0, state = TimerState.Finished) }
                    break
                }
                _uiState.update { it.copy(remainingMs = remaining) }
                delay(200)
            }
        }
    }
}


@Composable
fun TimeInput(
    modifier: Modifier = Modifier,
    hours: TextFieldState,
    minutes: TextFieldState,
    seconds: TextFieldState
) {
    Row(modifier = modifier) {
        BasicTextField(
            state = hours,
            textStyle = TextStyle(
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
            modifier = Modifier
                .border(border = BorderStroke(2.dp, Color.Black))
                .padding(5.dp)
                .width(60.dp)
        )
        BasicTextField(
            state = minutes,
            textStyle = TextStyle(
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
            modifier = Modifier
                .border(border = BorderStroke(2.dp, Color.Black))
                .padding(5.dp)
                .width(60.dp)
        )
        BasicTextField(
            state = seconds,
            textStyle = TextStyle(
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
            modifier = Modifier
                .border(border = BorderStroke(2.dp, Color.Black))
                .padding(5.dp)
                .width(60.dp)
        )
    }
}