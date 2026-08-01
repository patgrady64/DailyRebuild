package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ReminderCenterDialog(
    preferences: DailyRebuildPreferences,
    appointments: List<CareAppointment>,
    meetings: List<SavedMeeting>,
    iopGroups: List<IopGroup>,
    notificationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAndroidNotificationSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val reminders = remember(
        preferences,
        appointments,
        meetings,
        iopGroups
    ) {
        ReminderPlanner.plan(
            preferences = preferences,
            appointments = appointments,
            meetings = meetings,
            iopGroups = iopGroups
        ).take(50)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Reminder Center")
                Text(
                    text = "Upcoming Daily Rebuild notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationPermissionCard(
                    notificationsEnabled = preferences.notificationsEnabled,
                    permissionGranted = notificationPermissionGranted,
                    onRequestPermission = onRequestPermission,
                    onOpenAndroidNotificationSettings = onOpenAndroidNotificationSettings
                )

                when {
                    !preferences.notificationsEnabled -> {
                        ReminderCenterEmptyCard(
                            "All Daily Rebuild notifications are turned off. Your individual reminder choices are preserved for later."
                        )
                    }

                    reminders.isEmpty() -> {
                        ReminderCenterEmptyCard(
                            "No upcoming notifications are currently scheduled from your saved appointments, meetings, or IOP groups."
                        )
                    }

                    else -> reminders.forEach { reminder ->
                        ReminderCenterItemCard(reminder)
                    }
                }

                if (reminders.size >= 50) {
                    Text(
                        text = "Showing the next 50 reminders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun NotificationPermissionCard(
    notificationsEnabled: Boolean,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAndroidNotificationSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (!notificationsEnabled) {
                    "Notifications disabled in Daily Rebuild"
                } else if (permissionGranted) {
                    "Android notification permission allowed"
                } else {
                    "Android notification permission needed"
                },
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (!notificationsEnabled) {
                    "No alarms or notifications will be delivered while the master switch is off."
                } else if (permissionGranted) {
                    "Daily Rebuild can deliver the reminder types you enabled."
                } else {
                    "Your reminder settings are saved, but Android will block the notifications until permission is allowed."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (notificationsEnabled && !permissionGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allow")
                    }
                    OutlinedButton(
                        onClick = onOpenAndroidNotificationSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Android Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCenterEmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderCenterItemCard(reminder: PlannedReminder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = reminderKindLabel(reminder.kind),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatReminderTime(reminder.triggerAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(reminder.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = reminder.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun reminderKindLabel(kind: ReminderKind): String = when (kind) {
    ReminderKind.APPOINTMENT -> "APPOINTMENT"
    ReminderKind.MEETING -> "RECOVERY MEETING"
    ReminderKind.IOP -> "IOP GROUP"
    ReminderKind.IOP_ATTENDANCE_FOLLOW_UP -> "IOP ATTENDANCE"
}

private fun formatReminderTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "EEE, MMM d · h:mm a",
                Locale.US
            )
        )
