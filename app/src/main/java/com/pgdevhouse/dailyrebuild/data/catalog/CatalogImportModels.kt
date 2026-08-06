package com.pgdevhouse.dailyrebuild.data.catalog

import java.util.Locale

internal data class CatalogImportDocument(
    val schemaVersion: Int,
    val generator: String,
    val exportedAtEpochMillis: Long,
    val sources: List<CatalogSourceValue>,
    val products: List<CatalogProductValue>,
    val pantryItems: List<CatalogPantryValue>,
    val roles: List<CatalogRoleValue>,
    val issues: List<CatalogIssueValue>
)

internal data class CatalogSourceValue(
    val fileName: String,
    val sha256: String,
    val lastModifiedEpochMillis: Long,
    val importedSections: List<String>
)

internal data class CatalogProductValue(
    val productId: String,
    val productType: String,
    val active: Boolean,
    val genericName: String,
    val productName: String,
    val flavorVariant: String?,
    val brand: String?,
    val category: String?,
    val walmartUrl: String?,
    val snapEbtEligible: Boolean?,
    val shelfStable: Boolean?,
    val storage: String?,
    val microwave: String?,
    val openedPackageRule: String?,
    val packageSize: String?,
    val packagePrice: Double,
    val labelServingsPerPackage: Double?,
    val planningUnit: String,
    val planningUnitsPerLabelServing: Double,
    val planningUnitsPerPackage: Double,
    val caloriesPerPlanningUnit: Double,
    val proteinGramsPerPlanningUnit: Double?,
    val carbohydrateGramsPerPlanningUnit: Double?,
    val fiberGramsPerPlanningUnit: Double?,
    val sodiumMilligramsPerPlanningUnit: Double?,
    val totalFatGramsPerPlanningUnit: Double?,
    val saturatedFatGramsPerPlanningUnit: Double?,
    val addedSugarGramsPerPlanningUnit: Double?,
    val notes: String?,
    val lastPriceCheckedEpochMillis: Long?,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int
)

internal data class CatalogPantryValue(
    val productId: String,
    val quantityOnHand: Double,
    val inventoryUnit: String,
    val usable: Boolean,
    val storageLocation: String?,
    val bestByEpochMillis: Long?,
    val lastCheckedEpochMillis: Long?,
    val notes: String?,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int
)

internal data class CatalogRoleValue(
    val productId: String,
    val role: String,
    val active: Boolean,
    val notes: String?,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int
)

internal data class CatalogIssueValue(
    val severity: String,
    val code: String,
    val message: String,
    val productId: String?
)

internal data class CatalogValidationResult(
    val errors: List<String>,
    val warnings: List<String>
)

internal object CatalogImportValidator {
    fun validate(document: CatalogImportDocument): CatalogValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (document.schemaVersion != 1) {
            errors += "Unsupported catalog schema ${document.schemaVersion}; this app supports schema 1."
        }
        if (document.generator != "DailyRebuildCatalogAssistant") {
            errors += "This JSON was not created by DailyRebuildCatalogAssistant."
        }
        if (document.sources.isEmpty()) {
            errors += "The catalog does not identify any source workbooks."
        }
        val sourceNames = mutableSetOf<String>()
        document.sources.forEach { source ->
            val name = source.fileName.trim().lowercase(Locale.US)
            if (name.isBlank()) errors += "A catalog source has no file name."
            if (!sourceNames.add(name)) errors += "Duplicate source file name: ${source.fileName}."
            if (source.sha256.isBlank()) errors += "${source.fileName} has no source hash."
        }

        if (document.products.isEmpty()) {
            errors += "The catalog contains no completed products."
        }
        if (document.products.size > 50_000) {
            errors += "The catalog contains more than 50,000 products."
        }

        document.issues.forEach { issue ->
            val detail = "${issue.code}: ${issue.message}"
            if (issue.severity.equals("Error", ignoreCase = true)) {
                errors += detail
            } else if (issue.severity.equals("Warning", ignoreCase = true)) {
                warnings += detail
            }
        }

        val productIds = linkedSetOf<String>()
        document.products.forEach { product ->
            val id = normalizeProductId(product.productId)
            if (id.isBlank()) errors += "A product has a blank Product ID."
            if (!productIds.add(id)) errors += "Duplicate Product ID: $id."
            if (product.productName.isBlank()) errors += "$id has no product name."
            if (product.genericName.isBlank()) errors += "$id has no generic food name."
            if (product.productType.isBlank()) errors += "$id has no product type."
            if (product.planningUnit.isBlank()) errors += "$id has no planning unit."
            if (product.packagePrice < 0.0) errors += "$id has a negative package price."
            if (product.planningUnitsPerLabelServing <= 0.0) {
                errors += "$id must have planning units per label serving greater than zero."
            }
            if (product.planningUnitsPerPackage <= 0.0) {
                errors += "$id must have planning units per package greater than zero."
            }
            if (product.caloriesPerPlanningUnit < 0.0) {
                errors += "$id has negative calories."
            }
            listOf(
                "protein" to product.proteinGramsPerPlanningUnit,
                "carbohydrates" to product.carbohydrateGramsPerPlanningUnit,
                "fiber" to product.fiberGramsPerPlanningUnit,
                "sodium" to product.sodiumMilligramsPerPlanningUnit,
                "total fat" to product.totalFatGramsPerPlanningUnit,
                "saturated fat" to product.saturatedFatGramsPerPlanningUnit,
                "added sugar" to product.addedSugarGramsPerPlanningUnit
            ).forEach { (name, number) ->
                if (number != null && number < 0.0) errors += "$id has negative $name."
            }
            if (product.totalFatGramsPerPlanningUnit == null) {
                warnings += "$id has no total-fat value; fat-aware planning will treat it as unknown."
            }
        }

        val roleKeys = mutableSetOf<String>()
        document.roles.forEach { role ->
            val id = normalizeProductId(role.productId)
            val roleName = role.role.trim()
            if (id !in productIds) errors += "Role '$roleName' references missing Product ID $id."
            if (roleName.isBlank()) errors += "$id has a blank product role."
            val key = "$id|${roleName.lowercase(Locale.US)}"
            if (!roleKeys.add(key)) errors += "Duplicate Product ID and role: $id + $roleName."
        }

        val pantryIds = mutableSetOf<String>()
        val productById = document.products.associateBy { normalizeProductId(it.productId) }
        document.pantryItems.forEach { pantry ->
            val id = normalizeProductId(pantry.productId)
            if (id !in productIds) errors += "Pantry item references missing Product ID $id."
            if (!pantryIds.add(id)) errors += "Duplicate pantry Product ID: $id."
            if (pantry.quantityOnHand < 0.0) errors += "$id has negative pantry quantity."
            if (pantry.inventoryUnit.isBlank()) errors += "$id has no pantry inventory unit."
            val productUnit = productById[id]?.planningUnit
            if (productUnit != null && normalizeUnit(productUnit) != normalizeUnit(pantry.inventoryUnit)) {
                warnings += "$id pantry unit '${pantry.inventoryUnit}' does not match product planning unit '$productUnit'."
            }
        }

        return CatalogValidationResult(
            errors = errors.distinct(),
            warnings = warnings.distinct()
        )
    }
}

internal fun normalizeProductId(value: String): String =
    value.trim().uppercase(Locale.US)

internal fun normalizeUnit(value: String): String =
    value.trim().lowercase(Locale.US)
