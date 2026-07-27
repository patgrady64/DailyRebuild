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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
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
    val highestPain: Float,
    val calories: Double,
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
    val onJournalTextChange: (String) -> Unit,
    val onSaveToday: () -> Unit
)

@Composable
fun TodayScreen(
    state: TodayScreenState,
    actions: TodayScreenActions,
    modifier: Modifier = Modifier
) {
    var showMoreToday by rememberSaveable { mutableStateOf(false) }
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Daily Rebuild",
            subtitle = formatTodayDate(state.date),
            onOpenHistory = actions.onOpenHistory
        )

        TodayPriorityCard(state, actions)

        RebuildSectionCard(
            title = "Today at a Glance",
            accentColor = RebuildBlue
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "calories",
                    value = if (state.calorieGoal == null) {
                        state.calories.toInt().toString()
                    } else {
                        "${state.calories.toInt()} / ${state.calorieGoal}"
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                RebuildMetricPill(
                    label = "water",
                    value = "${formatOunces(state.waterOunces)} oz",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "highest pain",
                    value = if (state.painRecorded) {
                        "${state.highestPain.toInt()} / 10"
                    } else {
                        "Not logged"
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                RebuildMetricPill(
                    label = "anchors",
                    value = "${state.completedTasks} / 4",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        HomeAppointmentCard(
            appointment = state.appointment,
            onSchedule = actions.onScheduleAppointment,
            onView = actions.onViewAppointment
        )

        HomeMeetingsCard(
            meetingsThisWeek = state.meetingsThisWeek,
            onOpenMeetings = actions.onOpenMeetings,
            onLogMeeting = actions.onLogMeeting
        )

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
            TextButton(onClick = actions.onOpenMobility) {
                Text("Open Mobility")
            }
        }

        RebuildSectionCard(
            title = "Quick Log",
            accentColor = RebuildAmber
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = actions.onOpenFood,
                    modifier = Modifier.weight(1f)
                ) { Text("Food") }
                OutlinedButton(
                    onClick = actions.onOpenWater,
                    modifier = Modifier.weight(1f)
                ) { Text("Water") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = actions.onOpenMobility,
                    modifier = Modifier.weight(1f)
                ) { Text("Mobility") }
                OutlinedButton(
                    onClick = actions.onOpenPain,
                    modifier = Modifier.weight(1f)
                ) { Text("Highest Pain") }
            }
        }

        Button(
            onClick = actions.onSaveToday,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Save Today")
            }
        }

        OutlinedButton(
            onClick = { showMoreToday = !showMoreToday },
            modifier = Modifier.fillMaxWidth()
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

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodayPriorityCard(
    state: TodayScreenState,
    actions: TodayScreenActions
) {
    val title: String
    val message: String
    val button: String
    val action: () -> Unit

    when {
        !state.foodRecorded -> {
            title = "Log today’s meal"
            message = "Food is the most important unfinished item for today."
            button = "Open Food"
            action = actions.onOpenFood
        }
        state.waterOunces <= 0.0 -> {
            title = "Log your water"
            message = "No water has been recorded today."
            button = "Add Water"
            action = actions.onOpenWater
        }
        !state.mobilityCompleted -> {
            title = "Do a mobility session"
            message = "A short seated or lying routine still counts."
            button = "Open Mobility"
            action = actions.onOpenMobility
        }
        !state.painRecorded -> {
            title = "Record today’s highest pain"
            message = "Use one number for the highest pain experienced today."
            button = "Log Highest Pain"
            action = actions.onOpenPain
        }
        else -> {
            title = "Today is ready to save"
            message = "Your four daily anchors are complete."
            button = "Save Today"
            action = actions.onSaveToday
        }
    }

    RebuildSectionCard(
        title = "Next Step",
        subtitle = title,
        accentColor = RebuildGreen
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = action,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(button)
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
        DailyAnchorRow("Walk or intentional movement", state.walkCompleted, actions.onWalkCompletedChange)
        DailyAnchorRow("Highest pain recorded", state.painRecorded, actions.onPainRecordedChange)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Food",
            subtitle = "Today, meal planning, pantry essentials, and Walmart shopping each have a clear home.",
            onOpenHistory = actions.onOpenHistory
        )

        HubSectionTabs(
            labels = listOf("Today", "Plan", "Pantry", "Shop"),
            selected = state.selectedSection,
            onSelected = actions.onSectionChange
        )

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
    val onDeleteSession: (MobilitySession) -> Unit
)

@Composable
fun MobilityHubScreen(
    state: MobilityHubState,
    actions: MobilityHubActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Mobility",
            subtitle = "Walking and intentional mobility are together without crowding the screen.",
            onOpenHistory = actions.onOpenHistory
        )
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
    isSaving: Boolean,
    onOpenHistory: () -> Unit,
    onLogMeeting: () -> Unit,
    onAddMeeting: () -> Unit,
    onEditAttendance: (MeetingAttendance) -> Unit,
    onDeleteAttendance: (MeetingAttendance) -> Unit,
    onViewFullHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Meetings",
            subtitle = "A focused weekly goal with only this week’s attendance on the main screen.",
            onOpenHistory = onOpenHistory
        )
        MeetingsTab(
            weeklyAttendance = weeklyAttendance,
            isSaving = isSaving,
            onLogMeeting = onLogMeeting,
            onAddMeeting = onAddMeeting,
            onEditAttendance = onEditAttendance,
            onDeleteAttendance = onDeleteAttendance,
            onViewFullHistory = onViewFullHistory
        )
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
    profileContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var openFeature by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Health",
            subtitle = "Appointments and quick logs first; records and settings open only when needed.",
            onOpenHistory = actions.onOpenHistory
        )

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
                ) { Text("Highest Pain") }
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
                onDeleteLog = actions.onDeleteMigraine
            )
            "profile" -> profileContent()
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
