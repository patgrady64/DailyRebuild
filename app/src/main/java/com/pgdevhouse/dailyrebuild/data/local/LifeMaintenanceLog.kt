package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * One completion of an occasional life-maintenance item on a local date.
 *
 * These records intentionally have no due date, cadence, inventory count, or
 * overdue state. They answer only: "When did I last do this?"
 */
@Entity(
    tableName = "life_maintenance_logs",
    primaryKeys = ["taskKey", "date"],
    indices = [Index(value = ["date"])]
)
data class LifeMaintenanceLog(
    val taskKey: String,
    val date: String,
    val completedAt: Long = System.currentTimeMillis()
)

data class LifeMaintenanceTask(
    val key: String,
    val label: String
)

object LifeMaintenanceTasks {
    const val LAUNDRY = "laundry"
    const val WASH_BEDDING = "wash_bedding"
    const val TRIM_NAILS = "trim_nails"
    const val HAIRCUT = "haircut"
    const val REPLACE_TOOTHBRUSH = "replace_toothbrush"

    val all = listOf(
        LifeMaintenanceTask(LAUNDRY, "Laundry"),
        LifeMaintenanceTask(WASH_BEDDING, "Wash bedding"),
        LifeMaintenanceTask(TRIM_NAILS, "Trim nails"),
        LifeMaintenanceTask(HAIRCUT, "Haircut"),
        LifeMaintenanceTask(REPLACE_TOOTHBRUSH, "Replace toothbrush")
    )

    fun labelFor(taskKey: String): String =
        all.firstOrNull { it.key == taskKey }?.label ?: taskKey
}
