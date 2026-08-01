package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferenceIds
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import com.pgdevhouse.dailyrebuild.ui.stats.StatsRange

private data class PreferenceOption(
    val id: String,
    val label: String,
    val description: String = ""
)

private val logSectionOptions = listOf(
    PreferenceOption(DailyRebuildPreferenceIds.LOG_FOOD, "Food"),
    PreferenceOption(DailyRebuildPreferenceIds.LOG_MOVEMENT, "Movement"),
    PreferenceOption(DailyRebuildPreferenceIds.LOG_MEETINGS, "Meetings & IOP"),
    PreferenceOption(DailyRebuildPreferenceIds.LOG_HEALTH, "Health"),
    PreferenceOption(DailyRebuildPreferenceIds.LOG_MAINTENANCE, "Maintenance")
)

private val quickLogOptions = listOf(
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_FOOD, "Food"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_WATER, "Water"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_MOBILITY, "Mobility"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_PAIN, "Pain"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_SHOWER, "Shower"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_MAINTENANCE, "Life Maintenance"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_MEETINGS, "Meeting"),
    PreferenceOption(DailyRebuildPreferenceIds.QUICK_HEALTH, "Health")
)

private val todaySectionOptions = listOf(
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_PRIORITY, "Priority card"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_GLANCE, "Today at a Glance"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_APPOINTMENTS, "Upcoming appointment"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_IOP, "Next IOP group"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_MEETINGS, "Recovery meetings"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_MOVEMENT, "Movement Today"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_QUICK_LOG, "Quick Log"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_RECENT, "Recent & Frequently Used"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_ACTIVITY, "Today’s Activity"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_SAVE_STATUS, "Automatic-save status"),
    PreferenceOption(DailyRebuildPreferenceIds.TODAY_MORE, "More Today")
)

private val statsOptions = listOf(
    PreferenceOption(DailyRebuildPreferenceIds.STATS_NUTRITION, "Nutrition"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_WATER, "Water"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_PAIN, "Pain"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_MOBILITY, "Mobility"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_MEETINGS, "Meetings & IOP"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_HEALTH, "Health measurements"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_MIGRAINE, "Migraine / visual aura"),
    PreferenceOption(DailyRebuildPreferenceIds.STATS_MAINTENANCE, "Life Maintenance")
)

