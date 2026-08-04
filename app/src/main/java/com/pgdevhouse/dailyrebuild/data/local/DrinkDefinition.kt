package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reusable beverage or container shortcut.
 *
 * Amounts are normalized to US fluid ounces in storage. Nutrition values are
 * the values for [defaultAmountFlOz], which keeps one-tap logging predictable.
 */
@Entity(
    tableName = "drink_definitions",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isFavorite", "isActive"])
    ]
)
data class DrinkDefinition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String = DrinkCategory.OTHER,
    val defaultAmountFlOz: Double = 12.0,
    val caloriesPerDefaultAmount: Double = 0.0,
    val carbohydrateGramsPerDefaultAmount: Double = 0.0,
    val sugarGramsPerDefaultAmount: Double = 0.0,
    val proteinGramsPerDefaultAmount: Double = 0.0,
    val caffeineMilligramsPerDefaultAmount: Double = 0.0,
    val countsAsWater: Boolean = false,
    val countsAsFood: Boolean = false,
    val containerName: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object DrinkCategory {
    const val WATER = "WATER"
    const val FLAVORED_WATER = "FLAVORED_WATER"
    const val COFFEE = "COFFEE"
    const val TEA = "TEA"
    const val SODA = "SODA"
    const val DIET_SODA = "DIET_SODA"
    const val JUICE = "JUICE"
    const val MILK = "MILK"
    const val PROTEIN_DRINK = "PROTEIN_DRINK"
    const val SPORTS_DRINK = "SPORTS_DRINK"
    const val ENERGY_DRINK = "ENERGY_DRINK"
    const val OTHER = "OTHER"

    val all = listOf(
        WATER,
        FLAVORED_WATER,
        COFFEE,
        TEA,
        SODA,
        DIET_SODA,
        JUICE,
        MILK,
        PROTEIN_DRINK,
        SPORTS_DRINK,
        ENERGY_DRINK,
        OTHER
    )

    fun label(category: String): String = when (category) {
        WATER -> "Water"
        FLAVORED_WATER -> "Flavored water"
        COFFEE -> "Coffee"
        TEA -> "Tea"
        SODA -> "Soda"
        DIET_SODA -> "Diet soda"
        JUICE -> "Juice"
        MILK -> "Milk"
        PROTEIN_DRINK -> "Protein drink"
        SPORTS_DRINK -> "Sports drink"
        ENERGY_DRINK -> "Energy drink"
        else -> "Other drink"
    }
}

fun defaultDrinkDefinitions(now: Long = System.currentTimeMillis()): List<DrinkDefinition> = listOf(
    DrinkDefinition(name = "Water", category = DrinkCategory.WATER, defaultAmountFlOz = 24.0, countsAsWater = true, containerName = "Reusable bottle", isFavorite = true, createdAt = now, updatedAt = now),
    DrinkDefinition(name = "MiO / flavored water", category = DrinkCategory.FLAVORED_WATER, defaultAmountFlOz = 24.0, countsAsWater = true, containerName = "Reusable bottle", isFavorite = true, createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Coffee", category = DrinkCategory.COFFEE, defaultAmountFlOz = 12.0, containerName = "Coffee mug", isFavorite = true, createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Tea", category = DrinkCategory.TEA, defaultAmountFlOz = 12.0, containerName = "Mug", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Soda", category = DrinkCategory.SODA, defaultAmountFlOz = 12.0, containerName = "Can", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Diet soda", category = DrinkCategory.DIET_SODA, defaultAmountFlOz = 12.0, containerName = "Can", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Juice", category = DrinkCategory.JUICE, defaultAmountFlOz = 8.0, containerName = "Glass", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Milk", category = DrinkCategory.MILK, defaultAmountFlOz = 8.0, containerName = "Glass", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Protein drink", category = DrinkCategory.PROTEIN_DRINK, defaultAmountFlOz = 11.0, countsAsFood = true, containerName = "Bottle", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Sports drink", category = DrinkCategory.SPORTS_DRINK, defaultAmountFlOz = 20.0, containerName = "Bottle", createdAt = now, updatedAt = now),
    DrinkDefinition(name = "Energy drink", category = DrinkCategory.ENERGY_DRINK, defaultAmountFlOz = 12.0, containerName = "Can", createdAt = now, updatedAt = now)
)
