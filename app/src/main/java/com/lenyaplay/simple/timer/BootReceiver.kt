package com.lenyaplay.simple.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val timerSettings = TimerSettings(sharedPreferences)

        // Таймер считается установленным, если он шел или стоял на паузе
        if (timerSettings.state != TimerState.Running &&
            timerSettings.state != TimerState.Paused
        ) return

        showTimerNotification(
            context = context,
            notificationId = NotificationConstants.TIMER_RESET_ID,
            title = "Таймер сброшен",
            text = "Таймер был остановлен после перезагрузки. Запустите новый!",
        )

        // Alarm'а после перезагрузки уже нет, а повторно уведомлять при следующей
        // загрузке не надо
        timerSettings.state = TimerState.Idle
        timerSettings.startElapsedMs = 0
        timerSettings.totalDurationMs = 0
        timerSettings.remainingDurationMs = 0
    }
}
