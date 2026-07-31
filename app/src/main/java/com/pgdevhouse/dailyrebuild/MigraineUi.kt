package com.pgdevhouse.dailyrebuild

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Information collected by the quick migraine / visual-aura dialog.
 */
data class MigraineLogDraft(
    val date: String,
    val occurredAt: Long,
    val auraDurationMinutes: Int?,
    val visualAura: Boolean,
    val headPain: Boolean,
    val foggyAfterward: Boolean,
    val notes: String
)

/**
 * Health-tab tracker for occasional migraine or visual-aura events.
 *
 * This card is always available so an event can be logged quickly. Migraine
 * counts should only be inserted into a weekly report when at least one event
 * exists in that report window.
 */
@Composable
fun MigraineTrackerCard(
    logs: List<MigraineLog>,
    onLogMigraine: () -> Unit,
    onDeleteLog: (MigraineLog) -> Unit
) {
    val latestLog = logs.maxByOrNull { it.occurredAt }

    RebuildSectionCard(
        title = "Migraine & visual aura",
        subtitle =
            "Record rare visual-aura events with their exact date and time.",
        accentColor = RebuildAmber,
        trailing = {
            TextButton(
                onClick = onLogMigraine
            ) {
                Text("Log")
            }
        }
    ) {
        Button(
            onClick = onLogMigraine,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Log Migraine / Visual Aura")
        }

        if (latestLog == null) {
            RebuildInsetPanel {
                Text(
                    text = "No events logged yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text =
                        "Nothing will appear in a weekly report unless an event is actually recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val thisWeekLogs = logs.filter(::isInCurrentWeek)

            if (thisWeekLogs.isNotEmpty()) {
                RebuildInsetPanel {
                    Text(
                        text = "This week",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text =
                            "${thisWeekLogs.size} event${if (thisWeekLogs.size == 1) "" else "s"} logged",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Event history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            logs.sortedByDescending { it.occurredAt }
                .forEach { log ->
                    MigraineHistoryRow(
                        log = log,
                        onDelete = {
                            onDeleteLog(log)
                        }
                    )
                }
        }

        Text(
            text =
                "Tracking only—not a diagnosis. Sudden, new, or unusual vision or neurologic symptoms can require urgent care.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MigraineHistoryRow(
    log: MigraineLog,
    onDelete: () -> Unit
) {
    RebuildInsetPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = formatMigraineDateTime(log.occurredAt),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = buildMigraineSummary(log),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.notes.isNotBlank()) {
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TextButton(
                onClick = onDelete
            ) {
                Text("Delete")
            }
        }
    }
}
@Composable
fun MigraineLogDialog(
    onDismiss: () -> Unit,
    onSave: (MigraineLogDraft) -> Unit
) {
    val context = LocalContext.current
    val initialDateTime = remember { LocalDateTime.now() }

    var dateText by rememberSaveable { mutableStateOf(initialDateTime.toLocalDate().toString()) }
    var selectedHour by rememberSaveable { mutableIntStateOf(initialDateTime.hour) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(initialDateTime.minute) }
    var durationText by rememberSaveable { mutableStateOf("") }
    var visualAura by rememberSaveable { mutableStateOf(true) }
    var headPain by rememberSaveable { mutableStateOf(false) }
    var foggyAfterward by rememberSaveable { mutableStateOf(true) }
    var notes by rememberSaveable { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedDate = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())

    RebuildInputDialog(
        title = "Log migraine or visual aura",
        subtitle = "Record when it happened, what you experienced, and how long it lasted.",
        onDismissRequest = onDismiss,
        primaryActionText = "Save event",
        onPrimaryAction = save@{
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
            if (date == null) {
                validationMessage = "Choose a valid date."
                return@save
            }

            val duration = durationText.takeIf { it.isNotBlank() }?.toIntOrNull()
            if (durationText.isNotBlank() && duration == null) {
                validationMessage = "Enter a valid duration or leave it blank."
                return@save
            }

            val occurredAt = date.atTime(LocalTime.of(selectedHour, selectedMinute))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            onSave(
                MigraineLogDraft(
                    date = date.toString(),
                    occurredAt = occurredAt,
                    auraDurationMinutes = duration,
                    visualAura = visualAura,
                    headPain = headPain,
                    foggyAfterward = foggyAfterward,
                    notes = notes.trim()
                )
            )
        }
    ) {
        RebuildDialogInfoPanel {
            Text(
                text = "The date and time default to now. Change them when recording an earlier event.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            dateText = LocalDate.of(year, month + 1, day).toString()
                            validationMessage = null
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text(formatMigraineDate(dateText)) }

            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> selectedHour = hour; selectedMinute = minute },
                        selectedHour,
                        selectedMinute,
                        false
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text(formatMigraineTime(selectedHour, selectedMinute)) }
        }

        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter(Char::isDigit).take(3); validationMessage = null },
            label = { Text("Aura duration") },
            suffix = { Text("minutes") },
            supportingText = { Text("Optional") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Symptoms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        MigraineOptionRow("Visual aura or flickering lines", visualAura) { visualAura = it }
        MigraineOptionRow("Head pain", headPain) { headPain = it }
        MigraineOptionRow("Felt foggy or out of it afterward", foggyAfterward) { foggyAfterward = it }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        validationMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        RebuildDialogInfoPanel(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
            Text(
                text = "Call 911 for ongoing or new confusion, trouble speaking, weakness, loss of balance, or sudden vision loss. Do not use this log in place of urgent care.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun MigraineOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatMigraineDateTime(
    occurredAt: Long
): String {
    return Instant.ofEpochMilli(occurredAt)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "EEE, MMM d, yyyy 'at' h:mm a",
                Locale.US
            )
        )
}

private fun formatMigraineDate(
    dateText: String
): String {
    return runCatching {
        LocalDate.parse(dateText)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMM d, yyyy",
                    Locale.US
                )
            )
    }.getOrDefault(dateText)
}

private fun formatMigraineTime(
    hour: Int,
    minute: Int
): String {
    return LocalTime.of(hour, minute)
        .format(
            DateTimeFormatter.ofPattern(
                "h:mm a",
                Locale.US
            )
        )
}

private fun buildMigraineSummary(
    log: MigraineLog
): String {
    val parts = mutableListOf<String>()

    if (log.visualAura) {
        parts += "visual aura"
    }
    if (log.headPain) {
        parts += "head pain"
    }
    if (log.foggyAfterward) {
        parts += "foggy afterward"
    }
    log.auraDurationMinutes?.let {
        parts += "$it min aura"
    }

    return if (parts.isEmpty()) {
        "Event logged"
    } else {
        parts.joinToString(" · ")
    }
}

private fun isInCurrentWeek(
    log: MigraineLog
): Boolean {
    val date =
        runCatching {
            LocalDate.parse(log.date)
        }.getOrNull()
            ?: return false

    val today = LocalDate.now()
    val weekStart =
        today.minusDays(
            (today.dayOfWeek.value - 1).toLong()
        )
    val weekEnd = weekStart.plusDays(6)

    return !date.isBefore(weekStart) &&
        !date.isAfter(weekEnd)
}
