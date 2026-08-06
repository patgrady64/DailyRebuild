package com.pgdevhouse.dailyrebuild.data.catalog

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.pgdevhouse.dailyrebuild.data.local.CatalogImportSource
import com.pgdevhouse.dailyrebuild.data.local.CatalogPantryItem
import com.pgdevhouse.dailyrebuild.data.local.CatalogProduct
import com.pgdevhouse.dailyrebuild.data.local.CatalogProductRole
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Imports schema-version-1 JSON produced by DailyRebuildCatalogAssistant.
 *
 * XLSX parsing remains a desktop concern. Android receives one normalized JSON
 * bundle, validates every reference, then commits the catalog in one Room
 * transaction.
 */
class CatalogJsonImportManager(
    private val context: Context,
    private val database: DailyRebuildDatabase
) {
    data class Inspection(
        val sourceLabel: String,
        val schemaVersion: Int,
        val generator: String,
        val exportedAtEpochMillis: Long,
        val sourceFileCount: Int,
        val productCount: Int,
        val activeProductCount: Int,
        val roleCount: Int,
        val pantryItemCount: Int,
        val warnings: List<String>,
        val blockingErrors: List<String>
    ) {
        val canImport: Boolean
            get() = blockingErrors.isEmpty()
    }

    data class ImportResult(
        val productCount: Int,
        val activeProductCount: Int,
        val roleCount: Int,
        val pantryItemCount: Int,
        val sourceFileCount: Int,
        val importedAtEpochMillis: Long,
        val warningCount: Int
    )

    data class CurrentStatus(
        val productCount: Int,
        val activeProductCount: Int,
        val roleCount: Int,
        val pantryItemCount: Int,
        val lastImportedAtEpochMillis: Long?
    )

    suspend fun inspectUri(uri: Uri): Inspection = withContext(Dispatchers.IO) {
        val document = readDocument(uri)
        val validation = CatalogImportValidator.validate(document)
        Inspection(
            sourceLabel = queryDisplayName(uri) ?: "Selected catalog JSON",
            schemaVersion = document.schemaVersion,
            generator = document.generator,
            exportedAtEpochMillis = document.exportedAtEpochMillis,
            sourceFileCount = document.sources.size,
            productCount = document.products.size,
            activeProductCount = document.products.count { it.active },
            roleCount = document.roles.count { it.active },
            pantryItemCount = document.pantryItems.size,
            warnings = validation.warnings,
            blockingErrors = validation.errors
        )
    }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val document = readDocument(uri)
        val validation = CatalogImportValidator.validate(document)
        require(validation.errors.isEmpty()) {
            "Catalog import was blocked: ${validation.errors.take(3).joinToString(" ")}"
        }

        val importedAt = System.currentTimeMillis()
        val dao = database.catalogImportDao()

        val products = document.products.map { value ->
            CatalogProduct(
                productId = normalizeProductId(value.productId),
                productType = value.productType.trim().uppercase(Locale.US),
                active = value.active,
                genericName = value.genericName.trim(),
                productName = value.productName.trim(),
                flavorVariant = value.flavorVariant.cleanOrNull(),
                brand = value.brand.cleanOrNull(),
                category = value.category.cleanOrNull(),
                walmartUrl = value.walmartUrl.cleanOrNull(),
                snapEbtEligible = value.snapEbtEligible,
                shelfStable = value.shelfStable,
                storage = value.storage.cleanOrNull(),
                microwave = value.microwave.cleanOrNull(),
                openedPackageRule = value.openedPackageRule.cleanOrNull(),
                packageSize = value.packageSize.cleanOrNull(),
                packagePrice = value.packagePrice,
                labelServingsPerPackage = value.labelServingsPerPackage,
                planningUnit = normalizeUnit(value.planningUnit),
                planningUnitsPerLabelServing = value.planningUnitsPerLabelServing,
                planningUnitsPerPackage = value.planningUnitsPerPackage,
                caloriesPerPlanningUnit = value.caloriesPerPlanningUnit,
                proteinGramsPerPlanningUnit = value.proteinGramsPerPlanningUnit,
                carbohydrateGramsPerPlanningUnit = value.carbohydrateGramsPerPlanningUnit,
                fiberGramsPerPlanningUnit = value.fiberGramsPerPlanningUnit,
                sodiumMilligramsPerPlanningUnit = value.sodiumMilligramsPerPlanningUnit,
                totalFatGramsPerPlanningUnit = value.totalFatGramsPerPlanningUnit,
                saturatedFatGramsPerPlanningUnit = value.saturatedFatGramsPerPlanningUnit,
                addedSugarGramsPerPlanningUnit = value.addedSugarGramsPerPlanningUnit,
                notes = value.notes.cleanOrNull(),
                lastPriceCheckedEpochMillis = value.lastPriceCheckedEpochMillis,
                sourceFile = value.sourceFile,
                sourceSheet = value.sourceSheet,
                sourceRow = value.sourceRow,
                importedAtEpochMillis = importedAt
            )
        }

        val roles = document.roles.map { value ->
            CatalogProductRole(
                productId = normalizeProductId(value.productId),
                role = value.role.trim(),
                active = value.active,
                notes = value.notes.cleanOrNull(),
                sourceFile = value.sourceFile,
                sourceSheet = value.sourceSheet,
                sourceRow = value.sourceRow,
                importedAtEpochMillis = importedAt
            )
        }

        val pantryItems = document.pantryItems.map { value ->
            CatalogPantryItem(
                productId = normalizeProductId(value.productId),
                quantityOnHand = value.quantityOnHand,
                inventoryUnit = normalizeUnit(value.inventoryUnit),
                usable = value.usable,
                storageLocation = value.storageLocation.cleanOrNull(),
                bestByEpochMillis = value.bestByEpochMillis,
                lastCheckedEpochMillis = value.lastCheckedEpochMillis,
                notes = value.notes.cleanOrNull(),
                sourceFile = value.sourceFile,
                sourceSheet = value.sourceSheet,
                sourceRow = value.sourceRow,
                importedAtEpochMillis = importedAt
            )
        }

        val sources = document.sources.map { value ->
            CatalogImportSource(
                fileName = value.fileName,
                sha256 = value.sha256,
                lastModifiedEpochMillis = value.lastModifiedEpochMillis,
                importedSectionsJson = JSONArray().apply {
                    value.importedSections.forEach { section -> put(section) }
                }.toString(),
                exportedAtEpochMillis = document.exportedAtEpochMillis,
                importedAtEpochMillis = importedAt
            )
        }

        database.withTransaction {
            // Preserve old Product IDs for future plan/history references, but
            // deactivate anything omitted from the newest complete bundle.
            // Imported rows then restore their explicit Active? values.
            dao.deactivateAllProducts()
            dao.upsertProducts(products)
            dao.clearRoles()
            dao.clearPantryItems()
            dao.clearSources()
            if (roles.isNotEmpty()) dao.insertRoles(roles)
            if (pantryItems.isNotEmpty()) dao.insertPantryItems(pantryItems)
            if (sources.isNotEmpty()) dao.insertSources(sources)
        }

        ImportResult(
            productCount = dao.productCount(),
            activeProductCount = dao.activeProductCount(),
            roleCount = dao.activeRoleCount(),
            pantryItemCount = dao.pantryItemCount(),
            sourceFileCount = sources.size,
            importedAtEpochMillis = importedAt,
            warningCount = validation.warnings.size
        )
    }

    suspend fun currentStatus(): CurrentStatus = withContext(Dispatchers.IO) {
        val dao = database.catalogImportDao()
        CurrentStatus(
            productCount = dao.productCount(),
            activeProductCount = dao.activeProductCount(),
            roleCount = dao.activeRoleCount(),
            pantryItemCount = dao.pantryItemCount(),
            lastImportedAtEpochMillis = dao.lastImportedAt()
        )
    }

    private fun readDocument(uri: Uri): CatalogImportDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Android could not open the selected JSON file.")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_JSON_BYTES) {
                    "Catalog JSON is larger than the supported 25 MB limit."
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }

        require(bytes.isNotEmpty()) { "The selected catalog JSON is empty." }
        val text = String(bytes, StandardCharsets.UTF_8).removePrefix("\uFEFF")
        return try {
            CatalogImportParser.parse(text)
        } catch (error: Throwable) {
            if (error is IllegalArgumentException) throw error
            throw IllegalArgumentException(
                "The selected file is not valid Daily Rebuild catalog JSON. ${error.message.orEmpty()}",
                error
            )
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private companion object {
        const val MAX_JSON_BYTES = 25L * 1024L * 1024L
    }
}

private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
