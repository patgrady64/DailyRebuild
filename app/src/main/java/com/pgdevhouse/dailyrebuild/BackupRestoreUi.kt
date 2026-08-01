package com.pgdevhouse.dailyrebuild

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.backup.DailyRebuildBackupManager
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import com.pgdevhouse.dailyrebuild.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BackupRestoreFeature(
    database: DailyRebuildDatabase,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val manager = remember(context, database) {
        DailyRebuildBackupManager(
            context = context.applicationContext,
            database = database
        )
    }
    val appPreferencesRepository = remember(context) {
        AppPreferencesRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingInspection by remember {
        mutableStateOf<DailyRebuildBackupManager.BackupInspection?>(null)
    }
    var emergencyBackup by remember {
        mutableStateOf<DailyRebuildBackupManager.EmergencyBackup?>(null)
    }
    var confirmEmergencyRestore by remember { mutableStateOf(false) }
    var restoreComplete by remember { mutableStateOf(false) }
    var lastBackupTime by remember {
        mutableStateOf(
            context.getSharedPreferences(
                BACKUP_STATE_PREFERENCES,
                Context.MODE_PRIVATE
            ).getLong(LAST_BACKUP_TIME, 0L)
        )
    }

    fun refreshEmergencyBackup() {
        scope.launch {
            emergencyBackup = manager.latestEmergencyBackup()
        }
    }

    LaunchedEffect(Unit) {
        emergencyBackup = manager.latestEmergencyBackup()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                errorMessage = null
                statusMessage = null
                runCatching {
                    manager.exportToUri(uri)
                }.onSuccess { summary ->
                    context.getSharedPreferences(
                        BACKUP_STATE_PREFERENCES,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putLong(LAST_BACKUP_TIME, summary.createdAtEpochMillis)
                        .apply()
                    lastBackupTime = summary.createdAtEpochMillis
                    statusMessage =
                        "Backup saved with ${summary.totalRecords} records."
                }.onFailure { error ->
                    errorMessage = error.userFacingBackupMessage(
                        "Could not create the backup."
                    )
                }
                isWorking = false
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                errorMessage = null
                statusMessage = null
                runCatching {
                    manager.inspectUri(uri)
                }.onSuccess { inspection ->
                    pendingRestoreUri = uri
                    pendingInspection = inspection
                }.onFailure { error ->
                    errorMessage = error.userFacingBackupMessage(
                        "Could not read this backup."
                    )
                }
                isWorking = false
            }
        }
    }

    fun restoreSelectedBackup() {
        val uri = pendingRestoreUri ?: return
        scope.launch {
            pendingInspection = null
            isWorking = true
            errorMessage = null
            statusMessage = null
            val restoreAttempt = runCatching {
                val oldAppointments = database
                    .careAppointmentDao()
                    .getAllAppointments()
                oldAppointments.forEach { appointment ->
                    AppointmentReminderScheduler.cancel(
                        context,
                        appointment.id
                    )
                }

                manager.restoreFromUri(uri)
            }

            // Whether the restore succeeds or rolls back, rebuild reminder
            // alarms from whichever appointment rows are now in the database,
            // but only when appointment reminders are enabled in Settings.
            if (appPreferencesRepository.load().appointmentRemindersEnabled) {
                database.careAppointmentDao()
                    .getAllAppointments()
                    .forEach { appointment ->
                        AppointmentReminderScheduler.schedule(
                            context,
                            appointment
                        )
                    }
            }

            restoreAttempt.onSuccess { result ->
                pendingRestoreUri = null
                statusMessage =
                    "Restored ${result.summary.totalRecords} records."
                restoreComplete = true
                refreshEmergencyBackup()
            }.onFailure { error ->
                errorMessage = error.userFacingBackupMessage(
                    "The restore did not complete. Your current data was not replaced."
                )
            }
            isWorking = false
        }
    }

    fun restoreEmergency() {
        val emergency = emergencyBackup ?: return
        scope.launch {
            confirmEmergencyRestore = false
            isWorking = true
            errorMessage = null
            statusMessage = null
            val restoreAttempt = runCatching {
                val oldAppointments = database
                    .careAppointmentDao()
                    .getAllAppointments()
                oldAppointments.forEach { appointment ->
                    AppointmentReminderScheduler.cancel(
                        context,
                        appointment.id
                    )
                }

                manager.restoreEmergencyBackup(emergency.file)
            }

            if (appPreferencesRepository.load().appointmentRemindersEnabled) {
                database.careAppointmentDao()
                    .getAllAppointments()
                    .forEach { appointment ->
                        AppointmentReminderScheduler.schedule(
                            context,
                            appointment
                        )
                    }
            }

            restoreAttempt.onSuccess { result ->
                statusMessage =
                    "Emergency backup restored with ${result.summary.totalRecords} records."
                restoreComplete = true
                refreshEmergencyBackup()
            }.onFailure { error ->
                errorMessage = error.userFacingBackupMessage(
                    "The emergency restore did not complete."
                )
            }
            isWorking = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildSectionCard(
            title = "Data & Backup",
            subtitle = "Protect all information stored locally by Daily Rebuild.",
            accentColor = RebuildBlue
        ) {
            Text(
                text = "A backup is one ZIP file containing readable JSON, record counts, the app version, and database version.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        createBackupLauncher.launch(
                            manager.suggestedFileName()
                        )
                    },
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create Backup")
                }
                OutlinedButton(
                    onClick = {
                        openBackupLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/octet-stream"
                            )
                        )
                    },
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore Backup")
                }
            }

            if (isWorking) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Working…")
                }
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        BackupCoverageCard(lastBackupTime)

        emergencyBackup?.let { emergency ->
            RebuildSectionCard(
                title = "Emergency Restore Point",
                subtitle = "Created automatically immediately before the most recent restore.",
                accentColor = RebuildAmber
            ) {
                Text(
                    "${formatBackupDate(emergency.createdAtEpochMillis)} · " +
                        "${emergency.totalRecords} records"
                )
                Text(
                    text = "This copy is stored privately inside Daily Rebuild and can undo a bad restore.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { confirmEmergencyRestore = true },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Emergency Copy")
                }
            }
        }
    }

    pendingInspection?.let { inspection ->
        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    pendingInspection = null
                    pendingRestoreUri = null
                }
            },
            title = { Text("Replace Current Data?") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(inspection.sourceLabel, fontWeight = FontWeight.SemiBold)
                    Text("Created: ${formatBackupDate(inspection.summary.createdAtEpochMillis)}")
                    Text("App version: ${inspection.summary.appVersionName}")
                    Text("Database version: ${inspection.summary.databaseVersion}")
                    Text("Records: ${inspection.summary.totalRecords}")
                    Text(
                        text = "Restoring replaces every local Daily Rebuild record. An emergency copy of your current data will be created first.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = ::restoreSelectedBackup,
                    enabled = !isWorking
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingInspection = null
                        pendingRestoreUri = null
                    },
                    enabled = !isWorking
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (confirmEmergencyRestore && emergencyBackup != null) {
        AlertDialog(
            onDismissRequest = { confirmEmergencyRestore = false },
            title = { Text("Restore Emergency Copy?") },
            text = {
                Text(
                    "This replaces current Daily Rebuild data with the copy made on " +
                        "${formatBackupDate(emergencyBackup!!.createdAtEpochMillis)}. " +
                        "Another emergency copy of the current database will be made first."
                )
            },
            confirmButton = {
                TextButton(onClick = ::restoreEmergency) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmergencyRestore = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (restoreComplete) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore Complete") },
            text = {
                Text(
                    "Daily Rebuild needs to reload its screens so every restored record appears correctly."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreComplete = false
                        activity?.recreate()
                    }
                ) {
                    Text("Reload App")
                }
            }
        )
    }
}

