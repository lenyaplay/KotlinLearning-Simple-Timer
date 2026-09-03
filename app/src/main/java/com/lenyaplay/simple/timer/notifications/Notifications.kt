package com.lenyaplay.simple.timer.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lenyaplay.simple.timer.NotificationConstants
import com.lenyaplay.simple.timer.R

// Разрешение запрашивается в MainActivity.requestNotificationPermissionIfNeeded() -
// вызывающая сторона здесь (TimerReceiver/BootReceiver) не имеет доступа к его результату
@SuppressLint("MissingPermission")
fun showTimerNotification(
    context: Context,
    notificationId: Int,
    title: String,
    text: String,
    channelId: String = NotificationConstants.CHANNEL_ID,
    fullScreenIntent: PendingIntent? = null,
) {
    ensureChannel(context, channelId)

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    if (fullScreenIntent != null) {
        builder
            .setFullScreenIntent(fullScreenIntent, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
    }

    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
}

private fun ensureChannel(context: Context, channelId: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = context.getSystemService(NotificationManager::class.java)
    // Звук канала фиксируется при первом создании и не меняется повторным вызовом
    // createNotificationChannel, поэтому пересоздавать уже существующий канал не нужно
    if (manager.getNotificationChannel(channelId) != null) return

    val channel = if (channelId == NotificationConstants.ALARM_CHANNEL_ID) {
        NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, attributes)
            }
    } else {
        NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_default_name),
            NotificationManager.IMPORTANCE_HIGH
        )
    }
    manager.createNotificationChannel(channel)
}
