package com.pgdevhouse.dailyrebuild

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class ReminderKind(val id: String) {
    APPOINTMENT("appointment"),
    MEETING("meeting"),
    IOP("iop"),
    IOP_ATTENDANCE_FOLLOW_UP("iop_follow_up");

    companion object {
        fun fromId(id: String?): ReminderKind? = entries.firstOrNull { it.id == id }
    }
}

internal data class PlannedReminder(
    val key: String,
    val kind: ReminderKind,
    val triggerAt: Long,
    val occurrenceAt: Long,
    val title: String,
    val text: String,
    val sourceLabel: String
)

internal object ReminderPlanner {
    private const val HORIZON_DAYS = 56L
    private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val TWO_HOURS_MILLIS = 2L * 60L * 60L * 1000L

    fun plan(
        preferences: DailyRebuildPreferences,
        appointments: List<CareAppointment>,
        meetings: List<SavedMeeting>,
        iopGroups: List<IopGroup>,
        now: Long = System.currentTimeMillis()
    ): List<PlannedReminder> {
        if (!preferences.notificationsEnabled) return emptyList()

        val planned = mutableListOf<PlannedReminder>()

        if (preferences.appointmentRemindersEnabled) {
            appointments.forEach { appointment ->
                planned += appointmentReminders(appointment, now)
            }
        }

        if (preferences.meetingRemindersEnabled) {
            meetings.filter { it.active }.forEach { meeting ->
                planned += recurringMeetingReminders(
                    meeting = meeting,
                    leadMinutes = preferences.meetingReminderLeadMinutes,
                    now = now
                )
            }
        }

        if (preferences.iopRemindersEnabled || preferences.iopAttendanceFollowUpEnabled) {
            iopGroups.filter { it.active }.forEach { group ->
                planned += recurringIopReminders(
                    group = group,
                    reminderLeadMinutes = preferences.iopReminderLeadMinutes,
                    includeStartReminder = preferences.iopRemindersEnabled,
                    includeAttendanceFollowUp = preferences.iopAttendanceFollowUpEnabled,
                    now = now
                )
            }
        }

        return planned
            .filter { it.triggerAt > now }
            .distinctBy { it.key }
            .sortedBy { it.triggerAt }
    }

    fun appointmentReminders(
        appointment: CareAppointment,
        now: Long = System.currentTimeMillis()
    ): List<PlannedReminder> {
        if (
            appointment.id <= 0L ||
            appointment.status !in setOf("Scheduled", "Confirmed")
        ) {
            return emptyList()
        }

        val result = mutableListOf<PlannedReminder>()
        val who = appointment.providerName.takeIf(String::isNotBlank)
            ?: appointment.visitCategory
        val place = appointment.placeName.takeIf(String::isNotBlank)
            ?: "your appointment location"
        val appointmentTime = formatOccurrence(appointment.scheduledAt)
        val body = "$who at $place · $appointmentTime"

        if (appointment.remindOneDayBefore) {
            val trigger = appointment.scheduledAt - ONE_DAY_MILLIS
            if (trigger > now) {
                result += PlannedReminder(
                    key = appointmentKey(appointment.id, "1d"),
                    kind = ReminderKind.APPOINTMENT,
                    triggerAt = trigger,
                    occurrenceAt = appointment.scheduledAt,
                    title = "Appointment tomorrow",
                    text = body,
                    sourceLabel = who
                )
            }
        }

        if (appointment.remindTwoHoursBefore) {
            val trigger = appointment.scheduledAt - TWO_HOURS_MILLIS
            if (trigger > now) {
                result += PlannedReminder(
                    key = appointmentKey(appointment.id, "2h"),
                    kind = ReminderKind.APPOINTMENT,
                    triggerAt = trigger,
                    occurrenceAt = appointment.scheduledAt,
                    title = "Appointment in about 2 hours",
                    text = body,
                    sourceLabel = who
                )
            }
        }

        return result
    }

    fun appointmentKey(appointmentId: Long, code: String): String =
        "appointment:$appointmentId:$code"

