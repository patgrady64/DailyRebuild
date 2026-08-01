package com.pgdevhouse.dailyrebuild

import android.content.Context
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository

/**
 * Compatibility wrapper used by appointment editing and restore flows.
 * Appointment reminders are now delivered by the shared Daily Rebuild
 * notification system so the master notification switch is always respected.
 */
object AppointmentReminderScheduler {
    const val CHANNEL_ID = DailyRebuildReminderScheduler.CHANNEL_APPOINTMENTS

    fun schedule(
        context: Context,
        appointment: CareAppointment
    ) {
        cancel(context, appointment.id)

        val preferences = AppPreferencesRepository(context).load()
        if (
            !preferences.notificationsEnabled ||
            !preferences.appointmentRemindersEnabled
        ) {
            return
        }

        ReminderPlanner.appointmentReminders(appointment).forEach { reminder ->
            DailyRebuildReminderScheduler.schedule(context, reminder)
        }
    }

    fun cancel(
        context: Context,
        appointmentId: Long
    ) {
        if (appointmentId <= 0L) return
        DailyRebuildReminderScheduler.cancel(
            context,
            ReminderPlanner.appointmentKey(appointmentId, "1d")
        )
        DailyRebuildReminderScheduler.cancel(
            context,
            ReminderPlanner.appointmentKey(appointmentId, "2h")
        )
    }
}
