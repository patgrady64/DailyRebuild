package com.pgdevhouse.dailyrebuild

import com.pgdevhouse.dailyrebuild.data.local.CalorieGoalChange
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.DrinkCategory
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.FoodSourceType
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.HealthProfile
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.IopMissedOccurrence
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import com.pgdevhouse.dailyrebuild.data.local.MedicationEntry
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.NutritionConfidence
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.PreparedFoodLeftover
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import com.pgdevhouse.dailyrebuild.data.preferences.DailyRebuildPreferences
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** One completed Monday-through-Sunday reporting window. */
data class WeeklyReportPeriod(
    val start: LocalDate,
    val end: LocalDate
) {
    init {
        require(!end.isBefore(start))
    }

    companion object {
        fun previousCompletedWeek(today: LocalDate = LocalDate.now()): WeeklyReportPeriod {
            val currentWeekMonday = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            val start = currentWeekMonday.minusWeeks(1)
            return WeeklyReportPeriod(start = start, end = start.plusDays(6))
        }
    }
}

data class WeeklyInsightsReport(
    val period: WeeklyReportPeriod,
    val fileName: String,
    val content: String
)

private data class WeeklyReportSnapshot(
    val profile: HealthProfile?,
    val medications: List<MedicationEntry>,
    val calorieGoalChanges: List<CalorieGoalChange>,
    val records: List<DailyRecord>,
    val foodEntries: List<FoodLogEntry>,
    val foodProducts: List<FoodProduct>,
    val drinks: List<DrinkEntry>,
    val activity: List<DailyActivitySnapshot>,
    val mobility: List<MobilitySession>,
    val showers: List<ShowerLog>,
    val migraines: List<MigraineLog>,
    val meetings: List<MeetingAttendance>,
    val savedMeetings: List<SavedMeeting>,
    val appointments: List<CareAppointment>,
    val visits: List<CareVisit>,
    val measurements: List<HealthMeasurement>,
    val maintenance: List<LifeMaintenanceLog>,
    val iopGroups: List<IopGroup>,
    val iopMisses: List<IopMissedOccurrence>,
    val pantry: List<PantryEssential>,
    val leftovers: List<PreparedFoodLeftover>
)

/**
 * Builds a private, upload-friendly weekly report from Daily Rebuild data.
 * The report intentionally excludes phone numbers and street addresses because
 * they do not improve weekly coaching and create unnecessary exposure.
 */
class WeeklyInsightsReportGenerator(
    private val repositories: DailyRebuildRepositories
) {
    suspend fun generate(
        preferences: DailyRebuildPreferences,
        visibleDataQualityWarnings: List<DataQualityWarning>,
        today: LocalDate = LocalDate.now(),
        generatedAt: Instant = Instant.now()
    ): WeeklyInsightsReport {
        val period = WeeklyReportPeriod.previousCompletedWeek(today)
        val snapshot = WeeklyReportSnapshot(
            profile = repositories.healthProfile.getProfile(),
            medications = repositories.healthProfile.getMedications(),
            calorieGoalChanges = repositories.healthProfile.getCalorieGoalChanges(),
            records = repositories.dailyRecords.getAllRecords(),
            foodEntries = repositories.food.getAllEntries(),
            foodProducts = repositories.food.getAllProducts(),
            drinks = repositories.drinks.getAllEntries(),
            activity = repositories.activity.getAllSnapshots(),
            mobility = repositories.mobility.getAllSessions(),
            showers = repositories.showers.getAllLogs(),
            migraines = repositories.migraines.getAllLogs(),
            meetings = repositories.meetings.getAllAttendance(),
            savedMeetings = repositories.meetings.getActiveMeetings(),
            appointments = repositories.appointments.getAllAppointments(),
            visits = repositories.careVisits.getAllVisits(),
            measurements = repositories.healthProfile.getAllMeasurements(),
            maintenance = repositories.lifeMaintenance.getAllLogs(),
            iopGroups = repositories.iopGroups.getAll(),
            iopMisses = repositories.iopAttendance.getAllMissed(),
            pantry = repositories.pantry.getAll(),
            leftovers = repositories.preparedFoods.getAll()
        )
        val content = WeeklyInsightsReportBuilder.build(
            period = period,
            snapshot = snapshot,
            preferences = preferences,
            warnings = visibleDataQualityWarnings,
            generatedAt = generatedAt
        )
        return WeeklyInsightsReport(
            period = period,
            fileName = "DailyRebuild_Weekly_Report_${period.start}_to_${period.end}.txt",
            content = content
        )
    }
}

private object WeeklyInsightsReportBuilder {
    private val readableDate = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    private val readableDateTime = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun build(
        period: WeeklyReportPeriod,
        snapshot: WeeklyReportSnapshot,
        preferences: DailyRebuildPreferences,
        warnings: List<DataQualityWarning>,
        generatedAt: Instant
    ): String {
        val prior = WeeklyReportPeriod(period.start.minusWeeks(1), period.end.minusWeeks(1))
        val planning = WeeklyReportPeriod(period.end.plusDays(1), period.end.plusDays(7))
        val current = PeriodData.from(snapshot, period)
        val previous = PeriodData.from(snapshot, prior)
        val productsById = snapshot.foodProducts.associateBy(FoodProduct::id)
        val zone = ZoneId.systemDefault()

        return buildString {
            appendLine("# Daily Rebuild Weekly Insights Report")
            appendLine()
            appendLine("Report version: 1")
            appendLine("Completed week: ${period.start.format(readableDate)} through ${period.end.format(readableDate)}")
            appendLine("Comparison week: ${prior.start.format(readableDate)} through ${prior.end.format(readableDate)}")
            appendLine("Next planning week: ${planning.start.format(readableDate)} through ${planning.end.format(readableDate)}")
            appendLine("Generated: ${generatedAt.atZone(zone).format(readableDateTime)}")
            appendLine("Time zone: ${zone.id}")
            appendLine()
            appendLine("This report is intended for private weekly review. A blank field means the app did not have that information; it does not necessarily mean the event did not happen.")
            appendLine("Nutrition marked as estimated is not exact. The report does not include street addresses or phone numbers.")

            appendProfileContext(snapshot, period)
            appendAppConfiguration(preferences)
            appendWeeklyScorecard(current, previous, snapshot, period, preferences)
            appendTrackingCoverage(current, period)
            appendNutrition(current, previous, snapshot, period, preferences, productsById)
            appendDrinks(current, previous, preferences)
            appendActivityAndPain(current, previous, period, preferences)
            appendMobility(current)
            appendRecovery(current, previous, snapshot, period)
            appendHealthEvents(current, snapshot, period, preferences)
            appendSelfCare(current)
            appendCare(current)
            appendDailyJournal(current, period)
            appendDayByDay(current, snapshot, period, preferences)
            appendDetailedFoodLogs(current.foodEntries, productsById)
            appendDetailedDrinkLogs(current.drinks, zone, preferences)
            appendPlanningContext(snapshot, planning, preferences)
            appendDataQuality(warnings, period)
            appendAppReviewSignals(current, period, productsById)
            appendLine()
            appendLine("# End of report")
        }
    }

