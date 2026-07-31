package com.pgdevhouse.dailyrebuild

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTask
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * History-only tracking for occasional life maintenance.
 *
 * Nothing is due or overdue. The screen records completions and shows the most
 * recent date for each fixed item.
 */
@Composable
fun LifeMaintenanceScreen(
    logs: List<LifeMaintenanceLog>,
    isSaving: Boolean,
    onMarkDone: (taskKey: String, date: String) -> Unit,
    onMoveLog: (log: LifeMaintenanceLog, newDate: String) -> Unit,
    onDeleteLog: (LifeMaintenanceLog) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedTaskKey by rememberSaveable { mutableStateOf<String?>(null) }
    var logPendingDeletion by remember { mutableStateOf<LifeMaintenanceLog?>(null) }
    val context = LocalContext.current
    val today = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildInsetPanel(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Text(
                text = "When did I last do this?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "There are no schedules, reminders, inventory counts, or overdue labels. Mark an item only when you actually complete it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LifeMaintenanceTasks.all.forEach { task ->
            val taskLogs = logs
                .filter { it.taskKey == task.key }
                .sortedWith(
                    compareByDescending<LifeMaintenanceLog> { it.date }
                        .thenByDescending { it.completedAt }
                )
            val latest = taskLogs.firstOrNull()
            val loggedToday = taskLogs.any { it.date == today.toString() }
            val expanded = expandedTaskKey == task.key

            RebuildSectionCard(
                title = task.label,
                subtitle = latest?.let { "Last done: ${formatMaintenanceDate(it.date)}" }
                    ?: "Last done: Not recorded",
                accentColor = RebuildTeal
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onMarkDone(task.key, today.toString()) },
                        enabled = !isSaving && !loggedToday,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (loggedToday) "✓ Logged Today" else "Mark Done")
                    }
                    OutlinedButton(
                        onClick = {
                            showMaintenanceDatePicker(
                                context = context,
                                initialDate = today,
                                maximumDate = today,
                                onDateSelected = { selectedDate ->
                                    onMarkDone(task.key, selectedDate.toString())
                                }
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Different Date")
                    }
                }

                TextButton(
                    onClick = {
                        expandedTaskKey = if (expanded) null else task.key
                    },
                    enabled = taskLogs.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            taskLogs.isEmpty() -> "No History"
                            expanded -> "Hide History"
                            else -> "View History (${taskLogs.size})"
                        }
                    )
                }

                if (expanded && taskLogs.isNotEmpty()) {
                    taskLogs.forEach { log ->
                        LifeMaintenanceHistoryRow(
                            task = task,
                            log = log,
                            isSaving = isSaving,
                            onEdit = {
                                val initialDate = parseMaintenanceDate(log.date) ?: today
                                showMaintenanceDatePicker(
                                    context = context,
                                    initialDate = initialDate,
                                    maximumDate = today,
                                    onDateSelected = { selectedDate ->
                                        if (selectedDate.toString() != log.date) {
                                            onMoveLog(log, selectedDate.toString())
                                        }
                                    }
                                )
                            },
                            onDelete = { logPendingDeletion = log }
                        )
                    }
                }
            }
        }

    }

    logPendingDeletion?.let { log ->
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) logPendingDeletion = null
            },
            title = { Text("Delete Completion?") },
            text = {
                Text(
                    "Remove ${LifeMaintenanceTasks.labelFor(log.taskKey)} from ${formatMaintenanceDate(log.date)}?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onDeleteLog(log)
                        logPendingDeletion = null
                    }
                ) {
                    Text(if (isSaving) "Deleting…" else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { logPendingDeletion = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LifeMaintenanceHistoryRow(
    task: LifeMaintenanceTask,
    log: LifeMaintenanceLog,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    RebuildInsetPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatMaintenanceDate(log.date),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${task.label} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit, enabled = !isSaving) {
                Text("Edit")
            }
            TextButton(onClick = onDelete, enabled = !isSaving) {
                Text("Delete")
            }
        }
    }
}

private fun showMaintenanceDatePicker(
    context: android.content.Context,
    initialDate: LocalDate,
    maximumDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val dialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    )
    dialog.datePicker.maxDate = maximumDate
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli() - 1L
    dialog.show()
}

private fun parseMaintenanceDate(dateText: String): LocalDate? =
    runCatching { LocalDate.parse(dateText) }.getOrNull()

fun formatMaintenanceDate(dateText: String): String =
    runCatching {
        LocalDate.parse(dateText).format(
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
        )
    }.getOrDefault(dateText)
