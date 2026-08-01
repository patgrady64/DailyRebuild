package com.pgdevhouse.dailyrebuild.ui.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill

/**
 * UI-only state for the barcode decision flow.
 *
 * Network and Room work remain in repositories/coordinators. This ViewModel
 * remembers which decision the user made so logging an online result cannot
 * accidentally overwrite a corrected local food.
 */
class FoodBarcodeViewModel : ViewModel() {

    var localMatch by mutableStateOf<FoodProduct?>(null)
        private set

    var onlineResult by mutableStateOf<ScannedFoodPrefill?>(null)
        private set

    var verifiedBarcode by mutableStateOf<String?>(null)
        private set

    var forMealBuilder by mutableStateOf(false)
        private set

    var savePolicy by mutableStateOf(BarcodeSavePolicy.NORMAL)
        private set

    val showLocalChoice: Boolean
        get() = localMatch != null && onlineResult == null

    val showOnlineChoice: Boolean
        get() = localMatch != null && onlineResult != null

    fun beginLocalChoice(
        barcode: String,
        product: FoodProduct,
        forMealBuilder: Boolean
    ) {
        verifiedBarcode = barcode
        localMatch = product
        onlineResult = null
        this.forMealBuilder = forMealBuilder
        savePolicy = BarcodeSavePolicy.NORMAL
    }

    fun recordOnlineResult(result: ScannedFoodPrefill) {
        onlineResult = result
    }

    fun chooseLocal(): BarcodeManualSelection? {
        val local = localMatch ?: return null
        savePolicy = BarcodeSavePolicy.USE_LOCAL_WITHOUT_UPDATE
        return BarcodeManualSelection(
            prefill = local.toScannedFoodPrefill(
                barcodeOverride = verifiedBarcode
            ),
            policy = savePolicy,
            existingProductId = local.id,
            forMealBuilder = forMealBuilder
        ).also { clearPromptsOnly() }
    }

    fun chooseOnlineOnce(): BarcodeManualSelection? {
        val online = onlineResult ?: return null
        val local = localMatch ?: return null
        savePolicy = BarcodeSavePolicy.USE_ONLINE_ONCE
        return BarcodeManualSelection(
            prefill = online.copy(
                productId = local.id,
                isFavorite = local.isFavorite,
                isCondiment = local.isCondiment
            ),
            policy = savePolicy,
            existingProductId = local.id,
            forMealBuilder = forMealBuilder
        ).also { clearPromptsOnly() }
    }

    fun chooseOnlineAndUpdate(): BarcodeManualSelection? {
        val online = onlineResult ?: return null
        val local = localMatch ?: return null
        savePolicy = BarcodeSavePolicy.UPDATE_LOCAL
        return BarcodeManualSelection(
            prefill = online.copy(
                productId = local.id,
                isFavorite = local.isFavorite,
                isCondiment = local.isCondiment
            ),
            policy = savePolicy,
            existingProductId = local.id,
            forMealBuilder = forMealBuilder
        ).also { clearPromptsOnly() }
    }

    fun cancel() {
        localMatch = null
        onlineResult = null
        verifiedBarcode = null
        forMealBuilder = false
        savePolicy = BarcodeSavePolicy.NORMAL
    }

    fun resetSavePolicy() {
        savePolicy = BarcodeSavePolicy.NORMAL
    }

    private fun clearPromptsOnly() {
        localMatch = null
        onlineResult = null
        verifiedBarcode = null
    }
}

enum class BarcodeSavePolicy {
    NORMAL,
    /** Use the existing local food without modifying its saved nutrition. */
    USE_LOCAL_WITHOUT_UPDATE,
    /** Log the reviewed online snapshot once without modifying the Saved Food. */
    USE_ONLINE_ONCE,
    /** Replace the existing Saved Food with the reviewed online values. */
    UPDATE_LOCAL
}

data class BarcodeManualSelection(
    val prefill: ScannedFoodPrefill,
    val policy: BarcodeSavePolicy,
    val existingProductId: Long,
    val forMealBuilder: Boolean
)

fun FoodProduct.toScannedFoodPrefill(
    barcodeOverride: String? = null
): ScannedFoodPrefill {
    return ScannedFoodPrefill(
        productId = id,
        barcode = barcodeOverride ?: barcode.orEmpty(),
        name = name,
        brand = brand,
        caloriesPerServing = caloriesPerServing,
        proteinGramsPerServing = proteinGramsPerServing,
        carbohydrateGramsPerServing = carbohydrateGramsPerServing,
        fatGramsPerServing = fatGramsPerServing,
        sodiumMilligramsPerServing = sodiumMilligramsPerServing,
        servingQuantity = servingQuantity,
        servingUnit = servingUnit,
        packageQuantity = packageQuantity,
        packageUnit = packageUnit.orEmpty(),
        isFavorite = isFavorite,
        isCondiment = isCondiment,
        originalServingSize = "$servingQuantity $servingUnit"
    )
}
