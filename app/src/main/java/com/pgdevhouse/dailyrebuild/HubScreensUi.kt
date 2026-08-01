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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.IopMissedOccurrence
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferenceIds
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Screen models keep the app coordinator from also becoming the renderer for
 * every feature. ViewModels and repositories own durable feature state and
 * data access; these files own layout and interaction hierarchy.
 */
data class TodayScreenState(
    val date: String,
    val completedTasks: Int,
    val foodRecorded: Boolean,
    val walkCompleted: Boolean,
    val painRecorded: Boolean,
    val mobilityCompleted: Boolean,
    val backPain: Float,
    val shinPain: Float,
    val calories: Double,
    val proteinGrams: Double,
    val calorieGoal: Int?,
    val waterOunces: Double,
    val showerDatesThisWeek: List<String>,
    val showeredToday: Boolean,
    val showersThisWeek: Int,
    val meetingsThisWeek: Int,
    val appointment: CareAppointment?,
    val activity: HealthActivityData,
    val activitySourceLabel: String?,
    val morningAspirinTaken: Boolean,
    val morningIbuprofenTaken: Boolean,
    val morningNaproxenTaken: Boolean,
    val morningAcetaminophenTaken: Boolean,
    val nightIbuprofenTaken: Boolean,
    val nightNaproxenTaken: Boolean,
    val nightAcetaminophenTaken: Boolean,
    val journalText: String,
    val maintenanceCompletedToday: List<String>,
    val iopOccurrence: IopOccurrence?,
    val repeatShortcuts: TodayShortcutCollection,
    val activityItems: List<TodayActivityItem>,
    val dataQualityWarnings: List<DataQualityWarning>,
    val preferences: DailyRebuildPreferences,
    val isSaving: Boolean
)

data class TodayScreenActions(
    val onOpenHistory: () -> Unit,
    val onOpenFood: () -> Unit,
    val onOpenWater: () -> Unit,
    val onOpenMobility: () -> Unit,
    val onOpenPain: () -> Unit,
    val onOpenMeetings: () -> Unit,
    val onOpenHealth: () -> Unit,
    val onScheduleAppointment: () -> Unit,
    val onViewAppointment: (CareAppointment) -> Unit,
    val onLogMeeting: () -> Unit,
    val onLogShower: () -> Unit,
    val onRemoveShower: () -> Unit,
    val onOpenLifeMaintenance: () -> Unit,
    val onOpenIopGroups: () -> Unit,
    val onRepeatShortcut: (TodayRepeatShortcut, Double) -> Unit,
    val onEditActivityItem: (TodayActivityItem) -> Unit,
    val onDeleteActivityItem: (TodayActivityItem) -> Unit,
    val onReviewDataQualityWarning: (DataQualityWarning) -> Unit,
    val onKeepDataQualityWarning: (DataQualityWarning) -> Unit,
    val onIgnoreDataQualityWarning: (DataQualityWarning) -> Unit,
    val onFoodRecordedChange: (Boolean) -> Unit,
    val onWalkCompletedChange: (Boolean) -> Unit,
    val onPainRecordedChange: (Boolean) -> Unit,
    val onMobilityCompletedChange: (Boolean) -> Unit,
    val onMorningAspirinChange: (Boolean) -> Unit,
    val onMorningIbuprofenChange: (Boolean) -> Unit,
    val onMorningNaproxenChange: (Boolean) -> Unit,
    val onMorningAcetaminophenChange: (Boolean) -> Unit,
    val onNightIbuprofenChange: (Boolean) -> Unit,
    val onNightNaproxenChange: (Boolean) -> Unit,
    val onNightAcetaminophenChange: (Boolean) -> Unit,
    val onJournalTextChange: (String) -> Unit
)