@Composable
private fun BackupCoverageCard(lastBackupTime: Long) {
    RebuildInsetPanel {
        Text("Included", fontWeight = FontWeight.SemiBold)
        Text(
            "Daily records, foods, saved meals, water, activity snapshots, mobility, pain, measurements, medications, showers, migraines, meetings, IOP schedules and missed-attendance reasons, appointments, care visits, pantry essentials, and Life Maintenance."
        )
        Text("Not included", fontWeight = FontWeight.SemiBold)
        Text(
            text = "Original Google Fit or Health Connect records. Daily Rebuild’s saved activity snapshots are included.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (lastBackupTime > 0L) {
                "Last successful exported backup: ${formatBackupDate(lastBackupTime)}"
            } else {
                "No exported backup has been recorded on this installation yet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Throwable.userFacingBackupMessage(fallback: String): String {
    val detail = generateSequence(this) { it.cause }
        .mapNotNull { it.message?.takeIf(String::isNotBlank) }
        .firstOrNull()
    return if (detail == null) fallback else "$fallback $detail"
}

private fun formatBackupDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "MMM d, yyyy 'at' h:mm a",
                Locale.US
            )
        )

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val BACKUP_STATE_PREFERENCES = "daily_rebuild_backup_state"
private const val LAST_BACKUP_TIME = "last_successful_export_epoch_millis"
