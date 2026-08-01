package com.pgdevhouse.dailyrebuild

import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.MedicationEntry
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * A data-quality warning is a neutral prompt to review an entry. It never
 * diagnoses a health problem or silently changes the user's data.
 */
enum class DataQualityWarningTarget {
    FOOD,
    SAVED_FOODS,
    WATER,
    APPOINTMENTS,
    HEALTH,
    MEETINGS,
    MOBILITY,
    HISTORY
}

data class DataQualityWarning(
    val id: String,
    val signature: String,
    val title: String,
    val summary: String,
    val details: String,
    val target: DataQualityWarningTarget,
    val date: String? = null,
    val priority: Int = 100
)

data class DataQualitySnapshot(
    val dailyRecords: List<DailyRecord> = emptyList(),
    val foodEntries: List<FoodLogEntry> = emptyList(),
    val foodProducts: List<FoodProduct> = emptyList(),
    val activitySnapshots: List<DailyActivitySnapshot> = emptyList(),
    val mobilitySessions: List<MobilitySession> = emptyList(),
    val showerLogs: List<ShowerLog> = emptyList(),
    val migraineLogs: List<MigraineLog> = emptyList(),
    val meetingAttendance: List<MeetingAttendance> = emptyList(),
    val careVisits: List<CareVisit> = emptyList(),
    val appointments: List<CareAppointment> = emptyList(),
    val healthMeasurements: List<HealthMeasurement> = emptyList(),
    val medications: List<MedicationEntry> = emptyList(),
    val lifeMaintenanceLogs: List<LifeMaintenanceLog> = emptyList()
)

object DataQualityWarningEngine {
    private val readableDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    fun build(
        snapshot: DataQualitySnapshot,
        today: LocalDate,
        ignoredSignatures: Set<String> = emptySet()
    ): List<DataQualityWarning> {
        val warnings = mutableListOf<DataQualityWarning>()

        addFutureHistoryWarnings(warnings, snapshot, today)
        addWaterWarnings(warnings, snapshot.dailyRecords)
        addFoodEntryWarnings(warnings, snapshot.foodEntries)
        addSavedFoodWarnings(warnings, snapshot.foodProducts)
        addDuplicateAppointmentWarnings(warnings, snapshot.appointments)
        addDuplicateMeetingWarnings(warnings, snapshot.meetingAttendance)
        addDuplicateMedicationWarnings(warnings, snapshot.medications)
        addHealthMeasurementWarnings(warnings, snapshot.healthMeasurements)
        addCareVisitMeasurementWarnings(warnings, snapshot.careVisits)
        addMobilityWarnings(warnings, snapshot.mobilitySessions)
        addMigraineWarnings(warnings, snapshot.migraineLogs)
        addActivityWarnings(warnings, snapshot.activitySnapshots)

        return warnings
            .asSequence()
            .filterNot { it.signature in ignoredSignatures }
            .distinctBy { it.signature }
            .sortedWith(
                compareBy<DataQualityWarning> { it.priority }
                    .thenByDescending { it.date.orEmpty() }
                    .thenBy { it.title.lowercase(Locale.US) }
            )
            .toList()
    }

