package com.pgdevhouse.dailyrebuild

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Retained for source and upgrade compatibility. New alarms use
 * [DailyRebuildReminderReceiver].
 */
class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyRebuildReminderReceiver().onReceive(context, intent)
    }
}
