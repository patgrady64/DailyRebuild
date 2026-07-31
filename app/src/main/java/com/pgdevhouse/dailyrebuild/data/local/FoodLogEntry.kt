package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodProduct::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["productId"])
    ]
)
data class FoodLogEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * ISO date such as 2026-07-24.
     */
    val date: String,

    val productId: Long,

    /*
     * Amount actually eaten.
     *
     * Examples:
     *
     * quantity = 4
     * unit = "patties"
     *
     * quantity = 2
     * unit = "slices"
     */
    val quantity: Double,

    val unit: String,

    /*
     * Optional grouping label.
     *
     * Examples:
     *
     * PBJ Sandwich
     * Four Hot Dogs
     * Dinner
     */
    val mealName: String? = null,
    val mealLogId: String? = null,

    /*
     * Saved-meal identity and accumulated quantity.
     *
     * These remain null / 1.0 for individual foods and older meal logs.
     * Re-adding the same saved meal on the same date updates the original
     * meal group instead of creating a second card.
     */
    val savedMealId: Long? = null,
    val mealQuantity: Double = 1.0,

    /*
     * Nutrition is saved as a snapshot.
     *
     * If the product information is corrected later,
     * old daily records keep their original values.
     */
    val productNameSnapshot: String,

    val calories: Double = 0.0,

    val proteinGrams: Double = 0.0,

    val carbohydrateGrams: Double = 0.0,

    val fatGrams: Double = 0.0,

    val sodiumMilligrams: Double = 0.0,

    val createdAt: Long =
        System.currentTimeMillis()
)