    private fun StringBuilder.appendProfileContext(
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod
    ) {
        section("Personal context and active goals")
        val profile = snapshot.profile
        if (profile == null) {
            bullet("Health profile: not configured")
        } else {
            bullet("Current calorie goal: ${profile.currentCalorieGoal?.let { "$it calories/day" } ?: "not set"}")
            bullet("Approximate starting weight: ${profile.approximateStartingWeightPounds?.let { format(it, 1) + " lb" } ?: "not set"}")
            bullet("Weight goal: ${profile.weightGoalPounds?.let { format(it, 1) + " lb" } ?: "not set"}")
            bullet("Food constraints: ${profile.foodConstraints.ifBlank { "none recorded" }}")
            bullet("Movement limitations: ${profile.movementLimitations.ifBlank { "none recorded" }}")
            bullet("Health context recorded in the app: ${profile.conditionsSummary.ifBlank { "none recorded" }}")
        }

        val goalChanges = snapshot.calorieGoalChanges.filterDate(period) { it.changedDate }
        if (goalChanges.isNotEmpty()) {
            appendLine("- Calorie-goal changes during the week:")
            goalChanges.sortedBy { it.changedDate }.forEach { change ->
                appendLine("  - ${change.changedDate}: ${change.previousGoal ?: "not set"} -> ${change.newGoal}; reason: ${change.reason.ifBlank { "not recorded" }}")
            }
        }

        val activeMedications = snapshot.medications.filter(MedicationEntry::isActive)
        if (activeMedications.isEmpty()) {
            bullet("Active medication list: none recorded")
        } else {
            appendLine("- Active medication list (${activeMedications.size}):")
            activeMedications.sortedBy(MedicationEntry::sortOrder).forEach { medication ->
                val dose = buildList {
                    medication.milligrams?.let { add("${format(it)} mg") }
                    medication.numberPerDose?.let { add("${format(it)} per dose") }
                    medication.timesPerDay?.let { add("${format(it)} times/day") }
                }.joinToString(", ").ifBlank { "dose not recorded" }
                appendLine("  - ${medication.name}: $dose${medication.notes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
            }
        }
    }


    private fun StringBuilder.appendAppConfiguration(
        preferences: DailyRebuildPreferences
    ) {
        section("App configuration relevant to the review")
        bullet("Enabled Log sections: ${preferences.enabledLogSections.sorted().joinToString().ifBlank { "none" }}")
        bullet("Quick Log order: ${preferences.quickLogOrder.joinToString()}")
        bullet("Hidden Quick Log actions: ${preferences.hiddenQuickLogActions.sorted().joinToString().ifBlank { "none" }}")
        bullet("Visible Today sections: ${preferences.visibleTodaySections.sorted().joinToString()}")
        bullet("Hidden Insights sections: ${preferences.hiddenStatsSections.sorted().joinToString().ifBlank { "none" }}")
        bullet("Default Insights range: ${preferences.statsDefaultRange}")
        bullet("Notifications: master ${onOff(preferences.notificationsEnabled)}, appointments ${onOff(preferences.appointmentRemindersEnabled)}, meetings ${onOff(preferences.meetingRemindersEnabled)}, IOP reminders ${onOff(preferences.iopRemindersEnabled)}, IOP attendance follow-up ${onOff(preferences.iopAttendanceFollowUpEnabled)}")
        bullet("Reminder timing: meetings ${preferences.meetingReminderLeadMinutes} minutes early, IOP ${preferences.iopReminderLeadMinutes} minutes early, snooze ${preferences.notificationSnoozeMinutes} minutes")
    }

    private fun StringBuilder.appendWeeklyScorecard(
        current: PeriodData,
        previous: PeriodData,
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences
    ) {
        section("Weekly scorecard")
        val currentCalories = current.caloriesByDate.values
        val previousCalories = previous.caloriesByDate.values
        val currentWater = current.waterByDate.values
        val previousWater = previous.waterByDate.values
        val currentSteps = current.activity.associateBy(DailyActivitySnapshot::date)
        val previousSteps = previous.activity.associateBy(DailyActivitySnapshot::date)
        val currentPain = current.records.filter(DailyRecord::painRecorded).map(::highestPain)
        val previousPain = previous.records.filter(DailyRecord::painRecorded).map(::highestPain)
        val iopCurrent = iopSummary(snapshot, period)
        val iopPrevious = iopSummary(
            snapshot,
            WeeklyReportPeriod(period.start.minusWeeks(1), period.end.minusWeeks(1))
        )

        bullet(metricWithComparison("Days with food nutrition", currentCalories.size.toDouble(), previousCalories.size.toDouble(), " days", 0))
        bullet(metricWithComparison("Average calories on logged days", currentCalories.averageOrNull(), previousCalories.averageOrNull(), " kcal", 0))
        bullet(metricWithComparison("Average water on logged days", displayFluid(currentWater.averageOrNull(), preferences), displayFluid(previousWater.averageOrNull(), preferences), " ${fluidUnit(preferences)}", 1))
        bullet(metricWithComparison("Average daily steps", currentSteps.values.map { it.steps.toDouble() }.averageOrNull(), previousSteps.values.map { it.steps.toDouble() }.averageOrNull(), "", 0))
        bullet(metricWithComparison("Average recorded highest pain", currentPain.averageOrNull(), previousPain.averageOrNull(), "/10", 1, lowerIsBetter = true))
        bullet(metricWithComparison("Mobility sessions", current.mobility.size.toDouble(), previous.mobility.size.toDouble(), "", 0))
        bullet(metricWithComparison("Recovery meetings", current.meetings.size.toDouble(), previous.meetings.size.toDouble(), "", 0))
        bullet("IOP: ${iopCurrent.attended}/${iopCurrent.scheduled} attended${comparisonPhrase(iopCurrent.attended.toDouble(), iopPrevious.attended.toDouble(), lowerIsBetter = false)}")
        bullet(metricWithComparison("Showers", current.showers.size.toDouble(), previous.showers.size.toDouble(), "", 0))
        bullet(metricWithComparison("Migraine/aura events", current.migraines.size.toDouble(), previous.migraines.size.toDouble(), "", 0, lowerIsBetter = true))
    }

    private fun StringBuilder.appendTrackingCoverage(
        current: PeriodData,
        period: WeeklyReportPeriod
    ) {
        section("Tracking coverage and anchor completion")
        val dates = dates(period)
        val recordsByDate = current.records.associateBy(DailyRecord::date)
        val foodDates = current.foodEntries.map(FoodLogEntry::date).toSet()
        val drinkDates = current.drinks.map(DrinkEntry::date).toSet()
        val activityDates = current.activity.map(DailyActivitySnapshot::date).toSet()
        val journalDates = current.records.filter { it.journalText.isNotBlank() }.map(DailyRecord::date).toSet()

        bullet("Daily records present: ${current.records.size}/7")
        bullet("Food entries present: ${foodDates.size}/7 days")
        bullet("Drink entries present: ${drinkDates.size}/7 days")
        bullet("Activity snapshots present: ${activityDates.size}/7 days")
        bullet("Pain explicitly recorded: ${current.records.count(DailyRecord::painRecorded)}/7 days")
        bullet("Journal text present: ${journalDates.size}/7 days")
        bullet("Food anchor completed: ${current.records.count(DailyRecord::foodRecorded)}/7 days")
        bullet("Walk anchor completed: ${current.records.count(DailyRecord::walkCompleted)}/7 days")
        bullet("Mobility anchor completed: ${current.records.count(DailyRecord::mobilityCompleted)}/7 days")
        bullet("Pain anchor completed: ${current.records.count(DailyRecord::painRecorded)}/7 days")

        val fullyCompleted = dates.count { date ->
            recordsByDate[date.toString()]?.let { record ->
                record.foodRecorded && record.walkCompleted && record.mobilityCompleted && record.painRecorded
            } == true
        }
        bullet("All four daily anchors completed: $fullyCompleted/7 days")

        val completelyBlank = dates.filter { date ->
            val key = date.toString()
            recordsByDate[key] == null && key !in foodDates && key !in drinkDates &&
                key !in activityDates && current.mobility.none { it.date == key } &&
                current.meetings.none { it.date == key } && current.migraines.none { it.date == key }
        }
        bullet("Completely blank dates: ${completelyBlank.joinToString { it.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } }.ifBlank { "none" }}")
    }

    private fun StringBuilder.appendNutrition(
        current: PeriodData,
        previous: PeriodData,
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences,
        productsById: Map<Long, FoodProduct>
    ) {
        section("Food and nutrition")
        val foodLoggedDays = current.foodEntries.map(FoodLogEntry::date).toSet().size
        val nutritionDays = current.caloriesByDate.keys.size
        val totals = current.foodEntries.fold(NutritionTotals()) { acc, row -> acc + row }
        val drinkTotals = current.drinks.fold(DrinkNutritionTotals()) { acc, row -> acc + row }
        val combinedCalories = totals.calories + drinkTotals.calories
        val combinedProtein = totals.protein + drinkTotals.protein
        val combinedCarbs = totals.carbs + drinkTotals.carbs
        val dayDivisor = max(nutritionDays, 1)
        val exact = current.foodEntries.count { it.nutritionConfidenceSnapshot == NutritionConfidence.EXACT }
        val good = current.foodEntries.count { it.nutritionConfidenceSnapshot == NutritionConfidence.GOOD_ESTIMATE }
        val rough = current.foodEntries.count { it.nutritionConfidenceSnapshot == NutritionConfidence.ROUGH_ESTIMATE }
        val unknown = current.foodEntries.count { it.nutritionConfidenceSnapshot == NutritionConfidence.UNKNOWN }
        val prepared = current.foodEntries.count { FoodSourceType.isPrepared(it.sourceTypeSnapshot) }
        val condimentIds = productsById.values.filter(FoodProduct::isCondiment).map(FoodProduct::id).toSet()
        val condimentEntries = current.foodEntries.count { it.productId in condimentIds }

        bullet("Food log entries: ${current.foodEntries.size} across $foodLoggedDays day${plural(foodLoggedDays)}")
        bullet("Days with enough numeric nutrition to calculate totals: $nutritionDays")
        bullet("Total calories from food and caloric drinks: ${format(combinedCalories)} kcal")
        bullet("Average calories on logged days: ${format(combinedCalories / dayDivisor)} kcal")
        bullet("Average protein on logged days: ${format(combinedProtein / dayDivisor, 1)} g")
        bullet("Average carbohydrate on logged days: ${format(combinedCarbs / dayDivisor, 1)} g")
        bullet("Average fat from foods on logged days: ${format(totals.fat / dayDivisor, 1)} g")
        bullet("Average sodium from foods on logged days: ${format(totals.sodium / dayDivisor)} mg")
        bullet("Prepared/takeout entries: $prepared; condiment entries: $condimentEntries")
        bullet("Nutrition confidence: $exact exact, $good good estimate, $rough rough estimate, $unknown unknown")

        val goalsByDate = dates(period).associate { date ->
            date.toString() to calorieGoalForDate(snapshot, date)
        }
        val comparableDays = current.caloriesByDate.mapNotNull { (date, calories) ->
            goalsByDate[date]?.let { goal -> Triple(date, calories, goal) }
        }
        val withinTenPercent = comparableDays.count { (_, calories, goal) ->
            calories in goal * 0.9..goal * 1.1
        }
        if (comparableDays.isNotEmpty()) {
            bullet("Days within ±10% of the active calorie goal: $withinTenPercent/${comparableDays.size}")
        } else {
            bullet("Calorie-goal comparison: unavailable because no goal or no logged calories were present")
        }

        val highDay = current.caloriesByDate.maxByOrNull { it.value }
        val lowDay = current.caloriesByDate.minByOrNull { it.value }
        highDay?.let { bullet("Highest logged calorie day: ${it.key}, ${format(it.value)} kcal") }
        lowDay?.let { bullet("Lowest logged calorie day: ${it.key}, ${format(it.value)} kcal") }

        val frequentFoods = current.foodEntries
            .filterNot { it.productId in condimentIds }
            .groupingBy { it.productNameSnapshot.trim().ifBlank { "Unnamed food" } }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(8)
        appendLine("- Most frequently logged foods:")
        if (frequentFoods.isEmpty()) appendLine("  - none")
        frequentFoods.forEach { appendLine("  - ${it.key}: ${it.value} log${plural(it.value)}") }

        val estimatedRows = current.foodEntries.filter {
            it.nutritionConfidenceSnapshot != NutritionConfidence.EXACT ||
                it.calorieEstimateLow != null || it.calorieEstimateHigh != null
        }
        if (estimatedRows.isNotEmpty()) {
            appendLine("- Estimated or uncertain entries requiring context:")
            estimatedRows.forEach { row ->
                val range = estimateRange(row.calorieEstimateLow, row.calorieEstimateHigh)
                appendLine("  - ${row.date}: ${row.productNameSnapshot}; ${NutritionConfidence.label(row.nutritionConfidenceSnapshot)}; ${format(row.calories)} kcal$range${row.nutritionNotes.takeIf(String::isNotBlank)?.let { "; $it" }.orEmpty()}")
            }
        }

        val previousAverage = previous.caloriesByDate.values.averageOrNull()
        val currentAverage = current.caloriesByDate.values.averageOrNull()
        bullet("Calorie trend versus the prior week: ${comparisonSummary(currentAverage, previousAverage, "kcal/day", 0)}")
        bullet("Preferred display units: fluids ${fluidUnit(preferences)}, weight ${preferences.weightUnit}, distance ${preferences.distanceUnit}")
    }

    private fun StringBuilder.appendDrinks(
        current: PeriodData,
        previous: PeriodData,
        preferences: DailyRebuildPreferences
    ) {
        section("Drinks and hydration")
        val water = current.waterByDate.values.sum()
        val other = current.otherFluidByDate.values.sum()
        val total = water + other
        val loggedDays = current.fluidByDate.size
        val totals = current.drinks.fold(DrinkNutritionTotals()) { acc, row -> acc + row }

        bullet("Water: ${format(displayFluid(water, preferences), 1)} ${fluidUnit(preferences)}")
        bullet("Other drinks: ${format(displayFluid(other, preferences), 1)} ${fluidUnit(preferences)}")
        bullet("Total fluids: ${format(displayFluid(total, preferences), 1)} ${fluidUnit(preferences)}")
        bullet("Average total fluids on logged days: ${format(displayFluid(total / max(loggedDays, 1), preferences), 1)} ${fluidUnit(preferences)}")
        bullet("Drink calories: ${format(totals.calories)} kcal")
        bullet("Drink carbohydrates: ${format(totals.carbs, 1)} g; sugar: ${format(totals.sugar, 1)} g")
        bullet("Caffeine recorded: ${format(totals.caffeine)} mg")
        bullet("Nutrition/meal drinks that count as food: ${current.drinks.count(DrinkEntry::countsAsFood)}")

        val breakdown = current.drinks.groupBy { it.drinkNameSnapshot.trim().ifBlank { "Unnamed drink" } }
            .mapValues { (_, rows) -> rows.sumOf(DrinkEntry::amountFlOz) }
            .entries.sortedByDescending { it.value }
        appendLine("- Beverage breakdown:")
        if (breakdown.isEmpty()) appendLine("  - none")
        breakdown.forEach {
            appendLine("  - ${it.key}: ${format(displayFluid(it.value, preferences), 1)} ${fluidUnit(preferences)}")
        }

        bullet("Water trend versus prior week: ${comparisonSummary(displayFluid(current.waterByDate.values.averageOrNull(), preferences), displayFluid(previous.waterByDate.values.averageOrNull(), preferences), "${fluidUnit(preferences)}/logged day", 1)}")
    }

    private fun StringBuilder.appendActivityAndPain(
        current: PeriodData,
        previous: PeriodData,
        period: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences
    ) {
        section("Activity, walking, and pain")
        val activityByDate = current.activity.associateBy(DailyActivitySnapshot::date)
        val previousActivity = previous.activity
        val totalSteps = current.activity.sumOf(DailyActivitySnapshot::steps)
        val totalDistance = current.activity.sumOf(DailyActivitySnapshot::distanceMiles)
        val totalMinutes = current.activity.sumOf(DailyActivitySnapshot::activityMinutes)
        val painRows = current.records.filter(DailyRecord::painRecorded)
        val highestValues = painRows.map(::highestPain)

        bullet("Steps: ${format(totalSteps.toDouble())} total; ${format(totalSteps / 7.0)} average per calendar day")
        bullet("Distance: ${format(displayDistance(totalDistance, preferences), 2)} ${distanceUnit(preferences)}")
        bullet("Activity minutes: $totalMinutes")
        bullet("Walk anchor completed: ${current.records.count(DailyRecord::walkCompleted)}/7 days")
        bullet("Days with a 0-step snapshot: ${current.activity.count { it.steps == 0L }}")
        bullet("Step trend versus prior week: ${comparisonSummary(current.activity.map { it.steps.toDouble() }.averageOrNull(), previousActivity.map { it.steps.toDouble() }.averageOrNull(), "steps/snapshot day", 0)}")

        if (painRows.isEmpty()) {
            bullet("Pain: no explicitly recorded days")
        } else {
            bullet("Pain recorded: ${painRows.size}/7 days")
            bullet("Average highest pain: ${format(highestValues.average(), 1)}/10")
            val maxRecord = painRows.maxByOrNull(::highestPain)
            maxRecord?.let { bullet("Highest pain: ${format(highestPain(it), 1)}/10 on ${it.date} (back ${format(it.backPain.toDouble(), 1)}, shin ${format(it.shinPain.toDouble(), 1)})") }
            bullet("Average back pain: ${format(painRows.map { it.backPain.toDouble() }.average(), 1)}/10")
            bullet("Average shin pain: ${format(painRows.map { it.shinPain.toDouble() }.average(), 1)}/10")
        }

        appendLine("- Daily activity and pain pairing:")
        dates(period).forEach { date ->
            val key = date.toString()
            val activity = activityByDate[key]
            val record = current.records.firstOrNull { it.date == key }
            val steps = activity?.steps?.toString() ?: "not recorded"
            val pain = record?.takeIf(DailyRecord::painRecorded)?.let { "${format(highestPain(it), 1)}/10" } ?: "not recorded"
            appendLine("  - $key: $steps steps; highest pain $pain; walk anchor ${yesNo(record?.walkCompleted == true)}")
        }
    }

    private fun StringBuilder.appendMobility(current: PeriodData) {
        section("Mobility")
        bullet("Sessions: ${current.mobility.size}")
        bullet("Total elapsed time: ${format(current.mobility.sumOf { it.elapsedSeconds } / 60.0, 1)} minutes")
        bullet("Completed movement selections: ${current.mobility.sumOf { splitIds(it.completedMovementIds).size }}")
        bullet("Skipped movement selections: ${current.mobility.sumOf { splitIds(it.skippedMovementIds).size }}")
        val routines = current.mobility.groupingBy { it.routineName.ifBlank { "Unnamed routine" } }
            .eachCount().entries.sortedByDescending(Map.Entry<String, Int>::value)
        appendLine("- Routines used:")
        if (routines.isEmpty()) appendLine("  - none")
        routines.forEach { appendLine("  - ${it.key}: ${it.value} session${plural(it.value)}") }
        current.mobility.filter { it.notes.isNotBlank() }.forEach {
            appendLine("- ${it.date} mobility note: ${it.notes}")
        }
    }

    private fun StringBuilder.appendRecovery(
        current: PeriodData,
        previous: PeriodData,
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod
    ) {
        section("Recovery meetings and IOP")
        val meetingMinutes = current.meetings.sumOf(MeetingAttendance::durationMinutes)
        bullet("Recovery meetings attended: ${current.meetings.size}; total time ${format(meetingMinutes / 60.0, 1)} hours")
        bullet("Meeting trend versus prior week: ${comparisonSummary(current.meetings.size.toDouble(), previous.meetings.size.toDouble(), "meetings", 0)}")
        current.meetings.sortedBy(MeetingAttendance::startedAt).forEach { meeting ->
            appendLine("- ${meeting.date} ${formatTime(meeting.startedAt)}: ${meeting.meetingName}; ${meeting.durationMinutes} minutes; role ${meeting.role}${meeting.notes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
        }
        if (current.meetings.isEmpty()) bullet("Meeting details: none recorded")

        val iop = iopSummary(snapshot, period)
        bullet("IOP occurrences: ${iop.scheduled} scheduled, ${iop.attended} attended, ${iop.missed} missed")
        if (iop.missedRows.isNotEmpty()) {
            appendLine("- Missed IOP occurrences:")
            iop.missedRows.forEach { miss ->
                appendLine("  - ${miss.occurrenceDate}: ${miss.groupNameSnapshot}; reason: ${miss.reason.ifBlank { "not recorded" }}")
            }
        }
    }

    private fun StringBuilder.appendHealthEvents(
        current: PeriodData,
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences
    ) {
        section("Health measurements, migraine/aura, and medication check-ins")
        if (current.measurements.isEmpty()) {
            bullet("Health measurements during the week: none")
        } else {
            current.measurements.sortedWith(compareBy<HealthMeasurement> { it.recordedDate }.thenBy { it.createdAt }).forEach { row ->
                appendLine("- ${row.recordedDate}: ${formatMeasurement(row, preferences)}${row.notes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
            }
        }

        listOf(
            HealthMeasurementType.WEIGHT,
            HealthMeasurementType.A1C,
            HealthMeasurementType.BLOOD_PRESSURE,
            HealthMeasurementType.CHOLESTEROL
        ).forEach { type ->
            snapshot.measurements
                .filter { it.type == type && parseDate(it.recordedDate)?.isAfter(period.end) != true }
                .maxWithOrNull(compareBy<HealthMeasurement> { it.recordedDate }.thenBy { it.createdAt })
                ?.let { row -> bullet("Latest ${measurementLabel(type).lowercase(Locale.US)} on or before week end: ${formatMeasurement(row, preferences)} (${row.recordedDate})") }
        }

        bullet("Migraine/aura events: ${current.migraines.size}")
        current.migraines.sortedBy(MigraineLog::occurredAt).forEach { event ->
            val features = buildList {
                if (event.visualAura) add("visual aura")
                if (event.headPain) add("head pain")
                if (event.foggyAfterward) add("foggy afterward")
                event.auraDurationMinutes?.let { add("aura $it minutes") }
            }.joinToString(", ").ifBlank { "no symptom details" }
            appendLine("- ${event.date} ${formatTime(event.occurredAt)}: $features${event.notes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
        }

        val medicationDays = current.records
        if (medicationDays.isNotEmpty()) {
            val checks = listOf(
                "Morning aspirin" to medicationDays.count(DailyRecord::morningAspirinTaken),
                "Morning ibuprofen" to medicationDays.count(DailyRecord::morningIbuprofenTaken),
                "Morning naproxen" to medicationDays.count(DailyRecord::morningNaproxenTaken),
                "Morning acetaminophen" to medicationDays.count(DailyRecord::morningAcetaminophenTaken),
                "Night ibuprofen" to medicationDays.count(DailyRecord::nightIbuprofenTaken),
                "Night naproxen" to medicationDays.count(DailyRecord::nightNaproxenTaken),
                "Night acetaminophen" to medicationDays.count(DailyRecord::nightAcetaminophenTaken)
            )
            appendLine("- Recorded pain-reliever check-ins (out of ${medicationDays.size} daily records):")
            checks.forEach { (label, count) -> appendLine("  - $label: $count/${medicationDays.size} marked taken") }
        }
    }

    private fun StringBuilder.appendSelfCare(current: PeriodData) {
        section("Self-care and life maintenance")
        bullet("Showers: ${current.showers.size}; dates: ${current.showers.map(ShowerLog::date).sorted().joinToString().ifBlank { "none" }}")
        if (current.maintenance.isEmpty()) {
            bullet("Life-maintenance completions: none")
        } else {
            current.maintenance.sortedBy(LifeMaintenanceLog::date).forEach { log ->
                appendLine("- ${log.date}: ${LifeMaintenanceTasks.labelFor(log.taskKey)}")
            }
        }
    }

    private fun StringBuilder.appendCare(current: PeriodData) {
        section("Care appointments and completed visits")
        bullet("Appointments dated during the week: ${current.appointments.size}")
        current.appointments.sortedBy(CareAppointment::scheduledAt).forEach { appointment ->
            appendLine("- ${appointment.date} ${formatTime(appointment.scheduledAt)}: ${appointment.visitCategory}; ${appointment.placeName}${appointment.providerName.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()}; status ${appointment.status}; transportation ${appointment.transportationMode}, confirmed ${yesNo(appointment.transportationConfirmed)}; reason: ${appointment.reasonForAppointment.ifBlank { "not recorded" }}")
        }
        bullet("Completed care visits: ${current.visits.size}")
        current.visits.sortedBy(CareVisit::startedAt).forEach { visit ->
            appendLine("- ${visit.date} ${formatTime(visit.startedAt)}: ${visit.visitCategory}; ${visit.placeName}${visit.providerName.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()}; reason: ${visit.reasonForVisit}")
            listOf(
                "Summary" to visit.visitSummary,
                "Tests/procedures" to visit.testsProcedures,
                "Results" to visit.resultsDiscussed,
                "Instructions" to visit.instructions,
                "Medication changes" to visit.medicationChanges,
                "Referrals" to visit.referrals,
                "Notes" to visit.notes
            ).filter { it.second.isNotBlank() }.forEach { (label, value) ->
                appendLine("  - $label: $value")
            }
        }
    }

    private fun StringBuilder.appendDailyJournal(current: PeriodData, period: WeeklyReportPeriod) {
        section("Daily journal")
        val byDate = current.records.associateBy(DailyRecord::date)
        dates(period).forEach { date ->
            val text = byDate[date.toString()]?.journalText?.trim().orEmpty()
            appendLine("- ${date.format(readableDate)}: ${text.ifBlank { "no journal text" }}")
        }
    }

    private fun StringBuilder.appendDayByDay(
        current: PeriodData,
        snapshot: WeeklyReportSnapshot,
        period: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences
    ) {
        section("Day-by-day summary")
        val records = current.records.associateBy(DailyRecord::date)
        val activity = current.activity.associateBy(DailyActivitySnapshot::date)
        val mobility = current.mobility.groupBy(MobilitySession::date)
        val meetings = current.meetings.groupBy(MeetingAttendance::date)
        val migraines = current.migraines.groupBy(MigraineLog::date)
        val showers = current.showers.map(ShowerLog::date).toSet()
        val maintenance = current.maintenance.groupBy(LifeMaintenanceLog::date)

        dates(period).forEach { date ->
            val key = date.toString()
            val record = records[key]
            val goal = calorieGoalForDate(snapshot, date)
            appendLine()
            appendLine("## ${date.format(readableDate)}")
            val hasFood = current.foodEntries.any { it.date == key }
            val calorieText = current.caloriesByDate[key]?.let { format(it) + " kcal" }
                ?: if (hasFood) "nutrition unknown or recorded as zero" else "not logged"
            bullet("Calories: $calorieText${goal?.let { "; goal $it" }.orEmpty()}")
            bullet("Nutrition: protein ${current.proteinByDate[key]?.let { format(it, 1) + " g" } ?: "not logged"}; carbohydrates ${current.carbsByDate[key]?.let { format(it, 1) + " g" } ?: "not logged"}; fat ${current.fatByDate[key]?.let { format(it, 1) + " g" } ?: "not logged"}; sodium ${current.sodiumByDate[key]?.let { format(it) + " mg" } ?: "not logged"}")
            bullet("Fluids: water ${current.waterByDate[key]?.let { format(displayFluid(it, preferences), 1) + " " + fluidUnit(preferences) } ?: "not logged"}; other ${current.otherFluidByDate[key]?.let { format(displayFluid(it, preferences), 1) + " " + fluidUnit(preferences) } ?: "not logged"}")
            bullet("Activity: ${activity[key]?.steps ?: "not recorded"} steps; ${activity[key]?.activityMinutes ?: "not recorded"} active minutes")
            bullet("Pain: ${record?.takeIf(DailyRecord::painRecorded)?.let { "highest ${format(highestPain(it), 1)}/10 (back ${format(it.backPain.toDouble(), 1)}, shin ${format(it.shinPain.toDouble(), 1)})" } ?: "not recorded"}")
            bullet("Anchors: food ${yesNo(record?.foodRecorded == true)}, walk ${yesNo(record?.walkCompleted == true)}, mobility ${yesNo(record?.mobilityCompleted == true)}, pain ${yesNo(record?.painRecorded == true)}")
            bullet("Mobility sessions: ${mobility[key]?.size ?: 0}; recovery meetings: ${meetings[key]?.size ?: 0}; migraine/aura events: ${migraines[key]?.size ?: 0}; shower: ${yesNo(key in showers)}")
            bullet("Life maintenance: ${maintenance[key]?.joinToString { LifeMaintenanceTasks.labelFor(it.taskKey) }?.ifBlank { "none" } ?: "none"}")
        }
    }

    private fun StringBuilder.appendDetailedFoodLogs(
        entries: List<FoodLogEntry>,
        productsById: Map<Long, FoodProduct>
    ) {
        section("Detailed food logs")
        if (entries.isEmpty()) {
            bullet("No food entries were logged.")
            return
        }
        entries.groupBy(FoodLogEntry::date).toSortedMap().forEach { (date, rows) ->
            appendLine("- $date:")
            rows.sortedBy(FoodLogEntry::createdAt).forEach { row ->
                val product = productsById[row.productId]
                val context = buildList {
                    add("${format(row.quantity, 2)} ${row.unit}")
                    row.mealName?.takeIf(String::isNotBlank)?.let { add("meal: $it") }
                    if (FoodSourceType.isPrepared(row.sourceTypeSnapshot)) {
                        add(FoodSourceType.label(row.sourceTypeSnapshot))
                        row.sourceNameSnapshot.takeIf(String::isNotBlank)?.let { add("source: $it") }
                    }
                    if (product?.isCondiment == true) add("condiment")
                    add(NutritionConfidence.label(row.nutritionConfidenceSnapshot))
                }.joinToString("; ")
                appendLine("  - ${row.productNameSnapshot}: $context; ${format(row.calories)} kcal, protein ${format(row.proteinGrams, 1)} g, carbs ${format(row.carbohydrateGrams, 1)} g, fat ${format(row.fatGrams, 1)} g, sodium ${format(row.sodiumMilligrams)} mg${estimateRange(row.calorieEstimateLow, row.calorieEstimateHigh)}${row.nutritionNotes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
            }
        }
    }

    private fun StringBuilder.appendDetailedDrinkLogs(
        entries: List<DrinkEntry>,
        zone: ZoneId,
        preferences: DailyRebuildPreferences
    ) {
        section("Detailed drink logs")
        if (entries.isEmpty()) {
            bullet("No drink entries were logged.")
            return
        }
        entries.groupBy(DrinkEntry::date).toSortedMap().forEach { (date, rows) ->
            appendLine("- $date:")
            rows.sortedBy(DrinkEntry::consumedAt).forEach { row ->
                val time = Instant.ofEpochMilli(row.consumedAt).atZone(zone).format(timeFormatter)
                appendLine("  - $time ${row.drinkNameSnapshot}: ${format(displayFluid(row.amountFlOz, preferences), 1)} ${fluidUnit(preferences)}; category ${drinkCategoryLabel(row.categorySnapshot)}; water ${yesNo(row.countsAsWater)}; ${format(row.calories)} kcal; carbs ${format(row.carbohydrateGrams, 1)} g; sugar ${format(row.sugarGrams, 1)} g; protein ${format(row.proteinGrams, 1)} g; caffeine ${format(row.caffeineMilligrams)} mg${row.notes.takeIf(String::isNotBlank)?.let { "; notes: $it" }.orEmpty()}")
            }
        }
    }

    private fun StringBuilder.appendPlanningContext(
        snapshot: WeeklyReportSnapshot,
        planning: WeeklyReportPeriod,
        preferences: DailyRebuildPreferences
    ) {
        section("Next-week planning context")
        val appointments = snapshot.appointments.filterDate(planning) { it.date }
            .filterNot { it.status.equals("Cancelled", ignoreCase = true) }
        bullet("Upcoming care appointments: ${appointments.size}")
        appointments.sortedBy(CareAppointment::scheduledAt).forEach { appointment ->
            appendLine("- ${appointment.date} ${formatTime(appointment.scheduledAt)}: ${appointment.visitCategory}; ${appointment.placeName}${appointment.providerName.takeIf(String::isNotBlank)?.let { " with $it" }.orEmpty()}; transportation ${appointment.transportationMode}, confirmed ${yesNo(appointment.transportationConfirmed)}${appointment.leaveByAt?.let { "; leave by ${formatTime(it)}" }.orEmpty()}")
            listOf(
                "Reason" to appointment.reasonForAppointment,
                "Questions" to appointment.questionsToAsk,
                "Documents" to appointment.documentsToBring,
                "Preparation" to appointment.preparationNotes,
                "Transportation details" to appointment.transportationDetails
            ).filter { it.second.isNotBlank() }.forEach { (label, value) -> appendLine("  - $label: $value") }
        }

        val plannedIop = iopOccurrencesInRange(
            groups = snapshot.iopGroups.filter(IopGroup::active),
            startDate = planning.start,
            endDate = planning.end,
            nowDate = planning.end,
            nowTime = LocalTime.MAX,
            includeStartedToday = true
        )
        bullet("Planned IOP groups: ${plannedIop.size}")
        plannedIop.forEach { occurrence ->
            appendLine("- ${occurrence.date}: ${occurrence.group.name} ${minutesToClock(occurrence.group.startMinutes)}-${minutesToClock(occurrence.group.endMinutes)}${occurrence.group.location.takeIf(String::isNotBlank)?.let { "; ${it}" }.orEmpty()}")
        }

        val meetingSchedule = scheduledMeetings(snapshot.savedMeetings, planning)
        bullet("Reusable recovery meetings on the schedule: ${meetingSchedule.size}")
        meetingSchedule.forEach { (date, meeting) ->
            appendLine("- $date: ${meeting.name}${meeting.usualStartMinutes?.let { " at ${minutesToClock(it)}" }.orEmpty()}; typical duration ${meeting.typicalDurationMinutes} minutes")
        }

        val needed = snapshot.pantry.filter(PantryEssential::isNeeded)
        bullet("Pantry items marked Need: ${needed.size}")
        needed.sortedBy(PantryEssential::name).forEach { item ->
            appendLine("- ${item.name}${item.preferredProduct.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}${item.expectedPrice?.let { "; expected \$${format(it, 2)}" }.orEmpty()}${item.notes.takeIf(String::isNotBlank)?.let { "; $it" }.orEmpty()}")
        }

        val leftovers = snapshot.leftovers.filter { it.remainingQuantity > 0.0 }
        bullet("Prepared-food leftovers available: ${leftovers.size}")
        leftovers.sortedBy(PreparedFoodLeftover::updatedAt).forEach { leftover ->
            appendLine("- ${leftover.foodName}: ${format(leftover.remainingQuantity, 2)} ${leftover.portionUnit}; ${format(leftover.caloriesPerUnit * leftover.remainingQuantity)} estimated kcal remaining; ${NutritionConfidence.label(leftover.nutritionConfidence)}${leftover.notes.takeIf(String::isNotBlank)?.let { "; $it" }.orEmpty()}")
        }

        val profile = snapshot.profile
        bullet("Current calorie goal entering next week: ${profile?.currentCalorieGoal?.toString() ?: "not set"}")
        bullet("Current preferred units: fluid ${fluidUnit(preferences)}, distance ${distanceUnit(preferences)}, weight ${preferences.weightUnit}")
    }

    private fun StringBuilder.appendDataQuality(
        warnings: List<DataQualityWarning>,
        period: WeeklyReportPeriod
    ) {
        section("Data-quality checks")
        val relevant = warnings.filter { warning ->
            warning.date == null || parseDate(warning.date)?.let { !it.isBefore(period.start) && !it.isAfter(period.end) } == true
        }
        bullet("Visible review warnings relevant to this report or current saved definitions: ${relevant.size}")
        relevant.forEach { warning ->
            appendLine("- ${warning.title}: ${warning.summary} ${warning.details}")
        }
        if (relevant.isEmpty()) bullet("No visible data-quality warnings matched this report.")
    }

    private fun StringBuilder.appendAppReviewSignals(
        current: PeriodData,
        period: WeeklyReportPeriod,
        productsById: Map<Long, FoodProduct>
    ) {
        section("App-use signals to review together")
        val dates = dates(period).map(LocalDate::toString)
        val missingFood = dates.filterNot { it in current.foodEntries.map(FoodLogEntry::date).toSet() }
        val missingDrinks = dates.filterNot { it in current.drinks.map(DrinkEntry::date).toSet() }
        val missingPain = dates.filterNot { date -> current.records.any { it.date == date && it.painRecorded } }
        val unknownNutrition = current.foodEntries.filter { it.nutritionConfidenceSnapshot == NutritionConfidence.UNKNOWN }
        val oneOffPrepared = current.foodEntries.filter { row ->
            FoodSourceType.isPrepared(row.sourceTypeSnapshot) && productsById[row.productId]?.isReusable == false
        }
        val repeatedOneOff = oneOffPrepared.groupingBy { it.productNameSnapshot.trim().lowercase(Locale.US) }
            .eachCount().filterValues { it > 1 }

        bullet("Dates without food entries: ${missingFood.joinToString().ifBlank { "none" }}")
        bullet("Dates without drink entries: ${missingDrinks.joinToString().ifBlank { "none" }}")
        bullet("Dates without a pain entry: ${missingPain.joinToString().ifBlank { "none" }}")
        bullet("Foods with unknown nutrition: ${unknownNutrition.size}")
        bullet("Repeated one-off prepared foods that might deserve a reusable shortcut: ${repeatedOneOff.entries.joinToString { "${it.key} (${it.value})" }.ifBlank { "none" }}")
        bullet("Available evidence for app improvements: tracking gaps, repeated logs, uncertain nutrition, skipped mobility movements, data-quality warnings, and notes above.")
        appendLine()
        appendLine("When this file is uploaded for review, useful goals are: identify one or two realistic changes for the next week, avoid overreacting to one unusual day, distinguish missing data from actual behavior, and identify any app workflow that made honest logging difficult.")
    }

    private data class PeriodData(
        val records: List<DailyRecord>,
        val foodEntries: List<FoodLogEntry>,
        val drinks: List<DrinkEntry>,
        val activity: List<DailyActivitySnapshot>,
        val mobility: List<MobilitySession>,
        val showers: List<ShowerLog>,
        val migraines: List<MigraineLog>,
        val meetings: List<MeetingAttendance>,
        val appointments: List<CareAppointment>,
        val visits: List<CareVisit>,
        val measurements: List<HealthMeasurement>,
        val maintenance: List<LifeMaintenanceLog>,
        val caloriesByDate: Map<String, Double>,
        val proteinByDate: Map<String, Double>,
        val carbsByDate: Map<String, Double>,
        val fatByDate: Map<String, Double>,
        val sodiumByDate: Map<String, Double>,
        val waterByDate: Map<String, Double>,
        val otherFluidByDate: Map<String, Double>,
        val fluidByDate: Map<String, Double>
    ) {
        companion object {
            fun from(snapshot: WeeklyReportSnapshot, period: WeeklyReportPeriod): PeriodData {
                val records = snapshot.records.filterDate(period) { it.date }
                val foods = snapshot.foodEntries.filterDate(period) { it.date }
                val drinks = snapshot.drinks.filterDate(period) { it.date }
                val foodCalories = foods.groupBy(FoodLogEntry::date).mapValues { (_, rows) -> rows.sumOf(FoodLogEntry::calories) }
                val drinkCalories = drinks.groupBy(DrinkEntry::date).mapValues { (_, rows) -> rows.sumOf(DrinkEntry::calories) }
                val foodProtein = foods.groupBy(FoodLogEntry::date).mapValues { (_, rows) -> rows.sumOf(FoodLogEntry::proteinGrams) }
                val drinkProtein = drinks.groupBy(DrinkEntry::date).mapValues { (_, rows) -> rows.sumOf(DrinkEntry::proteinGrams) }
                val foodCarbs = foods.groupBy(FoodLogEntry::date).mapValues { (_, rows) -> rows.sumOf(FoodLogEntry::carbohydrateGrams) }
                val drinkCarbs = drinks.groupBy(DrinkEntry::date).mapValues { (_, rows) -> rows.sumOf(DrinkEntry::carbohydrateGrams) }
                val legacyWater = records.associate { it.date to legacyWaterOunces(it) }.filterValues { it > 0.0 }
                val water = addMaps(
                    drinks.groupBy(DrinkEntry::date).mapValues { (_, rows) -> rows.filter(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz) },
                    legacyWater
                )
                val other = drinks.groupBy(DrinkEntry::date).mapValues { (_, rows) -> rows.filterNot(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz) }
                return PeriodData(
                    records = records,
                    foodEntries = foods,
                    drinks = drinks,
                    activity = snapshot.activity.filterDate(period) { it.date },
                    mobility = snapshot.mobility.filterDate(period) { it.date },
                    showers = snapshot.showers.filterDate(period) { it.date },
                    migraines = snapshot.migraines.filterDate(period) { it.date },
                    meetings = snapshot.meetings.filterDate(period) { it.date },
                    appointments = snapshot.appointments.filterDate(period) { it.date },
                    visits = snapshot.visits.filterDate(period) { it.date },
                    measurements = snapshot.measurements.filterDate(period) { it.recordedDate },
                    maintenance = snapshot.maintenance.filterDate(period) { it.date },
                    caloriesByDate = addMaps(foodCalories, drinkCalories),
                    proteinByDate = addMaps(foodProtein, drinkProtein),
                    carbsByDate = addMaps(foodCarbs, drinkCarbs),
                    fatByDate = foods.groupBy(FoodLogEntry::date).mapValues { (_, rows) -> rows.sumOf(FoodLogEntry::fatGrams) },
                    sodiumByDate = foods.groupBy(FoodLogEntry::date).mapValues { (_, rows) -> rows.sumOf(FoodLogEntry::sodiumMilligrams) },
                    waterByDate = water,
                    otherFluidByDate = other,
                    fluidByDate = addMaps(water, other)
                )
            }
        }
    }

    private data class NutritionTotals(
        val calories: Double = 0.0,
        val protein: Double = 0.0,
        val carbs: Double = 0.0,
        val fat: Double = 0.0,
        val sodium: Double = 0.0
    ) {
        operator fun plus(row: FoodLogEntry) = NutritionTotals(
            calories + row.calories,
            protein + row.proteinGrams,
            carbs + row.carbohydrateGrams,
            fat + row.fatGrams,
            sodium + row.sodiumMilligrams
        )
    }

    private data class DrinkNutritionTotals(
        val calories: Double = 0.0,
        val protein: Double = 0.0,
        val carbs: Double = 0.0,
        val sugar: Double = 0.0,
        val caffeine: Double = 0.0
    ) {
        operator fun plus(row: DrinkEntry) = DrinkNutritionTotals(
            calories + row.calories,
            protein + row.proteinGrams,
            carbs + row.carbohydrateGrams,
            sugar + row.sugarGrams,
            caffeine + row.caffeineMilligrams
        )
    }

    private data class IopSummary(
        val scheduled: Int,
        val attended: Int,
        val missed: Int,
        val missedRows: List<IopMissedOccurrence>
    )

    private fun iopSummary(snapshot: WeeklyReportSnapshot, period: WeeklyReportPeriod): IopSummary {
        val occurrences = iopOccurrencesInRange(
            groups = snapshot.iopGroups,
            startDate = period.start,
            endDate = period.end,
            nowDate = period.end,
            nowTime = LocalTime.MAX,
            includeStartedToday = true
        )
        val misses = snapshot.iopMisses.filterDate(period) { it.occurrenceDate }
        val missedKeys = misses.map { "${it.groupId}|${it.occurrenceDate}" }.toSet()
        val occurrenceKeys = occurrences.map { "${it.group.id}|${it.date}" }.toSet()
        val attended = occurrences.count { "${it.group.id}|${it.date}" !in missedKeys }
        val orphanMisses = misses.count { "${it.groupId}|${it.occurrenceDate}" !in occurrenceKeys }
        return IopSummary(
            scheduled = occurrences.size + orphanMisses,
            attended = attended,
            missed = misses.size,
            missedRows = misses.sortedBy(IopMissedOccurrence::occurrenceDate)
        )
    }

    private fun calorieGoalForDate(snapshot: WeeklyReportSnapshot, date: LocalDate): Int? {
        val changes = snapshot.calorieGoalChanges.sortedWith(
            compareBy<CalorieGoalChange> { it.changedDate }.thenBy { it.createdAt }
        )
        return changes.lastOrNull { parseDate(it.changedDate)?.isAfter(date) == false }?.newGoal
            ?: changes.firstOrNull()?.previousGoal
            ?: snapshot.profile?.currentCalorieGoal
    }

    private fun scheduledMeetings(
        meetings: List<SavedMeeting>,
        period: WeeklyReportPeriod
    ): List<Pair<LocalDate, SavedMeeting>> = meetings.filter(SavedMeeting::active).mapNotNull { meeting ->
        val day = meeting.usualDayOfWeek ?: return@mapNotNull null
        val date = dates(period).firstOrNull { it.dayOfWeek.value == day } ?: return@mapNotNull null
        date to meeting
    }.sortedWith(compareBy<Pair<LocalDate, SavedMeeting>> { it.first }.thenBy { it.second.usualStartMinutes ?: Int.MAX_VALUE })

    private fun StringBuilder.section(title: String) {
        appendLine()
        appendLine("# $title")
    }

    private fun StringBuilder.bullet(value: String) {
        appendLine("- $value")
    }

    private fun metricWithComparison(
        label: String,
        current: Double?,
        previous: Double?,
        suffix: String,
        decimals: Int,
        lowerIsBetter: Boolean = false
    ): String {
        val value = current?.let { format(it, decimals) + suffix } ?: "not available"
        return "$label: $value${comparisonPhrase(current, previous, lowerIsBetter)}"
    }

    private fun comparisonPhrase(current: Double?, previous: Double?, lowerIsBetter: Boolean): String {
        if (current == null || previous == null) return ""
        val difference = current - previous
        if (abs(difference) < 0.0001) return " (unchanged from prior week)"
        val direction = if (difference > 0) "up" else "down"
        val outcome = when {
            lowerIsBetter && difference < 0 -> "; favorable direction"
            lowerIsBetter && difference > 0 -> "; review"
            else -> ""
        }
        return " ($direction ${format(abs(difference), 1)} from prior week$outcome)"
    }

    private fun comparisonSummary(current: Double?, previous: Double?, suffix: String, decimals: Int): String {
        if (current == null) return "current week unavailable"
        if (previous == null) return "${format(current, decimals)} $suffix; prior week unavailable"
        val change = current - previous
        val direction = when {
            abs(change) < 0.0001 -> "unchanged"
            change > 0 -> "up ${format(abs(change), decimals)}"
            else -> "down ${format(abs(change), decimals)}"
        }
        return "${format(current, decimals)} $suffix, $direction from ${format(previous, decimals)}"
    }

    private fun highestPain(record: DailyRecord): Double = max(record.backPain, record.shinPain).toDouble()

    private fun legacyWaterOunces(record: DailyRecord): Double =
        (record.plainReusableBottleCount + record.mioReusableBottleCount) * 24.0 +
            (record.plainDisposableBottleCount + record.mioDisposableBottleCount) * 16.9

    private fun addMaps(first: Map<String, Double>, second: Map<String, Double>): Map<String, Double> =
        (first.keys + second.keys).associateWith { key ->
            first.getOrDefault(key, 0.0) + second.getOrDefault(key, 0.0)
        }.filterValues { it != 0.0 }

    private fun <T> List<T>.filterDate(period: WeeklyReportPeriod, selector: (T) -> String): List<T> =
        filter { row ->
            parseDate(selector(row))?.let { date ->
                !date.isBefore(period.start) && !date.isAfter(period.end)
            } == true
        }

    private fun dates(period: WeeklyReportPeriod): List<LocalDate> = buildList {
        var date = period.start
        while (!date.isAfter(period.end)) {
            add(date)
            date = date.plusDays(1)
        }
    }

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private fun Collection<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun format(value: Double, decimals: Int = 0): String {
        if (!value.isFinite()) return value.toString()
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    private fun formatTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

    private fun formatMeasurement(row: HealthMeasurement, preferences: DailyRebuildPreferences): String = when (row.type) {
        HealthMeasurementType.WEIGHT -> {
            val value = if (preferences.weightUnit == "kg") row.primaryValue * 0.45359237 else row.primaryValue
            "Weight ${format(value, 1)} ${preferences.weightUnit}"
        }
        HealthMeasurementType.A1C -> "A1C ${format(row.primaryValue, 1)}%"
        HealthMeasurementType.BLOOD_PRESSURE -> "Blood pressure ${format(row.primaryValue)}/${row.secondaryValue?.let { format(it) } ?: "?"}"
        HealthMeasurementType.CHOLESTEROL -> "Cholesterol total ${format(row.primaryValue)}, LDL ${row.secondaryValue?.let { format(it) } ?: "?"}, HDL ${row.tertiaryValue?.let { format(it) } ?: "?"}, triglycerides ${row.quaternaryValue?.let { format(it) } ?: "?"} mg/dL"
        else -> "${row.type}: ${format(row.primaryValue, 1)}"
    }

    private fun measurementLabel(type: String): String = when (type) {
        HealthMeasurementType.WEIGHT -> "Weight"
        HealthMeasurementType.A1C -> "A1C"
        HealthMeasurementType.BLOOD_PRESSURE -> "Blood pressure"
        HealthMeasurementType.CHOLESTEROL -> "Cholesterol"
        else -> type
    }

    private fun displayFluid(ounces: Double?, preferences: DailyRebuildPreferences): Double? =
        ounces?.let { if (preferences.waterUnit == "ml") it * 29.5735 else it }

    private fun displayFluid(ounces: Double, preferences: DailyRebuildPreferences): Double =
        if (preferences.waterUnit == "ml") ounces * 29.5735 else ounces

    private fun fluidUnit(preferences: DailyRebuildPreferences): String =
        if (preferences.waterUnit == "ml") "mL" else "fl oz"

    private fun displayDistance(miles: Double, preferences: DailyRebuildPreferences): Double =
        if (preferences.distanceUnit == "km") miles * 1.609344 else miles

    private fun distanceUnit(preferences: DailyRebuildPreferences): String =
        if (preferences.distanceUnit == "km") "km" else "mi"

    private fun estimateRange(low: Double?, high: Double?): String = when {
        low != null && high != null -> "; likely range ${format(low)}-${format(high)} kcal"
        low != null -> "; estimated minimum ${format(low)} kcal"
        high != null -> "; estimated maximum ${format(high)} kcal"
        else -> ""
    }

    private fun drinkCategoryLabel(value: String): String = when (value) {
        DrinkCategory.WATER -> "Water"
        DrinkCategory.FLAVORED_WATER -> "Flavored water"
        DrinkCategory.COFFEE -> "Coffee"
        DrinkCategory.TEA -> "Tea"
        DrinkCategory.SODA -> "Soda"
        DrinkCategory.DIET_SODA -> "Diet soda"
        DrinkCategory.JUICE -> "Juice"
        DrinkCategory.MILK -> "Milk"
        DrinkCategory.PROTEIN_DRINK -> "Protein drink"
        DrinkCategory.SPORTS_DRINK -> "Sports drink"
        DrinkCategory.ENERGY_DRINK -> "Energy drink"
        else -> "Other"
    }

    private fun splitIds(value: String): List<String> = value.split('|').map(String::trim).filter(String::isNotBlank)

    private fun minutesToClock(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60).format(timeFormatter)

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

    private fun onOff(value: Boolean): String = if (value) "on" else "off"

    private fun plural(count: Int): String = if (count == 1) "" else "s"
}
