package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class IopOccurrence(
    val group: IopGroup,
    val date: LocalDate
)

fun defaultIopGroupSchedule(): List<IopGroup> =
    (DayOfWeek.MONDAY.value..DayOfWeek.THURSDAY.value).map { day ->
        IopGroup(
            name = "IOP Group",
            dayOfWeek = day,
            startMinutes = 18 * 60 + 30,
            endMinutes = 20 * 60 + 30
        )
    }

fun findNextIopOccurrence(
    groups: List<IopGroup>,
    nowDate: LocalDate = LocalDate.now(),
    nowTime: LocalTime = LocalTime.now()
): IopOccurrence? {
    val active = groups.filter(IopGroup::active)
    if (active.isEmpty()) return null

    return (0L..7L)
        .asSequence()
        .flatMap { offset ->
            val date = nowDate.plusDays(offset)
            val matching = active.filter { it.dayOfWeek == date.dayOfWeek.value }
            matching.asSequence().map { IopOccurrence(it, date) }
        }
        .filter { occurrence ->
            if (occurrence.date != nowDate) {
                true
            } else {
                val end = minutesToLocalTime(occurrence.group.endMinutes)
                !nowTime.isAfter(end)
            }
        }
        .minWithOrNull(
            compareBy<IopOccurrence> { it.date }
                .thenBy { it.group.startMinutes }
        )
}

