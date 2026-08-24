package com.lenyaplay.simple.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showTimerFinishedNotification(context)
    }

    private fun showTimerFinishedNotification(context: Context) {
        showTimerNotification(
            context = context,
            notificationId = NotificationConstants.TIMER_FINISHED_ID,
            title = "Таймер завершён",
            text = "Время вышло!",
        )
    }
}
