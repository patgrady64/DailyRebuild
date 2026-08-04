package com.pgdevhouse.dailyrebuild.domain

import kotlin.math.max
import kotlin.math.min

/** One ingredient or comparison item used to reconstruct an unlabelled meal. */
data class PreparedFoodComponent(
    val name: String,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0
)

data class PreparedFoodNutrition(
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0
) {
    fun scaled(multiplier: Double): PreparedFoodNutrition {
        val safeMultiplier = multiplier.coerceAtLeast(0.0)
        return copy(
            calories = calories * safeMultiplier,
            proteinGrams = proteinGrams * safeMultiplier,
            carbohydrateGrams = carbohydrateGrams * safeMultiplier,
            fatGrams = fatGrams * safeMultiplier,
            sodiumMilligrams = sodiumMilligrams * safeMultiplier
        )
    }
}

fun combinePreparedFoodComponents(
    components: List<PreparedFoodComponent>
): PreparedFoodNutrition = PreparedFoodNutrition(
    calories = components.sumOf { it.calories.coerceAtLeast(0.0) },
    proteinGrams = components.sumOf { it.proteinGrams.coerceAtLeast(0.0) },
    carbohydrateGrams = components.sumOf { it.carbohydrateGrams.coerceAtLeast(0.0) },
    fatGrams = components.sumOf { it.fatGrams.coerceAtLeast(0.0) },
    sodiumMilligrams = components.sumOf { it.sodiumMilligrams.coerceAtLeast(0.0) }
)

/** Keeps a calorie range ordered and ensures it includes the chosen estimate. */
fun normalizedCalorieRange(
    estimate: Double,
    low: Double?,
    high: Double?
): Pair<Double?, Double?> {
    if (low == null && high == null) return null to null

    val positiveEstimate = estimate.coerceAtLeast(0.0)
    val first = (low ?: positiveEstimate).coerceAtLeast(0.0)
    val second = (high ?: positiveEstimate).coerceAtLeast(0.0)
    val orderedLow = min(first, second)
    val orderedHigh = max(first, second)

    return min(orderedLow, positiveEstimate) to max(orderedHigh, positiveEstimate)
}

fun remainingAfterLogging(
    available: Double,
    eaten: Double
): Double = (available.coerceAtLeast(0.0) - eaten.coerceAtLeast(0.0))
    .coerceAtLeast(0.0)