    private fun addFutureHistoryWarnings(
        warnings: MutableList<DataQualityWarning>,
        snapshot: DataQualitySnapshot,
        today: LocalDate
    ) {
        val categoriesByDate = linkedMapOf<String, MutableSet<String>>()

        fun record(date: String, category: String) {
            val parsed = date.toLocalDateOrNull() ?: return
            if (parsed > today) {
                categoriesByDate.getOrPut(date) { linkedSetOf() } += category
            }
        }

        snapshot.dailyRecords.forEach { record(it.date, "daily record") }
        snapshot.foodEntries.forEach { record(it.date, "food") }
        snapshot.activitySnapshots.forEach { record(it.date, "activity") }
        snapshot.mobilitySessions.forEach { record(it.date, "mobility") }
        snapshot.showerLogs.forEach { record(it.date, "shower") }
        snapshot.migraineLogs.forEach { record(it.date, "migraine / aura") }
        snapshot.meetingAttendance.forEach { record(it.date, "meeting") }
        snapshot.careVisits.forEach { record(it.date, "care visit") }
        snapshot.healthMeasurements.forEach { record(it.recordedDate, "health measurement") }
        snapshot.lifeMaintenanceLogs.forEach { record(it.date, "life maintenance") }

        categoriesByDate.forEach { (date, categories) ->
            val signature = "future-history|$date|${categories.sorted().joinToString(",")}" 
            warnings += DataQualityWarning(
                id = signature,
                signature = signature,
                title = "History is dated in the future",
                summary = "${formatDate(date)} contains ${categories.joinToString()}.",
                details = "This may be intentional. Review the date so future entries do not appear in the wrong place.",
                target = DataQualityWarningTarget.HISTORY,
                date = date,
                priority = 10
            )
        }
    }

