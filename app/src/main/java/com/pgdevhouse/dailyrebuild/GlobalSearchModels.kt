package com.pgdevhouse.dailyrebuild

import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CarePlace
import com.pgdevhouse.dailyrebuild.data.local.CareProvider
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.DrinkCategory
import com.pgdevhouse.dailyrebuild.data.local.DrinkDefinition
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.FoodSourceType
import com.pgdevhouse.dailyrebuild.data.local.NutritionConfidence
import com.pgdevhouse.dailyrebuild.data.local.isPreparedFood
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import com.pgdevhouse.dailyrebuild.data.local.MedicationEntry
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.PreparedFoodLeftover
import com.pgdevhouse.dailyrebuild.data.local.SavedMealWithIngredients
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** A fresh, read-only snapshot used by Global Search. */
data class GlobalSearchMobilityMovement(
    val id: String,
    val name: String,
    val primaryCategory: String,
    val categories: String,
    val positions: String,
    val instructions: String
)

data class GlobalSearchSnapshot(
    val products: List<FoodProduct> = emptyList(),
    val meals: List<SavedMealWithIngredients> = emptyList(),
    val foodEntries: List<FoodLogEntry> = emptyList(),
    val preparedFoodLeftovers: List<PreparedFoodLeftover> = emptyList(),
    val drinkDefinitions: List<DrinkDefinition> = emptyList(),
    val drinkEntries: List<DrinkEntry> = emptyList(),
    val dailyRecords: List<DailyRecord> = emptyList(),
    val carePlaces: List<CarePlace> = emptyList(),
    val careProviders: List<CareProvider> = emptyList(),
    val careVisits: List<CareVisit> = emptyList(),
    val careAppointments: List<CareAppointment> = emptyList(),
    val medications: List<MedicationEntry> = emptyList(),
    val measurements: List<HealthMeasurement> = emptyList(),
    val migraineLogs: List<MigraineLog> = emptyList(),
    val savedMeetings: List<SavedMeeting> = emptyList(),
    val meetingAttendance: List<MeetingAttendance> = emptyList(),
    val iopGroups: List<IopGroup> = emptyList(),
    val mobilitySessions: List<MobilitySession> = emptyList(),
    val mobilityMovements: List<GlobalSearchMobilityMovement> = emptyList(),
    val maintenanceLogs: List<LifeMaintenanceLog> = emptyList(),
    val pantryItems: List<PantryEssential> = emptyList(),
    val showerLogs: List<ShowerLog> = emptyList()
)

enum class GlobalSearchFilter(val label: String) {
    ALL("All"),
    FOOD("Food"),
    DATES("Dates"),
    CARE("Care"),
    HEALTH("Health"),
    MEETINGS("Meetings & IOP"),
    MOVEMENT("Movement"),
    MAINTENANCE("Maintenance"),
    PANTRY("Pantry")
}

sealed interface GlobalSearchTarget {
    data class SavedFood(val id: Long) : GlobalSearchTarget
    data class SavedMeal(val id: Long) : GlobalSearchTarget
    data class DrinkDefinitionTarget(val id: Long) : GlobalSearchTarget
    data class PreparedFoodLeftoverTarget(val id: Long) : GlobalSearchTarget
    data class HistoryDate(val date: String) : GlobalSearchTarget
    data class CarePlaceTarget(val id: Long) : GlobalSearchTarget
    data class CareProviderTarget(val id: Long) : GlobalSearchTarget
    data class CareVisitTarget(val id: Long) : GlobalSearchTarget
    data class AppointmentTarget(val id: Long) : GlobalSearchTarget
    data class MedicationTarget(val id: Long) : GlobalSearchTarget
    data class MeasurementTarget(val id: Long) : GlobalSearchTarget
    data class MigraineTarget(val id: Long) : GlobalSearchTarget
    data class SavedMeetingTarget(val id: Long) : GlobalSearchTarget
    data class MeetingAttendanceTarget(val id: Long) : GlobalSearchTarget
    data class IopGroupTarget(val id: Long) : GlobalSearchTarget
    data class MobilitySessionTarget(val id: Long) : GlobalSearchTarget
    data class MobilityMovementTarget(val id: String) : GlobalSearchTarget
    data class MaintenanceTarget(val taskKey: String) : GlobalSearchTarget
    data class PantryTarget(val id: Long) : GlobalSearchTarget
}

