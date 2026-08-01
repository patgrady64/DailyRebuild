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

    val createdAt: Long =
        System.currentTimeMillis(),

    val updatedAt: Long =
        System.currentTimeMillis()
)