    private fun addWaterWarnings(
        warnings: MutableList<DataQualityWarning>,
        records: List<DailyRecord>
    ) {
        records.forEach { record ->
            val counts = listOf(
                record.plainReusableBottleCount,
                record.mioReusableBottleCount,
                record.plainDisposableBottleCount,
                record.mioDisposableBottleCount
            )
            val ounces =
                (record.plainReusableBottleCount + record.mioReusableBottleCount) * 24.0 +
                    (record.plainDisposableBottleCount + record.mioDisposableBottleCount) * 16.9

            if (counts.any { it < 0 }) {
                val signature = "water-negative|${record.date}|${counts.joinToString("|")}" 
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Water count needs review",
                    summary = "${formatDate(record.date)} contains a negative bottle count.",
                    details = "Bottle counts normally cannot be negative. Review the day's water entries.",
                    target = DataQualityWarningTarget.WATER,
                    date = record.date,
                    priority = 15
                )
            } else if (ounces > 300.0) {
                val signature = "water-large|${record.date}|${counts.joinToString("|")}" 
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Very large water total",
                    summary = "${formatDate(record.date)} has ${formatNumber(ounces)} fl oz recorded.",
                    details = "Daily Rebuild found an unusually large single-day total. It may be correct; review the bottle counts for accidental extra taps.",
                    target = DataQualityWarningTarget.WATER,
                    date = record.date,
                    priority = 25
                )
            }
        }
    }

    private fun addFoodEntryWarnings(
        warnings: MutableList<DataQualityWarning>,
        entries: List<FoodLogEntry>
    ) {
        entries.filter { it.mealLogId.isNullOrBlank() }.forEach { entry ->
            val quantityLimit = quantityReviewLimit(entry.unit)
            val reasons = buildList {
                if (!entry.quantity.isFinite() || entry.quantity <= 0.0) {
                    add("quantity is ${formatNumber(entry.quantity)}")
                } else if (entry.quantity > quantityLimit) {
                    add("quantity is ${formatNumber(entry.quantity)} ${entry.unit}")
                }
                if (!entry.calories.isFinite() || entry.calories < 0.0 || entry.calories > 5_000.0) {
                    add("calories are ${formatNumber(entry.calories)}")
                }
            }
            if (reasons.isNotEmpty()) {
                val signature = "food-entry|${entry.id}|${entry.quantity}|${entry.unit}|${entry.calories}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Food amount needs review",
                    summary = "${entry.productNameSnapshot} on ${formatDate(entry.date)}: ${reasons.joinToString()}.",
                    details = "The entry is outside broad input ranges used only to catch typing mistakes. Keeping it will not change the food log.",
                    target = DataQualityWarningTarget.FOOD,
                    date = entry.date,
                    priority = 30
                )
            }
        }

        entries
            .filter { !it.mealLogId.isNullOrBlank() }
            .groupBy { it.mealLogId.orEmpty() }
            .forEach { (mealLogId, mealEntries) ->
                val first = mealEntries.firstOrNull() ?: return@forEach
                val quantity = first.mealQuantity
                if (!quantity.isFinite() || quantity <= 0.0 || quantity > 20.0) {
                    val mealName = first.mealName?.takeIf(String::isNotBlank) ?: "Saved meal"
                    val signature = "meal-quantity|$mealLogId|$quantity"
                    warnings += DataQualityWarning(
                        id = signature,
                        signature = signature,
                        title = "Saved-meal quantity needs review",
                        summary = "$mealName on ${formatDate(first.date)} has quantity ${formatNumber(quantity)}.",
                        details = "Review the meal multiplier in case it was entered accidentally.",
                        target = DataQualityWarningTarget.FOOD,
                        date = first.date,
                        priority = 30
                    )
                }
            }
    }

    private fun addSavedFoodWarnings(
        warnings: MutableList<DataQualityWarning>,
        products: List<FoodProduct>
    ) {
        products.forEach { product ->
            val reasons = buildList {
                if (!product.servingQuantity.isFinite() || product.servingQuantity <= 0.0) {
                    add("serving quantity is ${formatNumber(product.servingQuantity)}")
                }
                if (!product.caloriesPerServing.isFinite() || product.caloriesPerServing < 0.0 || product.caloriesPerServing > 5_000.0) {
                    add("calories per serving are ${formatNumber(product.caloriesPerServing)}")
                }
                val packageQuantity = product.packageQuantity
                if (packageQuantity != null) {
                    if (!packageQuantity.isFinite() || packageQuantity <= 0.0) {
                        add("package quantity is ${formatNumber(packageQuantity)}")
                    } else if (
                        normalizeUnit(product.packageUnit) == normalizeUnit(product.servingUnit) &&
                        packageQuantity < product.servingQuantity
                    ) {
                        add("package quantity is smaller than one serving")
                    }
                }
            }

            if (reasons.isNotEmpty()) {
                val signature = listOf(
                    "saved-food",
                    product.id,
                    product.servingQuantity,
                    product.servingUnit,
                    product.packageQuantity,
                    product.packageUnit,
                    product.caloriesPerServing
                ).joinToString("|")
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Saved-food label needs review",
                    summary = "${product.name}: ${reasons.joinToString()}.",
                    details = "Review the serving and package fields. Daily Rebuild will not alter the saved food automatically.",
                    target = DataQualityWarningTarget.SAVED_FOODS,
                    priority = 35
                )
            }
        }
    }

    private fun addDuplicateAppointmentWarnings(
        warnings: MutableList<DataQualityWarning>,
        appointments: List<CareAppointment>
    ) {
        appointments
            .filterNot { it.status.equals("Cancelled", ignoreCase = true) }
            .groupBy {
                listOf(
                    it.date,
                    normalizeText(it.placeName),
                    normalizeText(it.providerName)
                ).joinToString("|")
            }
            .values
            .forEach { group ->
                val sorted = group.sortedBy { it.scheduledAt }
                sorted.forEachIndexed { index, first ->
                    for (secondIndex in index + 1..sorted.lastIndex) {
                        val second = sorted[secondIndex]
                        val difference = abs(second.scheduledAt - first.scheduledAt)
                        if (difference >= THIRTY_MINUTES_MS) break
                        val lowId = minOf(first.id, second.id)
                        val highId = maxOf(first.id, second.id)
                        val signature = "duplicate-appointment|$lowId|$highId|${first.scheduledAt}|${second.scheduledAt}"
                        warnings += DataQualityWarning(
                            id = signature,
                            signature = signature,
                            title = "Possible duplicate appointment",
                            summary = "Two ${first.visitCategory.ifBlank { "appointments" }} are close together on ${formatDate(first.date)}.",
                            details = "The place and provider match and the times are less than 30 minutes apart. Review them before deleting anything.",
                            target = DataQualityWarningTarget.APPOINTMENTS,
                            date = first.date,
                            priority = 20
                        )
                    }
                }
            }
    }

    private fun addDuplicateMeetingWarnings(
        warnings: MutableList<DataQualityWarning>,
        attendance: List<MeetingAttendance>
    ) {
        attendance
            .groupBy { "${it.date}|${normalizeText(it.meetingName)}" }
            .values
            .forEach { group ->
                val sorted = group.sortedBy { it.startedAt }
                sorted.forEachIndexed { index, first ->
                    for (secondIndex in index + 1..sorted.lastIndex) {
                        val second = sorted[secondIndex]
                        val difference = abs(second.startedAt - first.startedAt)
                        if (difference >= THIRTY_MINUTES_MS) break
                        val lowId = minOf(first.id, second.id)
                        val highId = maxOf(first.id, second.id)
                        val signature = "duplicate-meeting|$lowId|$highId|${first.startedAt}|${second.startedAt}"
                        warnings += DataQualityWarning(
                            id = signature,
                            signature = signature,
                            title = "Possible duplicate meeting",
                            summary = "${first.meetingName} appears twice close together on ${formatDate(first.date)}.",
                            details = "The names match and the start times are less than 30 minutes apart. Review the attendance history.",
                            target = DataQualityWarningTarget.MEETINGS,
                            date = first.date,
                            priority = 22
                        )
                    }
                }
            }

        attendance.forEach { meeting ->
            if (meeting.durationMinutes <= 0 || meeting.durationMinutes > 720) {
                val signature = "meeting-duration|${meeting.id}|${meeting.durationMinutes}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Meeting duration needs review",
                    summary = "${meeting.meetingName} on ${formatDate(meeting.date)} is ${meeting.durationMinutes} minutes.",
                    details = "Review the duration in case the number was typed incorrectly.",
                    target = DataQualityWarningTarget.MEETINGS,
                    date = meeting.date,
                    priority = 35
                )
            }
        }
    }

    private fun addDuplicateMedicationWarnings(
        warnings: MutableList<DataQualityWarning>,
        medications: List<MedicationEntry>
    ) {
        medications
            .filter { it.isActive }
            .groupBy {
                "${normalizeText(it.name)}|${it.milligrams?.let(::formatNumber).orEmpty()}"
            }
            .filterValues { it.size > 1 }
            .values
            .forEach { duplicates ->
                val first = duplicates.first()
                val ids = duplicates.map { it.id }.sorted().joinToString("|")
                val signature = "duplicate-medication|$ids|${first.name}|${first.milligrams}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Possible duplicate medication",
                    summary = "${first.name}${first.milligrams?.let { " ${formatNumber(it)} mg" }.orEmpty()} appears ${duplicates.size} times as active.",
                    details = "Different entries may be intentional. Review the medication list before changing anything.",
                    target = DataQualityWarningTarget.HEALTH,
                    priority = 25
                )
            }
    }

    private fun addHealthMeasurementWarnings(
        warnings: MutableList<DataQualityWarning>,
        measurements: List<HealthMeasurement>
    ) {
        measurements.forEach { measurement ->
            val reason = measurementReviewReason(measurement) ?: return@forEach
            val signature = listOf(
                "measurement",
                measurement.id,
                measurement.type,
                measurement.primaryValue,
                measurement.secondaryValue,
                measurement.tertiaryValue,
                measurement.quaternaryValue
            ).joinToString("|")
            warnings += DataQualityWarning(
                id = signature,
                signature = signature,
                title = "Health measurement needs review",
                summary = "${measurementLabel(measurement.type)} on ${formatDate(measurement.recordedDate)}: $reason.",
                details = "This check uses only broad entry limits to catch likely typing mistakes. It is not a medical interpretation.",
                target = DataQualityWarningTarget.HEALTH,
                date = measurement.recordedDate,
                priority = 18
            )
        }
    }

    private fun addCareVisitMeasurementWarnings(
        warnings: MutableList<DataQualityWarning>,
        visits: List<CareVisit>
    ) {
        visits.forEach { visit ->
            val reasons = buildList {
                visit.weightPounds?.let { if (!it.isFinite() || it <= 0.0 || it > 1_000.0) add("weight is ${formatNumber(it)} lb") }
                if (visit.systolic != null || visit.diastolic != null) {
                    val systolic = visit.systolic
                    val diastolic = visit.diastolic
                    if (
                        systolic == null || diastolic == null ||
                        systolic <= 0 || systolic > 350 ||
                        diastolic <= 0 || diastolic > 250 ||
                        systolic <= diastolic
                    ) {
                        add("blood pressure is ${systolic ?: "—"}/${diastolic ?: "—"}")
                    }
                }
                visit.a1c?.let { if (!it.isFinite() || it <= 0.0 || it > 30.0) add("A1C is ${formatNumber(it)}") }
            }
            if (reasons.isNotEmpty()) {
                val signature = "care-visit-measurement|${visit.id}|${reasons.joinToString("|")}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Care-visit measurement needs review",
                    summary = "${visit.placeName} on ${formatDate(visit.date)}: ${reasons.joinToString()}.",
                    details = "This is a broad data-entry check, not a medical interpretation. Review the saved visit values.",
                    target = DataQualityWarningTarget.HEALTH,
                    date = visit.date,
                    priority = 18
                )
            }
        }
    }

    private fun addMobilityWarnings(
        warnings: MutableList<DataQualityWarning>,
        sessions: List<MobilitySession>
    ) {
        sessions.forEach { session ->
            val reasons = buildList {
                if (session.elapsedSeconds <= 0) add("elapsed time is not positive")
                if (session.elapsedSeconds > 43_200) add("elapsed time exceeds 12 hours")
                if (session.movementSeconds < 0) add("movement time is negative")
                if (session.movementSeconds > session.elapsedSeconds + 60) {
                    add("movement time is longer than elapsed time")
                }
            }
            if (reasons.isNotEmpty()) {
                val signature = "mobility-time|${session.id}|${session.elapsedSeconds}|${session.movementSeconds}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Mobility duration needs review",
                    summary = "${session.routineName} on ${formatDate(session.date)}: ${reasons.joinToString()}.",
                    details = "Review the session duration and movement time for an accidental value.",
                    target = DataQualityWarningTarget.MOBILITY,
                    date = session.date,
                    priority = 32
                )
            }
        }
    }

    private fun addMigraineWarnings(
        warnings: MutableList<DataQualityWarning>,
        logs: List<MigraineLog>
    ) {
        logs.forEach { log ->
            val duration = log.auraDurationMinutes ?: return@forEach
            if (duration <= 0 || duration > 1_440) {
                val signature = "migraine-duration|${log.id}|$duration"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Aura duration needs review",
                    summary = "The event on ${formatDate(log.date)} has a duration of $duration minutes.",
                    details = "Review the duration for a possible typing mistake. This message does not interpret the event medically.",
                    target = DataQualityWarningTarget.HEALTH,
                    date = log.date,
                    priority = 32
                )
            }
        }
    }

    private fun addActivityWarnings(
        warnings: MutableList<DataQualityWarning>,
        snapshots: List<DailyActivitySnapshot>
    ) {
        snapshots.forEach { snapshot ->
            val reasons = buildList {
                if (snapshot.steps < 0L) add("steps are negative")
                if (!snapshot.distanceMiles.isFinite() || snapshot.distanceMiles < 0.0) add("distance is negative or invalid")
                if (snapshot.activityMinutes < 0L || snapshot.activityMinutes > 1_440L) add("activity minutes are outside one day")
            }
            if (reasons.isNotEmpty()) {
                val signature = "activity|${snapshot.date}|${snapshot.steps}|${snapshot.distanceMiles}|${snapshot.activityMinutes}"
                warnings += DataQualityWarning(
                    id = signature,
                    signature = signature,
                    title = "Activity snapshot needs review",
                    summary = "${formatDate(snapshot.date)}: ${reasons.joinToString()}.",
                    details = "This may reflect a source-data issue. Review the day before relying on the totals.",
                    target = DataQualityWarningTarget.HISTORY,
                    date = snapshot.date,
                    priority = 20
                )
            }
        }
    }

    private fun measurementReviewReason(measurement: HealthMeasurement): String? {
        return when (measurement.type) {
            HealthMeasurementType.WEIGHT -> {
                if (!measurement.primaryValue.isFinite() || measurement.primaryValue <= 0.0 || measurement.primaryValue > 1_000.0) {
                    "weight is ${formatNumber(measurement.primaryValue)} lb"
                } else null
            }

            HealthMeasurementType.A1C -> {
                if (!measurement.primaryValue.isFinite() || measurement.primaryValue <= 0.0 || measurement.primaryValue > 30.0) {
                    "A1C is ${formatNumber(measurement.primaryValue)}"
                } else null
            }

            HealthMeasurementType.BLOOD_PRESSURE -> {
                val systolic = measurement.primaryValue
                val diastolic = measurement.secondaryValue
                if (
                    !systolic.isFinite() || systolic <= 0.0 || systolic > 350.0 ||
                    diastolic == null || !diastolic.isFinite() || diastolic <= 0.0 || diastolic > 250.0 ||
                    systolic <= diastolic
                ) {
                    "blood pressure is ${formatNumber(systolic)}/${diastolic?.let(::formatNumber) ?: "—"}"
                } else null
            }

            HealthMeasurementType.CHOLESTEROL -> {
                val values = listOfNotNull(
                    measurement.primaryValue,
                    measurement.secondaryValue,
                    measurement.tertiaryValue,
                    measurement.quaternaryValue
                )
                if (values.any { !it.isFinite() || it < 0.0 || it > 2_000.0 }) {
                    "one or more cholesterol values are outside the broad entry range"
                } else null
            }

            else -> null
        }
    }

    private fun quantityReviewLimit(unit: String): Double {
        val normalized = normalizeUnit(unit)
        return when {
            normalized.contains("gram") || normalized == "g" ||
                normalized.contains("milliliter") || normalized == "ml" ||
                normalized.contains("ounce") || normalized == "oz" -> 1_000.0

            normalized.contains("serving") || normalized.contains("package") ||
                normalized.contains("bottle") || normalized.contains("can") ||
                normalized.contains("meal") -> 20.0

            normalized.contains("slice") || normalized.contains("piece") ||
                normalized.contains("patty") || normalized.contains("tablet") -> 50.0

            else -> 100.0
        }
    }

    private fun normalizeText(value: String): String =
        value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")

    private fun normalizeUnit(value: String?): String =
        value.orEmpty().trim().lowercase(Locale.US)
            .removeSuffix("s")
            .replace("fl oz", "oz")

    private fun measurementLabel(type: String): String = when (type) {
        HealthMeasurementType.WEIGHT -> "Weight"
        HealthMeasurementType.A1C -> "A1C"
        HealthMeasurementType.BLOOD_PRESSURE -> "Blood pressure"
        HealthMeasurementType.CHOLESTEROL -> "Cholesterol"
        else -> "Measurement"
    }

    private fun formatDate(value: String): String =
        value.toLocalDateOrNull()?.format(readableDateFormatter) ?: value

    private fun String.toLocalDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this) }.getOrNull()

    private fun formatNumber(value: Double): String = when {
        !value.isFinite() -> value.toString()
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> String.format(Locale.US, "%.1f", value)
    }

    private const val THIRTY_MINUTES_MS = 30L * 60L * 1_000L
}
