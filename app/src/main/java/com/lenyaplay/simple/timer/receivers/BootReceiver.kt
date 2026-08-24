package com.lenyaplay.simple.timer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lenyaplay.simple.timer.NotificationConstants
import com.lenyaplay.simple.timer.data.TimerState
import com.lenyaplay.simple.timer.data.timerSettings
import com.lenyaplay.simple.timer.notifications.showTimerNotification

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val timerSettings = context.timerSettings()

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
