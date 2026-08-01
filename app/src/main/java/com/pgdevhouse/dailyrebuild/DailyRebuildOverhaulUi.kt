package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferenceIds
import com.pgdevhouse.dailyrebuild.ui.navigation.AppNavigationViewModel

/** Shared page frame used by the reorganized Log and More destinations. */
@Composable
fun RebuildFeaturePage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }
        }
        HubScreenHeader(
            title = title,
            subtitle = subtitle,
            onOpenHistory = onHistory,
            onOpenSearch = onSearch
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
    }
}

@Composable
fun DailyRebuildLogScreen(
    selectedSection: Int,
    enabledSections: Set<String>,
    onSelectSection: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogWater: () -> Unit,
    onLogPain: () -> Unit,
    onLogShower: () -> Unit,
    onLogMeeting: () -> Unit,
    onOpenIopAttendance: () -> Unit,
    onLogMigraine: () -> Unit,
    onLogCareVisit: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onOpenPantry: () -> Unit,
    modifier: Modifier = Modifier,
    featureContent: @Composable () -> Unit
) {
    if (selectedSection == AppNavigationViewModel.LOG_HOME_SECTION) {
        LogHomeScreen(
            enabledSections = enabledSections,
            onOpenSearch = onOpenSearch,
            onOpenHistory = onOpenHistory,
            onSelectSection = onSelectSection,
            onLogWater = onLogWater,
            onLogPain = onLogPain,
            onLogShower = onLogShower,
            onLogMeeting = onLogMeeting,
            onOpenIopAttendance = onOpenIopAttendance,
            onLogMigraine = onLogMigraine,
            onLogCareVisit = onLogCareVisit,
            onOpenMeasurements = onOpenMeasurements,
            onOpenPantry = onOpenPantry,
            modifier = modifier
        )
        return
    }

    val (title, subtitle) = when (selectedSection) {
        AppNavigationViewModel.LOG_FOOD_SECTION ->
            "Food & Water" to "Log food, meals, condiments, and hydration."
        AppNavigationViewModel.LOG_MOVEMENT_SECTION ->
            "Movement" to "Walking, connected activity, mobility routines, and completed sessions."
        AppNavigationViewModel.LOG_MEETINGS_SECTION ->
            "Meetings & IOP" to "Record recovery meetings and manage automatic IOP attendance."
        AppNavigationViewModel.LOG_HEALTH_SECTION ->
            "Health Quick Log" to "Record pain, measurements, migraines, care visits, and self-care."
        else ->
            "Life Maintenance" to "Laundry, bedding, nails, haircuts, and toothbrush replacement."
    }

    RebuildFeaturePage(
        title = title,
        subtitle = subtitle,
        onBack = { onSelectSection(AppNavigationViewModel.LOG_HOME_SECTION) },
        onSearch = onOpenSearch,
        onHistory = onOpenHistory,
        modifier = modifier
    ) {
        featureContent()
    }
}