data class GlobalSearchResult(
    val key: String,
    val group: String,
    val filter: GlobalSearchFilter,
    val title: String,
    val subtitle: String,
    val searchableText: String,
    val target: GlobalSearchTarget,
    val newestFirst: Long = 0L
)

fun buildGlobalSearchResults(
    snapshot: GlobalSearchSnapshot,
    query: String,
    selectedFilter: GlobalSearchFilter
): List<GlobalSearchResult> {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return emptyList()

    val productById = snapshot.products.associateBy(FoodProduct::id)
    val results = mutableListOf<GlobalSearchResult>()

    snapshot.products.forEach { product ->
        results += GlobalSearchResult(
            key = "saved-food:${product.id}",
            group = when {
                product.isPreparedFood() -> "Frequent orders"
                product.isCondiment -> "Condiments"
                else -> "Saved foods"
            },
            filter = GlobalSearchFilter.FOOD,
            title = product.name,
            subtitle = listOf(
                product.sourceName.takeIf { product.isPreparedFood() }.orEmpty(),
                product.brand.takeUnless { product.isPreparedFood() }.orEmpty(),
                if (product.isPreparedFood()) FoodSourceType.label(product.sourceType) else "",
                if (product.isPreparedFood()) NutritionConfidence.label(product.nutritionConfidence) else "",
                if (product.isCondiment) "Condiment" else "",
                formatServing(product)
            ).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(
                product.name,
                product.brand,
                product.barcode.orEmpty(),
                product.servingUnit,
                product.sourceName,
                FoodSourceType.label(product.sourceType),
                NutritionConfidence.label(product.nutritionConfidence),
                if (product.isPreparedFood()) "takeout delivery restaurant prepared order" else "",
                if (product.isCondiment) "condiment sauce dressing topping" else "food"
            ).joinToString(" "),
            target = GlobalSearchTarget.SavedFood(product.id),
            newestFirst = product.updatedAt
        )
    }

    snapshot.meals.forEach { savedMeal ->
        val ingredientNames = savedMeal.ingredients.mapNotNull { ingredient ->
            productById[ingredient.productId]?.let { product ->
                buildString {
                    append(product.name)
                    if (ingredient.isOptional) append(" (optional)")
                    if (product.isCondiment) append(" condiment")
                }
            }
        }
        results += GlobalSearchResult(
            key = "saved-meal:${savedMeal.meal.id}",
            group = "Saved meals",
            filter = GlobalSearchFilter.FOOD,
            title = savedMeal.meal.name,
            subtitle = ingredientNames.joinToString(", ").ifBlank { "Saved meal" },
            searchableText = (listOf(savedMeal.meal.name) + ingredientNames).joinToString(" "),
            target = GlobalSearchTarget.SavedMeal(savedMeal.meal.id),
            newestFirst = savedMeal.meal.updatedAt
        )
    }

    val mealGroups = snapshot.foodEntries
        .filter { !it.mealLogId.isNullOrBlank() }
        .groupBy { it.mealLogId.orEmpty() }
    mealGroups.forEach { (mealLogId, entries) ->
        val first = entries.maxByOrNull(FoodLogEntry::createdAt) ?: return@forEach
        val ingredientNames = entries.map(FoodLogEntry::productNameSnapshot).distinct()
        results += GlobalSearchResult(
            key = "meal-history:$mealLogId",
            group = "Food history",
            filter = GlobalSearchFilter.FOOD,
            title = first.mealName ?: "Saved meal",
            subtitle = "${formatSearchDate(first.date)} · Quantity ${formatSearchNumber(first.mealQuantity)} · ${ingredientNames.joinToString(", ")}",
            searchableText = (listOf(first.mealName.orEmpty(), first.date, formatSearchDate(first.date)) + ingredientNames).joinToString(" "),
            target = GlobalSearchTarget.HistoryDate(first.date),
            newestFirst = dateSortValue(first.date, first.createdAt)
        )
    }
    snapshot.foodEntries
        .filter { it.mealLogId.isNullOrBlank() }
        .forEach { entry ->
            val isCondiment = productById[entry.productId]?.isCondiment == true
            results += GlobalSearchResult(
                key = "food-history:${entry.id}",
                group = if (isCondiment) "Condiment history" else "Food history",
                filter = GlobalSearchFilter.FOOD,
                title = entry.productNameSnapshot,
                subtitle = listOf(
                    formatSearchDate(entry.date),
                    "${formatSearchNumber(entry.quantity)} ${entry.unit}",
                    entry.sourceNameSnapshot,
                    if (entry.isPreparedFood()) NutritionConfidence.label(entry.nutritionConfidenceSnapshot) else "",
                    if (isCondiment) "Condiment" else ""
                ).filter(String::isNotBlank).joinToString(" · "),
                searchableText = listOf(
                    entry.productNameSnapshot,
                    entry.mealName.orEmpty(),
                    entry.date,
                    formatSearchDate(entry.date),
                    entry.unit,
                    entry.sourceNameSnapshot,
                    FoodSourceType.label(entry.sourceTypeSnapshot),
                    NutritionConfidence.label(entry.nutritionConfidenceSnapshot),
                    entry.nutritionNotes,
                    if (entry.isPreparedFood()) "takeout delivery restaurant prepared order" else "",
                    if (isCondiment) "condiment" else "food"
                ).joinToString(" "),
                target = GlobalSearchTarget.HistoryDate(entry.date),
                newestFirst = dateSortValue(entry.date, entry.createdAt)
            )
        }

    snapshot.preparedFoodLeftovers.forEach { leftover ->
        results += GlobalSearchResult(
            key = "prepared-leftover:${leftover.id}",
            group = "Available leftovers",
            filter = GlobalSearchFilter.FOOD,
            title = leftover.foodName,
            subtitle = listOf(
                leftover.sourceName,
                "${formatSearchNumber(leftover.remainingQuantity)} ${leftover.portionUnit} left",
                NutritionConfidence.label(leftover.nutritionConfidence),
                "from ${formatSearchDate(leftover.originDate)}"
            ).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(
                leftover.foodName,
                leftover.sourceName,
                FoodSourceType.label(leftover.sourceType),
                NutritionConfidence.label(leftover.nutritionConfidence),
                leftover.notes,
                leftover.originDate,
                formatSearchDate(leftover.originDate),
                "leftover takeout delivery restaurant prepared food"
            ).joinToString(" "),
            target = GlobalSearchTarget.PreparedFoodLeftoverTarget(leftover.id),
            newestFirst = leftover.updatedAt
        )
    }

    snapshot.drinkDefinitions.forEach { definition ->
        results += GlobalSearchResult(
            key = "drink-definition:${definition.id}",
            group = "Beverage library",
            filter = GlobalSearchFilter.FOOD,
            title = definition.name,
            subtitle = listOf(
                DrinkCategory.label(definition.category),
                definition.containerName,
                "${formatSearchNumber(definition.defaultAmountFlOz)} fl oz",
                if (definition.isFavorite) "Favorite" else "",
                if (!definition.isActive) "Hidden" else ""
            ).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(
                definition.name,
                DrinkCategory.label(definition.category),
                definition.containerName,
                definition.notes,
                "drink beverage hydration water coffee tea soda juice milk protein sports energy"
            ).joinToString(" "),
            target = GlobalSearchTarget.DrinkDefinitionTarget(definition.id),
            newestFirst = definition.updatedAt
        )
    }

    snapshot.drinkEntries.forEach { entry ->
        results += GlobalSearchResult(
            key = "drink-entry:${entry.id}",
            group = "Drink history",
            filter = GlobalSearchFilter.FOOD,
            title = entry.drinkNameSnapshot,
            subtitle = listOf(
                formatSearchDate(entry.date),
                "${formatSearchNumber(entry.amountFlOz)} fl oz",
                if (entry.countsAsWater) "Water" else "Other drink",
                entry.notes
            ).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(
                entry.drinkNameSnapshot,
                DrinkCategory.label(entry.categorySnapshot),
                entry.date,
                formatSearchDate(entry.date),
                entry.notes,
                "drink beverage hydration"
            ).joinToString(" "),
            target = GlobalSearchTarget.HistoryDate(entry.date),
            newestFirst = dateSortValue(entry.date, entry.consumedAt)
        )
    }

    buildDateResults(snapshot).forEach(results::add)

    snapshot.carePlaces.forEach { place ->
        results += GlobalSearchResult(
            key = "care-place:${place.id}",
            group = "Care places",
            filter = GlobalSearchFilter.CARE,
            title = place.name,
            subtitle = listOf(place.placeCategory, place.city, place.state).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(place.name, place.placeCategory, place.address, place.city, place.state, place.zipCode, place.phone, place.notes).joinToString(" "),
            target = GlobalSearchTarget.CarePlaceTarget(place.id),
            newestFirst = place.updatedAt
        )
    }
    snapshot.careProviders.forEach { provider ->
        val placeName = snapshot.carePlaces.firstOrNull { it.id == provider.placeId }?.name.orEmpty()
        results += GlobalSearchResult(
            key = "care-provider:${provider.id}",
            group = "Care providers",
            filter = GlobalSearchFilter.CARE,
            title = provider.name,
            subtitle = listOf(provider.credentials, provider.specialty, placeName).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(provider.name, provider.credentials, provider.specialty, provider.phone, provider.notes, placeName).joinToString(" "),
            target = GlobalSearchTarget.CareProviderTarget(provider.id),
            newestFirst = provider.updatedAt
        )
    }
    snapshot.careAppointments.forEach { appointment ->
        results += GlobalSearchResult(
            key = "appointment:${appointment.id}",
            group = "Appointments",
            filter = GlobalSearchFilter.CARE,
            title = appointment.placeName.ifBlank { appointment.visitCategory },
            subtitle = listOf(formatSearchDate(appointment.date), appointment.providerName, appointment.reasonForAppointment, appointment.status).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(appointment.placeName, appointment.providerName, appointment.visitCategory, appointment.reasonForAppointment, appointment.date, formatSearchDate(appointment.date), appointment.status, appointment.preparationNotes).joinToString(" "),
            target = GlobalSearchTarget.AppointmentTarget(appointment.id),
            newestFirst = dateSortValue(appointment.date, appointment.scheduledAt)
        )
    }
    snapshot.careVisits.forEach { visit ->
        results += GlobalSearchResult(
            key = "care-visit:${visit.id}",
            group = "Care visits",
            filter = GlobalSearchFilter.CARE,
            title = visit.placeName.ifBlank { visit.visitCategory },
            subtitle = listOf(formatSearchDate(visit.date), visit.providerName, visit.reasonForVisit).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(visit.placeName, visit.providerName, visit.visitCategory, visit.reasonForVisit, visit.visitSummary, visit.notes, visit.date, formatSearchDate(visit.date)).joinToString(" "),
            target = GlobalSearchTarget.CareVisitTarget(visit.id),
            newestFirst = dateSortValue(visit.date, visit.startedAt)
        )
    }

    snapshot.medications.forEach { medication ->
        results += GlobalSearchResult(
            key = "medication:${medication.id}",
            group = "Medications",
            filter = GlobalSearchFilter.HEALTH,
            title = medication.name,
            subtitle = medicationDoseSummary(medication),
            searchableText = listOf(medication.name, medication.purchaseSource, medication.notes, medicationDoseSummary(medication)).joinToString(" "),
            target = GlobalSearchTarget.MedicationTarget(medication.id)
        )
    }
    snapshot.measurements.forEach { measurement ->
        results += GlobalSearchResult(
            key = "measurement:${measurement.id}",
            group = "Health measurements",
            filter = GlobalSearchFilter.HEALTH,
            title = healthMeasurementLabel(measurement),
            subtitle = "${formatSearchDate(measurement.recordedDate)} · ${healthMeasurementValue(measurement)}",
            searchableText = listOf(healthMeasurementLabel(measurement), healthMeasurementValue(measurement), measurement.notes, measurement.recordedDate, formatSearchDate(measurement.recordedDate)).joinToString(" "),
            target = GlobalSearchTarget.MeasurementTarget(measurement.id),
            newestFirst = dateSortValue(measurement.recordedDate, measurement.createdAt)
        )
    }
    snapshot.migraineLogs.forEach { log ->
        val eventName = if (log.visualAura && !log.headPain) "Visual aura" else "Migraine"
        results += GlobalSearchResult(
            key = "migraine:${log.id}",
            group = "Migraine & aura",
            filter = GlobalSearchFilter.HEALTH,
            title = eventName,
            subtitle = listOf(formatSearchDate(log.date), log.auraDurationMinutes?.let { "$it minutes" }.orEmpty(), log.notes).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(eventName, "migraine aura visual head pain foggy", log.notes, log.date, formatSearchDate(log.date)).joinToString(" "),
            target = GlobalSearchTarget.MigraineTarget(log.id),
            newestFirst = dateSortValue(log.date, log.occurredAt)
        )
    }

    snapshot.savedMeetings.forEach { meeting ->
        results += GlobalSearchResult(
            key = "saved-meeting:${meeting.id}",
            group = "Saved meetings",
            filter = GlobalSearchFilter.MEETINGS,
            title = meeting.name,
            subtitle = listOf(meeting.city, meeting.state, meeting.notes).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(meeting.name, meeting.address, meeting.city, meeting.state, meeting.zipCode, meeting.notes).joinToString(" "),
            target = GlobalSearchTarget.SavedMeetingTarget(meeting.id),
            newestFirst = meeting.updatedAt
        )
    }
    snapshot.meetingAttendance.forEach { attendance ->
        results += GlobalSearchResult(
            key = "meeting-attendance:${attendance.id}",
            group = "Meeting history",
            filter = GlobalSearchFilter.MEETINGS,
            title = attendance.meetingName,
            subtitle = listOf(formatSearchDate(attendance.date), attendance.role, attendance.notes).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(attendance.meetingName, attendance.address, attendance.city, attendance.state, attendance.role, attendance.notes, attendance.date, formatSearchDate(attendance.date)).joinToString(" "),
            target = GlobalSearchTarget.MeetingAttendanceTarget(attendance.id),
            newestFirst = dateSortValue(attendance.date, attendance.startedAt)
        )
    }
    snapshot.iopGroups.forEach { group ->
        results += GlobalSearchResult(
            key = "iop:${group.id}",
            group = "IOP groups",
            filter = GlobalSearchFilter.MEETINGS,
            title = group.name,
            subtitle = "${dayOfWeekLabel(group.dayOfWeek)} · ${minutesToTimeLabel(group.startMinutes)}–${minutesToTimeLabel(group.endMinutes)}${group.location.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}",
            searchableText = listOf(group.name, "IOP group", dayOfWeekLabel(group.dayOfWeek), group.location, group.notes).joinToString(" "),
            target = GlobalSearchTarget.IopGroupTarget(group.id),
            newestFirst = group.updatedAt
        )
    }

    snapshot.mobilityMovements.forEach { movement ->
        results += GlobalSearchResult(
            key = "mobility-movement:${movement.id}",
            group = "Mobility movements",
            filter = GlobalSearchFilter.MOVEMENT,
            title = movement.name,
            subtitle = listOf(movement.primaryCategory, movement.positions).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(movement.name, movement.primaryCategory, movement.categories, movement.positions, movement.instructions).joinToString(" "),
            target = GlobalSearchTarget.MobilityMovementTarget(movement.id)
        )
    }
    snapshot.mobilitySessions.forEach { session ->
        results += GlobalSearchResult(
            key = "mobility-session:${session.id}",
            group = "Mobility history",
            filter = GlobalSearchFilter.MOVEMENT,
            title = session.routineName,
            subtitle = "${formatSearchDate(session.date)} · ${formatMobilitySearchDuration(session.elapsedSeconds)}${session.notes.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}",
            searchableText = listOf(session.routineName, session.notes, session.date, formatSearchDate(session.date), session.plannedMovementIds).joinToString(" "),
            target = GlobalSearchTarget.MobilitySessionTarget(session.id),
            newestFirst = dateSortValue(session.date, session.createdAt)
        )
    }

    LifeMaintenanceTasks.all.forEach { task ->
        val latest = snapshot.maintenanceLogs
            .filter { it.taskKey == task.key }
            .maxByOrNull { it.date }
        results += GlobalSearchResult(
            key = "maintenance:${task.key}",
            group = "Life Maintenance",
            filter = GlobalSearchFilter.MAINTENANCE,
            title = task.label,
            subtitle = latest?.let { "Last completed ${formatSearchDate(it.date)}" } ?: "No completion recorded yet",
            searchableText = listOf(task.label, task.key, latest?.date.orEmpty(), latest?.date?.let(::formatSearchDate).orEmpty()).joinToString(" "),
            target = GlobalSearchTarget.MaintenanceTarget(task.key),
            newestFirst = latest?.completedAt ?: 0L
        )
    }

    snapshot.pantryItems.forEach { item ->
        results += GlobalSearchResult(
            key = "pantry:${item.id}",
            group = "Pantry",
            filter = GlobalSearchFilter.PANTRY,
            title = item.name,
            subtitle = listOf(if (item.isNeeded) "Need" else "Have", item.category, item.preferredProduct, item.brandPreference).filter(String::isNotBlank).joinToString(" · "),
            searchableText = listOf(item.name, item.category, item.preferredProduct, item.brandPreference, item.notes, if (item.isNeeded) "need" else "have").joinToString(" "),
            target = GlobalSearchTarget.PantryTarget(item.id),
            newestFirst = item.updatedAt
        )
    }

    return results.asSequence()
        .filter { selectedFilter == GlobalSearchFilter.ALL || it.filter == selectedFilter }
        .mapNotNull { result ->
            searchScore(result, normalizedQuery)?.let { score -> score to result }
        }
        .sortedWith(
            compareBy<Pair<Int, GlobalSearchResult>> { it.first }
                .thenBy { groupPriority(it.second.group) }
                .thenByDescending { it.second.newestFirst }
                .thenBy { it.second.title.lowercase(Locale.US) }
        )
        .map { it.second }
        .take(250)
        .toList()
}


