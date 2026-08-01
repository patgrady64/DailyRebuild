package com.pgdevhouse.dailyrebuild

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Restores all optional Daily Rebuild alarms after reboot, clock, or time-zone changes. */
class AppointmentBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = DailyRebuildDatabase.getDatabase(applicationContext)
                DailyRebuildReminderCoordinator.sync(
                    context = applicationContext,
                    preferences = AppPreferencesRepository(applicationContext).load(),
                    appointments = database.careAppointmentDao()
                        .getUpcomingAppointments(System.currentTimeMillis()),
                    meetings = database.meetingDao().getActiveMeetings(),
                    iopGroups = database.iopGroupDao().getActive()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