    private fun recurringMeetingReminders(
        meeting: SavedMeeting,
        leadMinutes: Int,
        now: Long
    ): List<PlannedReminder> {
        val dayOfWeek = meeting.usualDayOfWeek ?: return emptyList()
        val startMinutes = meeting.usualStartMinutes ?: return emptyList()

        return occurrences(dayOfWeek, startMinutes, now).mapNotNull { occurrenceAt ->
            val triggerAt = occurrenceAt - leadMinutes * 60_000L
            if (triggerAt <= now) return@mapNotNull null

            val location = listOf(meeting.address, meeting.city, meeting.state)
                .filter(String::isNotBlank)
                .joinToString(", ")
                .takeIf(String::isNotBlank)
                ?: "Saved meeting location"

            PlannedReminder(
                key = "meeting:${meeting.id}:$occurrenceAt:start",
                kind = ReminderKind.MEETING,
                triggerAt = triggerAt,
                occurrenceAt = occurrenceAt,
                title = "Recovery meeting ${leadLabel(leadMinutes)}",
                text = "${meeting.name} · $location · ${formatOccurrence(occurrenceAt)}",
                sourceLabel = meeting.name
            )
        }
    }

    private fun recurringIopReminders(
        group: IopGroup,
        reminderLeadMinutes: Int,
        includeStartReminder: Boolean,
        includeAttendanceFollowUp: Boolean,
        now: Long
    ): List<PlannedReminder> {
        val result = mutableListOf<PlannedReminder>()
        val durationMinutes = (group.endMinutes - group.startMinutes).coerceAtLeast(1)

        occurrences(group.dayOfWeek, group.startMinutes, now).forEach { occurrenceAt ->
            if (includeStartReminder) {
                val triggerAt = occurrenceAt - reminderLeadMinutes * 60_000L
                if (triggerAt > now) {
                    val location = group.location.takeIf(String::isNotBlank)
                        ?: "Saved IOP location"
                    result += PlannedReminder(
                        key = "iop:${group.id}:$occurrenceAt:start",
                        kind = ReminderKind.IOP,
                        triggerAt = triggerAt,
                        occurrenceAt = occurrenceAt,
                        title = "IOP group ${leadLabel(reminderLeadMinutes)}",
                        text = "${group.name} · $location · ${formatOccurrence(occurrenceAt)}",
                        sourceLabel = group.name
                    )
                }
            }

            if (includeAttendanceFollowUp) {
                val followUpAt = occurrenceAt + durationMinutes * 60_000L + 10L * 60_000L
                if (followUpAt > now) {
                    result += PlannedReminder(
                        key = "iop:${group.id}:$occurrenceAt:follow_up",
                        kind = ReminderKind.IOP_ATTENDANCE_FOLLOW_UP,
                        triggerAt = followUpAt,
                        occurrenceAt = occurrenceAt,
                        title = "IOP attendance recorded",
                        text = "${group.name} was counted as attended. Mark it missed only if needed.",
                        sourceLabel = group.name
                    )
                }
            }
        }

        return result
    }

    private fun occurrences(
        dayOfWeek: Int,
        startMinutes: Int,
        now: Long
    ): List<Long> {
        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val endDate = startDate.plusDays(HORIZON_DAYS)
        val hour = (startMinutes / 60).coerceIn(0, 23)
        val minute = (startMinutes % 60).coerceIn(0, 59)

        val result = mutableListOf<Long>()
        var date = startDate
        while (!date.isAfter(endDate)) {
            if (date.dayOfWeek.value == dayOfWeek.coerceIn(1, 7)) {
                result += date.atTime(hour, minute)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }
            date = date.plusDays(1)
        }
        return result
    }

    private fun leadLabel(minutes: Int): String = when (minutes) {
        15 -> "in 15 minutes"
        30 -> "in 30 minutes"
        60 -> "in about 1 hour"
        120 -> "in about 2 hours"
        1_440 -> "tomorrow"
        else -> "soon"
    }

    fun formatOccurrence(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofPattern(
                    "EEE, MMM d · h:mm a",
                    Locale.US
                )
            )
}

internal object DailyRebuildReminderCoordinator {
    fun sync(
        context: Context,
        preferences: DailyRebuildPreferences,
        appointments: List<CareAppointment>,
        meetings: List<SavedMeeting>,
        iopGroups: List<IopGroup>
    ) {
        if (!preferences.notificationsEnabled) {
            DailyRebuildReminderScheduler.cancelAllScheduled(context)
            DailyRebuildReminderScheduler.cancelAllDisplayed(context)
            return
        }

        DailyRebuildReminderScheduler.cancelPlannedScheduled(context)

        ReminderPlanner.plan(
            preferences = preferences,
            appointments = appointments,
            meetings = meetings,
            iopGroups = iopGroups
        ).forEach { reminder ->
            DailyRebuildReminderScheduler.schedule(context, reminder)
        }
    }
}

