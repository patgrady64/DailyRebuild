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
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository

class DailyRebuildReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_COMMAND)
            ?: DailyRebuildReminderScheduler.COMMAND_DELIVER
        val notificationId = intent.getIntExtra(
            DailyRebuildReminderScheduler.EXTRA_NOTIFICATION_ID,
            9_001
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (command) {
            DailyRebuildReminderScheduler.COMMAND_DISMISS -> {
                notificationManager.cancel(notificationId)
                return
            }

            DailyRebuildReminderScheduler.COMMAND_SNOOZE -> {
                val kind = ReminderKind.fromId(
                    intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_KIND)
                ) ?: return
                val preferences = AppPreferencesRepository(context).load()
                if (!isEnabled(kind, preferences)) {
                    notificationManager.cancel(notificationId)
                    return
                }

                DailyRebuildReminderScheduler.scheduleSnooze(
                    context = context,
                    kind = kind,
                    title = intent.getStringExtra(
                        DailyRebuildReminderScheduler.EXTRA_TITLE
                    ) ?: "Daily Rebuild reminder",
                    text = intent.getStringExtra(
                        DailyRebuildReminderScheduler.EXTRA_TEXT
                    ) ?: "You have a reminder.",
                    occurrenceAt = intent.getLongExtra(
                        DailyRebuildReminderScheduler.EXTRA_OCCURRENCE_AT,
                        System.currentTimeMillis()
                    ),
                    minutes = preferences.notificationSnoozeMinutes,
                    notificationId = notificationId
                )
                notificationManager.cancel(notificationId)
                return
            }
        }

        val reminderKey = intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_KEY)
            ?: return
        DailyRebuildReminderScheduler.markDelivered(context, reminderKey)

        val kind = ReminderKind.fromId(
            intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_KIND)
        ) ?: return
        val preferences = AppPreferencesRepository(context).load()
        if (!isEnabled(kind, preferences)) return

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannels(notificationManager)

        val title = intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_TITLE)
            ?: "Daily Rebuild reminder"
        val text = intent.getStringExtra(DailyRebuildReminderScheduler.EXTRA_TEXT)
            ?: "You have a reminder."
        val occurrenceAt = intent.getLongExtra(
            DailyRebuildReminderScheduler.EXTRA_OCCURRENCE_AT,
            System.currentTimeMillis()
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            Intent(context, DailyRebuildReminderReceiver::class.java).apply {
                action = "com.pgdevhouse.dailyrebuild.REMINDER_SNOOZE.$notificationId"
                putExtra(
                    DailyRebuildReminderScheduler.EXTRA_COMMAND,
                    DailyRebuildReminderScheduler.COMMAND_SNOOZE
                )
                putExtra(DailyRebuildReminderScheduler.EXTRA_KIND, kind.id)
                putExtra(DailyRebuildReminderScheduler.EXTRA_TITLE, title)
                putExtra(DailyRebuildReminderScheduler.EXTRA_TEXT, text)
                putExtra(DailyRebuildReminderScheduler.EXTRA_OCCURRENCE_AT, occurrenceAt)
                putExtra(DailyRebuildReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            Intent(context, DailyRebuildReminderReceiver::class.java).apply {
                action = "com.pgdevhouse.dailyrebuild.REMINDER_DISMISS.$notificationId"
                putExtra(
                    DailyRebuildReminderScheduler.EXTRA_COMMAND,
                    DailyRebuildReminderScheduler.COMMAND_DISMISS
                )
                putExtra(DailyRebuildReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelFor(kind))
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(
                Notification.Action.Builder(
                    0,
                    "Snooze ${preferences.notificationSnoozeMinutes} min",
                    snoozeIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    0,
                    "Dismiss",
                    dismissIntent
                ).build()
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun isEnabled(
        kind: ReminderKind,
        preferences: com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
    ): Boolean {
        if (!preferences.notificationsEnabled) return false
        return when (kind) {
            ReminderKind.APPOINTMENT -> preferences.appointmentRemindersEnabled
            ReminderKind.MEETING -> preferences.meetingRemindersEnabled
            ReminderKind.IOP -> preferences.iopRemindersEnabled
            ReminderKind.IOP_ATTENDANCE_FOLLOW_UP ->
                preferences.iopAttendanceFollowUpEnabled
        }
    }

    private fun channelFor(kind: ReminderKind): String = when (kind) {
        ReminderKind.APPOINTMENT -> DailyRebuildReminderScheduler.CHANNEL_APPOINTMENTS
        ReminderKind.MEETING -> DailyRebuildReminderScheduler.CHANNEL_MEETINGS
        ReminderKind.IOP,
        ReminderKind.IOP_ATTENDANCE_FOLLOW_UP -> DailyRebuildReminderScheduler.CHANNEL_IOP
    }

    private fun createChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        listOf(
            NotificationChannel(
                DailyRebuildReminderScheduler.CHANNEL_APPOINTMENTS,
                "Appointment reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Optional reminders for scheduled care appointments."
            },
            NotificationChannel(
                DailyRebuildReminderScheduler.CHANNEL_MEETINGS,
                "Recovery meeting reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional reminders for saved recurring recovery meetings."
            },
            NotificationChannel(
                DailyRebuildReminderScheduler.CHANNEL_IOP,
                "IOP group reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional IOP start reminders and attendance follow-up notices."
            }
        ).forEach(notificationManager::createNotificationChannel)
    }
}
