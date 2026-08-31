package com.lenyaplay.simple.timer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.lenyaplay.simple.timer.AlarmConstants
import com.lenyaplay.simple.timer.receivers.TimerReceiver

internal class SystemAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            AlarmConstants.TIMER_REQUEST_CODE,
            Intent(context, TimerReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    override fun schedule(afterMs: Long) {
        val triggerAtMillis = SystemClock.elapsedRealtime() + afterMs

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent
        )
    }

    override fun cancel() {
        val pendingIntent = pendingIntent

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
