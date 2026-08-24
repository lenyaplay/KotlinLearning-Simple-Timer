package com.lenyaplay.simple.timer.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.lenyaplay.simple.timer.NotificationConstants
import com.lenyaplay.simple.timer.TimerFinishedText
import com.lenyaplay.simple.timer.trace
import com.lenyaplay.simple.timer.ui.theme.TimerForKotlinLearningTheme

class TimerFinishedActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockscreen()
        enableEdgeToEdge()
        startAlarmSound()

        setContent {
            TimerForKotlinLearningTheme {
                TimerFinishedScreen(onDismiss = {
                    stopAlarmSound()
                    // Уведомление открылось через full-screen intent, а не тапом по
                    // нему, поэтому setAutoCancel его не убирает - снимаем сами
                    NotificationManagerCompat.from(this@TimerFinishedActivity).cancel(
                        NotificationConstants.TIMER_FINISHED_ID
                    )
                    finish()
                })
            }
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }

    private fun startAlarmSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@TimerFinishedActivity, Settings.System.DEFAULT_ALARM_ALERT_URI)
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            // Нет доступного звука будильника (например на эмуляторе) - экран и так
            // выполняет свою роль, падать из-за отсутствия звука не нужно
            trace("ТаймерЗавершён") { "не удалось запустить звук: ${e.message}" }
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                // MediaPlayer мог не успеть подготовиться (prepareAsync асинхронный) -
                // stop() на неподготовленном плеере кидает исключение, это не ошибка
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun TimerFinishedScreen(onDismiss: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = TimerFinishedText.TITLE,
                    fontSize = 24.sp,
                )
                Text(
                    text = TimerFinishedText.MESSAGE,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            ) {
                Text(text = "Ок", fontSize = 20.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerFinishedScreenPreview() {
    TimerForKotlinLearningTheme {
        TimerFinishedScreen(onDismiss = {})
    }
}
