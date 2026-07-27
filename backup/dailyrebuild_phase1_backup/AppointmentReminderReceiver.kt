package com.pgdevhouse.dailyrebuild

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    AppointmentReminderScheduler.CHANNEL_ID,
                    "Care appointment reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Reminders for scheduled medical, vision, dental, and other care appointments."
                }
            )
        }

        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentIntent =
            PendingIntent.getActivity(
                context,
                9001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val title =
            intent.getStringExtra(
                AppointmentReminderScheduler.EXTRA_NOTIFICATION_TITLE
            ) ?: "Upcoming appointment"

        val text =
            intent.getStringExtra(
                AppointmentReminderScheduler.EXTRA_NOTIFICATION_TEXT
            ) ?: "You have an upcoming care appointment."

        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(
                    context,
                    AppointmentReminderScheduler.CHANNEL_ID
                )
            } else {
                Notification.Builder(context)
            }.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(
                    Notification.BigTextStyle()
                        .bigText(text)
                )
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build()

        val notificationId =
            intent.getIntExtra(
                AppointmentReminderScheduler.EXTRA_NOTIFICATION_ID,
                9001
            )

        notificationManager.notify(
            notificationId,
            notification
        )
    }
}
