package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Values stored as text so backups remain readable and Room needs no converter. */
object FoodSourceType {
    const val PACKAGED = "PACKAGED"
    const val TAKEOUT = "TAKEOUT"
    const val DELIVERY = "DELIVERY"
    const val RESTAURANT = "RESTAURANT"
    const val MANUAL = "MANUAL"
    const val PROVIDED = "PROVIDED"

    val preparedTypes = setOf(TAKEOUT, DELIVERY, RESTAURANT, MANUAL, PROVIDED)

    fun isPrepared(value: String): Boolean = value in preparedTypes

    fun label(value: String): String = when (value) {
        TAKEOUT -> "Takeout"
        DELIVERY -> "Delivery"
        RESTAURANT -> "Restaurant"
        PROVIDED -> "Prepared by someone else"
        MANUAL -> "Unscannable food"
        else -> "Packaged food"
    }
}

object NutritionConfidence {
    const val EXACT = "EXACT"
    const val GOOD_ESTIMATE = "GOOD_ESTIMATE"
    const val ROUGH_ESTIMATE = "ROUGH_ESTIMATE"
    const val UNKNOWN = "UNKNOWN"

    fun label(value: String): String = when (value) {
        EXACT -> "Exact"
        GOOD_ESTIMATE -> "Good estimate"
        ROUGH_ESTIMATE -> "Rough estimate"
        UNKNOWN -> "Unknown"
        else -> "Estimate"
    }

    fun explanation(value: String): String = when (value) {
        EXACT -> "Copied from an official label, restaurant listing, or known recipe."
        GOOD_ESTIMATE -> "Built from measured ingredients or a close restaurant comparison."
        ROUGH_ESTIMATE -> "Based on visual portions or incomplete ingredient information."
        UNKNOWN -> "The meal is recorded, but nutrition is intentionally left unknown."
        else -> "Nutrition information is estimated."
    }
}

/**
 * A remaining portion from a prepared-food order.
 *
 * Nutrition is stored per one leftover unit so logging part of it later can
 * calculate totals without changing the original historical entry.
 */
@Entity(
    tableName = "prepared_food_leftovers",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["originEntryId"]),
        Index(value = ["originDate"]),
        Index(value = ["updatedAt"])
    ]
)
data class PreparedFoodLeftover(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long? = null,
    val originEntryId: Long? = null,
    val originDate: String,
    val sourceType: String = FoodSourceType.TAKEOUT,
    val sourceName: String = "",
    val foodName: String,
    val portionUnit: String = "portion",
    val remainingQuantity: Double,
    val caloriesPerUnit: Double = 0.0,
    val proteinGramsPerUnit: Double = 0.0,
    val carbohydrateGramsPerUnit: Double = 0.0,
    val fatGramsPerUnit: Double = 0.0,
    val sodiumMilligramsPerUnit: Double = 0.0,
    val nutritionConfidence: String = NutritionConfidence.ROUGH_ESTIMATE,
    val calorieEstimateLowPerUnit: Double? = null,
    val calorieEstimateHighPerUnit: Double? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun FoodProduct.isPreparedFood(): Boolean = FoodSourceType.isPrepared(sourceType)
fun FoodLogEntry.isPreparedFood(): Boolean = FoodSourceType.isPrepared(sourceTypeSnapshot)
