package com.pgdevhouse.dailyrebuild

import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceTasks
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog
import java.util.Locale

enum class TodayRepeatShortcutType {
    FOOD,
    MEAL
}

data class TodayRepeatShortcut(
    val key: String,
    val type: TodayRepeatShortcutType,
    val title: String,
    val detail: String,
    val lastUsedDate: String,
    val lastUsedAt: Long,
    val usageCount: Int,
    val defaultQuantity: Double,
    val quantityUnit: String,
    val sourceEntries: List<FoodLogEntry>
)

data class TodayShortcutCollection(
    val recent: List<TodayRepeatShortcut> = emptyList(),
    val frequent: List<TodayRepeatShortcut> = emptyList()
)

data class TodayActivityItem(
    val key: String,
    val category: String,
    val title: String,
    val detail: String,
    val occurredAt: Long
)

/**
 * Builds shortcut cards from the food history already stored by Daily Rebuild.
 * No extra usage-count table is needed: recent and frequent choices are derived
 * directly from the history so corrections and deletions are reflected.
 */
fun buildTodayRepeatShortcuts(
    entries: List<FoodLogEntry>,
    maximumPerSection: Int = 6
): TodayShortcutCollection {
    if (entries.isEmpty()) return TodayShortcutCollection()

    val individualCandidates = entries
        .filter { it.mealLogId.isNullOrBlank() }
        .map { entry ->
            val identity = individualFoodIdentity(entry)
            ShortcutCandidate(
                identity = identity,
                shortcut = TodayRepeatShortcut(
                    key = "food:$identity",
                    type = TodayRepeatShortcutType.FOOD,
                    title = entry.productNameSnapshot,
                    detail = "Last amount: ${formatCompactNumber(entry.quantity)} ${entry.unit}",
                    lastUsedDate = entry.date,
                    lastUsedAt = entry.createdAt,
                    usageCount = 1,
                    defaultQuantity = entry.quantity.coerceAtLeast(0.01),
                    quantityUnit = entry.unit,
                    sourceEntries = listOf(entry)
                )
            )
        }

    val mealCandidates = entries
        .filter { !it.mealLogId.isNullOrBlank() }
        .groupBy { "${it.date}|${it.mealLogId}" }
        .values
        .mapNotNull { group ->
            val first = group.firstOrNull() ?: return@mapNotNull null
            val mealQuantity = group.maxOfOrNull { it.mealQuantity }
                ?.takeIf { it > 0.0 }
                ?: 1.0
            val identity = mealIdentity(group)
            val occurredAt = group.maxOfOrNull { it.createdAt } ?: first.createdAt
            val mealName = first.mealName?.takeIf(String::isNotBlank) ?: "Saved meal"

            ShortcutCandidate(
                identity = identity,
                shortcut = TodayRepeatShortcut(
                    key = "meal:$identity",
                    type = TodayRepeatShortcutType.MEAL,
                    title = mealName,
                    detail = "Last amount: ${formatCompactNumber(mealQuantity)} ${mealQuantityLabel(mealQuantity)}",
                    lastUsedDate = first.date,
                    lastUsedAt = occurredAt,
                    usageCount = 1,
                    defaultQuantity = mealQuantity.coerceAtLeast(0.01),
                    quantityUnit = "meals",
                    sourceEntries = group.sortedBy { it.createdAt }
                )
            )
        }

    val candidates = individualCandidates + mealCandidates
    val grouped = candidates.groupBy { it.identity }

    val recent = grouped.values
        .mapNotNull { identityCandidates ->
            identityCandidates.maxWithOrNull(shortcutCandidateComparator)
                ?.shortcut
                ?.copy(usageCount = identityCandidates.size)
        }
        .sortedWith(shortcutComparator)
        .take(maximumPerSection)

    val frequent = grouped.values
        .mapNotNull { identityCandidates ->
            val latest = identityCandidates.maxWithOrNull(shortcutCandidateComparator)
                ?: return@mapNotNull null
            latest.shortcut.copy(usageCount = identityCandidates.size)
        }
        .sortedWith(
            compareByDescending<TodayRepeatShortcut> { it.usageCount }
                .thenByDescending { it.lastUsedDate }
                .thenByDescending { it.lastUsedAt }
                .thenBy { it.title.lowercase(Locale.US) }
        )
        .take(maximumPerSection)

    return TodayShortcutCollection(
        recent = recent,
        frequent = frequent
    )
}

