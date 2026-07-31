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
import androidx.compose.runtime.remember
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
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
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
 * One history day.
 *
 * Daily information is stored automatically. Past dates can also be corrected
 * from History when food or water was forgotten.
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
    val careAppointments: List<CareAppointment> = emptyList(),
    val lifeMaintenanceLogs: List<LifeMaintenanceLog> = emptyList()
)

data class WaterBottleCounts(
    val plainReusable: Int = 0,
    val mioReusable: Int = 0,
    val plainDisposable: Int = 0,
    val mioDisposable: Int = 0
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
    isUpdatingDay: Boolean,
    onAddSavedFood: (DailyHistoryDay) -> Unit,
    onAddSavedMeal: (DailyHistoryDay) -> Unit,
    onAddFoodManually: (DailyHistoryDay) -> Unit,
    onUpdateWater: (DailyHistoryDay, WaterBottleCounts) -> Unit,
    onUpdateFoodEntryQuantity: (DailyHistoryDay, FoodLogEntry, Double) -> Unit,
    onDeleteFoodEntry: (DailyHistoryDay, FoodLogEntry) -> Unit,
    onDeleteMealLog: (DailyHistoryDay, String) -> Unit,
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

    var dayPendingDeletion by remember {
        mutableStateOf<String?>(null)
    }

    var waterEditorDate by remember {
        mutableStateOf<String?>(null)
    }

    var foodEntryPendingDeletionId by remember {
        mutableStateOf<Long?>(null)
    }

    var foodEntryQuantityEditorId by remember {
        mutableStateOf<Long?>(null)
    }

    var mealLogPendingDeletionId by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(selectedDate) {
        dayPendingDeletion = null
        waterEditorDate = null
        foodEntryPendingDeletionId = null
        foodEntryQuantityEditorId = null
        mealLogPendingDeletionId = null
    }

    val filteredDays =
        days.filter { day ->
            dayMatchesHistoryFilter(day, selectedFilter)
        }

    val selectedDay =
        selectedDate?.let { date ->
            filteredDays.firstOrNull {
                it.date == date
            } ?: if (
                selectedFilter == DailyHistoryFilter.ALL &&
                runCatching {
                    !LocalDate.parse(date).isAfter(LocalDate.now())
                }.getOrDefault(false)
            ) {
                DailyHistoryDay(
                    date = date,
                    record = null,
                    foodEntries = emptyList()
                )
            } else {
                null
            }
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
                    onSelectDate = {
                        selectedDate = it
                    },
                    onDismiss = onDismiss
                )
            } else {
                DailyHistoryDetailPage(
                    day = selectedDay,
                    isDeletingDay = isDeletingDay,
                    isUpdatingDay = isUpdatingDay,
                    onBack = {
                        selectedDate = null
                    },
                    onAddSavedFood = { onAddSavedFood(selectedDay) },
                    onAddSavedMeal = { onAddSavedMeal(selectedDay) },
                    onAddFoodManually = { onAddFoodManually(selectedDay) },
                    onEditWater = { waterEditorDate = selectedDay.date },
                    onRequestEditFoodEntry = { entry ->
                        foodEntryQuantityEditorId = entry.id
                    },
                    onRequestDeleteFoodEntry = { entry ->
                        foodEntryPendingDeletionId = entry.id
                    },
                    onRequestDeleteMeal = { mealLogId ->
                        mealLogPendingDeletionId = mealLogId
                    },
                    onRequestDelete = {
                        dayPendingDeletion = selectedDay.date
                    }
                )
            }
        }
    }

    val waterEditorDay =
        waterEditorDate?.let { date ->
            days.firstOrNull { it.date == date }
                ?: if (
                    runCatching {
                        !LocalDate.parse(date).isAfter(LocalDate.now())
                    }.getOrDefault(false)
                ) {
                    DailyHistoryDay(
                        date = date,
                        record = null,
                        foodEntries = emptyList()
                    )
                } else {
                    null
                }
        }
    if (waterEditorDay != null) {
        HistoricalWaterEditorDialog(
            day = waterEditorDay,
            isSaving = isUpdatingDay,
            onCountsChange = { counts ->
                onUpdateWater(waterEditorDay, counts)
            },
            onDismiss = { waterEditorDate = null }
        )
    }

    val quantityEditorEntry =
        selectedDay?.foodEntries?.firstOrNull {
            it.id == foodEntryQuantityEditorId
        }

    if (quantityEditorEntry != null && selectedDay != null) {
        FoodQuantityEditDialog(
            entry = quantityEditorEntry,
            isSaving = isUpdatingDay,
            onDismiss = {
                if (!isUpdatingDay) {
                    foodEntryQuantityEditorId = null
                }
            },
            onSave = { newQuantity ->
                foodEntryQuantityEditorId = null
                onUpdateFoodEntryQuantity(
                    selectedDay,
                    quantityEditorEntry,
                    newQuantity
                )
            }
        )
    }

    val pendingFoodEntry = selectedDay?.foodEntries?.firstOrNull {
        it.id == foodEntryPendingDeletionId
    }
    if (pendingFoodEntry != null && selectedDay != null) {
        AlertDialog(
            onDismissRequest = {
                foodEntryPendingDeletionId = null
            },
            title = { Text("Remove Food Entry?") },
            text = {
                Text("Remove ${pendingFoodEntry.productNameSnapshot} from ${formatHistoryDate(selectedDay.date)}?")
            },
            confirmButton = {
                TextButton(
                    enabled = !isUpdatingDay,
                    onClick = {
                        onDeleteFoodEntry(selectedDay, pendingFoodEntry)
                        foodEntryPendingDeletionId = null
                    }
                ) { Text(if (isUpdatingDay) "Removing…" else "Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = { foodEntryPendingDeletionId = null }
                ) { Text("Cancel") }
            }
        )
    }

    val pendingMealLogId = mealLogPendingDeletionId
    val pendingMealEntries =
        if (pendingMealLogId == null) {
            emptyList()
        } else {
            selectedDay?.foodEntries?.filter {
                it.mealLogId == pendingMealLogId
            }.orEmpty()
        }
    if (pendingMealEntries.isNotEmpty() && selectedDay != null && pendingMealLogId != null) {
        val mealName = pendingMealEntries.first().mealName?.takeIf { it.isNotBlank() } ?: "Saved Meal"
        AlertDialog(
            onDismissRequest = {
                mealLogPendingDeletionId = null
            },
            title = { Text("Remove Logged Meal?") },
            text = {
                Text("Remove $mealName and all of its ingredients from ${formatHistoryDate(selectedDay.date)}?")
            },
            confirmButton = {
                TextButton(
                    enabled = !isUpdatingDay,
                    onClick = {
                        onDeleteMealLog(selectedDay, pendingMealLogId)
                        mealLogPendingDeletionId = null
                    }
                ) { Text(if (isUpdatingDay) "Removing…" else "Remove Meal") }
            },
            dismissButton = {
                TextButton(
                    onClick = { mealLogPendingDeletionId = null }
                ) { Text("Cancel") }
            }
        )
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
                            "mobility sessions, shower log, life-maintenance completions, migraine events, meeting attendance, care appointments, care visits, individual foods, and logged meals for this date. Saved Places, Providers, Saved Foods, and " +
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
    onSelectDate: (String) -> Unit,
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
                        "Tap any past date to add forgotten food or water. Marked dates already contain saved information."
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
                allowEmptyDates = selectedFilter == DailyHistoryFilter.ALL,
                onSelectDate = onSelectDate
            )
        }

        if (!isLoading && days.isEmpty()) {
            RebuildInsetPanel {
                Text("No history yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (selectedFilter == DailyHistoryFilter.ALL) {
                        "Tap any past date to add food or water. New information will be saved automatically."
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
    allowEmptyDates: Boolean,
    onSelectDate: (String) -> Unit
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

                        val isSelectable =
                            !date.isAfter(LocalDate.now()) &&
                                (historyDay != null || allowEmptyDates)

                        CalendarDayCell(
                            date = date,
                            historyDay = historyDay,
                            isSelectable = isSelectable,
                            onClick = {
                                if (isSelectable) {
                                    onSelectDate(date.toString())
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
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSavedRecord =
        historyDay?.record != null ||
            historyDay?.activitySnapshot != null ||
            historyDay?.mobilitySessions?.isNotEmpty() == true ||
            historyDay?.showerLogged == true ||
            historyDay?.migraineLogs?.isNotEmpty() == true ||
            historyDay?.meetingAttendance?.isNotEmpty() == true ||
            historyDay?.careVisits?.isNotEmpty() == true ||
            historyDay?.careAppointments?.isNotEmpty() == true ||
            historyDay?.lifeMaintenanceLogs?.isNotEmpty() == true
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
            .clickable(enabled = isSelectable, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (historyDay != null || isSelectable) 2.dp else 0.dp
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
    isUpdatingDay: Boolean,
    onBack: () -> Unit,
    onAddSavedFood: () -> Unit,
    onAddSavedMeal: () -> Unit,
    onAddFoodManually: () -> Unit,
    onEditWater: () -> Unit,
    onRequestEditFoodEntry: (FoodLogEntry) -> Unit,
    onRequestDeleteFoodEntry: (FoodLogEntry) -> Unit,
    onRequestDeleteMeal: (String) -> Unit,
    onRequestDelete: () -> Unit
) {
    val hasSavedData =
        day.record != null ||
            day.activitySnapshot != null ||
            day.mobilitySessions.isNotEmpty() ||
            day.showerLogged ||
            day.migraineLogs.isNotEmpty() ||
            day.meetingAttendance.isNotEmpty() ||
            day.careVisits.isNotEmpty() ||
            day.careAppointments.isNotEmpty() ||
            day.lifeMaintenanceLogs.isNotEmpty()

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
            RebuildStatusBadge(
                text =
                    if (
                        hasSavedData &&
                        day.foodEntries.isNotEmpty()
                    ) {
                        "Saved + Food"
                    } else if (hasSavedData) {
                        "Saved"
                    } else if (day.foodEntries.isNotEmpty()) {
                        "Food only"
                    } else {
                        "Empty date"
                    }
            )
        }

        Text(
            text = formatHistoryDate(day.date),
            style = MaterialTheme.typography.headlineMedium
        )

        RebuildSectionCard(
            title = "Correct this day",
            subtitle = "Add something you forgot or remove a mistaken food entry.",
            accentColor = RebuildTeal
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditWater,
                    enabled = !isUpdatingDay,
                    modifier = Modifier.weight(1f)
                ) { Text("Edit Water") }
                OutlinedButton(
                    onClick = onAddSavedFood,
                    enabled = !isUpdatingDay,
                    modifier = Modifier.weight(1f)
                ) { Text("Saved Food") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddSavedMeal,
                    enabled = !isUpdatingDay,
                    modifier = Modifier.weight(1f)
                ) { Text("Saved Meal") }
                OutlinedButton(
                    onClick = onAddFoodManually,
                    enabled = !isUpdatingDay,
                    modifier = Modifier.weight(1f)
                ) { Text("Enter Food") }
            }
            Text(
                text = "Changes to this date are saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val record = day.record
        if (record == null) {
            RebuildInsetPanel {
                Text("No daily details yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No water, checklist, pain, medication, or journal details have been recorded for this date. Anything you add here is saved automatically.",
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

        if (day.lifeMaintenanceLogs.isNotEmpty()) {
            HistoryLifeMaintenanceCard(day.lifeMaintenanceLogs)
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

        HistoryFoodCard(
            entries = day.foodEntries,
            isUpdating = isUpdatingDay,
            onRequestEditEntry = onRequestEditFoodEntry,
            onRequestDeleteEntry = onRequestDeleteFoodEntry,
            onRequestDeleteMeal = onRequestDeleteMeal
        )
        if (record != null) {
            HistoryJournalCard(journalText = record.journalText)
        }

        val hasAnythingToDelete =
            hasSavedData || day.foodEntries.isNotEmpty()

        if (hasAnythingToDelete) {
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
}

@Composable
private fun HistoricalWaterEditorDialog(
    day: DailyHistoryDay,
    isSaving: Boolean,
    onCountsChange: (WaterBottleCounts) -> Unit,
    onDismiss: () -> Unit
) {
    val record = day.record
    var counts by remember(day.date) {
        mutableStateOf(
            WaterBottleCounts(
                plainReusable = record?.plainReusableBottleCount ?: 0,
                mioReusable = record?.mioReusableBottleCount ?: 0,
                plainDisposable = record?.plainDisposableBottleCount ?: 0,
                mioDisposable = record?.mioDisposableBottleCount ?: 0
            )
        )
    }

    fun update(newCounts: WaterBottleCounts) {
        counts = newCounts
        onCountsChange(newCounts)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Water for ${formatHistoryDate(day.date)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Each change is saved automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HistoricalWaterCountRow(
                    label = "24 oz plain water",
                    count = counts.plainReusable,
                    onDecrease = {
                        if (counts.plainReusable > 0) update(counts.copy(plainReusable = counts.plainReusable - 1))
                    },
                    onIncrease = { update(counts.copy(plainReusable = counts.plainReusable + 1)) }
                )
                HistoricalWaterCountRow(
                    label = "24 oz MiO water",
                    count = counts.mioReusable,
                    onDecrease = {
                        if (counts.mioReusable > 0) update(counts.copy(mioReusable = counts.mioReusable - 1))
                    },
                    onIncrease = { update(counts.copy(mioReusable = counts.mioReusable + 1)) }
                )
                HistoricalWaterCountRow(
                    label = "16.9 oz plain water",
                    count = counts.plainDisposable,
                    onDecrease = {
                        if (counts.plainDisposable > 0) update(counts.copy(plainDisposable = counts.plainDisposable - 1))
                    },
                    onIncrease = { update(counts.copy(plainDisposable = counts.plainDisposable + 1)) }
                )
                HistoricalWaterCountRow(
                    label = "16.9 oz MiO water",
                    count = counts.mioDisposable,
                    onDecrease = {
                        if (counts.mioDisposable > 0) update(counts.copy(mioDisposable = counts.mioDisposable - 1))
                    },
                    onIncrease = { update(counts.copy(mioDisposable = counts.mioDisposable + 1)) }
                )
                if (isSaving) {
                    Text(
                        "Saving…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun HistoricalWaterCountRow(
    label: String,
    count: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onDecrease, enabled = count > 0) { Text("−") }
        Text(count.toString(), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onIncrease) { Text("+") }
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
private fun HistoryLifeMaintenanceCard(
    logs: List<LifeMaintenanceLog>
) {
    RebuildSectionCard(
        title = "Life Maintenance",
        subtitle = "Completed on this date",
        accentColor = RebuildTeal
    ) {
        logs.sortedBy { LifeMaintenanceTasks.labelFor(it.taskKey) }.forEach { log ->
            Text(
                text = "✓ ${LifeMaintenanceTasks.labelFor(log.taskKey)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
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
    entries: List<FoodLogEntry>,
    isUpdating: Boolean,
    onRequestEditEntry: (FoodLogEntry) -> Unit,
    onRequestDeleteEntry: (FoodLogEntry) -> Unit,
    onRequestDeleteMeal: (String) -> Unit
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
                    entries = mealEntries,
                    isUpdating = isUpdating,
                    onRequestDelete = {
                        mealEntries.firstOrNull()?.mealLogId?.let(onRequestDeleteMeal)
                    }
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
                HistoryFoodEntry(
                    entry = entry,
                    isUpdating = isUpdating,
                    onRequestEdit = { onRequestEditEntry(entry) },
                    onRequestDelete = { onRequestDeleteEntry(entry) }
                )

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
    entries: List<FoodLogEntry>,
    isUpdating: Boolean,
    onRequestDelete: () -> Unit
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

    TextButton(
        onClick = onRequestDelete,
        enabled = !isUpdating
    ) { Text("Remove logged meal") }
}

@Composable
private fun HistoryFoodEntry(
    entry: FoodLogEntry,
    isUpdating: Boolean,
    onRequestEdit: () -> Unit,
    onRequestDelete: () -> Unit
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            onClick = onRequestEdit,
            enabled = !isUpdating
        ) { Text("Edit quantity") }

        TextButton(
            onClick = onRequestDelete,
            enabled = !isUpdating
        ) { Text("Remove") }
    }
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
                day.lifeMaintenanceLogs.isNotEmpty() ||
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