private fun groupPriority(group: String): Int = when (group) {
    "Saved foods" -> 0
    "Condiments" -> 1
    "Saved meals" -> 2
    "Food history" -> 3
    "Condiment history" -> 4
    "Dates" -> 5
    "Appointments" -> 6
    "Care visits" -> 7
    "Care places" -> 8
    "Care providers" -> 9
    "Medications" -> 10
    "Health measurements" -> 11
    "Migraine & aura" -> 12
    "IOP groups" -> 13
    "Saved meetings" -> 14
    "Meeting history" -> 15
    "Mobility movements" -> 16
    "Mobility history" -> 17
    "Life Maintenance" -> 18
    "Pantry" -> 19
    else -> 99
}

private fun buildDateResults(snapshot: GlobalSearchSnapshot): List<GlobalSearchResult> {
    val categoryByDate = linkedMapOf<String, MutableSet<String>>()
    fun mark(date: String, label: String) {
        if (date.isBlank()) return
        categoryByDate.getOrPut(date) { linkedSetOf() }.add(label)
    }
    snapshot.dailyRecords.forEach { mark(it.date, "Daily record") }
    snapshot.foodEntries.forEach { mark(it.date, "Food") }
    snapshot.drinkEntries.forEach { mark(it.date, "Drinks") }
    snapshot.mobilitySessions.forEach { mark(it.date, "Mobility") }
    snapshot.showerLogs.forEach { mark(it.date, "Shower") }
    snapshot.migraineLogs.forEach { mark(it.date, "Migraine/aura") }
    snapshot.meetingAttendance.forEach { mark(it.date, "Meeting") }
    snapshot.careVisits.forEach { mark(it.date, "Care visit") }
    snapshot.careAppointments.forEach { mark(it.date, "Appointment") }
    snapshot.maintenanceLogs.forEach { mark(it.date, "Maintenance") }

    return categoryByDate.map { (date, labels) ->
        GlobalSearchResult(
            key = "date:$date",
            group = "Dates",
            filter = GlobalSearchFilter.DATES,
            title = formatSearchDate(date),
            subtitle = labels.joinToString(" · "),
            searchableText = "${searchDateAliases(date)} ${labels.joinToString(" ")}",
            target = GlobalSearchTarget.HistoryDate(date),
            newestFirst = dateSortValue(date, 0L)
        )
    }
}

