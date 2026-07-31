package com.pgdevhouse.dailyrebuild

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val DEFAULT_WEEKLY_MEETING_GOAL = 3

data class SavedMeetingDraft(
    val id: Long = 0L,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val typicalDurationMinutes: Int,
    val favorite: Boolean,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class MeetingAttendanceDraft(
    val id: Long = 0L,
    val savedMeetingId: Long? = null,
    val date: String,
    val startedAt: Long,
    val durationMinutes: Int,
    val meetingName: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val role: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Composable
fun MeetingsTab(
    weeklyAttendance: List<MeetingAttendance>,
    isSaving: Boolean,
    onLogMeeting: () -> Unit,
    onAddMeeting: () -> Unit,
    onEditAttendance: (MeetingAttendance) -> Unit,
    onDeleteAttendance: (MeetingAttendance) -> Unit,
    onViewFullHistory: () -> Unit
) {
    val count = weeklyAttendance.size
    val goalReached = count >= DEFAULT_WEEKLY_MEETING_GOAL

    RebuildSectionCard(
        title = "Weekly meeting goal",
        subtitle =
            if (goalReached) {
                "$count of $DEFAULT_WEEKLY_MEETING_GOAL · weekly goal reached"
            } else {
                "$count of $DEFAULT_WEEKLY_MEETING_GOAL · ${DEFAULT_WEEKLY_MEETING_GOAL - count} remaining"
            },
        accentColor = RebuildTeal
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            progress =
                (count.toFloat() / DEFAULT_WEEKLY_MEETING_GOAL.toFloat())
                    .coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLogMeeting,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Meeting")
            }

            OutlinedButton(
                onClick = onAddMeeting,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Meeting")
            }
        }
    }

    RebuildSectionCard(
        title = "This week",
        subtitle = "Only meetings you attended this Monday through Sunday appear here.",
        accentColor = RebuildBlue
    ) {
        if (weeklyAttendance.isEmpty()) {
            Text(
                text = "No meetings have been logged this week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            weeklyAttendance.forEachIndexed { index, attendance ->
                MeetingAttendanceRow(
                    attendance = attendance,
                    onEdit = { onEditAttendance(attendance) },
                    onDelete = { onDeleteAttendance(attendance) }
                )

                if (index < weeklyAttendance.lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        OutlinedButton(
            onClick = onViewFullHistory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("View Full Meeting History")
        }
    }
}

@Composable
fun HomeMeetingsCard(
    meetingsThisWeek: Int,
    onOpenMeetings: () -> Unit,
    onLogMeeting: () -> Unit
) {
    RebuildSectionCard(
        title = "Meetings",
        subtitle = "$meetingsThisWeek of $DEFAULT_WEEKLY_MEETING_GOAL this week",
        accentColor = RebuildTeal
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLogMeeting,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Meeting")
            }

            OutlinedButton(
                onClick = onOpenMeetings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open Meetings")
            }
        }
    }
}

@Composable
private fun MeetingAttendanceRow(
    attendance: MeetingAttendance,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTime = remember(attendance.startedAt) {
        Instant.ofEpochMilli(attendance.startedAt)
            .atZone(ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendance.meetingName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = dateTime.format(
                        DateTimeFormatter.ofPattern(
                            "EEEE, MMMM d · h:mm a",
                            Locale.US
                        )
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                meetingLocationText(attendance)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${attendance.durationMinutes} minutes · ${attendance.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onEdit) {
                Text("Edit")
            }
        }

        if (attendance.notes.isNotBlank()) {
            Text(
                text = attendance.notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
fun MeetingPickerDialog(
    recentMeetings: List<SavedMeeting>,
    onSelectMeeting: (SavedMeeting) -> Unit,
    onEditMeeting: (SavedMeeting) -> Unit,
    onAddMeeting: () -> Unit,
    onLogOneTime: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        RebuildStatusBadge(text = "Fast attendance")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Log meeting",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Choose a meeting you have used before, add a new one, or log a one-time meeting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddMeeting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Add New")
                    }

                    OutlinedButton(
                        onClick = onLogOneTime,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("One-Time")
                    }
                }

                RebuildSectionCard(
                    title = "Recent meetings",
                    subtitle = "Most recently attended meetings appear first. Favorites remain easy to find.",
                    accentColor = RebuildTeal
                ) {
                    if (recentMeetings.isEmpty()) {
                        Text(
                            text = "No recent meetings yet. Add a meeting or log a one-time meeting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentMeetings.forEachIndexed { index, meeting ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectMeeting(meeting)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text =
                                                if (meeting.favorite) {
                                                    "★ ${meeting.name}"
                                                } else {
                                                    meeting.name
                                                },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = savedMeetingLocationText(meeting),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            onEditMeeting(meeting)
                                        }
                                    ) {
                                        Text("Edit")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        onSelectMeeting(meeting)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Log This Meeting")
                                }
                            }

                            if (index < recentMeetings.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingEditorDialog(
    existingMeeting: SavedMeeting?,
    isSaving: Boolean,
    onSave: (SavedMeetingDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.name.orEmpty())
    }
    var address by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.address.orEmpty())
    }
    var city by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.city.orEmpty())
    }
    var state by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.state.orEmpty())
    }
    var zipCode by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.zipCode.orEmpty())
    }
    var durationText by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(
            (existingMeeting?.typicalDurationMinutes ?: 60).toString()
        )
    }
    var favorite by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.favorite ?: false)
    }
    var notes by rememberSaveable(existingMeeting?.id) {
        mutableStateOf(existingMeeting?.notes.orEmpty())
    }
    var validationMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            if (existingMeeting == null) {
                                "Add meeting"
                            } else {
                                "Edit meeting"
                            },
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Close")
                    }
                }

                Text(
                    text = "Save the meeting once, then choose it from Recent Meetings when you attend again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meeting name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it.uppercase(Locale.US).take(2) },
                        label = { Text("State") },
                        modifier = Modifier.weight(0.55f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = zipCode,
                    onValueChange = { zipCode = it.take(10) },
                    label = { Text("ZIP code (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        durationText = it.filter(Char::isDigit).take(3)
                    },
                    label = { Text("Typical duration in minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = favorite,
                        onCheckedChange = { favorite = it }
                    )
                    Text("Favorite meeting")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Meeting notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                validationMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull()
                        validationMessage = when {
                            name.isBlank() -> "Enter the meeting name."
                            address.isBlank() -> "Enter the street address."
                            city.isBlank() -> "Enter the city."
                            state.length != 2 -> "Enter a two-letter state abbreviation."
                            duration == null || duration !in 1..480 ->
                                "Enter a duration from 1 to 480 minutes."
                            else -> null
                        }

                        if (validationMessage == null) {
                            onSave(
                                SavedMeetingDraft(
                                    id = existingMeeting?.id ?: 0L,
                                    name = name.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    state = state.trim().uppercase(Locale.US),
                                    zipCode = zipCode.trim(),
                                    typicalDurationMinutes = duration!!,
                                    favorite = favorite,
                                    notes = notes.trim(),
                                    createdAt = existingMeeting?.createdAt
                                        ?: System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            if (existingMeeting == null) {
                                "Save Meeting"
                            } else {
                                "Save Changes"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingAttendanceDialog(
    savedMeeting: SavedMeeting?,
    existingAttendance: MeetingAttendance?,
    isOneTimeMeeting: Boolean,
    isSaving: Boolean,
    onSave: (MeetingAttendanceDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialDateTime = remember(existingAttendance?.id, savedMeeting?.id) {
        existingAttendance?.let {
            Instant.ofEpochMilli(it.startedAt)
                .atZone(ZoneId.systemDefault())
        } ?: java.time.ZonedDateTime.now()
    }

    var selectedDateText by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(initialDateTime.toLocalDate().toString())
    }
    var selectedHour by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(initialDateTime.hour)
    }
    var selectedMinute by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(initialDateTime.minute)
    }
    var meetingName by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            existingAttendance?.meetingName
                ?: savedMeeting?.name
                ?: ""
        )
    }
    var address by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            existingAttendance?.address
                ?: savedMeeting?.address
                ?: ""
        )
    }
    var city by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            existingAttendance?.city
                ?: savedMeeting?.city
                ?: ""
        )
    }
    var state by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            existingAttendance?.state
                ?: savedMeeting?.state
                ?: ""
        )
    }
    var zipCode by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            existingAttendance?.zipCode
                ?: savedMeeting?.zipCode
                ?: ""
        )
    }
    var durationText by rememberSaveable(existingAttendance?.id, savedMeeting?.id) {
        mutableStateOf(
            (existingAttendance?.durationMinutes
                ?: savedMeeting?.typicalDurationMinutes
                ?: 60).toString()
        )
    }
    var role by rememberSaveable(existingAttendance?.id) {
        mutableStateOf(existingAttendance?.role ?: "Attended")
    }
    var notes by rememberSaveable(existingAttendance?.id) {
        mutableStateOf(existingAttendance?.notes.orEmpty())
    }
    var validationMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val selectedDate = runCatching {
        LocalDate.parse(selectedDateText)
    }.getOrDefault(LocalDate.now())

    val selectedTime = LocalTime.of(selectedHour, selectedMinute)

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text =
                                if (existingAttendance == null) {
                                    "Log meeting attendance"
                                } else {
                                    "Edit attendance"
                                },
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text =
                                if (savedMeeting != null) {
                                    "Using saved meeting information"
                                } else {
                                    "One-time meeting"
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Close")
                    }
                }

                if (
                    isOneTimeMeeting ||
                    (
                        existingAttendance != null &&
                            existingAttendance.savedMeetingId == null
                    )
                ) {
                    OutlinedTextField(
                        value = meetingName,
                        onValueChange = { meetingName = it },
                        label = { Text("Meeting name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it.uppercase(Locale.US).take(2) },
                            label = { Text("State") },
                            modifier = Modifier.weight(0.55f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = zipCode,
                        onValueChange = { zipCode = it.take(10) },
                        label = { Text("ZIP code (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    RebuildInsetPanel {
                        Text(
                            text = meetingName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOf(
                                address,
                                listOf(city, state, zipCode)
                                    .filter(String::isNotBlank)
                                    .joinToString(" ")
                            ).filter(String::isNotBlank).joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    selectedDateText =
                                        LocalDate.of(year, month + 1, day).toString()
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            selectedDate.format(
                                DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                },
                                selectedHour,
                                selectedMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            selectedTime.format(
                                DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        durationText = it.filter(Char::isDigit).take(3)
                    },
                    label = { Text("Duration in minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (optional)") },
                    supportingText = {
                        Text("Examples: Attended, Chaired, Spoke, Service work")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                validationMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull()
                        validationMessage = when {
                            meetingName.isBlank() -> "Enter the meeting name."
                            isOneTimeMeeting && address.isBlank() ->
                                "Enter the street address."
                            isOneTimeMeeting && city.isBlank() ->
                                "Enter the city."
                            isOneTimeMeeting && state.length != 2 ->
                                "Enter a two-letter state abbreviation."
                            duration == null || duration !in 1..480 ->
                                "Enter a duration from 1 to 480 minutes."
                            else -> null
                        }

                        if (validationMessage == null) {
                            val startedAt = selectedDate
                                .atTime(selectedTime)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()

                            onSave(
                                MeetingAttendanceDraft(
                                    id = existingAttendance?.id ?: 0L,
                                    savedMeetingId = existingAttendance?.savedMeetingId
                                        ?: savedMeeting?.id,
                                    date = selectedDate.toString(),
                                    startedAt = startedAt,
                                    durationMinutes = duration!!,
                                    meetingName = meetingName.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    state = state.trim().uppercase(Locale.US),
                                    zipCode = zipCode.trim(),
                                    role = role.trim().ifBlank { "Attended" },
                                    notes = notes.trim(),
                                    createdAt = existingAttendance?.createdAt
                                        ?: System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            if (existingAttendance == null) {
                                "Log Attendance"
                            } else {
                                "Save Changes"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingHistoryDialog(
    attendance: List<MeetingAttendance>,
    onEdit: (MeetingAttendance) -> Unit,
    onDelete: (MeetingAttendance) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        RebuildStatusBadge(text = "${attendance.size} attendance records")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Meeting history",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                if (attendance.isEmpty()) {
                    RebuildInsetPanel {
                        Text("No meetings logged yet.")
                    }
                } else {
                    RebuildSectionCard(
                        title = "All attendance",
                        accentColor = RebuildBlue
                    ) {
                        attendance.forEachIndexed { index, item ->
                            MeetingAttendanceRow(
                                attendance = item,
                                onEdit = { onEdit(item) },
                                onDelete = { onDelete(item) }
                            )
                            if (index < attendance.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteMeetingAttendanceDialog(
    attendance: MeetingAttendance,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        title = { Text("Delete Meeting Attendance?") },
        text = {
            Text(
                "Remove ${attendance.meetingName} from ${attendance.date}? " +
                    "The saved meeting remains available for future logging."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                Text(if (isDeleting) "Deleting…" else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun savedMeetingLocationText(
    meeting: SavedMeeting
): String =
    listOf(
        meeting.address,
        listOf(meeting.city, meeting.state, meeting.zipCode)
            .filter(String::isNotBlank)
            .joinToString(" ")
    ).filter(String::isNotBlank).joinToString(" · ")

private fun meetingLocationText(
    attendance: MeetingAttendance
): String? {
    val location = listOf(
        attendance.address,
        listOf(attendance.city, attendance.state, attendance.zipCode)
            .filter(String::isNotBlank)
            .joinToString(" ")
    ).filter(String::isNotBlank).joinToString(" · ")

    return location.takeIf(String::isNotBlank)
}
