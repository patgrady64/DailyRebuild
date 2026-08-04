package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One timestamped drink. Snapshot fields preserve history if a shortcut changes. */
@Entity(
    tableName = "drink_entries",
    foreignKeys = [
        ForeignKey(
            entity = DrinkDefinition::class,
            parentColumns = ["id"],
            childColumns = ["definitionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["definitionId"]),
        Index(value = ["consumedAt"])
    ]
)
data class DrinkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val consumedAt: Long,
    val definitionId: Long? = null,
    val drinkNameSnapshot: String,
    val categorySnapshot: String = DrinkCategory.OTHER,
    val amountFlOz: Double,
    val calories: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val caffeineMilligrams: Double = 0.0,
    val countsAsWater: Boolean = false,
    val countsAsFood: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
