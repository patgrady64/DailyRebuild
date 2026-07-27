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
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class RawStatsData(
    val records: List<DailyRecord>,
    val foodEntries: List<FoodLogEntry>,
    val activity: List<DailyActivitySnapshot>,
    val mobility: List<MobilitySession>,
    val showers: List<ShowerLog>,
    val migraines: List<MigraineLog>,
    val meetings: List<MeetingAttendance>,
    val appointments: List<CareAppointment>,
    val visits: List<CareVisit>,
    val measurements: List<HealthMeasurement>
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

class StatsViewModel(
    private val repositories: DailyRebuildRepositories
) : ViewModel() {

    var state by mutableStateOf(StatsUiState())
        private set

    init {
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

    fun movePrevious() {
        val nextAnchor = when (state.selectedRange) {
            StatsRange.WEEK -> state.anchorDate.minusWeeks(1)
            StatsRange.MONTH -> state.anchorDate.minusMonths(1)
            StatsRange.YEAR -> state.anchorDate.minusYears(1)
            StatsRange.ALL_TIME -> state.anchorDate
        }
        if (nextAnchor != state.anchorDate) {
            state = state.copy(anchorDate = nextAnchor)
            refresh()
        }
    }

    fun moveNext() {
        if (!state.canMoveNext) return
        val today = LocalDate.now()
        val nextAnchor = when (state.selectedRange) {
            StatsRange.WEEK -> state.anchorDate.plusWeeks(1)
            StatsRange.MONTH -> state.anchorDate.plusMonths(1)
            StatsRange.YEAR -> state.anchorDate.plusYears(1)
            StatsRange.ALL_TIME -> state.anchorDate
        }.coerceAtMost(today)
        if (nextAnchor != state.anchorDate) {
            state = state.copy(anchorDate = nextAnchor)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            try {
                val raw = loadAllData()
                val earliest = findEarliestDate(raw)
                val period = periodFor(state.selectedRange, state.anchorDate, earliest)
                val previous = previousPeriod(state.selectedRange, period, earliest)
                val sections = buildSections(raw, period, previous, state.selectedRange)
                state = state.copy(
                    periodLabel = period.label,
                    canMoveNext = state.selectedRange != StatsRange.ALL_TIME && period.end < LocalDate.now(),
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

    private suspend fun loadAllData(): RawStatsData {
        return RawStatsData(
            records = repositories.dailyRecords.getAllRecords(),
            foodEntries = repositories.food.getAllEntries(),
            activity = repositories.activity.getAllSnapshots(),
            mobility = repositories.mobility.getAllSessions(),
            showers = repositories.showers.getAllLogs(),
            migraines = repositories.migraines.getAllLogs(),
            meetings = repositories.meetings.getAllAttendance(),
            appointments = repositories.appointments.getAllAppointments(),
            visits = repositories.careVisits.getAllVisits(),
            measurements = repositories.healthProfile.getAllMeasurements()
        )
    }

    private fun findEarliestDate(raw: RawStatsData): LocalDate {
        val dates = buildList<LocalDate> {
            raw.records.mapNotNullTo(this) { parseDate(it.date) }
            raw.foodEntries.mapNotNullTo(this) { parseDate(it.date) }
            raw.activity.mapNotNullTo(this) { parseDate(it.date) }
            raw.mobility.mapNotNullTo(this) { parseDate(it.date) }
            raw.showers.mapNotNullTo(this) { parseDate(it.date) }
            raw.migraines.mapNotNullTo(this) { parseDate(it.date) }
            raw.meetings.mapNotNullTo(this) { parseDate(it.date) }
            raw.appointments.mapNotNullTo(this) { parseDate(it.date) }
            raw.visits.mapNotNullTo(this) { parseDate(it.date) }
            raw.measurements.mapNotNullTo(this) { parseDate(it.recordedDate) }
        }
        return dates.minOrNull() ?: LocalDate.now()
    }

    private fun periodFor(
        range: StatsRange,
        anchor: LocalDate,
        earliest: LocalDate
    ): DatePeriod {
        val today = LocalDate.now()
        return when (range) {
            StatsRange.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val calendarEnd = start.plusDays(6)
                DatePeriod(
                    start = start,
                    end = minOf(calendarEnd, today),
                    label = "${formatDate(start)}–${formatDate(calendarEnd)}"
                )
            }
            StatsRange.MONTH -> {
                val month = YearMonth.from(anchor)
                val start = month.atDay(1)
                val calendarEnd = month.atEndOfMonth()
                DatePeriod(
                    start = start,
                    end = minOf(calendarEnd, today),
                    label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                )
            }
            StatsRange.YEAR -> {
                val start = LocalDate.of(anchor.year, 1, 1)
                val calendarEnd = LocalDate.of(anchor.year, 12, 31)
                DatePeriod(
                    start = start,
                    end = minOf(calendarEnd, today),
                    label = anchor.year.toString()
                )
            }
            StatsRange.ALL_TIME -> DatePeriod(
                start = minOf(earliest, today),
                end = today,
                label = "${formatDate(minOf(earliest, today))}–${formatDate(today)}"
            )
        }
    }

    private fun previousPeriod(
        range: StatsRange,
        current: DatePeriod,
        earliest: LocalDate
    ): DatePeriod? {
        return when (range) {
            StatsRange.WEEK -> periodFor(range, current.start.minusWeeks(1), earliest)
            StatsRange.MONTH -> periodFor(range, current.start.minusMonths(1), earliest)
            StatsRange.YEAR -> periodFor(range, current.start.minusYears(1), earliest)
            StatsRange.ALL_TIME -> null
        }
    }

    private fun buildSections(
        raw: RawStatsData,
        current: DatePeriod,
        previous: DatePeriod?,
        range: StatsRange
    ): Map<StatsFilter, StatsSection> {
        val days = dayCount(current)
        val hasPreviousPeriod = previous != null
        val currentRecords = raw.records.inPeriod(current) { it.date }
        val previousRecords = previous?.let { raw.records.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentFood = raw.foodEntries.inPeriod(current) { it.date }
        val previousFood = previous?.let { raw.foodEntries.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentActivity = raw.activity.inPeriod(current) { it.date }
        val previousActivity = previous?.let { raw.activity.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMobility = raw.mobility.inPeriod(current) { it.date }
        val previousMobility = previous?.let { raw.mobility.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentShowers = raw.showers.inPeriod(current) { it.date }
        val previousShowers = previous?.let { raw.showers.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMigraines = raw.migraines.inPeriod(current) { it.date }
        val previousMigraines = previous?.let { raw.migraines.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMeetings = raw.meetings.inPeriod(current) { it.date }
        val previousMeetings = previous?.let { raw.meetings.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentAppointments = raw.appointments.inPeriod(current) { it.date }
        val previousAppointments = previous?.let { raw.appointments.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentVisits = raw.visits.inPeriod(current) { it.date }
        val previousVisits = previous?.let { raw.visits.inPeriod(it) { row -> row.date } }.orEmpty()
        val currentMeasurements = raw.measurements.inPeriod(current) { it.recordedDate }
        val previousMeasurements = previous?.let { raw.measurements.inPeriod(it) { row -> row.recordedDate } }.orEmpty()

        val calories = averageDailyFood(currentFood) { it.calories }
        val previousCalories = averageDailyFood(previousFood) { it.calories }
        val water = averageWater(currentRecords)
        val previousWater = averageWater(previousRecords)
        val steps = averageActivity(currentActivity) { it.steps.toDouble() }
        val previousSteps = averageActivity(previousActivity) { it.steps.toDouble() }
        val pain = averagePain(currentRecords)
        val previousPain = averagePain(previousRecords)
        val weights = currentMeasurements.filter { it.type == HealthMeasurementType.WEIGHT }
        val previousWeights = previousMeasurements.filter { it.type == HealthMeasurementType.WEIGHT }
        val latestWeight = weights.maxByOrNull { it.recordedDate }?.primaryValue
        val previousLatestWeight = previousWeights.maxByOrNull { it.recordedDate }?.primaryValue

        val meetingGoal = weeklyGoalText(current, currentMeetings.map { it.date }, 3)
        val meetingGoalCounts = weeklyGoalCounts(current, currentMeetings.map { it.date }, 3)
        val previousMeetingGoalCounts = previous?.let {
            weeklyGoalCounts(it, previousMeetings.map { row -> row.date }, 3)
        } ?: (0 to 0)
        val showerMinimum = weeklyGoalCounts(current, currentShowers.map { it.date }, 2)
        val previousShowerMinimum = previous?.let {
            weeklyGoalCounts(it, previousShowers.map { row -> row.date }, 2)
        } ?: (0 to 0)
        val showerPreferred = weeklyGoalCounts(current, currentShowers.map { it.date }, 3)
        val previousShowerPreferred = previous?.let {
            weeklyGoalCounts(it, previousShowers.map { row -> row.date }, 3)
        } ?: (0 to 0)

        val caloriesChart = numericChart(
            title = "Calories",
            subtitle = coverageText(calories.coverage, days, "food-logged days"),
            type = StatsChartType.LINE,
            range = range,
            period = current,
            valuesByDate = currentFood.groupByDate { it.date }
                .mapValues { (_, entries) -> entries.sumOf { it.calories } },
            monthlyMode = AggregateMode.AVERAGE,
            formatter = { formatNumber(it, 0) + " cal" }
        )
        val waterChart = numericChart(
            title = "Water",
            subtitle = coverageText(water.coverage, days, "hydration days"),
            type = StatsChartType.BAR,
            range = range,
            period = current,
            valuesByDate = currentRecords.mapNotNull { record ->
                val ounces = waterOunces(record)
                if (ounces > 0.0) record.date to ounces else null
            }.toMap(),
            monthlyMode = AggregateMode.AVERAGE,
            formatter = { formatNumber(it, 1) + " oz" }
        )
        val stepsChart = numericChart(
            title = "Steps",
            subtitle = coverageText(steps.coverage, days, "activity-data days"),
            type = StatsChartType.LINE,
            range = range,
            period = current,
            valuesByDate = currentActivity.associate { it.date to it.steps.toDouble() },
            monthlyMode = AggregateMode.AVERAGE,
            formatter = { formatNumber(it, 0) }
        )
        val painChart = numericChart(
            title = "Highest Pain",
            subtitle = coverageText(pain.coverage, days, "pain-recorded days"),
            type = StatsChartType.LINE,
            range = range,
            period = current,
            valuesByDate = currentRecords.filter { it.painRecorded }
                .associate { it.date to maxOf(it.backPain, it.shinPain).toDouble() },
            monthlyMode = AggregateMode.AVERAGE,
            formatter = { formatNumber(it, 1) + " / 10" }
        )
        val weightChart = numericChart(
            title = "Weight",
            subtitle = "${weights.size} measurement${if (weights.size == 1) "" else "s"} in this period",
            type = StatsChartType.LINE,
            range = range,
            period = current,
            valuesByDate = weights.groupBy { it.recordedDate }
                .mapValues { (_, rows) -> rows.maxByOrNull { it.createdAt }!!.primaryValue },
            monthlyMode = AggregateMode.LATEST,
            formatter = { formatNumber(it, 1) + " lb" }
        )
        val mobilityMinutesByDate = currentMobility.groupByDate { it.date }
            .mapValues { (_, sessions) -> sessions.sumOf { it.elapsedSeconds } / 60.0 }
        val mobilityChart = numericChart(
            title = "Mobility Minutes",
            subtitle = "${currentMobility.size} session${if (currentMobility.size == 1) "" else "s"}",
            type = StatsChartType.BAR,
            range = range,
            period = current,
            valuesByDate = mobilityMinutesByDate,
            monthlyMode = AggregateMode.SUM,
            formatter = { formatNumber(it, 0) + " min" }
        )
        val meetingChart = countChart(
            "Meetings",
            "Weekly goal: at least 3",
            range,
            current,
            currentMeetings.map { it.date }
        )
        val migraineChart = countChart(
            "Migraine / Aura Events",
            "Events recorded in the selected period",
            range,
            current,
            currentMigraines.map { it.date }
        )
        val showerChart = countChart(
            "Showers",
            "Weekly minimum 2; preferred 3",
            range,
            current,
            currentShowers.map { it.date }
        )

        val overview = StatsSection(
            metrics = listOf(
                averageMetric("Average calories", calories, previousCalories, "cal", days, "food-logged days", 0),
                averageMetric("Average water", water, previousWater, "oz", days, "hydration days", 1),
                averageMetric("Average steps", steps, previousSteps, "steps", days, "activity-data days", 0),
                averageMetric("Average highest pain", pain, previousPain, "/ 10", days, "pain-recorded days", 1),
                countMetric("AA meetings", currentMeetings.size, previousMeetings.size.takeIf { hasPreviousPeriod }, meetingGoal),
                countMetric("Showers", currentShowers.size, previousShowers.size.takeIf { hasPreviousPeriod }, "Minimum reached in ${showerMinimum.first} of ${showerMinimum.second} week(s)")
            ),
            charts = listOf(stepsChart, painChart),
            notes = listOf("Missing days are not counted as zero. Every average shows its data coverage.")
        )

        val food = StatsSection(
            metrics = listOf(
                averageMetric("Average calories", calories, previousCalories, "cal", days, "food-logged days", 0),
                averageMetric("Average water", water, previousWater, "oz", days, "hydration days", 1),
                countMetric("Food-logged days", calories.coverage, previousCalories.coverage.takeIf { hasPreviousPeriod }, coverageText(calories.coverage, days, "days")),
                countMetric("Saved-meal logs", currentFood.mapNotNull { it.mealLogId }.distinct().size, previousFood.mapNotNull { it.mealLogId }.distinct().size.takeIf { hasPreviousPeriod }, "Distinct meal additions")
            ),
            charts = listOf(caloriesChart, waterChart)
        )

        val movement = StatsSection(
            metrics = listOf(
                averageMetric("Average steps", steps, previousSteps, "steps", days, "activity-data days", 0),
                countMetric("Total miles", currentActivity.sumOf { it.distanceMiles }.roundToInt(), previousActivity.sumOf { it.distanceMiles }.roundToInt().takeIf { hasPreviousPeriod }, "Rounded total miles"),
                countMetric("Mobility sessions", currentMobility.size, previousMobility.size.takeIf { hasPreviousPeriod }, "${formatNumber(currentMobility.sumOf { it.elapsedSeconds } / 60.0, 0)} total minutes"),
                countMetric("Movement days", (currentActivity.map { it.date } + currentMobility.map { it.date }).distinct().size, (previousActivity.map { it.date } + previousMobility.map { it.date }).distinct().size.takeIf { hasPreviousPeriod }, "Connected activity or mobility logged")
            ),
            charts = listOf(stepsChart, mobilityChart)
        )

        val meetings = StatsSection(
            metrics = listOf(
                countMetric("Meetings attended", currentMeetings.size, previousMeetings.size.takeIf { hasPreviousPeriod }, meetingGoal),
                countMetric("Meeting minutes", currentMeetings.sumOf { it.durationMinutes }, previousMeetings.sumOf { it.durationMinutes }.takeIf { hasPreviousPeriod }, "Total logged time"),
                countMetric("Different groups", currentMeetings.map { it.meetingName }.distinct().size, previousMeetings.map { it.meetingName }.distinct().size.takeIf { hasPreviousPeriod }, "Unique meeting names"),
                countMetric("Goal weeks reached", meetingGoalCounts.first, previousMeetingGoalCounts.first.takeIf { hasPreviousPeriod }, meetingGoal)
            ),
            charts = listOf(meetingChart),
            notes = listOf("The weekly goal is measured Monday through Sunday. Going above 3 remains valid.")
        )

        val health = StatsSection(
            metrics = listOf(
                averageMetric("Average highest pain", pain, previousPain, "/ 10", days, "pain-recorded days", 1),
                valueMetric("Latest weight", latestWeight, previousLatestWeight, "lb", weights.size, 1),
                countMetric("Migraine / aura events", currentMigraines.size, previousMigraines.size.takeIf { hasPreviousPeriod }, "${currentMigraines.count { it.visualAura }} with visual aura"),
                countMetric("Appointments", currentAppointments.size, previousAppointments.size.takeIf { hasPreviousPeriod }, appointmentStatusDetail(currentAppointments)),
                countMetric("Completed care visits", currentVisits.size, previousVisits.size.takeIf { hasPreviousPeriod }, "Visits recorded in this period")
            ),
            charts = listOf(painChart, weightChart, migraineChart),
            notes = listOf("Pain uses one value per day: the highest pain experienced that day.")
        )

        val selfCare = StatsSection(
            metrics = listOf(
                countMetric("Showers", currentShowers.size, previousShowers.size.takeIf { hasPreviousPeriod }, "Minimum goal: ${showerMinimum.first} of ${showerMinimum.second} week(s); previous ${previousShowerMinimum.first} of ${previousShowerMinimum.second}"),
                countMetric("Preferred shower weeks", showerPreferred.first, previousShowerPreferred.first.takeIf { hasPreviousPeriod }, "${showerPreferred.first} of ${showerPreferred.second} week(s) reached 3"),
                countMetric("Journal days", currentRecords.count { it.journalText.isNotBlank() }, previousRecords.count { it.journalText.isNotBlank() }.takeIf { hasPreviousPeriod }, "Days with a saved journal note"),
                countMetric("Saved daily records", currentRecords.size, previousRecords.size.takeIf { hasPreviousPeriod }, coverageText(currentRecords.size, days, "days"))
            ),
            charts = listOf(showerChart)
        )

        return mapOf(
            StatsFilter.OVERVIEW to overview,
            StatsFilter.FOOD to food,
            StatsFilter.MOVEMENT to movement,
            StatsFilter.MEETINGS to meetings,
            StatsFilter.HEALTH to health,
            StatsFilter.SELF_CARE to selfCare
        )
    }

    private fun averageDailyFood(
        entries: List<FoodLogEntry>,
        selector: (FoodLogEntry) -> Double
    ): NumericSummary {
        val daily = entries.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf(selector) }
        return NumericSummary(daily.values.takeIf { it.isNotEmpty() }?.average(), daily.size)
    }

    private fun averageWater(records: List<DailyRecord>): NumericSummary {
        val values = records.map(::waterOunces).filter { it > 0.0 }
        return NumericSummary(values.takeIf { it.isNotEmpty() }?.average(), values.size)
    }

    private fun averageActivity(
        snapshots: List<DailyActivitySnapshot>,
        selector: (DailyActivitySnapshot) -> Double
    ): NumericSummary {
        val values = snapshots.map(selector)
        return NumericSummary(values.takeIf { it.isNotEmpty() }?.average(), values.size)
    }

    private fun averagePain(records: List<DailyRecord>): NumericSummary {
        val values = records.filter { it.painRecorded }
            .map { maxOf(it.backPain, it.shinPain).toDouble() }
        return NumericSummary(values.takeIf { it.isNotEmpty() }?.average(), values.size)
    }

    private fun averageMetric(
        label: String,
        current: NumericSummary,
        previous: NumericSummary,
        suffix: String,
        days: Int,
        coverageLabel: String,
        decimals: Int
    ): StatsMetric {
        return StatsMetric(
            label = label,
            value = current.value?.let { "${formatNumber(it, decimals)} $suffix" } ?: "No data",
            detail = coverageText(current.coverage, days, coverageLabel),
            comparison = comparisonText(current.value, previous.value, suffix, decimals)
        )
    }

    private fun valueMetric(
        label: String,
        current: Double?,
        previous: Double?,
        suffix: String,
        coverage: Int,
        decimals: Int
    ): StatsMetric = StatsMetric(
        label = label,
        value = current?.let { "${formatNumber(it, decimals)} $suffix" } ?: "No data",
        detail = "$coverage measurement${if (coverage == 1) "" else "s"}",
        comparison = comparisonText(current, previous, suffix, decimals)
    )

    private fun countMetric(
        label: String,
        current: Int,
        previous: Int?,
        detail: String
    ): StatsMetric = StatsMetric(
        label = label,
        value = String.format(Locale.US, "%,d", current),
        detail = detail,
        comparison = comparisonText(current.toDouble(), previous?.toDouble(), "", 0)
    )

    private fun comparisonText(
        current: Double?,
        previous: Double?,
        suffix: String,
        decimals: Int
    ): String {
        if (current == null || previous == null) return "No previous-period comparison"
        val difference = current - previous
        if (abs(difference) < 0.0001) return "No change from previous period"
        val sign = if (difference > 0) "+" else ""
        val suffixText = suffix.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        return "$sign${formatNumber(difference, decimals)}$suffixText vs previous period"
    }

    private enum class AggregateMode { AVERAGE, SUM, LATEST }

    private fun numericChart(
        title: String,
        subtitle: String,
        type: StatsChartType,
        range: StatsRange,
        period: DatePeriod,
        valuesByDate: Map<String, Double>,
        monthlyMode: AggregateMode,
        formatter: (Double) -> String,
        treatMissingAsZero: Boolean = false
    ): StatsChart {
        val points = if (range == StatsRange.WEEK || range == StatsRange.MONTH) {
            dateSequence(period.start, period.end).map { date ->
                val storedValue = valuesByDate[date.toString()]
                val value = storedValue ?: if (treatMissingAsZero) 0.0 else null
                StatsPoint(
                    label = if (range == StatsRange.WEEK) {
                        date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
                    } else {
                        date.dayOfMonth.toString()
                    },
                    value = value ?: 0.0,
                    valueText = value?.let(formatter) ?: "No data",
                    historyDate = storedValue?.let { date.toString() },
                    hasData = value != null
                )
            }
        } else {
            monthSequence(YearMonth.from(period.start), YearMonth.from(period.end)).map { month ->
                val rows = valuesByDate.entries.mapNotNull { (dateText, value) ->
                    val date = parseDate(dateText) ?: return@mapNotNull null
                    if (YearMonth.from(date) == month) date to value else null
                }.sortedBy { it.first }
                val value = when {
                    rows.isEmpty() && treatMissingAsZero -> 0.0
                    rows.isEmpty() -> null
                    monthlyMode == AggregateMode.SUM -> rows.sumOf { it.second }
                    monthlyMode == AggregateMode.LATEST -> rows.last().second
                    else -> rows.map { it.second }.average()
                }
                StatsPoint(
                    label = month.format(
                        DateTimeFormatter.ofPattern(
                            if (range == StatsRange.ALL_TIME) "MMM yy" else "MMM",
                            Locale.US
                        )
                    ),
                    value = value ?: 0.0,
                    valueText = value?.let(formatter) ?: "No data",
                    historyDate = rows.lastOrNull()?.first?.toString(),
                    hasData = value != null
                )
            }
        }
        return StatsChart(title, subtitle, type, points)
    }

    private fun countChart(
        title: String,
        subtitle: String,
        range: StatsRange,
        period: DatePeriod,
        dateTexts: List<String>
    ): StatsChart {
        val counts = dateTexts.groupingBy { it }.eachCount().mapValues { it.value.toDouble() }
        return numericChart(
            title = title,
            subtitle = subtitle,
            type = StatsChartType.BAR,
            range = range,
            period = period,
            valuesByDate = counts,
            monthlyMode = AggregateMode.SUM,
            formatter = { formatNumber(it, 0) },
            treatMissingAsZero = true
        )
    }

    private fun weeklyGoalCounts(
        period: DatePeriod,
        dateTexts: List<String>,
        goal: Int
    ): Pair<Int, Int> {
        var weekStart = period.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var reached = 0
        var weeks = 0
        while (!weekStart.isAfter(period.end)) {
            val weekEnd = weekStart.plusDays(6)
            val count = dateTexts.count { text ->
                val date = parseDate(text) ?: return@count false
                !date.isBefore(maxOf(weekStart, period.start)) &&
                    !date.isAfter(minOf(weekEnd, period.end))
            }
            if (count >= goal) reached++
            weeks++
            weekStart = weekStart.plusWeeks(1)
        }
        return reached to weeks.coerceAtLeast(1)
    }

    private fun weeklyGoalText(
        period: DatePeriod,
        dates: List<String>,
        goal: Int
    ): String {
        val (reached, weeks) = weeklyGoalCounts(period, dates, goal)
        return if (weeks == 1) {
            val count = dates.count { text ->
                parseDate(text)?.let { !it.isBefore(period.start) && !it.isAfter(period.end) } == true
            }
            "$count / $goal this week${if (count >= goal) " · goal reached" else ""}"
        } else {
            "$reached of $weeks weeks reached the $goal goal"
        }
    }

    private fun appointmentStatusDetail(appointments: List<CareAppointment>): String {
        if (appointments.isEmpty()) return "No appointments in this period"
        return appointments.groupingBy { it.status }.eachCount()
            .entries.sortedByDescending { it.value }
            .joinToString(" · ") { "${it.value} ${it.key.lowercase(Locale.US)}" }
    }

    private fun coverageText(count: Int, days: Int, label: String): String =
        "Based on $count of $days $label"

    private fun waterOunces(record: DailyRecord): Double =
        (record.plainReusableBottleCount + record.mioReusableBottleCount) * 24.0 +
            (record.plainDisposableBottleCount + record.mioDisposableBottleCount) * 16.9

    private fun dayCount(period: DatePeriod): Int =
        (ChronoUnit.DAYS.between(period.start, period.end) + 1L).toInt().coerceAtLeast(1)

    private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    private fun formatNumber(value: Double, decimals: Int): String =
        if (decimals == 0) {
            String.format(Locale.US, "%,.0f", value)
        } else {
            String.format(Locale.US, "%,.${decimals}f", value)
        }

    private fun dateSequence(start: LocalDate, end: LocalDate): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var date = start
        while (!date.isAfter(end)) {
            result += date
            date = date.plusDays(1)
        }
        return result
    }

    private fun monthSequence(start: YearMonth, end: YearMonth): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var month = start
        while (!month.isAfter(end)) {
            result += month
            month = month.plusMonths(1)
        }
        return result
    }

    private fun <T> List<T>.inPeriod(
        period: DatePeriod,
        dateSelector: (T) -> String
    ): List<T> = filter { row ->
        val date = parseDate(dateSelector(row)) ?: return@filter false
        !date.isBefore(period.start) && !date.isAfter(period.end)
    }

    private fun <T> List<T>.groupByDate(dateSelector: (T) -> String): Map<String, List<T>> =
        groupBy(dateSelector)

    companion object {
        fun factory(repositories: DailyRebuildRepositories): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(repositories) as T
                }
            }
        }
    }
}