private fun searchScore(result: GlobalSearchResult, normalizedQuery: String): Int? {
    val title = normalizeSearchText(result.title)
    val subtitle = normalizeSearchText(result.subtitle)
    val searchable = normalizeSearchText(result.searchableText)
    val terms = normalizedQuery.split(' ').filter(String::isNotBlank)
    if (terms.any { it !in searchable && it !in title && it !in subtitle }) return null
    return when {
        title == normalizedQuery -> 0
        title.startsWith(normalizedQuery) -> 1
        title.contains(normalizedQuery) -> 2
        subtitle.contains(normalizedQuery) -> 3
        else -> 4
    }
}

private fun normalizeSearchText(value: String): String {
    val monthAliases = mapOf(
        "january" to "jan",
        "february" to "feb",
        "march" to "mar",
        "april" to "apr",
        "may" to "may",
        "june" to "jun",
        "july" to "jul",
        "august" to "aug",
        "september" to "sep",
        "october" to "oct",
        "november" to "nov",
        "december" to "dec"
    )

    return value
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { monthAliases[it] ?: it }
}

private fun formatServing(product: FoodProduct): String {
    return "${formatSearchNumber(product.servingQuantity)} ${product.servingUnit}"
}

private fun formatSearchNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

private fun formatSearchDate(date: String): String {
    return try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    } catch (_: DateTimeParseException) {
        date
    }
}