@Composable
fun TodayScreen(
    state: TodayScreenState,
    actions: TodayScreenActions,
    modifier: Modifier = Modifier
) {
    var showMoreToday by rememberSaveable { mutableStateOf(false) }
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedActivityItem by remember { mutableStateOf<TodayActivityItem?>(null) }
    val visibleSections = state.preferences.visibleTodaySections

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Daily Rebuild",
            subtitle = formatTodayDate(state.date),
            onOpenHistory = actions.onOpenHistory
        )

        if (DailyRebuildPreferenceIds.TODAY_PRIORITY in visibleSections) {
            TodayPriorityCard(state, actions)
        }

        if (DailyRebuildPreferenceIds.TODAY_GLANCE in visibleSections) {
            TodayAtAGlanceCard(state)
        }

        DataQualitySummaryCard(
            warnings = state.dataQualityWarnings,
            onReview = actions.onReviewDataQualityWarning,
            onKeep = actions.onKeepDataQualityWarning,
            onIgnoreExactValue = actions.onIgnoreDataQualityWarning
        )

        if (DailyRebuildPreferenceIds.TODAY_QUICK_LOG in visibleSections) {
            TodayQuickLogSection(state, actions)
        }

        if (
            DailyRebuildPreferenceIds.TODAY_RECENT in visibleSections &&
            (
                state.repeatShortcuts.recent.isNotEmpty() ||
                    state.repeatShortcuts.frequent.isNotEmpty()
                )
        ) {
            TodayRecentAndFrequentSection(
                shortcuts = state.repeatShortcuts,
                onRepeat = actions.onRepeatShortcut,
                onOpenFood = actions.onOpenFood
            )
        }

        if (DailyRebuildPreferenceIds.TODAY_APPOINTMENTS in visibleSections) {
            HomeAppointmentCard(
                appointment = state.appointment,
                onSchedule = actions.onScheduleAppointment,
                onView = actions.onViewAppointment
            )
        }

        if (
            DailyRebuildPreferenceIds.TODAY_IOP in visibleSections &&
            DailyRebuildPreferenceIds.LOG_MEETINGS in state.preferences.enabledLogSections
        ) {
            HomeIopCard(
                occurrence = state.iopOccurrence,
                onManage = actions.onOpenIopGroups
            )
        }

        if (
            DailyRebuildPreferenceIds.TODAY_MEETINGS in visibleSections &&
            DailyRebuildPreferenceIds.LOG_MEETINGS in state.preferences.enabledLogSections
        ) {
            HomeMeetingsCard(
                meetingsThisWeek = state.meetingsThisWeek,
                onOpenMeetings = actions.onOpenMeetings,
                onLogMeeting = actions.onLogMeeting
            )
        }

        if (DailyRebuildPreferenceIds.TODAY_ACTIVITY in visibleSections) {
            TodayActivityTimeline(
                items = state.activityItems,
                onOpenFood = actions.onOpenFood,
                onOpenMobility = actions.onOpenMobility,
                onOpenMeetings = actions.onOpenMeetings,
                onOpenMaintenance = actions.onOpenLifeMaintenance,
                onItemClick = { selectedActivityItem = it }
            )
        }

        if (
            DailyRebuildPreferenceIds.TODAY_MOVEMENT in visibleSections &&
            DailyRebuildPreferenceIds.LOG_MOVEMENT in state.preferences.enabledLogSections
        ) {
            RebuildSectionCard(
                title = "Movement Today",
                subtitle = state.activitySourceLabel ?: "No connected activity recorded yet",
                accentColor = RebuildGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RebuildMetricPill(
                        label = "steps",
                        value = state.activity.steps.toString(),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    RebuildMetricPill(
                        label = if (state.preferences.distanceUnit == "km") "km" else "miles",
                        value = formatPreferredDistance(
                            state.activity.distanceMiles,
                            state.preferences.distanceUnit
                        ),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    RebuildMetricPill(
                        label = "time",
                        value = formatActivityMinutes(state.activity.activityMinutes),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                TextButton(onClick = actions.onOpenMobility) {
                    Text("Open Mobility")
                }
            }
        }

        if (DailyRebuildPreferenceIds.TODAY_SAVE_STATUS in visibleSections) {
            TodaySaveStatus(isSaving = state.isSaving)
        }

        if (DailyRebuildPreferenceIds.TODAY_MORE in visibleSections) {
            OutlinedButton(
                onClick = { showMoreToday = !showMoreToday },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (showMoreToday) "Hide More Today" else "More Today")
            }

            if (showMoreToday) {
                TodayExpandableRow(
                    title = "Daily anchors",
                    summary = "${state.completedTasks} of 4 complete",
                    expanded = expandedSection == "anchors",
                    onClick = {
                        expandedSection = if (expandedSection == "anchors") null else "anchors"
                    }
                )
                if (expandedSection == "anchors") {
                    DailyAnchorPanel(state, actions)
                }

                TodayExpandableRow(
                    title = "Showering",
                    summary = "${state.showersThisWeek} this week · goal 2–3",
                    expanded = expandedSection == "shower",
                    onClick = {
                        expandedSection = if (expandedSection == "shower") null else "shower"
                    }
                )
                if (expandedSection == "shower") {
                    WeeklyShowerControls(
                        showerDates = state.showerDatesThisWeek,
                        showeredToday = state.showeredToday,
                        onLogToday = actions.onLogShower,
                        onRemoveToday = actions.onRemoveShower
                    )
                }

                TodayExpandableRow(
                    title = "Medication check-in",
                    summary = "Morning and night reference checks",
                    expanded = expandedSection == "medication",
                    onClick = {
                        expandedSection = if (expandedSection == "medication") null else "medication"
                    }
                )
                if (expandedSection == "medication") {
                    MedicationCheckPanel(state, actions)
                }

                TodayExpandableRow(
                    title = "Journal",
                    summary = if (state.journalText.isBlank()) "No note yet" else "Today has a note",
                    expanded = expandedSection == "journal",
                    onClick = {
                        expandedSection = if (expandedSection == "journal") null else "journal"
                    }
                )
                if (expandedSection == "journal") {
                    OutlinedTextField(
                        value = state.journalText,
                        onValueChange = actions.onJournalTextChange,
                        label = { Text("Today’s notes") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    selectedActivityItem?.let { item ->
        TodayActivityDetailsDialog(
            item = item,
            onDismiss = { selectedActivityItem = null },
            onEdit = {
                selectedActivityItem = null
                actions.onEditActivityItem(item)
            },
            onDelete = {
                selectedActivityItem = null
                actions.onDeleteActivityItem(item)
            }
        )
    }
}

@Composable
private fun TodayAtAGlanceCard(
    state: TodayScreenState
) {
    val highestPain = maxOf(state.backPain, state.shinPain)

    RebuildSectionCard(
        title = "Today at a Glance",
        subtitle = "The details you are most likely to check during the day.",
        accentColor = RebuildBlue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TodayMetricTile(
                label = "Calories",
                value = state.calories.toInt().toString(),
                supporting = state.calorieGoal?.let { "Goal $it" } ?: "Logged today",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            TodayMetricTile(
                label = "Protein",
                value = "${formatCompactNumber(state.proteinGrams)} g",
                supporting = "Logged today",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TodayMetricTile(
                label = "Water",
                value = formatPreferredWater(
                    state.waterOunces,
                    state.preferences.waterUnit
                ),
                supporting = if (state.waterOunces > 0.0) "Recorded" else "Not logged",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
            TodayMetricTile(
                label = "Highest pain",
                value = if (state.painRecorded) {
                    "${highestPain.toInt()} / 10"
                } else {
                    "—"
                },
                supporting = if (state.painRecorded) "Back or shin" else "Not logged",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TodayMetricTile(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayQuickLogSection(
    state: TodayScreenState,
    actions: TodayScreenActions
) {
    val visibleActions = state.preferences.quickLogOrder
        .filterNot { it in state.preferences.hiddenQuickLogActions }
        .filter { actionId ->
            quickLogSectionFor(actionId)?.let {
                it in state.preferences.enabledLogSections
            } ?: true
        }

    RebuildSectionCard(
        title = "Quick Log",
        subtitle = "The controls you use most, in the order you chose.",
        accentColor = RebuildAmber
    ) {
        if (visibleActions.isEmpty()) {
            Text(
                "All Quick Log buttons are hidden. Change this under Health → Customize Daily Rebuild.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            visibleActions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowActions.forEach { actionId ->
                        TodayQuickLogButton(
                            actionId = actionId,
                            state = state,
                            actions = actions,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (
            state.maintenanceCompletedToday.isNotEmpty() &&
            DailyRebuildPreferenceIds.QUICK_MAINTENANCE in visibleActions
        ) {
            Text(
                text = "Completed today: ${state.maintenanceCompletedToday.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayQuickLogButton(
    actionId: String,
    state: TodayScreenState,
    actions: TodayScreenActions,
    modifier: Modifier = Modifier
) {
    val label: String
    val supporting: String
    val enabled: Boolean
    val onClick: () -> Unit
    val accent: Color

    when (actionId) {
        DailyRebuildPreferenceIds.QUICK_FOOD -> {
            label = "Food"
            supporting = if (state.foodRecorded) {
                "${state.calories.toInt()} calories"
            } else {
                "Add food or a meal"
            }
            enabled = true
            onClick = actions.onOpenFood
            accent = RebuildBlue
        }
        DailyRebuildPreferenceIds.QUICK_WATER -> {
            label = "Water"
            supporting = if (state.waterOunces > 0.0) {
                formatPreferredWater(state.waterOunces, state.preferences.waterUnit)
            } else {
                "Add a bottle"
            }
            enabled = true
            onClick = actions.onOpenWater
            accent = RebuildBlue
        }
        DailyRebuildPreferenceIds.QUICK_MOBILITY -> {
            label = "Mobility"
            supporting = if (state.mobilityCompleted) "Logged today" else "Start a routine"
            enabled = true
            onClick = actions.onOpenMobility
            accent = RebuildGreen
        }
        DailyRebuildPreferenceIds.QUICK_PAIN -> {
            label = "Pain"
            supporting = if (state.painRecorded) {
                "${maxOf(state.backPain, state.shinPain).toInt()} / 10 highest"
            } else {
                "Record current levels"
            }
            enabled = true
            onClick = actions.onOpenPain
            accent = RebuildAmber
        }
        DailyRebuildPreferenceIds.QUICK_SHOWER -> {
            label = if (state.showeredToday) "Shower logged" else "Shower"
            supporting = if (state.showeredToday) "Saved today" else "One tap to log"
            enabled = !state.showeredToday
            onClick = actions.onLogShower
            accent = RebuildGreen
        }
        DailyRebuildPreferenceIds.QUICK_MAINTENANCE -> {
            label = "Maintenance"
            supporting = when (state.maintenanceCompletedToday.size) {
                0 -> "Laundry, bedding, and more"
                1 -> "1 item completed"
                else -> "${state.maintenanceCompletedToday.size} items completed"
            }
            enabled = true
            onClick = actions.onOpenLifeMaintenance
            accent = RebuildGreen
        }
        DailyRebuildPreferenceIds.QUICK_MEETINGS -> {
            label = "Meeting"
            supporting = "${state.meetingsThisWeek} logged this week"
            enabled = true
            onClick = actions.onLogMeeting
            accent = RebuildBlue
        }
        else -> {
            label = "Health"
            supporting = "Measurements and events"
            enabled = true
            onClick = actions.onOpenHealth
            accent = RebuildAmber
        }
    }

    Surface(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accent.copy(alpha = 0.18f)
            ) {
                Text(
                    text = if (!enabled && actionId == DailyRebuildPreferenceIds.QUICK_SHOWER) {
                        "✓"
                    } else {
                        "LOG"
                    },
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayRecentAndFrequentSection(
    shortcuts: TodayShortcutCollection,
    onRepeat: (TodayRepeatShortcut, Double) -> Unit,
    onOpenFood: () -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf("recent") }
    var quantityShortcut by remember { mutableStateOf<TodayRepeatShortcut?>(null) }
    var quantityText by rememberSaveable { mutableStateOf("") }

    val selectedShortcuts =
        if (selectedMode == "frequent") shortcuts.frequent else shortcuts.recent

    RebuildSectionCard(
        title = "Recent & Frequently Used",
        subtitle = "Repeat a food or saved meal without rebuilding the entry.",
        accentColor = RebuildGreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedMode == "recent",
                onClick = { selectedMode = "recent" },
                label = { Text("Recent") }
            )
            FilterChip(
                selected = selectedMode == "frequent",
                onClick = { selectedMode = "frequent" },
                label = { Text("Frequently Used") }
            )
        }

        if (selectedShortcuts.isEmpty()) {
            Text(
                "Log a few foods or meals and shortcuts will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedShortcuts.take(4).forEach { shortcut ->
                TodayRepeatShortcutCard(
                    shortcut = shortcut,
                    showUsageCount = selectedMode == "frequent",
                    onAddAgain = {
                        onRepeat(shortcut, shortcut.defaultQuantity)
                    },
                    onChangeQuantity = {
                        quantityShortcut = shortcut
                        quantityText = formatCompactNumber(shortcut.defaultQuantity)
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenFood) {
                Text("Open Food")
            }
        }
    }

    quantityShortcut?.let { shortcut ->
        val parsedQuantity = quantityText.toDoubleOrNull()

        AlertDialog(
            onDismissRequest = {
                quantityShortcut = null
                quantityText = ""
            },
            title = {
                Text(
                    if (shortcut.type == TodayRepeatShortcutType.MEAL) {
                        "Choose Meal Quantity"
                    } else {
                        "Choose Food Quantity"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        shortcut.title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (shortcut.type == TodayRepeatShortcutType.MEAL) {
                            "Enter how many of this saved meal to add."
                        } else {
                            "Enter the amount to add in ${shortcut.quantityUnit}."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { value ->
                            quantityText = value.filter {
                                it.isDigit() || it == '.'
                            }
                        },
                        label = {
                            Text(
                                if (shortcut.type == TodayRepeatShortcutType.MEAL) {
                                    "Meal quantity"
                                } else {
                                    "Quantity (${shortcut.quantityUnit})"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        isError = quantityText.isNotBlank() &&
                            (parsedQuantity == null || parsedQuantity <= 0.0),
                        supportingText = {
                            if (
                                quantityText.isNotBlank() &&
                                (parsedQuantity == null || parsedQuantity <= 0.0)
                            ) {
                                Text("Enter a number greater than zero.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quantity = parsedQuantity ?: return@Button
                        onRepeat(shortcut, quantity)
                        quantityShortcut = null
                        quantityText = ""
                    },
                    enabled = parsedQuantity != null && parsedQuantity > 0.0
                ) {
                    Text("Add to Today")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        quantityShortcut = null
                        quantityText = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun TodayRepeatShortcutCard(
    shortcut: TodayRepeatShortcut,
    showUsageCount: Boolean,
    onAddAgain: () -> Unit,
    onChangeQuantity: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        shortcut.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        shortcut.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (shortcut.type == TodayRepeatShortcutType.MEAL) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        if (shortcut.type == TodayRepeatShortcutType.MEAL) "MEAL" else "FOOD",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showUsageCount) {
                Text(
                    "Logged ${shortcut.usageCount} ${if (shortcut.usageCount == 1) "time" else "times"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Again")
                }
                OutlinedButton(
                    onClick = onChangeQuantity,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Change Amount")
                }
            }
        }
    }
}

@Composable
private fun TodayActivityTimeline(
    items: List<TodayActivityItem>,
    onOpenFood: () -> Unit,
    onOpenMobility: () -> Unit,
    onOpenMeetings: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onItemClick: (TodayActivityItem) -> Unit
) {
    RebuildSectionCard(
        title = "Today’s Activity",
        subtitle = "A time-ordered view of records that were added today.",
        accentColor = RebuildBlue
    ) {
        if (items.isEmpty()) {
            RebuildInsetPanel {
                Text(
                    "No time-stamped activity yet.",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Food, saved meals, mobility, showers, meetings, and maintenance will appear here as they are logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items.forEachIndexed { index, item ->
                TodayActivityRow(
                    item = item,
                    onClick = { onItemClick(item) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onOpenFood) { Text("Food") }
                TextButton(onClick = onOpenMobility) { Text("Mobility") }
                TextButton(onClick = onOpenMeetings) { Text("Meetings") }
                TextButton(onClick = onOpenMaintenance) { Text("Maintenance") }
            }
        }
    }
}

@Composable
private fun TodayActivityRow(
    item: TodayActivityItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(62.dp)
        ) {
            Text(
                formatActivityClockTime(item.occurredAt),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                item.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                item.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayActivityDetailsDialog(
    item: TodayActivityItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val canEdit = item.category in setOf(
        "Food",
        "Meal",
        "Mobility",
        "Meeting",
        "Maintenance"
    )
    var confirmDelete by remember(item.key) {
        mutableStateOf(false)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${item.title}?") },
            text = {
                Text(
                    "This removes the logged entry. You can immediately restore it with Undo."
                )
            },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildStatusBadge(text = item.category)
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatActivityClockTime(item.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (canEdit) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun TodaySaveStatus(
    isSaving: Boolean
) {
    RebuildInsetPanel(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = RebuildGreen.copy(alpha = 0.18f)
                ) {
                    Text(
                        "✓",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = RebuildGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isSaving) "Saving changes…" else "Everything is saved",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Daily Rebuild saves changes as you make them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun quickLogSectionFor(actionId: String): String? =
    when (actionId) {
        DailyRebuildPreferenceIds.QUICK_FOOD,
        DailyRebuildPreferenceIds.QUICK_WATER -> DailyRebuildPreferenceIds.LOG_FOOD

        DailyRebuildPreferenceIds.QUICK_MOBILITY -> DailyRebuildPreferenceIds.LOG_MOVEMENT
        DailyRebuildPreferenceIds.QUICK_MEETINGS -> DailyRebuildPreferenceIds.LOG_MEETINGS
        DailyRebuildPreferenceIds.QUICK_PAIN,
        DailyRebuildPreferenceIds.QUICK_SHOWER,
        DailyRebuildPreferenceIds.QUICK_HEALTH -> DailyRebuildPreferenceIds.LOG_HEALTH

        DailyRebuildPreferenceIds.QUICK_MAINTENANCE -> DailyRebuildPreferenceIds.LOG_MAINTENANCE
        else -> null
    }

private fun formatPreferredWater(
    ounces: Double,
    unit: String
): String {
    return if (unit == "ml") {
        "${String.format(Locale.US, "%.0f", ounces * 29.5735)} mL"
    } else {
        "${formatOunces(ounces)} oz"
    }
}

private fun formatPreferredDistance(
    miles: Double,
    unit: String
): String {
    val value = if (unit == "km") miles * 1.60934 else miles
    return String.format(Locale.US, "%.2f", value)
}

private fun formatActivityClockTime(timestamp: Long): String {
    return runCatching {
        java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }.getOrDefault("")
}

@Composable
private fun TodayPriorityCard(
    state: TodayScreenState,
    actions: TodayScreenActions
) {
    var showAnchorDetails by rememberSaveable { mutableStateOf(false) }
    val title: String
    val message: String
    val button: String
    val action: () -> Unit
    val status = "${state.completedTasks} of 4 daily anchors"

    when {
        !state.foodRecorded &&
            DailyRebuildPreferenceIds.LOG_FOOD in state.preferences.enabledLogSections -> {
            title = "Log today’s food"
            message = "Start with a food, saved meal, or recent shortcut."
            button = "Open Food"
            action = actions.onOpenFood
        }
        state.waterOunces <= 0.0 &&
            DailyRebuildPreferenceIds.LOG_FOOD in state.preferences.enabledLogSections -> {
            title = "Add today’s first water"
            message = "No water has been recorded yet."
            button = "Add Water"
            action = actions.onOpenWater
        }
        !state.walkCompleted &&
            DailyRebuildPreferenceIds.LOG_MOVEMENT in state.preferences.enabledLogSections -> {
            title = "Take a walk"
            message = "Connected activity will check the Walk anchor automatically after 500 steps or 0.25 miles."
            button = "Open Movement"
            action = actions.onOpenMobility
        }
        !state.mobilityCompleted &&
            DailyRebuildPreferenceIds.LOG_MOVEMENT in state.preferences.enabledLogSections -> {
            title = "Do a mobility session"
            message = "A short seated or lying routine still counts."
            button = "Open Mobility"
            action = actions.onOpenMobility
        }
        !state.painRecorded &&
            DailyRebuildPreferenceIds.LOG_HEALTH in state.preferences.enabledLogSections -> {
            title = "Record today’s pain"
            message = "Save the highest back and shin pain experienced so far."
            button = "Log Pain"
            action = actions.onOpenPain
        }
        else -> {
            title = "Today is up to date"
            message = "The main daily anchors are complete. Keep logging only what is useful."
            button = "View History"
            action = actions.onOpenHistory
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.clickable { showAnchorDetails = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "View anchors ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = action,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(button)
            }
        }
    }

    if (showAnchorDetails) {
        AlertDialog(
            onDismissRequest = { showAnchorDetails = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Today’s daily anchors",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "These are the four basic things the daily-anchor count is measuring.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DailyAnchorDetailsRow(
                        title = "Food recorded",
                        description = "At least one food or saved meal has been logged today.",
                        complete = state.foodRecorded,
                        actionLabel = "Open Food",
                        onAction = {
                            showAnchorDetails = false
                            actions.onOpenFood()
                        }
                    )
                    DailyAnchorDetailsRow(
                        title = "Walk",
                        description = "A walk was completed today. Connected activity marks this automatically after at least 500 steps or 0.25 miles.",
                        complete = state.walkCompleted,
                        actionLabel = if (state.walkCompleted) "Mark not done" else "Mark done",
                        onAction = {
                            actions.onWalkCompletedChange(!state.walkCompleted)
                        }
                    )
                    DailyAnchorDetailsRow(
                        title = "Back and shin pain recorded",
                        description = "Today’s highest back and shin pain values have been recorded.",
                        complete = state.painRecorded,
                        actionLabel = "Log Pain",
                        onAction = {
                            showAnchorDetails = false
                            actions.onOpenPain()
                        }
                    )
                    DailyAnchorDetailsRow(
                        title = "Mobility or stretching",
                        description = "A mobility or stretching session was completed today.",
                        complete = state.mobilityCompleted,
                        actionLabel = "Open Mobility",
                        onAction = {
                            showAnchorDetails = false
                            actions.onOpenMobility()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnchorDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DailyAnchorDetailsRow(
    title: String,
    description: String,
    complete: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (complete) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (complete) "✓" else "○",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (complete) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (complete) "Complete today" else "Not completed yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (complete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayExpandableRow(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(if (expanded) "▲" else "▼")
        }
    }
}

@Composable
private fun DailyAnchorPanel(
    state: TodayScreenState,
    actions: TodayScreenActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DailyAnchorRow("Food recorded", state.foodRecorded, actions.onFoodRecordedChange)
        DailyAnchorRow("Walk", state.walkCompleted, actions.onWalkCompletedChange)
        DailyAnchorRow("Back and shin pain recorded", state.painRecorded, actions.onPainRecordedChange)
        DailyAnchorRow("Mobility or stretching", state.mobilityCompleted, actions.onMobilityCompletedChange)
    }
}

@Composable
private fun DailyAnchorRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun MedicationCheckPanel(
    state: TodayScreenState,
    actions: TodayScreenActions
) {
    RebuildInsetPanel {
        Text("Morning", fontWeight = FontWeight.SemiBold)
        DailyAnchorRow("Aspirin", state.morningAspirinTaken, actions.onMorningAspirinChange)
        DailyAnchorRow("Ibuprofen", state.morningIbuprofenTaken, actions.onMorningIbuprofenChange)
        DailyAnchorRow("Naproxen", state.morningNaproxenTaken, actions.onMorningNaproxenChange)
        DailyAnchorRow("Acetaminophen", state.morningAcetaminophenTaken, actions.onMorningAcetaminophenChange)
        HorizontalDivider()
        Text("Night", fontWeight = FontWeight.SemiBold)
        DailyAnchorRow("Ibuprofen", state.nightIbuprofenTaken, actions.onNightIbuprofenChange)
        DailyAnchorRow("Naproxen", state.nightNaproxenTaken, actions.onNightNaproxenChange)
        DailyAnchorRow("Acetaminophen", state.nightAcetaminophenTaken, actions.onNightAcetaminophenChange)
    }
}

/* FOOD */
data class FoodHubState(
    val selectedSection: Int,
    val totalWaterOunces: Double,
    val totalBottleCount: Int,
    val entries: List<FoodLogEntry>,
    val savedFoodCount: Int,
    val savedMealCount: Int,
    val lastScannedBarcode: String?,
    val isScanningBarcode: Boolean,
    val currentCalorieGoal: Int?,
    val pantryItems: List<PantryEssential>,
    val isSavingPantry: Boolean
)

data class FoodHubActions(
    val onSectionChange: (Int) -> Unit,
    val onOpenHistory: () -> Unit,
    val onAddWater: () -> Unit,
    val onScanFood: () -> Unit,
    val onAddFoodManually: () -> Unit,
    val onOpenSavedFoods: () -> Unit,
    val onBuildMeal: () -> Unit,
    val onOpenSavedMeals: () -> Unit,
    val onUpdateEntryQuantity: (FoodLogEntry, Double) -> Unit,
    val onUpdateMealQuantity: (String, Double) -> Unit,
    val onDeleteEntry: (FoodLogEntry) -> Unit,
    val onDeleteMealLog: (String) -> Unit,
    val onSavePantryItem: (PantryEssential) -> Unit,
    val onDeletePantryItem: (PantryEssential) -> Unit,
    val onPantryStatusChange: (PantryEssential, String) -> Unit,
    val onMarkNeededPurchased: () -> Unit
)

@Composable
fun FoodHubScreen(
    state: FoodHubState,
    actions: FoodHubActions,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    showSectionTabs: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            HubScreenHeader(
                title = "Food",
                subtitle = "Today, meal planning, pantry essentials, and Walmart shopping each have a clear home.",
                onOpenHistory = actions.onOpenHistory
            )
        }

        if (showSectionTabs) {
            HubSectionTabs(
                labels = listOf("Today", "Plan", "Pantry", "Shop"),
                selected = state.selectedSection,
                onSelected = actions.onSectionChange
            )
        }

        when (state.selectedSection) {
            0 -> {
                FoodHydrationCard(
                    totalWaterOunces = state.totalWaterOunces,
                    totalBottleCount = state.totalBottleCount,
                    onAddWater = actions.onAddWater
                )
                FoodSection(
                    entries = state.entries,
                    savedFoodCount = state.savedFoodCount,
                    savedMealCount = state.savedMealCount,
                    lastScannedBarcode = state.lastScannedBarcode,
                    isScanningBarcode = state.isScanningBarcode,
                    onScanFood = actions.onScanFood,
                    onAddFoodManually = actions.onAddFoodManually,
                    onOpenSavedFoods = actions.onOpenSavedFoods,
                    onBuildMeal = actions.onBuildMeal,
                    onOpenSavedMeals = actions.onOpenSavedMeals,
                    onUpdateQuantity = actions.onUpdateEntryQuantity,
                    onUpdateMealQuantity = actions.onUpdateMealQuantity,
                    onDeleteEntry = actions.onDeleteEntry,
                    onDeleteMealLog = actions.onDeleteMealLog
                )
            }
            1 -> FoodPlanSection(state, actions)
            2 -> PantryEssentialsSection(
                items = state.pantryItems,
                isSaving = state.isSavingPantry,
                onSave = actions.onSavePantryItem,
                onDelete = actions.onDeletePantryItem,
                onStatusChange = actions.onPantryStatusChange
            )
            else -> PantryShopStagingSection(
                items = state.pantryItems,
                onOpenPantry = { actions.onSectionChange(2) },
                onMarkAllPurchased = actions.onMarkNeededPurchased
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FoodPlanSection(
    state: FoodHubState,
    actions: FoodHubActions
) {
    RebuildSectionCard(
        title = "Meal Planning",
        subtitle = "Build and reuse meals now. Weighted Walmart meal-plan generation will be connected here after this workflow is stable.",
        accentColor = RebuildGreen
    ) {
        RebuildMetricPill(
            label = "current calorie goal",
            value = state.currentCalorieGoal?.toString() ?: "Set in Health",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        )
        Button(
            onClick = actions.onBuildMeal,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Build a Reusable Meal") }
        OutlinedButton(
            onClick = actions.onOpenSavedMeals,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Saved Meals (${state.savedMealCount})") }
        OutlinedButton(
            onClick = actions.onOpenSavedFoods,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Meal Foods (${state.savedFoodCount})") }
    }

    RebuildInsetPanel {
        Text("One meal per day", fontWeight = FontWeight.SemiBold)
        Text(
            "The future generator will build each day as one complete eating session around the active calorie goal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* MOBILITY */
data class MobilityHubState(
    val selectedSection: Int,
    val availability: HealthConnectAvailability,
    val hasPermissions: Boolean,
    val isLoadingActivity: Boolean,
    val activity: HealthActivityData,
    val sourceLabel: String?,
    val sessions: List<MobilitySession>,
    val isSaving: Boolean
)

data class MobilityHubActions(
    val onSectionChange: (Int) -> Unit,
    val onOpenHistory: () -> Unit,
    val onRefresh: () -> Unit,
    val onManageHealth: () -> Unit,
    val onSaveSession: (MobilitySessionDraft) -> Unit,
    val onUpdateSession: (MobilitySession) -> Unit,
    val onDeleteSession: (MobilitySession) -> Unit
)

@Composable
fun MobilityHubScreen(
    state: MobilityHubState,
    actions: MobilityHubActions,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            HubScreenHeader(
                title = "Mobility",
                subtitle = "Walking and intentional mobility are together without crowding the screen.",
                onOpenHistory = actions.onOpenHistory
            )
        }
        HubSectionTabs(
            labels = listOf("Today", "Routines", "History"),
            selected = state.selectedSection,
            onSelected = actions.onSectionChange
        )

        when (state.selectedSection) {
            0 -> {
                MobilityActivitySummary(state, actions)
                RebuildSectionCard(
                    title = "Today’s Mobility",
                    accentColor = RebuildGreen
                ) {
                    Text(
                        if (state.sessions.isEmpty()) {
                            "No mobility sessions logged today."
                        } else {
                            "${state.sessions.size} session${if (state.sessions.size == 1) "" else "s"} logged today."
                        }
                    )
                    Button(
                        onClick = { actions.onSectionChange(1) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start or Quick Log Mobility") }
                }
            }
            1 -> MobilitySection(
                sessions = state.sessions,
                isSaving = state.isSaving,
                onSaveSession = actions.onSaveSession,
                onUpdateSession = actions.onUpdateSession,
                onDeleteSession = actions.onDeleteSession
            )
            else -> {
                RebuildSectionCard(
                    title = "Mobility History",
                    subtitle = "Use the global calendar to review walking snapshots and completed sessions.",
                    accentColor = RebuildBlue
                ) {
                    OutlinedButton(
                        onClick = actions.onOpenHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Filtered History") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MobilityActivitySummary(
    state: MobilityHubState,
    actions: MobilityHubActions
) {
    RebuildSectionCard(
        title = "Today’s Walking",
        subtitle = state.sourceLabel ?: "No connected activity recorded yet",
        accentColor = RebuildBlue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "steps",
                value = state.activity.steps.toString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            RebuildMetricPill(
                label = "miles",
                value = String.format(Locale.US, "%.2f", state.activity.distanceMiles),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            RebuildMetricPill(
                label = "time",
                value = formatActivityMinutes(state.activity.activityMinutes),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = actions.onRefresh,
                enabled = !state.isLoadingActivity,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isLoadingActivity) "Refreshing…" else "Refresh") }
            OutlinedButton(
                onClick = actions.onManageHealth,
                modifier = Modifier.weight(1f)
            ) { Text("Manage") }
        }
    }
}

/* MEETINGS */
@Composable
fun MeetingsHubScreen(
    weeklyAttendance: List<MeetingAttendance>,
    iopGroups: List<IopGroup>,
    iopMissedOccurrences: List<IopMissedOccurrence>,
    selectedSection: Int,
    isSaving: Boolean,
    isSavingIop: Boolean,
    isSavingIopAttendance: Boolean,
    onSectionChange: (Int) -> Unit,
    onOpenHistory: () -> Unit,
    onLogMeeting: () -> Unit,
    onAddMeeting: () -> Unit,
    onEditAttendance: (MeetingAttendance) -> Unit,
    onDeleteAttendance: (MeetingAttendance) -> Unit,
    onViewFullHistory: () -> Unit,
    onSaveIopGroup: (IopGroup) -> Unit,
    onDeleteIopGroup: (IopGroup) -> Unit,
    onMarkIopMissed: (IopOccurrence, String) -> Unit,
    onMarkIopAttended: (IopOccurrence) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            HubScreenHeader(
                title = "Meetings & IOP",
                subtitle = "Recovery meeting attendance and your recurring IOP schedule have separate homes.",
                onOpenHistory = onOpenHistory
            )
        }

        HubSectionTabs(
            labels = listOf("Meetings", "IOP Groups"),
            selected = selectedSection.coerceIn(0, 1),
            onSelected = onSectionChange
        )

        if (selectedSection == 0) {
            MeetingsTab(
                weeklyAttendance = weeklyAttendance,
                isSaving = isSaving,
                onLogMeeting = onLogMeeting,
                onAddMeeting = onAddMeeting,
                onEditAttendance = onEditAttendance,
                onDeleteAttendance = onDeleteAttendance,
                onViewFullHistory = onViewFullHistory
            )
        } else {
            IopGroupsScreen(
                groups = iopGroups,
                missedOccurrences = iopMissedOccurrences,
                isSaving = isSavingIop,
                isSavingAttendance = isSavingIopAttendance,
                onSave = onSaveIopGroup,
                onDelete = onDeleteIopGroup,
                onMarkMissed = onMarkIopMissed,
                onMarkAttended = onMarkIopAttended
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* HEALTH */
data class HealthHubState(
    val appointments: List<CareAppointment>,
    val visits: List<CareVisit>,
    val migraineLogs: List<MigraineLog>,
    val availability: HealthConnectAvailability,
    val hasPermissions: Boolean,
    val isLoadingActivity: Boolean,
    val activity: HealthActivityData,
    val activitySourceLabel: String?
)

data class HealthHubActions(
    val onOpenHistory: () -> Unit,
    val onScheduleAppointment: () -> Unit,
    val onOpenAppointmentHistory: () -> Unit,
    val onViewAppointment: (CareAppointment) -> Unit,
    val onLogVisit: () -> Unit,
    val onOpenVisitHistory: () -> Unit,
    val onLogMigraine: () -> Unit,
    val onEditMigraine: (MigraineLog) -> Unit,
    val onDeleteMigraine: (MigraineLog) -> Unit,
    val onLogPain: () -> Unit,
    val onConnectHealth: () -> Unit,
    val onRefreshActivity: () -> Unit,
    val onManageAccess: () -> Unit,
    val onInstallOrUpdate: () -> Unit
)

@Composable
fun HealthHubScreen(
    state: HealthHubState,
    actions: HealthHubActions,
    requestedFeature: String? = null,
    requestToken: Int = 0,
    onRequestedFeatureConsumed: () -> Unit = {},
    profileContent: @Composable () -> Unit,
    customizationContent: @Composable () -> Unit,
    backupContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    var openFeature by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(requestToken) {
        if (!requestedFeature.isNullOrBlank()) {
            openFeature = requestedFeature
            onRequestedFeatureConsumed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            HubScreenHeader(
                title = "Health",
                subtitle = "Appointments and quick logs first; records and settings open only when needed.",
                onOpenHistory = actions.onOpenHistory
            )
        }

        CareAppointmentTrackerCard(
            appointments = state.appointments,
            onSchedule = actions.onScheduleAppointment,
            onOpenHistory = actions.onOpenAppointmentHistory,
            onViewAppointment = actions.onViewAppointment
        )

        RebuildSectionCard(
            title = "Quick Log",
            accentColor = RebuildAmber
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = actions.onLogMigraine,
                    modifier = Modifier.weight(1f)
                ) { Text("Migraine") }
                OutlinedButton(
                    onClick = actions.onLogPain,
                    modifier = Modifier.weight(1f)
                ) { Text("Pain") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = actions.onLogVisit,
                    modifier = Modifier.weight(1f)
                ) { Text("Care Visit") }
                OutlinedButton(
                    onClick = { openFeature = "profile" },
                    modifier = Modifier.weight(1f)
                ) { Text("Measurement") }
            }
        }

        RebuildSectionCard(
            title = "Health Records",
            accentColor = RebuildBlue
        ) {
            HealthRecordRow("Appointments", "Schedule, prepare, reminders, transportation") {
                openFeature = if (openFeature == "appointments") null else "appointments"
            }
            HealthRecordRow("Care Visits", "Places, providers, completed visit history") {
                openFeature = if (openFeature == "visits") null else "visits"
            }
            HealthRecordRow("Migraine / Visual Aura", "Only appears in reports when an event exists") {
                openFeature = if (openFeature == "migraine") null else "migraine"
            }
            HealthRecordRow("Profile, Goals & Measurements", "Weight, A1C, blood pressure, cholesterol, medication reference") {
                openFeature = if (openFeature == "profile") null else "profile"
            }
            HealthRecordRow("Connected Activity", "Health Connect permissions and walking source") {
                openFeature = if (openFeature == "activity") null else "activity"
            }
            HealthRecordRow("Customize Daily Rebuild", "Sections, Quick Log order, Today cards, units, and reminder preferences") {
                openFeature = if (openFeature == "customize") null else "customize"
            }
            HealthRecordRow("Data & Backup", "Export or restore all local Daily Rebuild records") {
                openFeature = if (openFeature == "backup") null else "backup"
            }
        }

        when (openFeature) {
            "appointments" -> CareAppointmentTrackerCard(
                appointments = state.appointments,
                onSchedule = actions.onScheduleAppointment,
                onOpenHistory = actions.onOpenAppointmentHistory,
                onViewAppointment = actions.onViewAppointment
            )
            "visits" -> CareVisitTrackerCard(
                visits = state.visits,
                onLogVisit = actions.onLogVisit,
                onOpenHistory = actions.onOpenVisitHistory
            )
            "migraine" -> MigraineTrackerCard(
                logs = state.migraineLogs,
                onLogMigraine = actions.onLogMigraine,
                onEditLog = actions.onEditMigraine,
                onDeleteLog = actions.onDeleteMigraine
            )
            "profile" -> profileContent()
            "customize" -> customizationContent()
            "activity" -> ActivitySection(
                availability = state.availability,
                hasPermissions = state.hasPermissions,
                isLoading = state.isLoadingActivity,
                activity = state.activity,
                sourceLabel = state.activitySourceLabel,
                onConnect = actions.onConnectHealth,
                onRefresh = actions.onRefreshActivity,
                onManageAccess = actions.onManageAccess,
                onInstallOrUpdate = actions.onInstallOrUpdate
            )
            "backup" -> backupContent()
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HealthRecordRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun HubSectionTabs(
    labels: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEachIndexed { itemIndex, label ->
                    val index = rowIndex * 2 + itemIndex
                    FilterChip(
                        selected = selected == index,
                        onClick = { onSelected(index) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatTodayDate(dateText: String): String {
    return runCatching {
        LocalDate.parse(dateText).format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
        )
    }.getOrDefault(dateText)
}

private fun formatActivityMinutes(minutes: Long): String {
    if (minutes <= 0L) return "0m"
    val hours = minutes / 60L
    val remainder = minutes % 60L
    return when {
        hours == 0L -> "${remainder}m"
        remainder == 0L -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}
