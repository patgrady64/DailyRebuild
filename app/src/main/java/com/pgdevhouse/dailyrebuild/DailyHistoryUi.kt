package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * One read-only history day.
 *
 * A day can contain a saved DailyRecord, food entries, or both. Food is stored
 * immediately, while checklist/water/pain/medication/journal data is stored
 * when Save Today is pressed.
 */
data class DailyHistoryDay(
    val date: String,
    val record: DailyRecord?,
    val foodEntries: List<FoodLogEntry>
)

@Composable
fun DailyHistoryDialog(
    days: List<DailyHistoryDay>,
    isLoading: Boolean,
    isDeletingDay: Boolean,
    onDeleteDay: (DailyHistoryDay) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var visibleMonthText by rememberSaveable {
        mutableStateOf(
            YearMonth.now().toString()
        )
    }

    var dayPendingDeletion by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val selectedDay =
        days.firstOrNull {
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
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
        ) {
            if (selectedDay == null) {
                DailyHistoryCalendarPage(
                    days = days,
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
                            "water, pain, medications, journal, individual foods, " +
                            "and logged meals for this date. Saved Foods and " +
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
    isLoading: Boolean,
    visibleMonth: YearMonth,
    onVisibleMonthChange: (YearMonth) -> Unit,
    onSelectDay: (DailyHistoryDay) -> Unit,
    onDismiss: () -> Unit
) {
    val currentMonth = YearMonth.now()
    val earliestMonth =
        YearMonth.from(
            LocalDate.now().minusDays(364)
        )

    Column(
        modifier = Modifier
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Daily Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Tap a marked date to open its complete record.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }

        HorizontalDivider()

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading daily calendar...")
            }
        } else {
            CalendarMonthControls(
                visibleMonth = visibleMonth,
                canGoPrevious =
                    visibleMonth > earliestMonth,
                canGoNext =
                    visibleMonth < currentMonth,
                onPrevious = {
                    onVisibleMonthChange(
                        visibleMonth.minusMonths(1)
                    )
                },
                onNext = {
                    onVisibleMonthChange(
                        visibleMonth.plusMonths(1)
                    )
                }
            )

            DailyCalendarMonth(
                visibleMonth = visibleMonth,
                days = days,
                onSelectDay = onSelectDay
            )

            Text(
                text =
                    "Marked dates contain a saved daily record, food entries, " +
                        "or both. The calendar currently covers the most recent " +
                        "365 days.",
                style = MaterialTheme.typography.bodySmall
            )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = canGoPrevious
        ) {
            Text("Previous")
        }

        Text(
            text =
                visibleMonth.format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy",
                        Locale.US
                    )
                ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Text("Next")
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
        historyDay?.record != null

    val hasFood =
        historyDay?.foodEntries
            ?.isNotEmpty() == true

    Card(
        modifier = modifier
            .height(68.dp)
            .padding(2.dp)
            .clickable(
                enabled = historyDay != null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight =
                    if (date == LocalDate.now()) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )

            when {
                hasSavedRecord && hasFood ->
                    Text(
                        text = "Saved + Food",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                hasSavedRecord ->
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                hasFood ->
                    Text(
                        text = "Food",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
            }

            if (date == LocalDate.now()) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall
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
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            enabled = !isDeletingDay
        ) {
            Text("Back to Calendar")
        }

        Text(
            text = formatHistoryDate(day.date),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        val record = day.record

        if (record == null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        "The daily checklist was not saved for this date, " +
                            "but its recorded foods are still available below.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            HistoryChecklistCard(record)
            HistoryWaterCard(record)
            HistoryPainCard(record)
            HistoryMedicationCard(record)
        }

        HistoryFoodCard(
            entries = day.foodEntries
        )

        if (record != null) {
            HistoryJournalCard(
                journalText = record.journalText
            )
        }

        HorizontalDivider()

        OutlinedButton(
            onClick = onRequestDelete,
            enabled = !isDeletingDay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isDeletingDay) {
                    "Deleting Day..."
                } else {
                    "Delete Entire Day"
                }
            )
        }

        Text(
            text =
                "Use this only for test data or a day recorded by mistake. " +
                    "This cannot be undone.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )
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
    HistorySectionCard(
        title = "Pain"
    ) {
        Text(
            text =
                "Lower back pain: ${formatHistoryPain(record.backPain)} / 10"
        )
        Text(
            text =
                "Shin pain: ${formatHistoryPain(record.shinPain)} / 10"
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            content()
        }
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
