package com.lenyaplay.simple.timer.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.lenyaplay.simple.timer.NotificationConstants
import com.lenyaplay.simple.timer.TimerFinishedText
import com.lenyaplay.simple.timer.trace
import com.lenyaplay.simple.timer.data.SharedPrefsTimerStorage
import com.lenyaplay.simple.timer.notifications.showTimerNotification
import com.lenyaplay.simple.timer.ui.TimerFinishedActivity


class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Срабатывание Alarm - источник истины о завершении таймера, поэтому хранилище
        // очищается прямо здесь, а не полагается на то, что открытый экран сам досчитает до нуля
        SharedPrefsTimerStorage(context).clear()

        val activityIntent = Intent(context, TimerFinishedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)

        if (isAppInForeground) {
            // Приложение и так на экране - уведомление избыточно, открываем сразу
            trace("ТаймерЗавершён") { "приложение открыто, уведомление пропущено" }
            context.startActivity(activityIntent)
        } else {
            showTimerFinishedNotification(context, activityIntent)
        }
    }

    private fun showTimerFinishedNotification(context: Context, activityIntent: Intent) {
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            NotificationConstants.FULL_SCREEN_REQUEST_CODE,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showTimerNotification(
            context = context,
            notificationId = NotificationConstants.TIMER_FINISHED_ID,
            title = TimerFinishedText.TITLE,
            text = TimerFinishedText.MESSAGE,
            channelId = NotificationConstants.ALARM_CHANNEL_ID,
            fullScreenIntent = fullScreenIntent,
        )

        // Разрешение "поверх других приложений" снимает запрет на запуск Activity из фона,
        // и экран показывается даже когда пользователь занят другим приложением.
        // Без него остается full screen intent - он разворачивается только на
        // заблокированном экране
        if (Settings.canDrawOverlays(context)) {
            context.startActivity(activityIntent)
        }
    }
}
