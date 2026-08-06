package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exact purchasable product imported from the Daily Rebuild catalog JSON.
 *
 * This is deliberately separate from [FoodProduct]. FoodProduct powers the
 * existing logging experience; CatalogProduct powers future meal planning,
 * package purchasing, pantry subtraction, and nutrition-aware shopping.
 */
@Entity(
    tableName = "catalog_products",
    indices = [
        Index(value = ["productType", "active"]),
        Index(value = ["category"]),
        Index(value = ["productName"])
    ]
)
data class CatalogProduct(
    @PrimaryKey
    val productId: String,
    val productType: String,
    val active: Boolean,
    val genericName: String,
    val productName: String,
    val flavorVariant: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val walmartUrl: String? = null,
    val snapEbtEligible: Boolean? = null,
    val shelfStable: Boolean? = null,
    val storage: String? = null,
    val microwave: String? = null,
    val openedPackageRule: String? = null,
    val packageSize: String? = null,
    val packagePrice: Double,
    val labelServingsPerPackage: Double? = null,
    val planningUnit: String,
    val planningUnitsPerLabelServing: Double,
    val planningUnitsPerPackage: Double,
    val caloriesPerPlanningUnit: Double,
    val proteinGramsPerPlanningUnit: Double? = null,
    val carbohydrateGramsPerPlanningUnit: Double? = null,
    val fiberGramsPerPlanningUnit: Double? = null,
    val sodiumMilligramsPerPlanningUnit: Double? = null,
    val totalFatGramsPerPlanningUnit: Double? = null,
    val saturatedFatGramsPerPlanningUnit: Double? = null,
    val addedSugarGramsPerPlanningUnit: Double? = null,
    val notes: String? = null,
    val lastPriceCheckedEpochMillis: Long? = null,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int,
    val importedAtEpochMillis: Long
)
