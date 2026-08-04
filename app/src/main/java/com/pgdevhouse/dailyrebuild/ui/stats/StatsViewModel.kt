package com.pgdevhouse.dailyrebuild.ui.stats

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.DrinkCategory
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.IopMissedOccurrence
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import com.pgdevhouse.dailyrebuild.iopOccurrencesInRange
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private data class RawStatsData(
    val records: List<DailyRecord>,
    val foodEntries: List<FoodLogEntry>,
    val drinkEntries: List<DrinkEntry>,
    val foodProducts: List<FoodProduct>,
    val activity: List<DailyActivitySnapshot>,
    val mobility: List<MobilitySession>,
    val showers: List<ShowerLog>,
    val migraines: List<MigraineLog>,
    val meetings: List<MeetingAttendance>,
    val appointments: List<CareAppointment>,
    val visits: List<CareVisit>,
    val measurements: List<HealthMeasurement>,
    val lifeMaintenance: List<LifeMaintenanceLog>,
    val iopGroups: List<IopGroup>,
    val iopMisses: List<IopMissedOccurrence>
)

private data class DatePeriod(
    val start: LocalDate,
    val end: LocalDate,
    val label: String
)

private data class NumericSummary(
    val value: Double?,
    val coverage: Int
)

private data class IopPeriodSummary(
    val scheduled: Int,
    val attended: Int,
    val missed: Int,
    val attendedDates: List<String>,
    val missedRows: List<IopMissedOccurrence>
)

