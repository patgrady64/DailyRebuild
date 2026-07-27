package com.pgdevhouse.dailyrebuild.domain

import com.pgdevhouse.dailyrebuild.data.local.PantryEssential

const val WALMART_ORDER_MINIMUM_DOLLARS = 35.0

data class PantryShoppingSummary(
    val requiredItems: List<PantryEssential>,
    val knownRequiredCost: Double,
    val unknownPriceCount: Int,
    val dollarsRemainingBeforeMinimum: Double,
    val requiredItemsMeetMinimum: Boolean
)

/** Required pantry items are locked into the next order; Have items are excluded. */
fun summarizePantryShopping(
    items: List<PantryEssential>,
    minimumDollars: Double = WALMART_ORDER_MINIMUM_DOLLARS
): PantryShoppingSummary {
    val required = items.filter { it.isNeeded }
    val knownCost = required.mapNotNull { it.expectedPrice }.sum()
    val unknownCount = required.count { it.expectedPrice == null }

    return PantryShoppingSummary(
        requiredItems = required,
        knownRequiredCost = knownCost,
        unknownPriceCount = unknownCount,
        dollarsRemainingBeforeMinimum =
            (minimumDollars - knownCost).coerceAtLeast(0.0),
        requiredItemsMeetMinimum = knownCost >= minimumDollars
    )
}
