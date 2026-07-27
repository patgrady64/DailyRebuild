package com.pgdevhouse.dailyrebuild

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppointmentReminderScheduler {
    const val CHANNEL_ID = "care_appointment_reminders"
    const val EXTRA_APPOINTMENT_ID = "appointment_id"
    const val EXTRA_NOTIFICATION_TITLE = "notification_title"
    const val EXTRA_NOTIFICATION_TEXT = "notification_text"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    private const val ONE_DAY_CODE = 1
    private const val TWO_HOURS_CODE = 2
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val TWO_HOURS_MILLIS = 2L * 60L * 60L * 1000L

    fun schedule(
        context: Context,
        appointment: CareAppointment
    ) {
        cancel(context, appointment.id)

        if (
            appointment.id <= 0L ||
            appointment.status !in setOf("Scheduled", "Confirmed")
        ) {
            return
        }

        if (appointment.remindOneDayBefore) {
            scheduleOne(
                context = context,
                appointment = appointment,
                reminderCode = ONE_DAY_CODE,
                triggerAt = appointment.scheduledAt - ONE_DAY_MILLIS,
                title = "Appointment tomorrow"
            )
        }

        if (appointment.remindTwoHoursBefore) {
            scheduleOne(
                context = context,
                appointment = appointment,
                reminderCode = TWO_HOURS_CODE,
                triggerAt = appointment.scheduledAt - TWO_HOURS_MILLIS,
                title = "Appointment in about 2 hours"
            )
        }
    }

    fun cancel(
        context: Context,
        appointmentId: Long
    ) {
        if (appointmentId <= 0L) return

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(ONE_DAY_CODE, TWO_HOURS_CODE).forEach { code ->
            reminderPendingIntent(
                context = context,
                appointmentId = appointmentId,
                reminderCode = code,
                flags = PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
            )?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun scheduleOne(
        context: Context,
        appointment: CareAppointment,
        reminderCode: Int,
        triggerAt: Long,
        title: String
    ) {
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent =
            reminderPendingIntent(
                context = context,
                appointmentId = appointment.id,
                reminderCode = reminderCode,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
                title = title,
                text = reminderText(appointment),
                notificationId = notificationId(
                    appointment.id,
                    reminderCode
                )
            ) ?: return

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    private fun reminderPendingIntent(
        context: Context,
        appointmentId: Long,
        reminderCode: Int,
        flags: Int,
        title: String = "",
        text: String = "",
        notificationId: Int = 0
    ): PendingIntent? {
        val intent =
            Intent(
                context,
                AppointmentReminderReceiver::class.java
            ).apply {
                action =
                    "com.pgdevhouse.dailyrebuild.APPOINTMENT_REMINDER." +
                        appointmentId + "." + reminderCode
                putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
                putExtra(EXTRA_NOTIFICATION_TITLE, title)
                putExtra(EXTRA_NOTIFICATION_TEXT, text)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }

        return PendingIntent.getBroadcast(
            context,
            requestCode(appointmentId, reminderCode),
            intent,
            flags
        )
    }

    private fun requestCode(
        appointmentId: Long,
        reminderCode: Int
    ): Int =
        (((appointmentId % 100_000_000L) * 10L) + reminderCode)
            .toInt()

    private fun notificationId(
        appointmentId: Long,
        reminderCode: Int
    ): Int = requestCode(appointmentId, reminderCode)

    private fun reminderText(
        appointment: CareAppointment
    ): String {
        val dateTime =
            Instant.ofEpochMilli(appointment.scheduledAt)
                .atZone(ZoneId.systemDefault())

        val time =
            dateTime.format(
                DateTimeFormatter.ofPattern(
                    "h:mm a",
                    Locale.US
                )
            )

        val who =
            appointment.providerName
                .takeIf(String::isNotBlank)
                ?: appointment.visitCategory

        return "$who at ${appointment.placeName} · $time"
    }
}
