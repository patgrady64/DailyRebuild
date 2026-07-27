package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_profile")
data class HealthProfile(
    @PrimaryKey val id: Int = 1,
    val birthDate: String = "1981-06-04",
    val heightInches: Int = 71,
    val approximateStartingWeightPounds: Double? = 275.0,
    val weightGoalPounds: Double? = 175.0,
    val conditionsSummary: String =
        "Type 2 diabetes; high blood pressure; two prior heart attacks (2003 and 2005) with stents.",
    val foodConstraints: String =
        "Low budget; no refrigerator; microwave-only kitchen access.",
    val movementLimitations: String =
        "Walking and prolonged standing chores such as showering, dishes, sweeping, and mopping increase pain.",
    val mobilitySeatedDefault: Boolean = true,
    val mobilityBedDefault: Boolean = true,
    val mobilityFloorDefault: Boolean = false,
    val mobilityStandingDefault: Boolean = false,
    val currentCalorieGoal: Int? = null,
    val medicationImportCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