@Composable
fun IopGroupsScreen(
    groups: List<IopGroup>,
    isSaving: Boolean,
    onSave: (IopGroup) -> Unit,
    onDelete: (IopGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingGroup by remember { mutableStateOf<IopGroup?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<IopGroup?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RebuildSectionCard(
            title = "IOP Groups",
            subtitle = "Manage the recurring group schedule without turning it into a daily completion requirement.",
            accentColor = RebuildBlue
        ) {
            val activeCount = groups.count(IopGroup::active)
            Text(
                text = "$activeCount active group${if (activeCount == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    editingGroup = null
                    showEditor = true
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add IOP Group")
            }
        }

        if (groups.isEmpty()) {
            RebuildInsetPanel {
                Text("No IOP groups are saved.", fontWeight = FontWeight.SemiBold)
                Text(
                    "Add a recurring day and time when your schedule is known.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groups.sortedWith(
                compareBy<IopGroup> { it.dayOfWeek }
                    .thenBy { it.startMinutes }
                    .thenBy { it.name.lowercase(Locale.US) }
            ).forEach { group ->
                IopGroupCard(
                    group = group,
                    isSaving = isSaving,
                    onEdit = {
                        editingGroup = group
                        showEditor = true
                    },
                    onToggleActive = {
                        onSave(
                            group.copy(
                                active = !group.active,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    },
                    onDelete = { pendingDelete = group }
                )
            }
        }
    }

    if (showEditor) {
        IopGroupEditorDialog(
            existing = editingGroup,
            isSaving = isSaving,
            onSave = { group ->
                onSave(group)
                showEditor = false
                editingGroup = null
            },
            onDismiss = {
                showEditor = false
                editingGroup = null
            }
        )
    }

    pendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete IOP group?") },
            text = {
                Text(
                    "${dayLabel(group.dayOfWeek)} · ${formatTimeRange(group.startMinutes, group.endMinutes)} will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(group)
                        pendingDelete = null
                    },
                    enabled = !isSaving
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IopGroupCard(
    group: IopGroup,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.active) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dayLabel(group.dayOfWeek)} · ${formatTimeRange(group.startMinutes, group.endMinutes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (group.location.isNotBlank()) {
                        Text(
                            text = group.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilterChip(
                    selected = group.active,
                    onClick = onToggleActive,
                    enabled = !isSaving,
                    label = { Text(if (group.active) "Active" else "Paused") }
                )
            }

            if (group.notes.isNotBlank()) {
                Text(
                    text = group.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun IopGroupEditorDialog(
    existing: IopGroup?,
    isSaving: Boolean,
    onSave: (IopGroup) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.name ?: "IOP Group")
    }
    var dayOfWeek by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.dayOfWeek ?: DayOfWeek.MONDAY.value)
    }
    var startText by rememberSaveable(existing?.id) {
        mutableStateOf(formatMinutesForInput(existing?.startMinutes ?: 18 * 60 + 30))
    }
    var endText by rememberSaveable(existing?.id) {
        mutableStateOf(formatMinutesForInput(existing?.endMinutes ?: 20 * 60 + 30))
    }
    var location by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.location.orEmpty())
    }
    var notes by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.notes.orEmpty())
    }
    var active by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.active ?: true)
    }
    var error by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (existing == null) "Add IOP Group" else "Edit IOP Group")
                Text(
                    "Set the recurring day and time. Nothing is marked overdue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Day", fontWeight = FontWeight.SemiBold)
                (1..7).toList().chunked(4).forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowDays.forEach { day ->
                            FilterChip(
                                selected = dayOfWeek == day,
                                onClick = { dayOfWeek = day },
                                label = { Text(shortDayLabel(day)) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Start time") },
                    supportingText = { Text("Example: 6:30 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("End time") },
                    supportingText = { Text("Example: 8:30 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location or online link (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = active,
                        onCheckedChange = { active = it }
                    )
                    Column {
                        Text("Active", fontWeight = FontWeight.Medium)
                        Text(
                            "Paused groups stay saved but do not appear as upcoming.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startMinutes = parseTimeToMinutes(startText)
                    val endMinutes = parseTimeToMinutes(endText)
                    error = when {
                        name.isBlank() -> "Enter a group name."
                        startMinutes == null -> "Enter a valid start time, such as 6:30 PM."
                        endMinutes == null -> "Enter a valid end time, such as 8:30 PM."
                        endMinutes <= startMinutes -> "The end time must be after the start time."
                        else -> null
                    }

                    if (error == null && startMinutes != null && endMinutes != null) {
                        val now = System.currentTimeMillis()
                        onSave(
                            (existing ?: IopGroup(
                                dayOfWeek = dayOfWeek,
                                startMinutes = startMinutes,
                                endMinutes = endMinutes
                            )).copy(
                                name = name.trim(),
                                dayOfWeek = dayOfWeek,
                                startMinutes = startMinutes,
                                endMinutes = endMinutes,
                                location = location.trim(),
                                notes = notes.trim(),
                                active = active,
                                updatedAt = now
                            )
                        )
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Save Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HomeIopCard(
    occurrence: IopOccurrence?,
    onManage: () -> Unit
) {
    if (occurrence == null) return

    val today = LocalDate.now()
    val whenLabel = when (occurrence.date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> dayLabel(occurrence.date.dayOfWeek.value)
    }

    RebuildSectionCard(
        title = "Next IOP Group",
        subtitle = "$whenLabel · ${formatTimeRange(occurrence.group.startMinutes, occurrence.group.endMinutes)}",
        accentColor = RebuildBlue
    ) {
        Text(
            text = occurrence.group.name,
            fontWeight = FontWeight.SemiBold
        )
        if (occurrence.group.location.isNotBlank()) {
            Text(
                occurrence.group.location,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = onManage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage IOP Groups")
        }
    }
}

fun dayLabel(dayOfWeek: Int): String =
    DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
        .getDisplayName(java.time.format.TextStyle.FULL, Locale.US)

private fun shortDayLabel(dayOfWeek: Int): String =
    DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
        .getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)

fun formatTimeRange(startMinutes: Int, endMinutes: Int): String =
    "${formatMinutesForInput(startMinutes)}–${formatMinutesForInput(endMinutes)}"

private fun formatMinutesForInput(minutes: Int): String =
    minutesToLocalTime(minutes).format(
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    )

private fun parseTimeToMinutes(value: String): Int? {
    val normalized = value.trim().uppercase(Locale.US).replace(".", "")
    val formatters = listOf(
        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
        DateTimeFormatter.ofPattern("h a", Locale.US),
        DateTimeFormatter.ofPattern("H:mm", Locale.US)
    )
    return formatters.firstNotNullOfOrNull { formatter ->
        runCatching {
            val time = LocalTime.parse(normalized, formatter)
            time.hour * 60 + time.minute
        }.getOrNull()
    }
}

private fun minutesToLocalTime(minutes: Int): LocalTime {
    val safe = minutes.coerceIn(0, 23 * 60 + 59)
    return LocalTime.of(safe / 60, safe % 60)
}