@Composable
fun CustomizeDailyRebuildScreen(
    preferences: DailyRebuildPreferences,
    onPreferencesChange: (DailyRebuildPreferences) -> Unit,
    ignoredDataQualityValueCount: Int = 0,
    onResetIgnoredDataQualityValues: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildSectionCard(
            title = "Customize Daily Rebuild",
            subtitle = "Hide what you do not use and put the most useful controls first.",
            accentColor = RebuildBlue
        ) {
            Text(
                "These choices change the interface only. Hiding a section never deletes its records.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RebuildSectionCard(
            title = "Log Sections",
            subtitle = "Choose which sections appear under Log.",
            accentColor = RebuildGreen
        ) {
            logSectionOptions.forEach { option ->
                PreferenceCheckboxRow(
                    label = option.label,
                    checked = option.id in preferences.enabledLogSections,
                    onCheckedChange = { checked ->
                        val updated = preferences.enabledLogSections.toMutableSet()
                        if (checked) updated += option.id else updated -= option.id
                        if (updated.isNotEmpty()) {
                            onPreferencesChange(
                                preferences.copy(enabledLogSections = updated)
                            )
                        }
                    }
                )
            }
            Text(
                "At least one Log section must remain visible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RebuildSectionCard(
            title = "Quick Log",
            subtitle = "Show, hide, and reorder the buttons on Today.",
            accentColor = RebuildAmber
        ) {
            preferences.quickLogOrder.forEachIndexed { index, id ->
                val option = quickLogOptions.firstOrNull { it.id == id } ?: return@forEachIndexed
                ReorderPreferenceRow(
                    label = option.label,
                    checked = id !in preferences.hiddenQuickLogActions,
                    canMoveUp = index > 0,
                    canMoveDown = index < preferences.quickLogOrder.lastIndex,
                    onCheckedChange = { checked ->
                        val hidden = preferences.hiddenQuickLogActions.toMutableSet()
                        if (checked) hidden -= id else hidden += id
                        onPreferencesChange(
                            preferences.copy(hiddenQuickLogActions = hidden)
                        )
                    },
                    onMoveUp = {
                        onPreferencesChange(
                            preferences.copy(
                                quickLogOrder = moveItem(preferences.quickLogOrder, index, index - 1)
                            )
                        )
                    },
                    onMoveDown = {
                        onPreferencesChange(
                            preferences.copy(
                                quickLogOrder = moveItem(preferences.quickLogOrder, index, index + 1)
                            )
                        )
                    }
                )
            }
        }

        RebuildSectionCard(
            title = "Today Screen",
            subtitle = "Choose which cards appear on Today.",
            accentColor = RebuildBlue
        ) {
            todaySectionOptions.forEach { option ->
                PreferenceCheckboxRow(
                    label = option.label,
                    checked = option.id in preferences.visibleTodaySections,
                    onCheckedChange = { checked ->
                        val visible = preferences.visibleTodaySections.toMutableSet()
                        if (checked) visible += option.id else visible -= option.id
                        onPreferencesChange(
                            preferences.copy(visibleTodaySections = visible)
                        )
                    }
                )
            }
        }

        RebuildSectionCard(
            title = "Statistics Order",
            subtitle = "Show, hide, and reorder the categories on Stats.",
            accentColor = RebuildBlue
        ) {
            preferences.statsOrder.forEachIndexed { index, id ->
                val option = statsOptions.firstOrNull { it.id == id } ?: return@forEachIndexed
                ReorderPreferenceRow(
                    label = option.label,
                    checked = id !in preferences.hiddenStatsSections,
                    canMoveUp = index > 0,
                    canMoveDown = index < preferences.statsOrder.lastIndex,
                    onCheckedChange = { checked ->
                        val hidden = preferences.hiddenStatsSections.toMutableSet()
                        if (checked) hidden -= id else hidden += id
                        onPreferencesChange(
                            preferences.copy(hiddenStatsSections = hidden)
                        )
                    },
                    onMoveUp = {
                        onPreferencesChange(
                            preferences.copy(
                                statsOrder = moveItem(preferences.statsOrder, index, index - 1)
                            )
                        )
                    },
                    onMoveDown = {
                        onPreferencesChange(
                            preferences.copy(
                                statsOrder = moveItem(preferences.statsOrder, index, index + 1)
                            )
                        )
                    }
                )
            }
            Text(
                "This order controls the category buttons on the expanded Stats screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Default date range",
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                StatsRange.LAST_7_DAYS,
                StatsRange.LAST_30_DAYS,
                StatsRange.LAST_90_DAYS,
                StatsRange.ALL_TIME
            ).chunked(2).forEach { ranges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ranges.forEach { range ->
                        FilterChip(
                            selected = preferences.statsDefaultRange == range.name,
                            onClick = {
                                onPreferencesChange(
                                    preferences.copy(statsDefaultRange = range.name)
                                )
                            },
                            label = { Text(range.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        RebuildSectionCard(
            title = "Reminders",
            subtitle = "Keep reminder preferences in one place.",
            accentColor = RebuildAmber
        ) {
            PreferenceCheckboxRow(
                label = "Appointment reminders",
                description = "Controls the appointment alarms Daily Rebuild schedules.",
                checked = preferences.appointmentRemindersEnabled,
                onCheckedChange = {
                    onPreferencesChange(
                        preferences.copy(appointmentRemindersEnabled = it)
                    )
                }
            )
            PreferenceCheckboxRow(
                label = "Recovery meeting reminders",
                description = "Saved for the upcoming reminder phase.",
                checked = preferences.meetingRemindersEnabled,
                onCheckedChange = {
                    onPreferencesChange(
                        preferences.copy(meetingRemindersEnabled = it)
                    )
                }
            )
            PreferenceCheckboxRow(
                label = "IOP group reminders",
                description = "Saved for the upcoming reminder phase.",
                checked = preferences.iopRemindersEnabled,
                onCheckedChange = {
                    onPreferencesChange(
                        preferences.copy(iopRemindersEnabled = it)
                    )
                }
            )
        }

        RebuildSectionCard(
            title = "Data-quality Warnings",
            subtitle = "Manage exact values you previously chose not to see again.",
            accentColor = RebuildAmber
        ) {
            Text(
                text = if (ignoredDataQualityValueCount == 0) {
                    "No exact-value warnings are hidden."
                } else {
                    "$ignoredDataQualityValueCount exact-value ${if (ignoredDataQualityValueCount == 1) "choice is" else "choices are"} hidden."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onResetIgnoredDataQualityValues,
                enabled = ignoredDataQualityValueCount > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Restore Hidden Exact-Value Warnings")
            }
            Text(
                text = "Restoring these choices does not change any records. It only allows matching warnings to appear again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RebuildSectionCard(
            title = "Preferred Units",
            subtitle = "The preference foundation is stored now; more screens will adopt it in later phases.",
            accentColor = RebuildGreen
        ) {
            UnitChoiceRow(
                label = "Weight",
                value = preferences.weightUnit,
                choices = listOf("lb" to "lb", "kg" to "kg"),
                onSelected = { onPreferencesChange(preferences.copy(weightUnit = it)) }
            )
            UnitChoiceRow(
                label = "Distance",
                value = preferences.distanceUnit,
                choices = listOf("mi" to "miles", "km" to "km"),
                onSelected = { onPreferencesChange(preferences.copy(distanceUnit = it)) }
            )
            UnitChoiceRow(
                label = "Water",
                value = preferences.waterUnit,
                choices = listOf("oz" to "fl oz", "ml" to "mL"),
                onSelected = { onPreferencesChange(preferences.copy(waterUnit = it)) }
            )
            UnitChoiceRow(
                label = "Temperature",
                value = preferences.temperatureUnit,
                choices = listOf("f" to "°F", "c" to "°C"),
                onSelected = { onPreferencesChange(preferences.copy(temperatureUnit = it)) }
            )
            UnitChoiceRow(
                label = "Food mass",
                value = preferences.foodMassUnit,
                choices = listOf("oz" to "oz", "g" to "grams"),
                onSelected = { onPreferencesChange(preferences.copy(foodMassUnit = it)) }
            )
            UnitChoiceRow(
                label = "Height",
                value = preferences.heightUnit,
                choices = listOf("ft_in" to "ft / in", "cm" to "cm"),
                onSelected = { onPreferencesChange(preferences.copy(heightUnit = it)) }
            )
        }

        OutlinedButton(
            onClick = {
                onPreferencesChange(DailyRebuildPreferences())
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Restore Customization Defaults")
        }
    }
}

@Composable
private fun PreferenceCheckboxRow(
    label: String,
    description: String = "",
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
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReorderPreferenceRow(
    label: String,
    checked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = onMoveUp, enabled = canMoveUp) {
            Text("↑")
        }
        TextButton(onClick = onMoveDown, enabled = canMoveDown) {
            Text("↓")
        }
    }
}

@Composable
private fun UnitChoiceRow(
    label: String,
    value: String,
    choices: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { (id, text) ->
                FilterChip(
                    selected = value == id,
                    onClick = { onSelected(id) },
                    label = { Text(text) }
                )
            }
        }
    }
}

private fun <T> moveItem(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int
): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices) return items
    val mutable = items.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}
