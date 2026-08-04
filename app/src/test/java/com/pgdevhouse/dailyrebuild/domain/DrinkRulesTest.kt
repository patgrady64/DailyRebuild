package com.pgdevhouse.dailyrebuild.domain

import com.pgdevhouse.dailyrebuild.data.local.DrinkCategory
import com.pgdevhouse.dailyrebuild.data.local.DrinkDefinition
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class DrinkRulesTest {
    @Test
    fun nutritionScalesWithLoggedAmount() {
        val definition = DrinkDefinition(
            id = 4L,
            name = "Protein drink",
            category = DrinkCategory.PROTEIN_DRINK,
            defaultAmountFlOz = 10.0,
            caloriesPerDefaultAmount = 200.0,
            carbohydrateGramsPerDefaultAmount = 8.0,
            proteinGramsPerDefaultAmount = 30.0,
            countsAsFood = true
        )

        val entry = definition.toDrinkEntry(
            date = "2026-08-04",
            consumedAt = 1L,
            amountFlOz = 5.0
        )

        assertEquals(100.0, entry.calories, 0.001)
        assertEquals(4.0, entry.carbohydrateGrams, 0.001)
        assertEquals(15.0, entry.proteinGrams, 0.001)
    }

    @Test
    fun waterAndOtherTotalsRemainSeparate() {
        val entries = listOf(
            DrinkEntry(
                date = "2026-08-04",
                consumedAt = 1L,
                drinkNameSnapshot = "Water",
                categorySnapshot = DrinkCategory.WATER,
                amountFlOz = 24.0,
                countsAsWater = true
            ),
            DrinkEntry(
                date = "2026-08-04",
                consumedAt = 2L,
                drinkNameSnapshot = "Coffee",
                categorySnapshot = DrinkCategory.COFFEE,
                amountFlOz = 12.0
            )
        )

        assertEquals(36.0, totalFluidOunces(entries), 0.001)
        assertEquals(24.0, totalWaterOunces(entries), 0.001)
        assertEquals(12.0, otherDrinkOunces(entries), 0.001)
    }
}