internal object DailyRebuildReminderScheduler {
    const val EXTRA_COMMAND = "reminder_command"
    const val EXTRA_KEY = "reminder_key"
    const val EXTRA_KIND = "reminder_kind"
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_TEXT = "reminder_text"
    const val EXTRA_OCCURRENCE_AT = "reminder_occurrence_at"
    const val EXTRA_NOTIFICATION_ID = "reminder_notification_id"

    const val COMMAND_DELIVER = "deliver"
    const val COMMAND_SNOOZE = "snooze"
    const val COMMAND_DISMISS = "dismiss"

    const val CHANNEL_APPOINTMENTS = "daily_rebuild_appointments"
    const val CHANNEL_MEETINGS = "daily_rebuild_meetings"
    const val CHANNEL_IOP = "daily_rebuild_iop"

    private const val REGISTRY_FILE = "daily_rebuild_scheduled_reminders"
    private const val REGISTRY_KEY = "scheduled_keys"

    @Synchronized
    fun schedule(context: Context, reminder: PlannedReminder) {
        if (reminder.triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(
            context = context,
            reminder = reminder,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerAt,
                pendingIntent
            )
        }

        val registry = registry(context).toMutableSet()
        registry += reminder.key
        saveRegistry(context, registry)
    }

    fun scheduleSnooze(
        context: Context,
        kind: ReminderKind,
        title: String,
        text: String,
        occurrenceAt: Long,
        minutes: Int,
        notificationId: Int
    ) {
        val triggerAt = System.currentTimeMillis() + minutes.coerceIn(5, 240) * 60_000L
        schedule(
            context,
            PlannedReminder(
                key = "snooze:$notificationId:$triggerAt",
                kind = kind,
                triggerAt = triggerAt,
                occurrenceAt = occurrenceAt,
                title = title,
                text = text,
                sourceLabel = title
            )
        )
    }

    @Synchronized
    fun cancel(context: Context, key: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntentForKey(
            context = context,
            key = key,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { alarmManager.cancel(it) }

        val registry = registry(context).toMutableSet()
        registry -= key
        saveRegistry(context, registry)
    }

    @Synchronized
    fun cancelPlannedScheduled(context: Context) {
        cancelMatching(context) { key -> !key.startsWith("snooze:") }
    }

    @Synchronized
    fun cancelAllScheduled(context: Context) {
        cancelMatching(context) { true }
    }

    private fun cancelMatching(
        context: Context,
        shouldCancel: (String) -> Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val current = registry(context)
        current.filter(shouldCancel).forEach { key ->
            pendingIntentForKey(
                context = context,
                key = key,
                flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it) }
        }
        saveRegistry(context, current.filterNot(shouldCancel).toSet())
    }

    fun cancelAllDisplayed(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    @Synchronized
    fun markDelivered(context: Context, key: String) {
        val registry = registry(context).toMutableSet()
        registry -= key
        saveRegistry(context, registry)
    }

    private fun pendingIntent(
        context: Context,
        reminder: PlannedReminder,
        flags: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(reminder.key),
        Intent(context, DailyRebuildReminderReceiver::class.java).apply {
            action = actionForKey(reminder.key)
            putExtra(EXTRA_COMMAND, COMMAND_DELIVER)
            putExtra(EXTRA_KEY, reminder.key)
            putExtra(EXTRA_KIND, reminder.kind.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_TEXT, reminder.text)
            putExtra(EXTRA_OCCURRENCE_AT, reminder.occurrenceAt)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId(reminder.key))
        },
        flags
    )

    private fun pendingIntentForKey(
        context: Context,
        key: String,
        flags: Int
    ): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode(key),
        Intent(context, DailyRebuildReminderReceiver::class.java).apply {
            action = actionForKey(key)
        },
        flags
    )

    private fun actionForKey(key: String): String =
        "com.pgdevhouse.dailyrebuild.REMINDER.$key"

    private fun requestCode(key: String): Int = key.hashCode() and Int.MAX_VALUE

    fun notificationId(key: String): Int = requestCode("notification:$key")

    private fun registry(context: Context): Set<String> =
        context.applicationContext
            .getSharedPreferences(REGISTRY_FILE, Context.MODE_PRIVATE)
            .getStringSet(REGISTRY_KEY, emptySet())
            ?.toSet()
            .orEmpty()

    private fun saveRegistry(context: Context, keys: Set<String>) {
        context.applicationContext
            .getSharedPreferences(REGISTRY_FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(REGISTRY_KEY, keys)
            .apply()
    }
}