class StatsViewModel(
    private val repositories: DailyRebuildRepositories,
    initialPreferences: DailyRebuildPreferences
) : ViewModel() {

    private var preferences = initialPreferences

    var state by mutableStateOf(
        StatsUiState(
            selectedRange = statsRangeFromPreference(initialPreferences.statsDefaultRange)
        )
    )
        private set

    init {
        refresh()
    }

    fun updatePreferences(value: DailyRebuildPreferences) {
        preferences = value
        refresh()
    }

    fun selectRange(range: StatsRange) {
        state = state.copy(
            selectedRange = range,
            anchorDate = LocalDate.now()
        )
        refresh()
    }

    fun selectFilter(filter: StatsFilter) {
        state = state.copy(selectedFilter = filter)
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        val safeStart = minOf(start, end)
        val safeEnd = maxOf(start, end).coerceAtMost(LocalDate.now())
        state = state.copy(
            selectedRange = StatsRange.CUSTOM,
            customStartDate = safeStart,
            customEndDate = safeEnd,
            anchorDate = safeEnd
        )
        refresh()
    }

    fun movePrevious() {
        val current = currentPeriodWithoutEarliest()
        if (state.selectedRange == StatsRange.ALL_TIME) return
        val length = ChronoUnit.DAYS.between(current.start, current.end) + 1L
        val newEnd = current.start.minusDays(1)
        val newStart = newEnd.minusDays(length - 1L)

        state = if (state.selectedRange == StatsRange.CUSTOM) {
            state.copy(
                customStartDate = newStart,
                customEndDate = newEnd,
                anchorDate = newEnd
            )
        } else {
            state.copy(anchorDate = newEnd)
        }
        refresh()
    }

    fun moveNext() {
        if (!state.canMoveNext || state.selectedRange == StatsRange.ALL_TIME) return
        val today = LocalDate.now()
        val current = currentPeriodWithoutEarliest()
        val length = ChronoUnit.DAYS.between(current.start, current.end) + 1L
        val candidateStart = current.end.plusDays(1)
        val candidateEnd = candidateStart.plusDays(length - 1L).coerceAtMost(today)
        val candidateAdjustedStart = candidateEnd.minusDays(length - 1L)

        state = if (state.selectedRange == StatsRange.CUSTOM) {
            state.copy(
                customStartDate = candidateAdjustedStart,
                customEndDate = candidateEnd,
                anchorDate = candidateEnd
            )
        } else {
            state.copy(anchorDate = candidateEnd)
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val raw = loadAllData()
                val earliest = findEarliestDate(raw)
                val period = periodFor(state.selectedRange, state.anchorDate, earliest)
                val previous = previousPeriod(period, state.selectedRange)
                val sections = buildSections(raw, period, previous)
                val dailySteps = buildDailyStepPoints(raw.activity, period)
                val selectedFilter = state.selectedFilter.takeIf { sections.containsKey(it) }
                    ?: StatsFilter.OVERVIEW

                state = state.copy(
                    selectedFilter = selectedFilter,
                    periodLabel = period.label,
                    comparisonPeriodLabel = previous?.label.orEmpty(),
                    canMoveNext = state.selectedRange != StatsRange.ALL_TIME && period.end < LocalDate.now(),
                    dailySteps = dailySteps,
                    sections = sections,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (exception: Exception) {
                Log.e("DailyRebuildStats", "Could not load statistics", exception)
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Could not load statistics."
                )
            }
        }
    }

    private suspend fun loadAllData(): RawStatsData = RawStatsData(
        records = repositories.dailyRecords.getAllRecords(),
        foodEntries = repositories.food.getAllEntries(),
        drinkEntries = repositories.drinks.getAllEntries(),
        foodProducts = repositories.food.getAllProducts(),
        activity = repositories.activity.getAllSnapshots(),
        mobility = repositories.mobility.getAllSessions(),
        showers = repositories.showers.getAllLogs(),
        migraines = repositories.migraines.getAllLogs(),
        meetings = repositories.meetings.getAllAttendance(),
        appointments = repositories.appointments.getAllAppointments(),
        visits = repositories.careVisits.getAllVisits(),
        measurements = repositories.healthProfile.getAllMeasurements(),
        lifeMaintenance = repositories.lifeMaintenance.getAllLogs(),
        iopGroups = repositories.iopGroups.getAll(),
        iopMisses = repositories.iopAttendance.getAllMissed()
    )

    private fun findEarliestDate(raw: RawStatsData): LocalDate {
        val dates = buildList {
            raw.records.mapNotNullTo(this) { parseDate(it.date) }
            raw.foodEntries.mapNotNullTo(this) { parseDate(it.date) }
            raw.drinkEntries.mapNotNullTo(this) { parseDate(it.date) }
            raw.activity.mapNotNullTo(this) { parseDate(it.date) }
            raw.mobility.mapNotNullTo(this) { parseDate(it.date) }
            raw.showers.mapNotNullTo(this) { parseDate(it.date) }
            raw.migraines.mapNotNullTo(this) { parseDate(it.date) }
            raw.meetings.mapNotNullTo(this) { parseDate(it.date) }
            raw.appointments.mapNotNullTo(this) { parseDate(it.date) }
            raw.visits.mapNotNullTo(this) { parseDate(it.date) }
            raw.measurements.mapNotNullTo(this) { parseDate(it.recordedDate) }
            raw.lifeMaintenance.mapNotNullTo(this) { parseDate(it.date) }
            raw.iopMisses.mapNotNullTo(this) { parseDate(it.occurrenceDate) }
            raw.iopGroups.mapNotNullTo(this) { group ->
                runCatching {
                    Instant.ofEpochMilli(group.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.getOrNull()
            }
        }
        return dates.minOrNull() ?: LocalDate.now()
    }

    private fun currentPeriodWithoutEarliest(): DatePeriod = periodFor(
        range = state.selectedRange,
        anchor = state.anchorDate,
        earliest = state.customStartDate
    )

    private fun periodFor(
        range: StatsRange,
        anchor: LocalDate,
        earliest: LocalDate
    ): DatePeriod {
        val today = LocalDate.now()
        return when (range) {
            StatsRange.LAST_7_DAYS,
            StatsRange.LAST_30_DAYS,
            StatsRange.LAST_90_DAYS -> {
                val dayCount = range.dayCount ?: 30
                val end = anchor.coerceAtMost(today)
                val start = end.minusDays((dayCount - 1).toLong())
                DatePeriod(start, end, formatDateRange(start, end))
            }
            StatsRange.CUSTOM -> {
                val start = minOf(state.customStartDate, state.customEndDate)
                val end = maxOf(state.customStartDate, state.customEndDate).coerceAtMost(today)
                DatePeriod(start, end, formatDateRange(start, end))
            }
            StatsRange.ALL_TIME -> {
                val start = minOf(earliest, today)
                DatePeriod(start, today, formatDateRange(start, today))
            }
        }
    }

    private fun previousPeriod(
        current: DatePeriod,
        range: StatsRange
    ): DatePeriod? {
        if (range == StatsRange.ALL_TIME) return null
        val length = ChronoUnit.DAYS.between(current.start, current.end) + 1L
        val end = current.start.minusDays(1)
        val start = end.minusDays(length - 1L)
        return DatePeriod(start, end, formatDateRange(start, end))
    }

    private fun buildSections(
        raw: RawStatsData,
        current: DatePeriod,
        previous: DatePeriod?
    ): Map<StatsFilter, StatsSection> {
        val days = dayCount(current)
        val currentRecords = raw.records.inPeriod(current) { it.date }
        val previousRecords = previous?.let { raw.records.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentFood = raw.foodEntries.inPeriod(current) { it.date }
        val previousFood = previous?.let { raw.foodEntries.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentDrinks = raw.drinkEntries.inPeriod(current) { it.date }
        val previousDrinks = previous?.let { raw.drinkEntries.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentActivity = raw.activity.inPeriod(current) { it.date }
        val previousActivity = previous?.let { raw.activity.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMobility = raw.mobility.inPeriod(current) { it.date }
        val previousMobility = previous?.let { raw.mobility.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMigraines = raw.migraines.inPeriod(current) { it.date }
        val previousMigraines = previous?.let { raw.migraines.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMeetings = raw.meetings.inPeriod(current) { it.date }
        val previousMeetings = previous?.let { raw.meetings.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMeasurements = raw.measurements.inPeriod(current) { it.recordedDate }
        val previousMeasurements = previous?.let { raw.measurements.inPeriod(it) { row -> row.recordedDate } }.orEmpty()
        val currentMaintenance = raw.lifeMaintenance.inPeriod(current) { it.date }
        val previousMaintenance = previous?.let { raw.lifeMaintenance.inPeriod(it) { row -> row.date } }.orEmpty()

        val currentFoodCaloriesByDate = currentFood.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.calories } }
        val previousFoodCaloriesByDate = previousFood.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.calories } }
        val currentDrinkCaloriesByDate = currentDrinks.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.calories } }
        val previousDrinkCaloriesByDate = previousDrinks.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.calories } }
        val currentCaloriesByDate = mergeNumericDateMaps(
            currentFoodCaloriesByDate,
            currentDrinkCaloriesByDate
        )
        val previousCaloriesByDate = mergeNumericDateMaps(
            previousFoodCaloriesByDate,
            previousDrinkCaloriesByDate
        )

        val currentFoodProteinByDate = currentFood.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.proteinGrams } }
        val previousFoodProteinByDate = previousFood.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.proteinGrams } }
        val currentDrinkProteinByDate = currentDrinks.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.proteinGrams } }
        val previousDrinkProteinByDate = previousDrinks.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.proteinGrams } }
        val currentProteinByDate = mergeNumericDateMaps(
            currentFoodProteinByDate,
            currentDrinkProteinByDate
        )
        val previousProteinByDate = mergeNumericDateMaps(
            previousFoodProteinByDate,
            previousDrinkProteinByDate
        )

        val calorieSummary = summaryOf(currentCaloriesByDate.values)
        val previousCalorieSummary = summaryOf(previousCaloriesByDate.values)
        val proteinSummary = summaryOf(currentProteinByDate.values)
        val previousProteinSummary = summaryOf(previousProteinByDate.values)

        val currentLegacyWaterByDate = currentRecords.mapNotNull { record ->
            val total = waterOunces(record)
            if (total > 0.0) record.date to total else null
        }.toMap()
        val previousLegacyWaterByDate = previousRecords.mapNotNull { record ->
            val total = waterOunces(record)
            if (total > 0.0) record.date to total else null
        }.toMap()
        val currentDrinkGroups = currentDrinks.groupBy { it.date }
        val previousDrinkGroups = previousDrinks.groupBy { it.date }
        val currentWaterByDate = mergeDrinkWithLegacy(
            currentDrinkGroups.mapValues { (_, rows) ->
                rows.filter(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
            },
            currentLegacyWaterByDate
        )
        val previousWaterByDate = mergeDrinkWithLegacy(
            previousDrinkGroups.mapValues { (_, rows) ->
                rows.filter(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
            },
            previousLegacyWaterByDate
        )
        val currentFluidByDate = mergeDrinkWithLegacy(
            currentDrinkGroups.mapValues { (_, rows) -> rows.sumOf(DrinkEntry::amountFlOz) },
            currentLegacyWaterByDate
        )
        val previousFluidByDate = mergeDrinkWithLegacy(
            previousDrinkGroups.mapValues { (_, rows) -> rows.sumOf(DrinkEntry::amountFlOz) },
            previousLegacyWaterByDate
        )
        val currentOtherByDate = currentDrinkGroups.mapValues { (_, rows) ->
            rows.filterNot(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
        }.filterValues { it > 0.0 }
        val previousOtherByDate = previousDrinkGroups.mapValues { (_, rows) ->
            rows.filterNot(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
        }.filterValues { it > 0.0 }
        val fluidSummary = summaryOf(currentFluidByDate.values)
        val previousFluidSummary = summaryOf(previousFluidByDate.values)
        val waterSummary = summaryOf(currentWaterByDate.values.filter { it > 0.0 })
        val previousWaterSummary = summaryOf(previousWaterByDate.values.filter { it > 0.0 })
        val plainWater = currentDrinks
            .filter { it.categorySnapshot == DrinkCategory.WATER }
            .sumOf(DrinkEntry::amountFlOz) +
            currentRecords.sumOf(::plainWaterOunces)
        val flavoredWater = currentDrinks
            .filter { it.categorySnapshot == DrinkCategory.FLAVORED_WATER }
            .sumOf(DrinkEntry::amountFlOz) +
            currentRecords.sumOf(::flavoredWaterOunces)
        val otherFluids = currentOtherByDate.values.sum()
        val caffeine = currentDrinks.sumOf(DrinkEntry::caffeineMilligrams)

        val currentBackPain = currentRecords.filter(DailyRecord::painRecorded).map { it.backPain.toDouble() }
        val previousBackPain = previousRecords.filter(DailyRecord::painRecorded).map { it.backPain.toDouble() }
        val currentShinPain = currentRecords.filter(DailyRecord::painRecorded).map { it.shinPain.toDouble() }
        val previousShinPain = previousRecords.filter(DailyRecord::painRecorded).map { it.shinPain.toDouble() }
        val currentHighestPainByDate = currentRecords.filter(DailyRecord::painRecorded)
            .associate { it.date to maxOf(it.backPain, it.shinPain).toDouble() }

        val currentStepsByDate = dailyStepsByDate(currentActivity, current)
        val previousStepsByDate = previous
            ?.let { dailyStepsByDate(previousActivity, it) }
            .orEmpty()
        val currentSteps = summaryOf(currentStepsByDate.values)
        val previousSteps = summaryOf(previousStepsByDate.values)
        val currentMobilityMinutes = currentMobility.sumOf { it.elapsedSeconds } / 60.0
        val previousMobilityMinutes = previousMobility.sumOf { it.elapsedSeconds } / 60.0
        val mobilityMinutesByDate = currentMobility.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.elapsedSeconds } / 60.0 }

        val currentIop = buildIopSummary(raw, current)
        val previousIop = previous?.let { buildIopSummary(raw, it) }

        val nutrition = StatsSection(
            metrics = listOf(
                averageMetric("Average calories", calorieSummary, previousCalorieSummary, "cal", days, "nutrition-logged days", previous),
                averageMetric("Average protein", proteinSummary, previousProteinSummary, "g", days, "nutrition-logged days", previous, 1),
                countMetric("Nutrition-logged days", currentCaloriesByDate.size, previousCaloriesByDate.size.takeIf { previous != null }, "of $days selected days", previous),
                countMetric(
                    "Saved meals logged",
                    currentFood.mapNotNull(FoodLogEntry::mealLogId).distinct().size,
                    previousFood.mapNotNull(FoodLogEntry::mealLogId).distinct().size.takeIf { previous != null },
                    "Distinct saved-meal additions",
                    previous
                )
            ),
            charts = listOf(
                numericChart("Daily calories", "Food and calorie-bearing drinks are included.", StatsChartType.LINE, current, currentCaloriesByDate, "cal", 0),
                numericChart("Daily protein", "Food and protein-bearing drinks are included.", StatsChartType.LINE, current, currentProteinByDate, "g", 1)
            ),
            highlights = nutritionHighlights(
                entries = currentFood,
                products = raw.foodProducts,
                caloriesByDate = currentCaloriesByDate
            ),
            notes = listOf("Nutrition totals include recorded drinks. Missing days are not counted as zero.")
        )

        val water = StatsSection(
            metrics = listOf(
                averageMetric("Average fluids", fluidSummary, previousFluidSummary, waterUnitLabel(), days, "drink-logged days", previous, 1, ::displayWater),
                valueMetric(
                    "Total fluids",
                    currentFluidByDate.values.takeIf { it.isNotEmpty() }?.sum()?.let(::displayWater),
                    previousFluidByDate.values.takeIf { it.isNotEmpty() }?.sum()?.let(::displayWater),
                    waterUnitLabel(),
                    currentFluidByDate.size,
                    previous
                ),
                valueMetric(
                    "Water",
                    currentWaterByDate.values.takeIf { it.isNotEmpty() }?.sum()?.let(::displayWater),
                    previousWaterByDate.values.takeIf { it.isNotEmpty() }?.sum()?.let(::displayWater),
                    waterUnitLabel(),
                    currentWaterByDate.count { it.value > 0.0 },
                    previous
                ),
                valueMetric(
                    "Other drinks",
                    otherFluids.takeIf { it > 0.0 }?.let(::displayWater),
                    previousOtherByDate.values.sum().takeIf { it > 0.0 }?.let(::displayWater),
                    waterUnitLabel(),
                    currentOtherByDate.size,
                    previous
                ),
                valueMetric(
                    "Caffeine",
                    caffeine.takeIf { it > 0.0 },
                    previousDrinks.sumOf(DrinkEntry::caffeineMilligrams).takeIf { it > 0.0 },
                    "mg",
                    currentDrinks.count { it.caffeineMilligrams > 0.0 },
                    previous
                )
            ),
            charts = listOf(
                numericChart(
                    "Total fluids by day",
                    "${currentFluidByDate.size} day${plural(currentFluidByDate.size)} with drink records.",
                    StatsChartType.BAR,
                    current,
                    currentFluidByDate.mapValues { displayWater(it.value) },
                    waterUnitLabel(),
                    1
                ),
                numericChart(
                    "Water by day",
                    "Plain and flavored water only.",
                    StatsChartType.BAR,
                    current,
                    currentWaterByDate.mapValues { displayWater(it.value) },
                    waterUnitLabel(),
                    1
                )
            ),
            highlights = buildList {
                if (plainWater > 0.0) add(StatsListItem("Plain water", "${formatNumber(displayWater(plainWater), 1)} ${waterUnitLabel()}"))
                if (flavoredWater > 0.0) add(StatsListItem("Flavored water", "${formatNumber(displayWater(flavoredWater), 1)} ${waterUnitLabel()}"))
                addAll(highLowHighlights(currentFluidByDate, waterUnitLabel(), 1, ::displayWater))
            },
            notes = listOf("Water and other drinks remain separate while Total fluids includes everything. Calorie-containing drinks also contribute to Nutrition statistics.")
        )

        val pain = StatsSection(
            metrics = listOf(
                averageMetric("Average back pain", summaryOf(currentBackPain), summaryOf(previousBackPain), "/ 10", days, "pain-recorded days", previous, 1),
                averageMetric("Average shin pain", summaryOf(currentShinPain), summaryOf(previousShinPain), "/ 10", days, "pain-recorded days", previous, 1),
                countMetric("Pain-recorded days", currentBackPain.size, previousBackPain.size.takeIf { previous != null }, "of $days selected days", previous),
                countMetric("Higher-pain days", currentHighestPainByDate.values.count { it >= 7.0 }, previousRecords.count { it.painRecorded && maxOf(it.backPain, it.shinPain) >= 7f }.takeIf { previous != null }, "Days with a recorded value of 7–10", previous)
            ),
            charts = listOf(
                numericChart("Highest recorded pain", "The higher of back or shin pain for each recorded day.", StatsChartType.LINE, current, currentHighestPainByDate, "/ 10", 1)
            ),
            highlights = painDistribution(currentHighestPainByDate),
            notes = listOf("These are recorded patterns, not medical conclusions. A change only describes the values entered in Daily Rebuild.")
        )

        val mobility = StatsSection(
            metrics = listOf(
                countMetric("Mobility sessions", currentMobility.size, previousMobility.size.takeIf { previous != null }, "Completed sessions", previous),
                valueMetric("Total mobility time", currentMobilityMinutes, previousMobilityMinutes.takeIf { previous != null }, "min", currentMobility.size, previous),
                valueMetric("Average session", currentMobility.takeIf { it.isNotEmpty() }?.let { currentMobilityMinutes / it.size }, previousMobility.takeIf { it.isNotEmpty() }?.let { previousMobilityMinutes / it.size }, "min", currentMobility.size, previous),
                averageMetric("Average steps", currentSteps, previousSteps, "steps", days, "selected days", previous, 0)
            ),
            charts = listOf(
                numericChart("Mobility minutes", "Total completed mobility time each day.", StatsChartType.BAR, current, mobilityMinutesByDate, "min", 0),
                numericChart("Steps", "Every selected day is included; a day without a Fit snapshot counts as 0 steps.", StatsChartType.LINE, current, currentStepsByDate, "steps", 0, treatMissingAsZero = true)
            ),
            highlights = frequentMobilityRoutines(currentMobility),
            notes = listOf("Mobility and connected walking data stay separate so one does not silently substitute for the other.")
        )

        val meetings = StatsSection(
            metrics = listOf(
                countMetric("Recovery meetings", currentMeetings.size, previousMeetings.size.takeIf { previous != null }, "Manually logged attendance", previous),
                valueMetric("Meeting minutes", currentMeetings.sumOf { it.durationMinutes }.toDouble(), previousMeetings.sumOf { it.durationMinutes }.toDouble().takeIf { previous != null }, "min", currentMeetings.size, previous),
                countMetric("IOP attended", currentIop.attended, previousIop?.attended, "Automatic unless marked missed", previous),
                countMetric("IOP missed", currentIop.missed, previousIop?.missed, "A reason is required for every missed group", previous),
                countMetric("IOP scheduled", currentIop.scheduled, previousIop?.scheduled, "Completed scheduled occurrences", previous)
            ),
            charts = listOf(
                countChart("Recovery meetings", "Manually logged meeting attendance.", current, currentMeetings.map { it.date }),
                countChart("IOP attended", "Scheduled IOP groups minus explicit missed records.", current, currentIop.attendedDates)
            ),
            highlights = currentIop.missedRows.map { miss ->
                StatsListItem(
                    label = "Missed ${miss.groupNameSnapshot}",
                    value = formatDate(parseDate(miss.occurrenceDate) ?: LocalDate.now()),
                    detail = miss.reason,
                    historyDate = miss.occurrenceDate
                )
            },
            notes = listOf("Every completed scheduled IOP group is treated as attended automatically unless you explicitly mark it missed and provide a reason.")
        )

        val health = healthSection(currentMeasurements, previousMeasurements, current, previous)
        val migraine = migraineSection(raw, currentMigraines, previousMigraines, current, previous)
        val maintenance = maintenanceSection(raw.lifeMaintenance, currentMaintenance, previousMaintenance, previous)

        val overview = StatsSection(
            metrics = listOf(
                averageMetric("Average calories", calorieSummary, previousCalorieSummary, "cal", days, "nutrition-logged days", previous),
                averageMetric("Average fluids", fluidSummary, previousFluidSummary, waterUnitLabel(), days, "drink-logged days", previous, 1, ::displayWater),
                averageMetric("Average highest pain", summaryOf(currentHighestPainByDate.values), summaryOf(previousRecords.filter(DailyRecord::painRecorded).map { maxOf(it.backPain, it.shinPain).toDouble() }), "/ 10", days, "pain-recorded days", previous, 1),
                countMetric("Mobility sessions", currentMobility.size, previousMobility.size.takeIf { previous != null }, "${formatNumber(currentMobilityMinutes, 0)} total minutes", previous),
                countMetric("Recovery meetings", currentMeetings.size, previousMeetings.size.takeIf { previous != null }, "Manually logged", previous),
                countMetric("IOP attended", currentIop.attended, previousIop?.attended, "${currentIop.missed} marked missed", previous)
            ),
            charts = listOf(
                numericChart("Calories", "Food and calorie-bearing drinks.", StatsChartType.LINE, current, currentCaloriesByDate, "cal", 0),
                numericChart("Drinks", "Drink-logged days only.", StatsChartType.BAR, current, currentFluidByDate.mapValues { displayWater(it.value) }, waterUnitLabel(), 1)
            ),
            notes = listOf("Every metric states its data coverage. No-data days remain different from a true value of zero.")
        )

        return linkedMapOf(
            StatsFilter.OVERVIEW to overview,
            StatsFilter.NUTRITION to nutrition,
            StatsFilter.WATER to water,
            StatsFilter.PAIN to pain,
            StatsFilter.MOBILITY to mobility,
            StatsFilter.MEETINGS to meetings,
            StatsFilter.HEALTH to health,
            StatsFilter.MIGRAINE to migraine,
            StatsFilter.MAINTENANCE to maintenance
        )
    }

    private fun healthSection(
        current: List<HealthMeasurement>,
        previous: List<HealthMeasurement>,
        period: DatePeriod,
        previousPeriod: DatePeriod?
    ): StatsSection {
        val weights = current.filter { it.type == HealthMeasurementType.WEIGHT }
        val previousWeights = previous.filter { it.type == HealthMeasurementType.WEIGHT }
        val bloodPressure = current.filter { it.type == HealthMeasurementType.BLOOD_PRESSURE }
        val a1c = current.filter { it.type == HealthMeasurementType.A1C }
        val cholesterol = current.filter { it.type == HealthMeasurementType.CHOLESTEROL }

        val latestWeight = weights.latestMeasurement()?.primaryValue
        val previousLatestWeight = previousWeights.latestMeasurement()?.primaryValue
        val displayedWeight = latestWeight?.let(::displayWeight)
        val displayedPreviousWeight = previousLatestWeight?.let(::displayWeight)

        val charts = buildList {
            if (weights.isNotEmpty()) {
                add(
                    numericChart(
                        "Weight",
                        "${weights.size} measurement${plural(weights.size)}.",
                        StatsChartType.LINE,
                        period,
                        weights.associate { it.recordedDate to displayWeight(it.primaryValue) },
                        weightUnitLabel(),
                        1
                    )
                )
            }
            if (bloodPressure.isNotEmpty()) {
                add(
                    numericChart(
                        "Systolic blood pressure",
                        "Tap a point to open the recorded date.",
                        StatsChartType.LINE,
                        period,
                        bloodPressure.associate { it.recordedDate to it.primaryValue },
                        "mmHg",
                        0
                    )
                )
            }
        }

        return StatsSection(
            metrics = listOf(
                valueMetric("Latest weight", displayedWeight, displayedPreviousWeight, weightUnitLabel(), weights.size, previousPeriod),
                latestMeasurementMetric("Latest blood pressure", bloodPressure) { row ->
                    val diastolic = row.secondaryValue?.let { "/${formatNumber(it, 0)}" }.orEmpty()
                    "${formatNumber(row.primaryValue, 0)}$diastolic mmHg"
                },
                latestMeasurementMetric("Latest A1C", a1c) { "${formatNumber(it.primaryValue, 1)}%" },
                latestMeasurementMetric("Latest cholesterol", cholesterol) { formatNumber(it.primaryValue, 0) }
            ),
            charts = charts,
            highlights = current.sortedByDescending { it.recordedDate }.take(6).map { row ->
                StatsListItem(
                    label = healthTypeLabel(row.type),
                    value = formatHealthMeasurement(row),
                    detail = formatDate(parseDate(row.recordedDate) ?: LocalDate.now()),
                    historyDate = row.recordedDate
                )
            },
            notes = listOf("Health trends summarize stored measurements only and do not interpret what the values mean medically.")
        )
    }

    private fun migraineSection(
        raw: RawStatsData,
        current: List<MigraineLog>,
        previous: List<MigraineLog>,
        period: DatePeriod,
        previousPeriod: DatePeriod?
    ): StatsSection {
        val durations = current.mapNotNull(MigraineLog::auraDurationMinutes)
        val latestAllTime = raw.migraines.maxByOrNull(MigraineLog::occurredAt)
        val daysSince = latestAllTime?.date?.let(::parseDate)?.let {
            ChronoUnit.DAYS.between(it, LocalDate.now()).coerceAtLeast(0)
        }

        return StatsSection(
            metrics = listOf(
                countMetric("Events", current.size, previous.size.takeIf { previousPeriod != null }, "Migraine or visual-aura logs", previousPeriod),
                countMetric("Visual aura", current.count(MigraineLog::visualAura), previous.count(MigraineLog::visualAura).takeIf { previousPeriod != null }, "Events marked with visual aura", previousPeriod),
                valueMetric("Average aura duration", durations.takeIf { it.isNotEmpty() }?.average(), previous.mapNotNull(MigraineLog::auraDurationMinutes).takeIf { it.isNotEmpty() }?.average(), "min", durations.size, previousPeriod),
                StatsMetric(
                    label = "Days since latest event",
                    value = daysSince?.toString() ?: "No events",
                    detail = latestAllTime?.date?.let(::parseDate)?.let(::formatDate) ?: "No migraine or aura event has been recorded"
                )
            ),
            charts = listOf(
                countChart("Migraine / aura events", "Event count by day.", period, current.map { it.date })
            ),
            highlights = current.sortedByDescending(MigraineLog::occurredAt).take(8).map { event ->
                StatsListItem(
                    label = when {
                        event.visualAura && event.headPain -> "Visual aura with head pain"
                        event.visualAura -> "Visual aura"
                        else -> "Migraine event"
                    },
                    value = event.auraDurationMinutes?.let { "$it min" } ?: "Duration not recorded",
                    detail = event.notes.ifBlank { formatDate(parseDate(event.date) ?: LocalDate.now()) },
                    historyDate = event.date
                )
            },
            notes = listOf("Event counts and durations describe what was recorded; they do not establish a cause or diagnosis.")
        )
    }

    private fun maintenanceSection(
        allLogs: List<LifeMaintenanceLog>,
        current: List<LifeMaintenanceLog>,
        previous: List<LifeMaintenanceLog>,
        previousPeriod: DatePeriod?
    ): StatsSection {
        val metrics = LifeMaintenanceTasks.all.map { task ->
            val latest = allLogs.filter { it.taskKey == task.key }
                .maxWithOrNull(compareBy<LifeMaintenanceLog> { it.date }.thenBy { it.completedAt })
            val currentCount = current.count { it.taskKey == task.key }
            val previousCount = previous.count { it.taskKey == task.key }
            StatsMetric(
                label = task.label,
                value = latest?.date?.let(::parseDate)?.let(::formatDate) ?: "Not recorded",
                detail = "$currentCount completion${plural(currentCount)} in this period",
                comparison = if (previousPeriod == null) "" else comparisonText(
                    currentCount.toDouble(),
                    previousCount.toDouble(),
                    "completion${plural(previousCount)}",
                    0,
                    previousPeriod
                )
            )
        }

        return StatsSection(
            metrics = metrics,
            highlights = current.sortedByDescending { it.date }.take(10).map { log ->
                val task = LifeMaintenanceTasks.all.firstOrNull { it.key == log.taskKey }
                StatsListItem(
                    label = task?.label ?: log.taskKey,
                    value = formatDate(parseDate(log.date) ?: LocalDate.now()),
                    detail = "Completed",
                    historyDate = log.date
                )
            },
            notes = listOf("Maintenance uses last-completed history only. It does not create schedules, due dates, or overdue labels.")
        )
    }

    private fun buildIopSummary(raw: RawStatsData, period: DatePeriod): IopPeriodSummary {
        val occurrences = iopOccurrencesInRange(
            groups = raw.iopGroups,
            startDate = period.start,
            endDate = period.end,
            includeStartedToday = false
        )
        val misses = raw.iopMisses.inPeriod(period) { it.occurrenceDate }
        val missedKeys = misses.map { "${it.groupId}|${it.occurrenceDate}" }.toSet()
        val attendedOccurrences = occurrences.filterNot {
            "${it.group.id}|${it.date}" in missedKeys
        }
        val occurrenceKeys = occurrences.map { "${it.group.id}|${it.date}" }.toSet()
        val orphanMisses = misses.count { "${it.groupId}|${it.occurrenceDate}" !in occurrenceKeys }

        return IopPeriodSummary(
            scheduled = occurrences.size + orphanMisses,
            attended = attendedOccurrences.size,
            missed = misses.size,
            attendedDates = attendedOccurrences.map { it.date.toString() },
            missedRows = misses.sortedByDescending { it.occurrenceDate }
        )
    }

    private fun nutritionHighlights(
        entries: List<FoodLogEntry>,
        products: List<FoodProduct>,
        caloriesByDate: Map<String, Double>
    ): List<StatsListItem> {
        val productsById = products.associateBy { it.id }
        val individualEntries = entries.filter { it.mealLogId == null }

        val individualFoods = individualEntries
            .filter { entry ->
                productsById[entry.productId]?.isCondiment != true
            }
            .groupingBy { it.productNameSnapshot.trim() }
            .eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { StatsListItem("Frequently logged food", it.key, "${it.value} log${plural(it.value)}") }

        val condiments = individualEntries
            .filter { entry ->
                productsById[entry.productId]?.isCondiment == true
            }
            .groupingBy { it.productNameSnapshot.trim() }
            .eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { StatsListItem("Frequently logged condiment", it.key, "${it.value} log${plural(it.value)}") }

        val savedMeals = entries.filter { it.mealLogId != null }
            .groupBy { it.mealLogId }
            .values.mapNotNull { rows -> rows.firstOrNull()?.mealName?.trim()?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .take(3)
            .map { StatsListItem("Frequently logged meal", it.key, "${it.value} log${plural(it.value)}") }

        return individualFoods + condiments + savedMeals +
            highLowHighlights(caloriesByDate, "cal", 0)
    }

    private fun frequentMobilityRoutines(sessions: List<MobilitySession>): List<StatsListItem> =
        sessions.groupingBy { it.routineName.ifBlank { "Mobility session" } }
            .eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(5)
            .map { StatsListItem("Frequently used routine", it.key, "${it.value} session${plural(it.value)}") }

    private fun painDistribution(valuesByDate: Map<String, Double>): List<StatsListItem> {
        val low = valuesByDate.values.count { it in 0.0..3.0 }
        val moderate = valuesByDate.values.count { it in 4.0..6.0 }
        val higher = valuesByDate.values.count { it >= 7.0 }
        val highest = valuesByDate.maxByOrNull { it.value }

        return buildList {
            add(StatsListItem("0–3", low.toString(), "recorded day${plural(low)}"))
            add(StatsListItem("4–6", moderate.toString(), "recorded day${plural(moderate)}"))
            add(StatsListItem("7–10", higher.toString(), "recorded day${plural(higher)}"))
            highest?.let { (date, value) ->
                add(
                    StatsListItem(
                        "Highest recorded day",
                        "${formatNumber(value, 1)} / 10",
                        formatDate(parseDate(date) ?: LocalDate.now()),
                        date
                    )
                )
            }
        }
    }

    private fun highLowHighlights(
        valuesByDate: Map<String, Double>,
        suffix: String,
        decimals: Int,
        transform: (Double) -> Double = { it }
    ): List<StatsListItem> {
        if (valuesByDate.isEmpty()) return emptyList()
        val high = valuesByDate.maxByOrNull { it.value }
        val low = valuesByDate.minByOrNull { it.value }
        return listOfNotNull(
            high?.let { (date, value) ->
                StatsListItem(
                    "Highest logged day",
                    "${formatNumber(transform(value), decimals)} $suffix",
                    formatDate(parseDate(date) ?: LocalDate.now()),
                    date
                )
            },
            low?.let { (date, value) ->
                StatsListItem(
                    "Lowest logged day",
                    "${formatNumber(transform(value), decimals)} $suffix",
                    formatDate(parseDate(date) ?: LocalDate.now()),
                    date
                )
            }
        )
    }

    private fun averageMetric(
        label: String,
        current: NumericSummary,
        previous: NumericSummary,
        suffix: String,
        days: Int,
        coverageLabel: String,
        previousPeriod: DatePeriod?,
        decimals: Int = 0,
        transform: (Double) -> Double = { it }
    ): StatsMetric = StatsMetric(
        label = label,
        value = current.value?.let { "${formatNumber(transform(it), decimals)} $suffix" } ?: "No data",
        detail = "Based on ${current.coverage} of $days $coverageLabel",
        comparison = comparisonText(
            current.value?.let(transform),
            previous.value?.let(transform),
            suffix,
            decimals,
            previousPeriod
        )
    )

    private fun valueMetric(
        label: String,
        current: Double?,
        previous: Double?,
        suffix: String,
        coverage: Int,
        previousPeriod: DatePeriod?
    ): StatsMetric = StatsMetric(
        label = label,
        value = current?.let { "${formatNumber(it, if (abs(it % 1.0) < 0.001) 0 else 1)} $suffix" } ?: "No data",
        detail = "$coverage record${plural(coverage)} in this period",
        comparison = comparisonText(current, previous, suffix, 1, previousPeriod)
    )

    private fun countMetric(
        label: String,
        current: Int,
        previous: Int?,
        detail: String,
        previousPeriod: DatePeriod?
    ): StatsMetric = StatsMetric(
        label = label,
        value = String.format(Locale.US, "%,d", current),
        detail = detail,
        comparison = comparisonText(current.toDouble(), previous?.toDouble(), "", 0, previousPeriod)
    )

    private fun latestMeasurementMetric(
        label: String,
        rows: List<HealthMeasurement>,
        formatter: (HealthMeasurement) -> String
    ): StatsMetric {
        val latest = rows.latestMeasurement()
        return StatsMetric(
            label = label,
            value = latest?.let(formatter) ?: "No data",
            detail = latest?.recordedDate?.let(::parseDate)?.let(::formatDate)
                ?: "No measurement recorded in this period"
        )
    }

    private fun comparisonText(
        current: Double?,
        previous: Double?,
        suffix: String,
        decimals: Int,
        previousPeriod: DatePeriod?
    ): String {
        if (previousPeriod == null || current == null || previous == null) return ""
        val difference = current - previous
        if (abs(difference) < 0.0001) return "No change from ${previousPeriod.label}"
        val sign = if (difference > 0.0) "+" else ""
        val suffixText = suffix.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
        return "$sign${formatNumber(difference, decimals)}$suffixText vs ${previousPeriod.label}"
    }

    private fun buildDailyStepPoints(
        activity: List<DailyActivitySnapshot>,
        period: DatePeriod
    ): List<StatsPoint> = dailyStepsByDate(activity, period).map { (dateText, steps) ->
        val date = LocalDate.parse(dateText)
        StatsPoint(
            label = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)),
            value = steps,
            valueText = String.format(Locale.US, "%,d steps", steps.toLong()),
            historyDate = dateText,
            hasData = true
        )
    }

    private fun dailyStepsByDate(
        activity: List<DailyActivitySnapshot>,
        period: DatePeriod
    ): Map<String, Double> {
        val recordedSteps = activity
            .inPeriod(period) { it.date }
            .associate { it.date to it.steps.toDouble() }

        return linkedMapOf<String, Double>().apply {
            dateSequence(period.start, period.end).forEach { date ->
                put(date.toString(), recordedSteps[date.toString()] ?: 0.0)
            }
        }
    }

    private fun numericChart(
        title: String,
        subtitle: String,
        type: StatsChartType,
        period: DatePeriod,
        valuesByDate: Map<String, Double>,
        suffix: String,
        decimals: Int,
        treatMissingAsZero: Boolean = false
    ): StatsChart {
        val useMonths = dayCount(period) > 120
        val points = if (!useMonths) {
            dateSequence(period.start, period.end).map { date ->
                val stored = valuesByDate[date.toString()]
                val value = stored ?: if (treatMissingAsZero) 0.0 else null
                StatsPoint(
                    label = when {
                        dayCount(period) <= 14 -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
                        else -> date.format(DateTimeFormatter.ofPattern("M/d", Locale.US))
                    },
                    value = value ?: 0.0,
                    valueText = value?.let { "${formatNumber(it, decimals)} $suffix" } ?: "No data",
                    historyDate = date.toString(),
                    hasData = value != null
                )
            }
        } else {
            monthSequence(YearMonth.from(period.start), YearMonth.from(period.end)).map { month ->
                val rows = valuesByDate.entries.mapNotNull { (dateText, value) ->
                    val date = parseDate(dateText) ?: return@mapNotNull null
                    if (YearMonth.from(date) == month) date to value else null
                }
                val value = when {
                    rows.isEmpty() && treatMissingAsZero -> 0.0
                    rows.isEmpty() -> null
                    type == StatsChartType.BAR -> rows.sumOf { it.second }
                    else -> rows.map { it.second }.average()
                }
                StatsPoint(
                    label = month.format(DateTimeFormatter.ofPattern("MMM yy", Locale.US)),
                    value = value ?: 0.0,
                    valueText = value?.let { "${formatNumber(it, decimals)} $suffix" } ?: "No data",
                    historyDate = rows.maxByOrNull { it.first }?.first?.toString(),
                    hasData = value != null
                )
            }
        }
        return StatsChart(title, subtitle, type, points)
    }

    private fun countChart(
        title: String,
        subtitle: String,
        period: DatePeriod,
        dates: List<String>
    ): StatsChart = numericChart(
        title = title,
        subtitle = subtitle,
        type = StatsChartType.BAR,
        period = period,
        valuesByDate = dates.groupingBy { it }.eachCount().mapValues { it.value.toDouble() },
        suffix = "",
        decimals = 0,
        treatMissingAsZero = true
    )

    private fun mergeNumericDateMaps(
        first: Map<String, Double>,
        second: Map<String, Double>
    ): Map<String, Double> = (first.keys + second.keys).associateWith { date ->
        first.getOrDefault(date, 0.0) + second.getOrDefault(date, 0.0)
    }.filterValues { it != 0.0 }

    private fun mergeDrinkWithLegacy(
        drinkValues: Map<String, Double>,
        legacyWaterValues: Map<String, Double>
    ): Map<String, Double> = (drinkValues.keys + legacyWaterValues.keys)
        .associateWith { date ->
            drinkValues.getOrDefault(date, 0.0) +
                legacyWaterValues.getOrDefault(date, 0.0)
        }
        .filterValues { it > 0.0 }

    private fun summaryOf(values: Collection<Double>): NumericSummary =
        NumericSummary(values.takeIf { it.isNotEmpty() }?.average(), values.size)

    private fun List<HealthMeasurement>.latestMeasurement(): HealthMeasurement? =
        maxWithOrNull(compareBy<HealthMeasurement> { it.recordedDate }.thenBy { it.createdAt })

    private fun plainWaterOunces(record: DailyRecord): Double =
        record.plainReusableBottleCount * 24.0 + record.plainDisposableBottleCount * 16.9

    private fun flavoredWaterOunces(record: DailyRecord): Double =
        record.mioReusableBottleCount * 24.0 + record.mioDisposableBottleCount * 16.9

    private fun waterOunces(record: DailyRecord): Double =
        plainWaterOunces(record) + flavoredWaterOunces(record)

    private fun displayWater(ounces: Double): Double =
        if (preferences.waterUnit == "ml") ounces * 29.5735 else ounces

    private fun waterUnitLabel(): String = if (preferences.waterUnit == "ml") "mL" else "fl oz"

    private fun displayWeight(pounds: Double): Double =
        if (preferences.weightUnit == "kg") pounds * 0.45359237 else pounds

    private fun weightUnitLabel(): String = if (preferences.weightUnit == "kg") "kg" else "lb"

    private fun healthTypeLabel(type: String): String = when (type) {
        HealthMeasurementType.WEIGHT -> "Weight"
        HealthMeasurementType.BLOOD_PRESSURE -> "Blood pressure"
        HealthMeasurementType.A1C -> "A1C"
        HealthMeasurementType.CHOLESTEROL -> "Cholesterol"
        else -> type.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() }
    }

    private fun formatHealthMeasurement(row: HealthMeasurement): String = when (row.type) {
        HealthMeasurementType.WEIGHT -> "${formatNumber(displayWeight(row.primaryValue), 1)} ${weightUnitLabel()}"
        HealthMeasurementType.BLOOD_PRESSURE -> {
            val secondary = row.secondaryValue?.let { "/${formatNumber(it, 0)}" }.orEmpty()
            "${formatNumber(row.primaryValue, 0)}$secondary mmHg"
        }
        HealthMeasurementType.A1C -> "${formatNumber(row.primaryValue, 1)}%"
        else -> formatNumber(row.primaryValue, 1)
    }

    private fun dayCount(period: DatePeriod): Int =
        (ChronoUnit.DAYS.between(period.start, period.end) + 1L).toInt().coerceAtLeast(1)

    private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    private fun formatDateRange(start: LocalDate, end: LocalDate): String =
        if (start == end) formatDate(start) else "${formatDate(start)}–${formatDate(end)}"

    private fun formatNumber(value: Double, decimals: Int): String =
        if (decimals == 0) String.format(Locale.US, "%,.0f", value)
        else String.format(Locale.US, "%,.${decimals}f", value)

    private fun plural(count: Int): String = if (count == 1) "" else "s"

    private fun dateSequence(start: LocalDate, end: LocalDate): List<LocalDate> = buildList {
        var date = start
        while (!date.isAfter(end)) {
            add(date)
            date = date.plusDays(1)
        }
    }

    private fun monthSequence(start: YearMonth, end: YearMonth): List<YearMonth> = buildList {
        var month = start
        while (!month.isAfter(end)) {
            add(month)
            month = month.plusMonths(1)
        }
    }

    private fun <T> List<T>.inPeriod(
        period: DatePeriod,
        dateSelector: (T) -> String
    ): List<T> = filter { row ->
        val date = parseDate(dateSelector(row)) ?: return@filter false
        !date.isBefore(period.start) && !date.isAfter(period.end)
    }

    companion object {
        fun statsRangeFromPreference(value: String): StatsRange =
            runCatching { StatsRange.valueOf(value) }.getOrDefault(StatsRange.LAST_30_DAYS)

        fun factory(
            repositories: DailyRebuildRepositories,
            preferences: DailyRebuildPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatsViewModel(repositories, preferences) as T
        }
    }
}
