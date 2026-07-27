package com.pgdevhouse.dailyrebuild.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodBarcodePolicyTest {
    @Test
    fun printedFormattingIsRemovedBeforeLookup() {
        assertEquals("012345678905", normalizeFoodBarcode("0 12345-67890 5"))
    }

    @Test
    fun commonFoodBarcodeLengthsAreAccepted() {
        assertTrue(isSupportedFoodBarcode("12345678"))
        assertTrue(isSupportedFoodBarcode("123456789012"))
        assertTrue(isSupportedFoodBarcode("1234567890123"))
        assertFalse(isSupportedFoodBarcode("1234567890"))
    }
}
