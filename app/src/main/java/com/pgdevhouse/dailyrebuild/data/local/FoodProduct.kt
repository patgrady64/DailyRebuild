package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_products",
    indices = [
        Index(
            value = ["barcode"],
            unique = true
        )
    ]
)
data class FoodProduct(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * UPC or EAN barcode.
     * Manual foods may not have one.
     */
    val barcode: String? = null,

    val name: String,

    val brand: String = "",

    /*
     * Nutrition for the label's stated serving.
     */
    val caloriesPerServing: Double = 0.0,

    val proteinGramsPerServing: Double = 0.0,

    val carbohydrateGramsPerServing: Double = 0.0,

    val fatGramsPerServing: Double = 0.0,

    val sodiumMilligramsPerServing: Double = 0.0,

    /*
     * Examples:
     *
     * 1 patty
     * 2 slices
     * 2 tablespoons
     * 56 grams
     */
    val servingQuantity: Double = 1.0,

    val servingUnit: String = "serving",

    /*
     * Optional package information.
     *
     * Examples:
     *
     * 12 patties
     * 8 hot dogs
     * 20 slices
     */
    val packageQuantity: Double? = null,

    val packageUnit: String? = null,

    val isFavorite: Boolean = false,

    /*
     * Condiments still contribute to nutrition totals, but they are
     * separated from regular foods for browsing, statistics, and the
     * daily Food anchor.
     */
    val isCondiment: Boolean = false,

    /*
     * Prepared foods reuse the normal food and nutrition pipeline, but retain
     * enough context to show where they came from and how trustworthy the
     * nutrition estimate is. Existing packaged foods keep the defaults.
     */
    val sourceType: String = FoodSourceType.PACKAGED,

    val sourceName: String = "",

    val nutritionConfidence: String = NutritionConfidence.EXACT,

    val calorieEstimateLow: Double? = null,

    val calorieEstimateHigh: Double? = null,

    val nutritionNotes: String = "",

    /* One-off takeout can be logged without cluttering Saved Foods. */
    val isReusable: Boolean = true,

    val createdAt: Long =
        System.currentTimeMillis(),

    val updatedAt: Long =
        System.currentTimeMillis()
)