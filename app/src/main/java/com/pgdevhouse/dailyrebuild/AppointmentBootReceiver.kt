package com.pgdevhouse.dailyrebuild

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Restores inexact appointment alarms after a reboot or clock change. */
class AppointmentBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        ).launch {
            try {
                val database =
                    DailyRebuildDatabase.getDatabase(
                        applicationContext
                    )

                database.careAppointmentDao()
                    .getUpcomingAppointments(
                        System.currentTimeMillis()
                    )
                    .forEach { appointment ->
                        AppointmentReminderScheduler.schedule(
                            applicationContext,
                            appointment
                        )
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
