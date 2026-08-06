package com.pgdevhouse.dailyrebuild.data.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogImportValidatorTest {
    @Test
    fun validDocumentHasNoBlockingErrors() {
        val result = CatalogImportValidator.validate(
            document(
                products = listOf(product("TUNA-BUFFALO")),
                roles = listOf(role("TUNA-BUFFALO", "Cracker Topping")),
                pantry = listOf(pantry("TUNA-BUFFALO", "pouch"))
            )
        )

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun orphanRoleBlocksImport() {
        val result = CatalogImportValidator.validate(
            document(
                products = listOf(product("TUNA-BUFFALO")),
                roles = listOf(role("MISSING", "Cracker Topping"))
            )
        )

        assertTrue(result.errors.any { it.contains("missing Product ID MISSING") })
    }

    @Test
    fun productIdsAreUniqueIgnoringCase() {
        val result = CatalogImportValidator.validate(
            document(
                products = listOf(
                    product("bread-001"),
                    product("BREAD-001")
                )
            )
        )

        assertTrue(result.errors.any { it.contains("Duplicate Product ID: BREAD-001") })
    }

    @Test
    fun pantryUnitMismatchIsWarningNotError() {
        val result = CatalogImportValidator.validate(
            document(
                products = listOf(product("BREAD-001", planningUnit = "slice")),
                pantry = listOf(pantry("BREAD-001", "loaf"))
            )
        )

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("does not match") })
    }

    @Test
    fun exporterErrorBlocksImport() {
        val result = CatalogImportValidator.validate(
            document(
                products = listOf(product("BREAD-001")),
                issues = listOf(
                    CatalogIssueValue(
                        severity = "Error",
                        code = "BROKEN_REFERENCE",
                        message = "A reference is broken.",
                        productId = null
                    )
                )
            )
        )

        assertFalse(result.errors.isEmpty())
    }

    private fun document(
        products: List<CatalogProductValue>,
        roles: List<CatalogRoleValue> = emptyList(),
        pantry: List<CatalogPantryValue> = emptyList(),
        issues: List<CatalogIssueValue> = emptyList()
    ) = CatalogImportDocument(
        schemaVersion = 1,
        generator = "DailyRebuildCatalogAssistant",
        exportedAtEpochMillis = 1L,
        sources = listOf(
            CatalogSourceValue(
                fileName = "catalog.xlsx",
                sha256 = "abc",
                lastModifiedEpochMillis = 1L,
                importedSections = listOf("products")
            )
        ),
        products = products,
        pantryItems = pantry,
        roles = roles,
        issues = issues
    )

    private fun product(
        id: String,
        planningUnit: String = "pouch"
    ) = CatalogProductValue(
        productId = id,
        productType = "MAIN_FOOD",
        active = true,
        genericName = "Tuna Pouch",
        productName = "Example Tuna Pouch",
        flavorVariant = "Buffalo",
        brand = "Example",
        category = "Protein",
        walmartUrl = null,
        snapEbtEligible = true,
        shelfStable = true,
        storage = "Pantry",
        microwave = "None",
        openedPackageRule = "Use full package",
        packageSize = "1 pouch",
        packagePrice = 1.25,
        labelServingsPerPackage = 1.0,
        planningUnit = planningUnit,
        planningUnitsPerLabelServing = 1.0,
        planningUnitsPerPackage = 1.0,
        caloriesPerPlanningUnit = 90.0,
        proteinGramsPerPlanningUnit = 15.0,
        carbohydrateGramsPerPlanningUnit = 2.0,
        fiberGramsPerPlanningUnit = 0.0,
        sodiumMilligramsPerPlanningUnit = 360.0,
        totalFatGramsPerPlanningUnit = 1.0,
        saturatedFatGramsPerPlanningUnit = 0.0,
        addedSugarGramsPerPlanningUnit = 0.0,
        notes = null,
        lastPriceCheckedEpochMillis = null,
        sourceFile = "catalog.xlsx",
        sourceSheet = "Product Catalog",
        sourceRow = 6
    )

    private fun role(id: String, role: String) = CatalogRoleValue(
        productId = id,
        role = role,
        active = true,
        notes = null,
        sourceFile = "roles.xlsx",
        sourceSheet = "Product Roles",
        sourceRow = 6
    )

    private fun pantry(id: String, unit: String) = CatalogPantryValue(
        productId = id,
        quantityOnHand = 2.0,
        inventoryUnit = unit,
        usable = true,
        storageLocation = "Pantry",
        bestByEpochMillis = null,
        lastCheckedEpochMillis = null,
        notes = null,
        sourceFile = "pantry.xlsx",
        sourceSheet = "PANTRY INVENTORY",
        sourceRow = 6
    )
}
