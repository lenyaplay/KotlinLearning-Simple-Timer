package com.lenyaplay.simple.timer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lenyaplay.simple.timer.NotificationConstants
import com.lenyaplay.simple.timer.R
import com.lenyaplay.simple.timer.data.SharedPrefsTimerStorage
import com.lenyaplay.simple.timer.data.TimerState
import com.lenyaplay.simple.timer.notifications.showTimerNotification

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val storage = SharedPrefsTimerStorage(context)
        val snapshot = storage.load()

        // Таймер считается установленным, если он шел или стоял на паузе
        if (snapshot.state != TimerState.Running && snapshot.state != TimerState.Paused) return

        showTimerNotification(
            context = context,
            notificationId = NotificationConstants.TIMER_RESET_ID,
            title = context.getString(R.string.boot_reset_notification_title),
            text = context.getString(R.string.boot_reset_notification_message),
        )

        // Alarm'а после перезагрузки уже нет, а повторно уведомлять при следующей
        // загрузке не надо
        storage.clear()
    }
}
