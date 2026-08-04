package com.pgdevhouse.dailyrebuild.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PreparedFoodRulesTest {
    @Test
    fun componentsAreCombinedWithoutAllowingNegativeNutrition() {
        val result = combinePreparedFoodComponents(
            listOf(
                PreparedFoodComponent("Sandwich", calories = 600.0, proteinGrams = 30.0, fatGrams = 20.0),
                PreparedFoodComponent("Fries", calories = 400.0, carbohydrateGrams = 55.0, sodiumMilligrams = 700.0),
                PreparedFoodComponent("Bad input", calories = -50.0, fatGrams = -2.0)
            )
        )

        assertEquals(1000.0, result.calories, 0.001)
        assertEquals(30.0, result.proteinGrams, 0.001)
        assertEquals(55.0, result.carbohydrateGrams, 0.001)
        assertEquals(20.0, result.fatGrams, 0.001)
        assertEquals(700.0, result.sodiumMilligrams, 0.001)
    }

    @Test
    fun calorieRangeIsOrderedAndIncludesEstimate() {
        val range = normalizedCalorieRange(1200.0, 1500.0, 900.0)
        assertEquals(900.0, range.first ?: 0.0, 0.001)
        assertEquals(1500.0, range.second ?: 0.0, 0.001)
    }

    @Test
    fun leftoversNeverGoNegative() {
        assertEquals(0.0, remainingAfterLogging(2.0, 3.0), 0.001)
        assertEquals(1.25, remainingAfterLogging(2.0, 0.75), 0.001)
    }
}