fun buildTodayActivityItems(
    date: String,
    foodEntries: List<FoodLogEntry>,
    mobilitySessions: List<MobilitySession>,
    showerLog: ShowerLog?,
    maintenanceLogs: List<LifeMaintenanceLog>,
    meetingAttendance: List<MeetingAttendance>,
    maximumItems: Int = 12
): List<TodayActivityItem> {
    val items = mutableListOf<TodayActivityItem>()

    foodEntries
        .filter { it.mealLogId.isNullOrBlank() }
        .forEach { entry ->
            items += TodayActivityItem(
                key = "food:${entry.id}",
                category = "Food",
                title = entry.productNameSnapshot,
                detail = "${formatCompactNumber(entry.quantity)} ${entry.unit} · ${entry.calories.toInt()} calories",
                occurredAt = entry.createdAt
            )
        }

    foodEntries
        .filter { !it.mealLogId.isNullOrBlank() }
        .groupBy { it.mealLogId.orEmpty() }
        .forEach { (mealLogId, group) ->
            val first = group.firstOrNull() ?: return@forEach
            val quantity = group.maxOfOrNull { it.mealQuantity }
                ?.takeIf { it > 0.0 }
                ?: 1.0
            items += TodayActivityItem(
                key = "meal:$mealLogId",
                category = "Meal",
                title = first.mealName?.takeIf(String::isNotBlank) ?: "Saved meal",
                detail = "Quantity ${formatCompactNumber(quantity)} · ${group.sumOf { it.calories }.toInt()} calories",
                occurredAt = group.maxOfOrNull { it.createdAt } ?: first.createdAt
            )
        }

    mobilitySessions.forEach { session ->
        val minutes = (session.elapsedSeconds / 60.0).coerceAtLeast(0.0)
        items += TodayActivityItem(
            key = "mobility:${session.id}",
            category = "Mobility",
            title = session.routineName,
            detail = if (minutes < 1.0) {
                "Less than 1 minute"
            } else {
                "${formatCompactNumber(minutes)} minutes"
            },
            occurredAt = session.createdAt
        )
    }

    showerLog
        ?.takeIf { it.date == date }
        ?.let { log ->
            items += TodayActivityItem(
                key = "shower:${log.date}",
                category = "Personal care",
                title = "Shower",
                detail = "Completed today",
                occurredAt = log.completedAt
            )
        }

    maintenanceLogs
        .filter { it.date == date }
        .forEach { log ->
            items += TodayActivityItem(
                key = "maintenance:${log.taskKey}:${log.date}",
                category = "Maintenance",
                title = LifeMaintenanceTasks.labelFor(log.taskKey),
                detail = "Completed today",
                occurredAt = log.completedAt
            )
        }

    meetingAttendance
        .filter { it.date == date }
        .forEach { attendance ->
            items += TodayActivityItem(
                key = "meeting:${attendance.id}",
                category = "Meeting",
                title = attendance.meetingName,
                detail = "${attendance.role} · ${attendance.durationMinutes} minutes",
                occurredAt = attendance.startedAt
            )
        }

    return items
        .sortedByDescending { it.occurredAt }
        .take(maximumItems)
}

private data class ShortcutCandidate(
    val identity: String,
    val shortcut: TodayRepeatShortcut
)

private val shortcutCandidateComparator =
    compareBy<ShortcutCandidate> { it.shortcut.lastUsedDate }
        .thenBy { it.shortcut.lastUsedAt }

private val shortcutComparator =
    compareByDescending<TodayRepeatShortcut> { it.lastUsedDate }
        .thenByDescending { it.lastUsedAt }
        .thenBy { it.title.lowercase(Locale.US) }

private fun individualFoodIdentity(entry: FoodLogEntry): String = buildString {
    append(entry.productId)
    append('|')
    append(entry.productNameSnapshot.trim().lowercase(Locale.US))
    append('|')
    append(entry.unit.trim().lowercase(Locale.US))
    append('|')
    append(entry.mealName.orEmpty().trim().lowercase(Locale.US))
}

private fun mealIdentity(entries: List<FoodLogEntry>): String {
    val first = entries.first()
    first.savedMealId?.let { return "saved:$it" }

    val ingredients = entries
        .map {
            "${it.productId}:${it.unit.trim().lowercase(Locale.US)}"
        }
        .sorted()
        .joinToString(",")

    return "legacy:${first.mealName.orEmpty().trim().lowercase(Locale.US)}|$ingredients"
}

private fun mealQuantityLabel(quantity: Double): String =
    if (quantity == 1.0) "meal" else "meals"

internal fun formatCompactNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
}