@Composable
private fun LogHomeScreen(
    enabledSections: Set<String>,
    onOpenSearch: () -> Unit,
    onOpenHistory: () -> Unit,
    onSelectSection: (Int) -> Unit,
    onLogWater: () -> Unit,
    onLogPain: () -> Unit,
    onLogShower: () -> Unit,
    onLogMeeting: () -> Unit,
    onOpenIopAttendance: () -> Unit,
    onLogMigraine: () -> Unit,
    onLogCareVisit: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onOpenPantry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Log",
            subtitle = "One clear place to record anything in Daily Rebuild.",
            onOpenHistory = onOpenHistory,
            onOpenSearch = onOpenSearch
        )

        RebuildSectionCard(
            title = "Quick Log",
            subtitle = "Common actions open immediately.",
            accentColor = RebuildAmber
        ) {
            ActionTileGrid(
                items = buildList {
                    if (DailyRebuildPreferenceIds.LOG_FOOD in enabledSections) {
                        add(LogActionTile("Food", "Foods, meals, condiments", RebuildBlue) {
                            onSelectSection(AppNavigationViewModel.LOG_FOOD_SECTION)
                        })
                        add(LogActionTile("Water", "Add a bottle", RebuildBlue, onLogWater))
                    }
                    if (DailyRebuildPreferenceIds.LOG_HEALTH in enabledSections) {
                        add(LogActionTile("Pain", "Back and shin", RebuildAmber, onLogPain))
                    }
                    if (DailyRebuildPreferenceIds.LOG_MOVEMENT in enabledSections) {
                        add(LogActionTile("Mobility", "Start or record a session", RebuildGreen) {
                            onSelectSection(AppNavigationViewModel.LOG_MOVEMENT_SECTION)
                        })
                    }
                    if (DailyRebuildPreferenceIds.LOG_HEALTH in enabledSections) {
                        add(LogActionTile("Shower", "One tap to record", RebuildGreen, onLogShower))
                    }
                    if (DailyRebuildPreferenceIds.LOG_MEETINGS in enabledSections) {
                        add(LogActionTile("Meeting", "Record attendance", RebuildBlue, onLogMeeting))
                    }
                }
            )
        }

        if (DailyRebuildPreferenceIds.LOG_HEALTH in enabledSections) {
            RebuildSectionCard(
                title = "Health",
                subtitle = "Measurements and occasional events.",
                accentColor = RebuildTeal
            ) {
                ActionListRow("Measurement", "Weight, blood pressure, glucose, and more", onOpenMeasurements)
                ActionListRow("Migraine or visual aura", "Record an event and duration", onLogMigraine)
                ActionListRow("Care visit", "Record a completed provider visit", onLogCareVisit)
                ActionListRow("Health quick log", "See every health logging action") {
                    onSelectSection(AppNavigationViewModel.LOG_HEALTH_SECTION)
                }
            }
        }

        if (DailyRebuildPreferenceIds.LOG_MEETINGS in enabledSections) {
            RebuildSectionCard(
                title = "Recovery & Schedule",
                subtitle = "Meeting and IOP records without attendance judgment.",
                accentColor = RebuildBlue
            ) {
                ActionListRow("Meeting attendance", "Saved and one-time meetings", onLogMeeting)
                ActionListRow("IOP attendance", "Sessions count as attended unless marked missed", onOpenIopAttendance)
                ActionListRow("Meetings & IOP", "Open the complete recovery log") {
                    onSelectSection(AppNavigationViewModel.LOG_MEETINGS_SECTION)
                }
            }
        }

        RebuildSectionCard(
            title = "Life",
            subtitle = "Occasional household and practical records.",
            accentColor = RebuildGreen
        ) {
            if (DailyRebuildPreferenceIds.LOG_MAINTENANCE in enabledSections) {
                ActionListRow("Life Maintenance", "Laundry, bedding, nails, haircut, toothbrush") {
                    onSelectSection(AppNavigationViewModel.LOG_MAINTENANCE_SECTION)
                }
            }
            ActionListRow("Pantry", "Essentials and shopping status", onOpenPantry)
        }

        Spacer(Modifier.height(12.dp))
    }
}

