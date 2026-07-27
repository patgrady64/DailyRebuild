package com.pgdevhouse.dailyrebuild.domain

import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.PantryEssentialStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryShoppingRulesTest {
    @Test
    fun haveItemsAreExcludedAndNeedItemsAreRequired() {
        val summary = summarizePantryShopping(
            listOf(
                PantryEssential(name = "Mustard", status = PantryEssentialStatus.HAVE),
                PantryEssential(name = "Sucralose", status = PantryEssentialStatus.NEED, expectedPrice = 4.25)
            )
        )

        assertEquals(listOf("Sucralose"), summary.requiredItems.map { it.name })
        assertEquals(4.25, summary.knownRequiredCost, 0.001)
        assertFalse(summary.requiredItemsMeetMinimum)
    }

    @Test
    fun unknownPricesAreCountedWithoutInventingCost() {
        val summary = summarizePantryShopping(
            listOf(
                PantryEssential(name = "Pepper", status = PantryEssentialStatus.NEED),
                PantryEssential(name = "Drink flavoring", status = PantryEssentialStatus.NEED, expectedPrice = 3.50)
            )
        )

        assertEquals(1, summary.unknownPriceCount)
        assertEquals(31.50, summary.dollarsRemainingBeforeMinimum, 0.001)
    }

    @Test
    fun requiredItemsCanMeetMinimumBeforeMealFoods() {
        val summary = summarizePantryShopping(
            listOf(
                PantryEssential(name = "Required item", status = PantryEssentialStatus.NEED, expectedPrice = 36.00)
            )
        )

        assertTrue(summary.requiredItemsMeetMinimum)
        assertEquals(0.0, summary.dollarsRemainingBeforeMinimum, 0.001)
    }
}
