package com.lenyaplay.simple.timer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showTimerFinishedNotification(context)
    }

    private fun showTimerFinishedNotification(context: Context) {
        val activityIntent = Intent(context, TimerFinishedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            NotificationConstants.FULL_SCREEN_REQUEST_CODE,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showTimerNotification(
            context = context,
            notificationId = NotificationConstants.TIMER_FINISHED_ID,
            title = "Таймер завершён",
            text = "Время вышло!",
            fullScreenIntent = fullScreenIntent,
        )
    }
}