private data class LogActionTile(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun ActionTileGrid(items: List<LogActionTile>) {
    items.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowItems.forEach { item ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = item.onClick),
                    shape = RoundedCornerShape(18.dp),
                    color = item.accent.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(item.title, fontWeight = FontWeight.Bold)
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (rowItems.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActionListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun DailyRebuildMoreScreen(
    onOpenSearch: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFoodLibrary: () -> Unit,
    onOpenSavedMeals: () -> Unit,
    onOpenMobilityRoutines: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenPantry: () -> Unit,
    onOpenIopGroups: () -> Unit,
    onOpenMeetings: () -> Unit,
    onOpenCareDirectory: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenCustomization: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenConnectedActivity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "More",
            subtitle = "Libraries, recovery setup, care records, and app settings.",
            onOpenHistory = onOpenHistory,
            onOpenSearch = onOpenSearch
        )

        RebuildSectionCard(
            title = "Your Libraries",
            subtitle = "Reusable information you manage occasionally.",
            accentColor = RebuildGreen
        ) {
            ActionListRow("Foods & Condiments", "Nutrition library and condiment classification", onOpenFoodLibrary)
            ActionListRow("Saved Meals", "Reusable meals and optional condiments", onOpenSavedMeals)
            ActionListRow("Mobility Routines", "Movement library and guided sessions", onOpenMobilityRoutines)
            ActionListRow("Medications & Measurements", "Health profile and reference records", onOpenMedications)
            ActionListRow("Pantry Essentials", "Items, shopping status, and replacements", onOpenPantry)
        }

        RebuildSectionCard(
            title = "Recovery & Care",
            subtitle = "Schedules, attendance, providers, and appointments.",
            accentColor = RebuildBlue
        ) {
            ActionListRow("IOP Groups", "Repeating schedule and missed-reason records", onOpenIopGroups)
            ActionListRow("Meetings", "Saved meetings and attendance history", onOpenMeetings)
            ActionListRow("Providers & Care Places", "Reusable care directory and visit history", onOpenCareDirectory)
            ActionListRow("Appointments", "Schedule, preparation, reminders, and history", onOpenAppointments)
        }

        RebuildSectionCard(
            title = "App Setup",
            subtitle = "Control what Daily Rebuild shows and how it behaves.",
            accentColor = RebuildTeal
        ) {
            ActionListRow("Customize Daily Rebuild", "Today, Quick Log, Insights, units, and visibility", onOpenCustomization)
            ActionListRow("Notifications & Reminders", "Master switch, timing, permission, and Reminder Center", onOpenNotifications)
            ActionListRow("Connected Activity", "Health Connect status, refresh, and permissions", onOpenConnectedActivity)
            ActionListRow("Data & Backup", "Portable export, restore, and emergency restore points", onOpenBackup)
            ActionListRow("Help", "Daily anchors, IOP attendance, steps, search, and backup", onOpenHelp)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DailyRebuildHelpScreen(
    onBack: () -> Unit,
    onOpenCustomization: () -> Unit,
    onOpenConnectedActivity: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    RebuildFeaturePage(
        title = "Help",
        subtitle = "Plain-language explanations for the parts of Daily Rebuild that work differently.",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RebuildSectionCard(
                title = "Daily Anchors",
                subtitle = "Four basic records used only as a daily reference.",
                accentColor = RebuildBlue
            ) {
                HelpLine("Food recorded", "A non-condiment food or saved meal was logged.")
                HelpLine("Walk", "Automatically completes after at least 500 connected steps or 0.25 miles, with manual correction available.")
                HelpLine("Pain recorded", "The highest back and shin pain values for the day were saved.")
                HelpLine("Mobility or stretching", "A mobility or stretching session was completed.")
            }

            RebuildSectionCard(
                title = "IOP Attendance",
                subtitle = "Scheduled groups count as attended automatically.",
                accentColor = RebuildGreen
            ) {
                Text("Use Mark missed only when you did not attend. A reason is required so the record remains understandable later.")
            }

            RebuildSectionCard(
                title = "Steps",
                subtitle = "The selected period always shows a number for every date.",
                accentColor = RebuildTeal
            ) {
                Text("When Fit or Health Connect has no data for a date, Daily Rebuild treats that date as 0 steps according to your tracking preference.")
                OutlinedButton(onClick = onOpenConnectedActivity, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Connected Activity")
                }
            }

            RebuildSectionCard(
                title = "Customization & Backup",
                subtitle = "Your layout and preferences are portable.",
                accentColor = RebuildAmber
            ) {
                Button(onClick = onOpenCustomization, modifier = Modifier.fillMaxWidth()) {
                    Text("Customize Daily Rebuild")
                }
                OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Data & Backup")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HelpLine(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodayUpcomingSection(
    state: TodayScreenState,
    actions: TodayScreenActions,
    showAppointments: Boolean,
    showIop: Boolean,
    showMeetings: Boolean
) {
    val hasAppointment = showAppointments && state.appointment != null
    val hasIop = showIop && state.iopOccurrence != null &&
        DailyRebuildPreferenceIds.LOG_MEETINGS in state.preferences.enabledLogSections
    val hasMeetings = showMeetings &&
        state.meetingsThisWeek > 0 &&
        DailyRebuildPreferenceIds.LOG_MEETINGS in state.preferences.enabledLogSections

    if (!hasAppointment && !hasIop && !hasMeetings) return

    RebuildSectionCard(
        title = "Upcoming",
        subtitle = "Scheduled items and recovery activity in one place.",
        accentColor = RebuildTeal
    ) {
        if (hasAppointment) {
            val appointment = state.appointment!!
            ActionListRow(
                title = appointment.placeName.ifBlank { "Appointment" },
                subtitle = "${formatAppointmentDateTime(appointment.scheduledAt)} · ${appointment.visitCategory}",
                onClick = { actions.onViewAppointment(appointment) }
            )
        }

        if (hasIop) {
            val occurrence = state.iopOccurrence!!
            val startHour = occurrence.group.startMinutes / 60
            val minute = occurrence.group.startMinutes % 60
            val hour12 = when (val hour = startHour % 12) {
                0 -> 12
                else -> hour
            }
            val amPm = if (startHour < 12) "AM" else "PM"
            val dateLabel = occurrence.date.format(
                java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.US)
            )
            ActionListRow(
                title = occurrence.group.name,
                subtitle = "$dateLabel · $hour12:${minute.toString().padStart(2, '0')} $amPm · attended automatically",
                onClick = actions.onOpenIopGroups
            )
        }

        if (hasMeetings) {
            ActionListRow(
                title = "Recovery meetings",
                subtitle = "${state.meetingsThisWeek} ${if (state.meetingsThisWeek == 1) "meeting" else "meetings"} recorded this week",
                onClick = actions.onOpenMeetings
            )
        }
    }
}

@Composable
fun DailyRebuildHistoryHomeScreen(
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "History",
            subtitle = "Browse the calendar, open any date, and correct past records.",
            onOpenSearch = onOpenSearch
        )
        RebuildSectionCard(
            title = "Daily History",
            subtitle = "Calendar and chronological list views stay synchronized with edits and Undo.",
            accentColor = RebuildBlue
        ) {
            Button(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Calendar & History")
            }
            Text(
                "Tap any date to see food, water, pain, activity, meetings, IOP, health events, and life maintenance recorded that day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RebuildSectionCard(
            title = "History tools",
            accentColor = RebuildTeal
        ) {
            ActionListRow("Calendar view", "Category markers and month navigation", onOpenHistory)
            ActionListRow("List view", "Chronological dates grouped by month", onOpenHistory)
            ActionListRow("Past-day corrections", "Add forgotten food or water and edit existing entries", onOpenHistory)
        }
    }
}
