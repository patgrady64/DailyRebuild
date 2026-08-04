package com.pgdevhouse.dailyrebuild.domain

import com.pgdevhouse.dailyrebuild.data.local.DrinkDefinition
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry

private const val MIN_DRINK_OUNCES = 0.1
private const val MAX_DRINK_OUNCES = 256.0

fun sanitizeDrinkAmountFlOz(value: Double): Double =
    value.coerceIn(MIN_DRINK_OUNCES, MAX_DRINK_OUNCES)

fun DrinkDefinition.toDrinkEntry(
    date: String,
    consumedAt: Long,
    amountFlOz: Double = defaultAmountFlOz,
    notesOverride: String? = null
): DrinkEntry {
    val safeAmount = sanitizeDrinkAmountFlOz(amountFlOz)
    val denominator = defaultAmountFlOz.takeIf { it > 0.0 } ?: safeAmount
    val scale = safeAmount / denominator
    return DrinkEntry(
        date = date,
        consumedAt = consumedAt,
        definitionId = id.takeIf { it > 0L },
        drinkNameSnapshot = name.trim().ifBlank { "Drink" },
        categorySnapshot = category,
        amountFlOz = safeAmount,
        calories = caloriesPerDefaultAmount * scale,
        carbohydrateGrams = carbohydrateGramsPerDefaultAmount * scale,
        sugarGrams = sugarGramsPerDefaultAmount * scale,
        proteinGrams = proteinGramsPerDefaultAmount * scale,
        caffeineMilligrams = caffeineMilligramsPerDefaultAmount * scale,
        countsAsWater = countsAsWater,
        countsAsFood = countsAsFood,
        notes = notesOverride?.trim() ?: notes.trim()
    )
}

fun totalFluidOunces(entries: List<DrinkEntry>): Double =
    entries.sumOf { it.amountFlOz.coerceAtLeast(0.0) }

fun totalWaterOunces(entries: List<DrinkEntry>): Double =
    entries.filter(DrinkEntry::countsAsWater)
        .sumOf { it.amountFlOz.coerceAtLeast(0.0) }

fun otherDrinkOunces(entries: List<DrinkEntry>): Double =
    entries.filterNot(DrinkEntry::countsAsWater)
        .sumOf { it.amountFlOz.coerceAtLeast(0.0) }
