package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * One read-only history day.
 *
 * A day can contain a saved DailyRecord, food entries, an activity snapshot,
 * or any combination of them. Food is stored immediately, while the checklist
 * and Health Connect snapshot are stored when Save Today is pressed.
 */
data class DailyHistoryDay(
    val date: String,
    val record: DailyRecord?,
    val foodEntries: List<FoodLogEntry>,
    val activitySnapshot: DailyActivitySnapshot? = null,
    val mobilitySessions: List<MobilitySession> = emptyList(),
    val showerLogged: Boolean = false,
    val migraineLogs: List<MigraineLog> = emptyList(),
    val meetingAttendance: List<MeetingAttendance> = emptyList(),
    val careVisits: List<CareVisit> = emptyList(),
    val careAppointments: List<CareAppointment> = emptyList()
)


enum class DailyHistoryFilter(
    val label: String
) {
    ALL("All"),
    FOOD("Food"),
    MOBILITY("Mobility"),
    MEETINGS("Meetings"),
    HEALTH("Health"),
    SELF_CARE("Self-care")
}

@Composable
fun DailyHistoryDialog(
    days: List<DailyHistoryDay>,
    initialSelectedDate: String? = null,
    selectedFilter: DailyHistoryFilter,
    onFilterChange: (DailyHistoryFilter) -> Unit,
    isLoading: Boolean,
    isDeletingDay: Boolean,
    onDeleteDay: (DailyHistoryDay) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by rememberSaveable(initialSelectedDate) {
        mutableStateOf(initialSelectedDate)
    }

    var visibleMonthText by rememberSaveable(initialSelectedDate) {
        mutableStateOf(
            initialSelectedDate
                ?.let { runCatching { YearMonth.from(LocalDate.parse(it)) }.getOrNull() }
                ?.toString()
                ?: YearMonth.now().toString()
        )
    }

    LaunchedEffect(initialSelectedDate) {
        if (initialSelectedDate != null) {
            selectedDate = initialSelectedDate
            visibleMonthText = runCatching {
                YearMonth.from(LocalDate.parse(initialSelectedDate)).toString()
            }.getOrDefault(visibleMonthText)
        }
    }

    var dayPendingDeletion by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val filteredDays =
        days.filter { day ->
            dayMatchesHistoryFilter(day, selectedFilter)
        }

    val selectedDay =
        filteredDays.firstOrNull {
            it.date == selectedDate
        }

    val pendingDeletionDay =
        days.firstOrNull {
            it.date == dayPendingDeletion
        }

    Dialog(
        onDismissRequest = {
            if (!isDeletingDay) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (selectedDay == null) {
                DailyHistoryCalendarPage(
                    days = filteredDays,
                    allDayCount = days.size,
                    selectedFilter = selectedFilter,
                    onFilterChange = { filter ->
                        onFilterChange(filter)
                        selectedDate = null
                    },
                    isLoading = isLoading,
                    visibleMonth =
                        runCatching {
                            YearMonth.parse(
                                visibleMonthText
                            )
                        }.getOrDefault(
                            YearMonth.now()
                        ),
                    onVisibleMonthChange = {
                        visibleMonthText =
                            it.toString()
                    },
                    onSelectDay = {
                        selectedDate = it.date
                    },
                    onDismiss = onDismiss
                )
            } else {
                DailyHistoryDetailPage(
                    day = selectedDay,
                    isDeletingDay = isDeletingDay,
                    onBack = {
                        selectedDate = null
                    },
                    onRequestDelete = {
                        dayPendingDeletion =
                            selectedDay.date
                    }
                )
            }
        }
    }

    if (pendingDeletionDay != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingDay) {
                    dayPendingDeletion = null
                }
            },
            title = {
                Text("Delete Entire Day?")
            },
            text = {
                Text(
                    text =
                        "Delete ${formatHistoryDate(pendingDeletionDay.date)}? " +
                            "This permanently removes the saved checklist, " +
                            "water, pain, medications, journal, activity snapshot, " +
                            "mobility sessions, shower log, migraine events, meeting attendance, care appointments, care visits, individual foods, and logged meals for this date. Saved Places, Providers, Saved Foods, and " +
                            "Saved Meal templates will not be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDay(
                            pendingDeletionDay
                        )
                        dayPendingDeletion = null
                    },
                    enabled = !isDeletingDay
                ) {
                    Text(
                        if (isDeletingDay) {
                            "Deleting..."
                        } else {
                            "Delete Entire Day"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dayPendingDeletion = null
                    },
                    enabled = !isDeletingDay
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DailyHistoryCalendarPage(
    days: List<DailyHistoryDay>,
    allDayCount: Int,
    selectedFilter: DailyHistoryFilter,
    onFilterChange: (DailyHistoryFilter) -> Unit,
    isLoading: Boolean,
    visibleMonth: YearMonth,
    onVisibleMonthChange: (YearMonth) -> Unit,
    onSelectDay: (DailyHistoryDay) -> Unit,
    onDismiss: () -> Unit
) {
    val currentMonth = YearMonth.now()
    val earliestMonth = YearMonth.from(LocalDate.now().minusDays(364))

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
            Column(Modifier.weight(1f)) {
                RebuildStatusBadge(text = "${selectedFilter.label} · ${days.size} days")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Daily calendar",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (selectedFilter == DailyHistoryFilter.ALL) {
                        "Marked dates contain saved records, food, activity, meetings, self-care, or health events."
                    } else {
                        "Showing ${selectedFilter.label.lowercase(Locale.US)} dates only. $allDayCount total saved days remain available."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDismiss) { Text("Close") }
        }

        HistoryFilterChips(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange
        )

        RebuildInsetPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildStatusBadge(
                    text = "Saved",
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                RebuildStatusBadge(
                    text = "Food",
                    modifier = Modifier.weight(1f),
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                RebuildStatusBadge(
                    text = "Saved + Food",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CalendarMonthControls(
            visibleMonth = visibleMonth,
            canGoPrevious = visibleMonth > earliestMonth,
            canGoNext = visibleMonth < currentMonth,
            onPrevious = { onVisibleMonthChange(visibleMonth.minusMonths(1)) },
            onNext = { onVisibleMonthChange(visibleMonth.plusMonths(1)) }
        )

        if (isLoading) {
            RebuildInsetPanel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading saved dates…")
                }
            }
        } else {
            DailyCalendarMonth(
                visibleMonth = visibleMonth,
                days = days,
                onSelectDay = onSelectDay
            )
        }

        if (!isLoading && days.isEmpty()) {
            RebuildInsetPanel {
                Text("No history yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (selectedFilter == DailyHistoryFilter.ALL) {
                        "Save a day or log an event to place it on this calendar."
                    } else {
                        "No ${selectedFilter.label.lowercase(Locale.US)} records match this filter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthControls(
    visibleMonth: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
                shape = RoundedCornerShape(14.dp)
            ) { Text("‹") }
            Text(
                text = visibleMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedButton(
                onClick = onNext,
                enabled = canGoNext,
                shape = RoundedCornerShape(14.dp)
            ) { Text("›") }
        }
    }
}

@Composable
private fun DailyCalendarMonth(
    visibleMonth: YearMonth,
    days: List<DailyHistoryDay>,
    onSelectDay: (DailyHistoryDay) -> Unit
) {
    val daysByDate =
        days.associateBy {
            it.date
        }

    val firstDate =
        visibleMonth.atDay(1)

    val leadingBlankCount =
        firstDate.dayOfWeek.value % 7

    val cells =
        buildList<LocalDate?> {
            repeat(leadingBlankCount) {
                add(null)
            }

            for (
                dayNumber in
                1..visibleMonth.lengthOfMonth()
            ) {
                add(
                    visibleMonth.atDay(
                        dayNumber
                    )
                )
            }

            while (size % 7 != 0) {
                add(null)
            }
        }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "Sun",
                "Mon",
                "Tue",
                "Wed",
                "Thu",
                "Fri",
                "Sat"
            ).forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .padding(2.dp)
                        )
                    } else {
                        val historyDay =
                            daysByDate[
                                date.toString()
                            ]

                        CalendarDayCell(
                            date = date,
                            historyDay = historyDay,
                            onClick = {
                                if (historyDay != null) {
                                    onSelectDay(
                                        historyDay
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    historyDay: DailyHistoryDay?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSavedRecord =
        historyDay?.record != null ||
            historyDay?.activitySnapshot != null ||
            historyDay?.mobilitySessions?.isNotEmpty() == true ||
            historyDay?.showerLogged == true ||
            historyDay?.migraineLogs?.isNotEmpty() == true ||
            historyDay?.meetingAttendance?.isNotEmpty() == true
    val hasFood = historyDay?.foodEntries?.isNotEmpty() == true
    val isToday = date == LocalDate.now()
    val containerColor = when {
        hasSavedRecord && hasFood -> MaterialTheme.colorScheme.secondaryContainer
        hasSavedRecord -> MaterialTheme.colorScheme.primaryContainer
        hasFood -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        hasSavedRecord && hasFood -> MaterialTheme.colorScheme.onSecondaryContainer
        hasSavedRecord -> MaterialTheme.colorScheme.onPrimaryContainer
        hasFood -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .height(76.dp)
            .padding(2.dp)
            .clickable(enabled = historyDay != null, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (historyDay != null) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday || historyDay != null) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = when {
                    hasSavedRecord && hasFood -> "Both"
                    hasSavedRecord -> "Saved"
                    hasFood -> "Food"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            if (isToday) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DailyHistoryDetailPage(
    day: DailyHistoryDay,
    isDeletingDay: Boolean,
    onBack: () -> Unit,
    onRequestDelete: () -> Unit
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
            OutlinedButton(
                onClick = onBack,
                enabled = !isDeletingDay,
                shape = RoundedCornerShape(16.dp)
            ) { Text("‹ Calendar") }
            Spacer(Modifier.weight(1f))
            val hasSavedData =
                day.record != null ||
                    day.activitySnapshot != null ||
                    day.mobilitySessions.isNotEmpty() ||
                    day.showerLogged ||
                    day.migraineLogs.isNotEmpty() ||
                    day.meetingAttendance.isNotEmpty() ||
                    day.careVisits.isNotEmpty() ||
                    day.careAppointments.isNotEmpty()

            RebuildStatusBadge(
                text =
                    if (
                        hasSavedData &&
                        day.foodEntries.isNotEmpty()
                    ) {
                        "Saved + Food"
                    } else if (hasSavedData) {
                        "Saved"
                    } else {
                        "Food only"
                    }
            )
        }

        Text(
            text = formatHistoryDate(day.date),
            style = MaterialTheme.typography.headlineMedium
        )

        val record = day.record
        if (record == null) {
            RebuildInsetPanel {
                Text("Checklist not saved", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The daily checklist was not saved, but the day's logged food, activity, mobility, shower, migraine, meeting, appointment, or care-visit information remains available below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            HistoryChecklistCard(record)
            HistoryWaterCard(record)
            HistoryPainCard(record)
            HistoryMedicationCard(record)
        }

        day.activitySnapshot?.let {
            HistoryActivityCard(
                snapshot = it
            )
        }

        if (day.mobilitySessions.isNotEmpty()) {
            HistoryMobilityCard(
                sessions = day.mobilitySessions
            )
        }

        if (day.showerLogged) {
            HistoryShowerCard()
        }

        if (day.migraineLogs.isNotEmpty()) {
            HistoryMigraineCard(
                logs = day.migraineLogs
            )
        }

        if (day.meetingAttendance.isNotEmpty()) {
            HistoryMeetingCard(
                attendance = day.meetingAttendance
            )
        }

        if (day.careAppointments.isNotEmpty()) {
            HistoryCareAppointmentCard(
                appointments = day.careAppointments
            )
        }

        if (day.careVisits.isNotEmpty()) {
            HistoryCareVisitCard(
                visits = day.careVisits
            )
        }

        HistoryFoodCard(entries = day.foodEntries)
        if (record != null) {
            HistoryJournalCard(journalText = record.journalText)
        }

        RebuildInsetPanel(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
        ) {
            Text(
                text = "Delete this day",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Use this for test data or a date recorded by mistake. Saved Foods and Saved Meal templates are not removed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(
                onClick = onRequestDelete,
                enabled = !isDeletingDay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isDeletingDay) "Deleting day…" else "Delete entire day")
            }
        }
    }
}

@Composable
private fun HistoryActivityCard(
    snapshot: DailyActivitySnapshot
) {
    HistorySectionCard(
        title = "Activity"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "steps",
                value =
                    String.format(
                        Locale.US,
                        "%,d",
                        snapshot.steps
                    ),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            )

            RebuildMetricPill(
                label = "miles",
                value =
                    String.format(
                        Locale.US,
                        "%.2f",
                        snapshot.distanceMiles
                    ),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer,
                contentColor =
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
            )

            RebuildMetricPill(
                label = "time",
                value =
                    formatHistoryActivityTime(
                        snapshot.activityMinutes
                    ),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme
                        .colorScheme
                        .tertiaryContainer,
                contentColor =
                    MaterialTheme
                        .colorScheme
                        .onTertiaryContainer
            )
        }

        Text(
            text = "Health Connect snapshot saved with this day.",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryShowerCard() {
    HistorySectionCard(
        title = "Showering"
    ) {
        RebuildMetricPill(
            label = "status",
            value = "Showered",
            modifier = Modifier.fillMaxWidth(),
            color =
                MaterialTheme.colorScheme
                    .secondaryContainer,
            contentColor =
                MaterialTheme.colorScheme
                    .onSecondaryContainer
        )

        Text(
            text =
                "This date counts toward the weekly goal of 2–3 showers.",
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryMigraineCard(
    logs: List<MigraineLog>
) {
    HistorySectionCard(
        title = "Migraine & Visual Aura"
    ) {
        logs.sortedBy { it.occurredAt }
            .forEach { log ->
                RebuildInsetPanel {
                    Text(
                        text = formatMigraineHistoryTime(
                            log.occurredAt
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = buildMigraineHistorySummary(log),
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
            }
    }
}

@Composable
private fun HistoryMeetingCard(
    attendance: List<MeetingAttendance>
) {
    HistorySectionCard(
        title = "Recovery Meetings"
    ) {
        attendance.sortedBy { it.startedAt }
            .forEach { item ->
                RebuildInsetPanel {
                    Text(
                        text = item.meetingName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatMeetingHistoryTime(item.startedAt) +
                            " · ${item.durationMinutes} minutes · ${item.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val location = listOf(
                        item.address,
                        listOf(item.city, item.state, item.zipCode)
                            .filter(String::isNotBlank)
                            .joinToString(" ")
                    ).filter(String::isNotBlank).joinToString(" · ")

                    if (location.isNotBlank()) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.notes.isNotBlank()) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
    }
}

@Composable
private fun HistoryCareAppointmentCard(
    appointments: List<CareAppointment>
) {
    HistorySectionCard(
        title = "Care Appointments"
    ) {
        appointments.sortedBy { it.scheduledAt }
            .forEach { appointment ->
                RebuildInsetPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appointment.placeName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatAppointmentDateTime(
                                    appointment.scheduledAt
                                ) + " · ${appointment.visitCategory}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RebuildStatusBadge(appointment.status)
                    }

                    val provider =
                        appointmentProviderDisplay(appointment)
                    if (provider.isNotBlank()) {
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (appointment.reasonForAppointment.isNotBlank()) {
                        Text(
                            text = "Reason: ${appointment.reasonForAppointment}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    careVisitDetailLine(
                        label = "Location",
                        value = appointmentLocationText(
                            appointment.address,
                            appointment.city,
                            appointment.state,
                            appointment.zipCode
                        )
                    )
                    careVisitDetailLine(
                        label = "Transportation",
                        value = listOf(
                            appointment.transportationMode
                                .takeUnless { it == "Not planned" }
                                .orEmpty(),
                            appointment.transportationDetails,
                            appointment.leaveByAt?.let {
                                "Leave by ${formatAppointmentDateTime(it)}"
                            }.orEmpty()
                        ).filter(String::isNotBlank)
                            .joinToString(" · ")
                    )
                    careVisitDetailLine(
                        label = "Questions",
                        value = appointment.questionsToAsk
                    )
                    careVisitDetailLine(
                        label = "Documents",
                        value = appointment.documentsToBring
                    )
                }
            }
    }
}

@Composable
private fun HistoryCareVisitCard(
    visits: List<CareVisit>
) {
    HistorySectionCard(
        title = "Care Visits"
    ) {
        visits.sortedBy { it.startedAt }
            .forEach { visit ->
                RebuildInsetPanel {
                    Text(
                        text = visit.placeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatCareVisitDateTime(visit.startedAt) +
                            " · ${visit.visitCategory} · ${visit.visitFormat}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val provider = listOf(
                        listOf(
                            visit.providerName,
                            visit.providerCredentials
                        ).filter(String::isNotBlank)
                            .joinToString(", "),
                        visit.providerSpecialty
                    ).filter(String::isNotBlank)
                        .joinToString(" · ")

                    if (provider.isNotBlank()) {
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    val location = careLocationText(
                        visit.address,
                        visit.city,
                        visit.state,
                        visit.zipCode
                    )
                    if (location.isNotBlank()) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Reason: ${visit.reasonForVisit}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (visit.visitSummary.isNotBlank()) {
                        Text(
                            text = visit.visitSummary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    careVisitDetailLine(
                        label = "Tests / procedures",
                        value = visit.testsProcedures
                    )
                    careVisitDetailLine(
                        label = "Results",
                        value = visit.resultsDiscussed
                    )
                    careVisitDetailLine(
                        label = "Instructions",
                        value = visit.instructions
                    )
                    careVisitDetailLine(
                        label = "Medication changes",
                        value = visit.medicationChanges
                    )
                    careVisitDetailLine(
                        label = "Referrals",
                        value = visit.referrals
                    )
                    visit.followUpDate?.let {
                        careVisitDetailLine(
                            label = "Follow-up",
                            value = formatHistoryDate(it)
                        )
                    }
                    careVisitDetailLine(
                        label = "Notes",
                        value = visit.notes
                    )

                    val measurements = buildList {
                        visit.weightPounds?.let {
                            add("Weight ${formatHistoryNumber(it)} lb")
                        }
                        if (
                            visit.systolic != null &&
                            visit.diastolic != null
                        ) {
                            add("BP ${visit.systolic}/${visit.diastolic}")
                        }
                        visit.a1c?.let {
                            add("A1C ${formatHistoryNumber(it)}%")
                        }
                        visit.bloodGlucose?.let {
                            add("Glucose ${formatHistoryNumber(it)}")
                        }
                        visit.cholesterolTotal?.let {
                            add("Total cholesterol ${formatHistoryNumber(it)}")
                        }
                    }

                    if (measurements.isNotEmpty()) {
                        Text(
                            text = measurements.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
    }
}

@Composable
private fun careVisitDetailLine(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryMobilityCard(
    sessions: List<MobilitySession>
) {
    val totalSeconds =
        sessions.sumOf { it.elapsedSeconds }

    HistorySectionCard(
        title = "Mobility & Stretching"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "sessions",
                value = sessions.size.toString(),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme.primaryContainer
            )

            RebuildMetricPill(
                label = "time",
                value =
                    formatMobilityDuration(
                        totalSeconds
                    ),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme.secondaryContainer,
                contentColor =
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        sessions.forEach { session ->
            val plannedCount =
                decodeMovementIds(
                    session.plannedMovementIds
                ).size
            val completedCount =
                decodeMovementIds(
                    session.completedMovementIds
                ).size
            val skippedCount =
                decodeMovementIds(
                    session.skippedMovementIds
                ).size

            RebuildInsetPanel {
                Text(
                    text = session.routineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = buildString {
                        append(
                            formatMobilityDuration(
                                session.elapsedSeconds
                            )
                        )

                        if (plannedCount > 0) {
                            append(" · ")
                            append(completedCount)
                            append(" of ")
                            append(plannedCount)
                            append(" completed")

                            if (skippedCount > 0) {
                                append(" · ")
                                append(skippedCount)
                                append(" skipped")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.notes.isNotBlank()) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryChecklistCard(
    record: DailyRecord
) {
    HistorySectionCard(
        title = "Daily Checklist"
    ) {
        HistoryStatusRow(
            label = "Record food and drinks",
            completed = record.foodRecorded
        )
        HistoryStatusRow(
            label = "Complete walk or movement",
            completed = record.walkCompleted
        )
        HistoryStatusRow(
            label = "Record pain",
            completed = record.painRecorded
        )
        HistoryStatusRow(
            label = "Complete mobility work",
            completed = record.mobilityCompleted
        )
    }
}

@Composable
private fun HistoryWaterCard(
    record: DailyRecord
) {
    val totalOunces =
        calculateHistoryWaterOunces(
            record
        )

    HistorySectionCard(
        title = "Water"
    ) {
        Text(
            text =
                "Total: ${formatHistoryOunces(totalOunces)} oz",
            fontWeight = FontWeight.SemiBold
        )

        HistoryCountLine(
            label = "24 oz plain water",
            count = record.plainReusableBottleCount
        )
        HistoryCountLine(
            label = "24 oz MiO water",
            count = record.mioReusableBottleCount
        )
        HistoryCountLine(
            label = "16.9 oz plain water",
            count = record.plainDisposableBottleCount
        )
        HistoryCountLine(
            label = "16.9 oz MiO water",
            count = record.mioDisposableBottleCount
        )

        if (
            record.plainReusableBottleCount == 0 &&
            record.mioReusableBottleCount == 0 &&
            record.plainDisposableBottleCount == 0 &&
            record.mioDisposableBottleCount == 0
        ) {
            Text("No water bottles recorded.")
        }
    }
}

@Composable
private fun HistoryPainCard(
    record: DailyRecord
) {
    val highestPain = maxOf(
        record.backPain,
        record.shinPain
    )

    HistorySectionCard(
        title = "Highest Pain"
    ) {
        Text(
            text =
                "Highest pain that day: ${formatHistoryPain(highestPain)} / 10"
        )
        Text(
            text = "Older records automatically use the higher of the former back and shin values.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryMedicationCard(
    record: DailyRecord
) {
    val morning = buildList {
        if (record.morningAspirinTaken) {
            add("Aspirin")
        }
        if (record.morningIbuprofenTaken) {
            add("Ibuprofen")
        }
        if (record.morningNaproxenTaken) {
            add("Naproxen")
        }
        if (record.morningAcetaminophenTaken) {
            add("Acetaminophen")
        }
    }

    val night = buildList {
        if (record.nightIbuprofenTaken) {
            add("Ibuprofen")
        }
        if (record.nightNaproxenTaken) {
            add("Naproxen")
        }
        if (record.nightAcetaminophenTaken) {
            add("Acetaminophen")
        }
    }

    HistorySectionCard(
        title = "OTC Pain Relievers"
    ) {
        Text(
            text = "Morning",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text =
                if (morning.isEmpty()) {
                    "None marked"
                } else {
                    morning.joinToString(", ")
                }
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "Night",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text =
                if (night.isEmpty()) {
                    "None marked"
                } else {
                    night.joinToString(", ")
                }
        )
    }
}

@Composable
private fun HistoryFoodCard(
    entries: List<FoodLogEntry>
) {
    val totalCalories =
        entries.sumOf { it.calories }
    val totalProtein =
        entries.sumOf { it.proteinGrams }
    val totalCarbohydrates =
        entries.sumOf { it.carbohydrateGrams }
    val totalFat =
        entries.sumOf { it.fatGrams }
    val totalSodium =
        entries.sumOf { it.sodiumMilligrams }

    val mealGroups =
        entries
            .filter {
                !it.mealLogId.isNullOrBlank()
            }
            .groupBy {
                it.mealLogId.orEmpty()
            }

    val individualEntries =
        entries.filter {
            it.mealLogId.isNullOrBlank()
        }

    HistorySectionCard(
        title = "Food and Nutrition"
    ) {
        if (entries.isEmpty()) {
            Text("No food recorded.")
        } else {
            Text(
                text =
                    "${formatHistoryNumber(totalCalories)} calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text =
                    "${formatHistoryNumber(totalProtein)} g protein  •  " +
                        "${formatHistoryNumber(totalCarbohydrates)} g carbs"
            )
            Text(
                text =
                    "${formatHistoryNumber(totalFat)} g fat  •  " +
                        "${formatHistoryNumber(totalSodium)} mg sodium"
            )

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 8.dp
                )
            )

            mealGroups.values.forEachIndexed { index, mealEntries ->
                HistoryMealGroup(
                    entries = mealEntries
                )

                if (
                    index < mealGroups.size - 1 ||
                    individualEntries.isNotEmpty()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }
            }

            individualEntries.forEachIndexed { index, entry ->
                HistoryFoodEntry(entry)

                if (index < individualEntries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryMealGroup(
    entries: List<FoodLogEntry>
) {
    val mealName =
        entries.firstOrNull()
            ?.mealName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Saved Meal"

    Text(
        text = mealName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Text(
        text =
            "${formatHistoryNumber(entries.sumOf { it.calories })} calories  •  " +
                "${formatHistoryNumber(entries.sumOf { it.proteinGrams })} g protein",
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    entries.forEach { entry ->
        Text(
            text =
                "• ${entry.productNameSnapshot} — " +
                    "${formatHistoryNumber(entry.quantity)} ${entry.unit} " +
                    "(${formatHistoryNumber(entry.calories)} calories)",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HistoryFoodEntry(
    entry: FoodLogEntry
) {
    if (!entry.mealName.isNullOrBlank()) {
        Text(
            text = entry.mealName,
            style = MaterialTheme.typography.labelLarge
        )
    }

    Text(
        text = entry.productNameSnapshot,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text =
            "${formatHistoryNumber(entry.quantity)} ${entry.unit}  •  " +
                "${formatHistoryNumber(entry.calories)} calories",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun HistoryJournalCard(
    journalText: String
) {
    HistorySectionCard(
        title = "Journal"
    ) {
        Text(
            text =
                journalText.takeIf {
                    it.isNotBlank()
                } ?: "No journal entry."
        )
    }
}

@Composable
private fun HistorySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    RebuildSectionCard(
        title = title,
        accentColor = when (title) {
            "Activity" -> RebuildBlue
            "Water" -> RebuildTeal
            "Pain" -> RebuildAmber
            "Food and Nutrition" -> RebuildGreen
            else -> RebuildBlue
        }
    ) {
        content()
    }
}

@Composable
private fun HistoryStatusRow(
    label: String,
    completed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text =
                if (completed) {
                    "✓"
                } else {
                    "○"
                },
            modifier = Modifier.padding(
                end = 8.dp
            ),
            fontWeight = FontWeight.Bold
        )

        Text(label)
    }
}

@Composable
private fun HistoryCountLine(
    label: String,
    count: Int
) {
    if (count > 0) {
        Text("$label × $count")
    }
}

private fun calculateHistoryWaterOunces(
    record: DailyRecord?
): Double {
    if (record == null) {
        return 0.0
    }

    val reusableCount =
        record.plainReusableBottleCount +
            record.mioReusableBottleCount

    val disposableCount =
        record.plainDisposableBottleCount +
            record.mioDisposableBottleCount

    return reusableCount * 24.0 +
        disposableCount * 16.9
}

private fun formatMeetingHistoryTime(
    startedAt: Long
): String {
    return Instant.ofEpochMilli(startedAt)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "h:mm a",
                Locale.US
            )
        )
}

private fun formatMigraineHistoryTime(
    occurredAt: Long
): String {
    return Instant.ofEpochMilli(occurredAt)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "h:mm a",
                Locale.US
            )
        )
}

private fun buildMigraineHistorySummary(
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

@Composable
private fun HistoryFilterChips(
    selectedFilter: DailyHistoryFilter,
    onFilterChange: (DailyHistoryFilter) -> Unit
) {
    DailyHistoryFilter.values().toList().chunked(3).forEach { filters ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(3 - filters.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun dayMatchesHistoryFilter(
    day: DailyHistoryDay,
    filter: DailyHistoryFilter
): Boolean {
    val record = day.record

    return when (filter) {
        DailyHistoryFilter.ALL -> true
        DailyHistoryFilter.FOOD ->
            day.foodEntries.isNotEmpty() ||
                record?.foodRecorded == true
        DailyHistoryFilter.MOBILITY ->
            day.activitySnapshot != null ||
                day.mobilitySessions.isNotEmpty() ||
                record?.walkCompleted == true ||
                record?.mobilityCompleted == true
        DailyHistoryFilter.MEETINGS ->
            day.meetingAttendance.isNotEmpty()
        DailyHistoryFilter.HEALTH ->
            day.migraineLogs.isNotEmpty() ||
                day.careVisits.isNotEmpty() ||
                day.careAppointments.isNotEmpty()
        DailyHistoryFilter.SELF_CARE ->
            day.showerLogged ||
                record?.painRecorded == true ||
                record?.journalText?.isNotBlank() == true
    }
}

private fun formatHistoryDate(
    dateText: String
): String {
    return runCatching {
        LocalDate.parse(dateText).format(
            DateTimeFormatter.ofPattern(
                "EEEE, MMMM d, yyyy",
                Locale.US
            )
        )
    }.getOrDefault(dateText)
}

private fun formatHistoryPain(
    value: Float
): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            value
        )
    }
}

private fun formatHistoryOunces(
    ounces: Double
): String {
    return if (ounces % 1.0 == 0.0) {
        ounces.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            ounces
        )
    }
}

private fun formatHistoryActivityTime(
    totalMinutes: Long
): String {
    if (totalMinutes <= 0L) {
        return "0m"
    }

    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

private fun formatHistoryNumber(
    value: Double
): String {
    if (abs(value) < 0.0000001) {
        return "0"
    }

    return BigDecimal.valueOf(value)
        .stripTrailingZeros()
        .toPlainString()
}