private fun searchDateAliases(date: String): String {
    return try {
        val parsed = LocalDate.parse(date)
        listOf(
            date,
            formatSearchDate(date),
            parsed.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)),
            "${parsed.monthValue}/${parsed.dayOfMonth}/${parsed.year}",
            "${parsed.monthValue}/${parsed.dayOfMonth}"
        ).joinToString(" ")
    } catch (_: DateTimeParseException) {
        date
    }
}

private fun dateSortValue(date: String, fallback: Long): Long {
    return try {
        LocalDate.parse(date).toEpochDay() * 86_400_000L + (fallback % 86_400_000L)
    } catch (_: DateTimeParseException) {
        fallback
    }
}

private fun medicationDoseSummary(medication: MedicationEntry): String {
    val parts = mutableListOf<String>()
    medication.milligrams?.let { parts += "${formatSearchNumber(it)} mg" }
    medication.numberPerDose?.let { parts += "${formatSearchNumber(it)} per dose" }
    medication.timesPerDay?.let { parts += "${formatSearchNumber(it)} times/day" }
    if (!medication.isActive) parts += "Inactive"
    return parts.joinToString(" · ").ifBlank { "Medication reference" }
}

private fun healthMeasurementLabel(measurement: HealthMeasurement): String = when (measurement.type) {
    HealthMeasurementType.WEIGHT -> "Weight"
    HealthMeasurementType.A1C -> "A1C"
    HealthMeasurementType.BLOOD_PRESSURE -> "Blood pressure"
    HealthMeasurementType.CHOLESTEROL -> "Cholesterol"
    else -> measurement.type.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun healthMeasurementValue(measurement: HealthMeasurement): String = when (measurement.type) {
    HealthMeasurementType.WEIGHT -> "${formatSearchNumber(measurement.primaryValue)} lb"
    HealthMeasurementType.A1C -> "${formatSearchNumber(measurement.primaryValue)}%"
    HealthMeasurementType.BLOOD_PRESSURE -> "${formatSearchNumber(measurement.primaryValue)}/${formatSearchNumber(measurement.secondaryValue ?: 0.0)}"
    HealthMeasurementType.CHOLESTEROL -> listOf(
        "Total ${formatSearchNumber(measurement.primaryValue)}",
        measurement.secondaryValue?.let { "LDL ${formatSearchNumber(it)}" },
        measurement.tertiaryValue?.let { "HDL ${formatSearchNumber(it)}" },
        measurement.quaternaryValue?.let { "Triglycerides ${formatSearchNumber(it)}" }
    ).filterNotNull().joinToString(" · ")
    else -> formatSearchNumber(measurement.primaryValue)
}

private fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Day $dayOfWeek"
}

private fun minutesToTimeLabel(minutes: Int): String {
    val hour24 = (minutes / 60).coerceIn(0, 23)
    val minute = minutes.mod(60)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val value = hour24 % 12) { 0 -> 12; else -> value }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
}

private fun formatMobilitySearchDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return when {
        minutes > 0 && remaining > 0 -> "$minutes min $remaining sec"
        minutes > 0 -> "$minutes min"
        else -> "$remaining sec"
    }
}
