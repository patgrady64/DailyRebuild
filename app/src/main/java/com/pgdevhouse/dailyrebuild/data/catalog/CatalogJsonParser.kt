package com.pgdevhouse.dailyrebuild.data.catalog

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal object CatalogImportParser {
    fun parse(jsonText: String): CatalogImportDocument = parse(JSONObject(jsonText))

    private fun parse(root: JSONObject): CatalogImportDocument {
        val schemaVersion = root.requiredInt("schemaVersion")
        val generator = root.requiredString("generator")
        val exportedAt = parseRequiredDate(
            root.requiredString("exportedAtUtc"),
            "exportedAtUtc"
        )

        return CatalogImportDocument(
            schemaVersion = schemaVersion,
            generator = generator,
            exportedAtEpochMillis = exportedAt,
            sources = root.requiredArray("sourceFiles").mapObjects(::parseSource),
            products = root.requiredArray("products").mapObjects(::parseProduct),
            pantryItems = root.requiredArray("pantryItems").mapObjects(::parsePantry),
            roles = root.requiredArray("productRoles").mapObjects(::parseRole),
            issues = root.optJSONArray("issues")?.mapObjects(::parseIssue).orEmpty()
        )
    }

    private fun parseSource(value: JSONObject) = CatalogSourceValue(
        fileName = value.requiredString("fileName"),
        sha256 = value.requiredString("sha256"),
        lastModifiedEpochMillis = parseRequiredDate(
            value.requiredString("lastModifiedUtc"),
            "sourceFiles.lastModifiedUtc"
        ),
        importedSections = value.requiredArray("importedSections").mapStrings()
    )

    private fun parseProduct(value: JSONObject) = CatalogProductValue(
        productId = value.requiredString("productId"),
        productType = value.requiredString("productType"),
        active = value.requiredBoolean("active"),
        genericName = value.requiredString("genericName"),
        productName = value.requiredString("productName"),
        flavorVariant = value.nullableString("flavorVariant"),
        brand = value.nullableString("brand"),
        category = value.nullableString("category"),
        walmartUrl = value.nullableString("walmartUrl"),
        snapEbtEligible = value.nullableBoolean("snapEbtEligible"),
        shelfStable = value.nullableBoolean("shelfStable"),
        storage = value.nullableString("storage"),
        microwave = value.nullableString("microwave"),
        openedPackageRule = value.nullableString("openedPackageRule"),
        packageSize = value.nullableString("packageSize"),
        packagePrice = value.requiredDouble("packagePrice"),
        labelServingsPerPackage = value.nullableDouble("labelServingsPerPackage"),
        planningUnit = value.requiredString("planningUnit"),
        planningUnitsPerLabelServing = value.requiredDouble("planningUnitsPerLabelServing"),
        planningUnitsPerPackage = value.requiredDouble("planningUnitsPerPackage"),
        caloriesPerPlanningUnit = value.requiredDouble("caloriesPerPlanningUnit"),
        proteinGramsPerPlanningUnit = value.nullableDouble("proteinGramsPerPlanningUnit"),
        carbohydrateGramsPerPlanningUnit = value.nullableDouble("carbohydrateGramsPerPlanningUnit"),
        fiberGramsPerPlanningUnit = value.nullableDouble("fiberGramsPerPlanningUnit"),
        sodiumMilligramsPerPlanningUnit = value.nullableDouble("sodiumMilligramsPerPlanningUnit"),
        totalFatGramsPerPlanningUnit = value.nullableDouble("totalFatGramsPerPlanningUnit"),
        saturatedFatGramsPerPlanningUnit = value.nullableDouble("saturatedFatGramsPerPlanningUnit"),
        addedSugarGramsPerPlanningUnit = value.nullableDouble("addedSugarGramsPerPlanningUnit"),
        notes = value.nullableString("notes"),
        lastPriceCheckedEpochMillis = value.nullableString("lastPriceChecked")?.let {
            parseRequiredDate(it, "products.lastPriceChecked")
        },
        sourceFile = value.requiredString("sourceFile"),
        sourceSheet = value.requiredString("sourceSheet"),
        sourceRow = value.requiredInt("sourceRow")
    )

    private fun parsePantry(value: JSONObject) = CatalogPantryValue(
        productId = value.requiredString("productId"),
        quantityOnHand = value.requiredDouble("quantityOnHand"),
        inventoryUnit = value.requiredString("inventoryUnit"),
        usable = value.requiredBoolean("usable"),
        storageLocation = value.nullableString("storageLocation"),
        bestByEpochMillis = value.nullableString("bestBy")?.let {
            parseRequiredDate(it, "pantryItems.bestBy")
        },
        lastCheckedEpochMillis = value.nullableString("lastChecked")?.let {
            parseRequiredDate(it, "pantryItems.lastChecked")
        },
        notes = value.nullableString("notes"),
        sourceFile = value.requiredString("sourceFile"),
        sourceSheet = value.requiredString("sourceSheet"),
        sourceRow = value.requiredInt("sourceRow")
    )

    private fun parseRole(value: JSONObject) = CatalogRoleValue(
        productId = value.requiredString("productId"),
        role = value.requiredString("role"),
        active = value.requiredBoolean("active"),
        notes = value.nullableString("notes"),
        sourceFile = value.requiredString("sourceFile"),
        sourceSheet = value.requiredString("sourceSheet"),
        sourceRow = value.requiredInt("sourceRow")
    )

    private fun parseIssue(value: JSONObject) = CatalogIssueValue(
        severity = value.requiredString("severity"),
        code = value.requiredString("code"),
        message = value.requiredString("message"),
        productId = value.nullableString("productId")
    )
}

private fun JSONObject.requiredString(name: String): String {
    require(has(name) && !isNull(name)) { "Missing required field '$name'." }
    return getString(name).trim()
}

private fun JSONObject.requiredInt(name: String): Int {
    require(has(name) && !isNull(name)) { "Missing required field '$name'." }
    return getInt(name)
}

private fun JSONObject.requiredDouble(name: String): Double {
    require(has(name) && !isNull(name)) { "Missing required field '$name'." }
    return getDouble(name)
}

private fun JSONObject.requiredBoolean(name: String): Boolean {
    require(has(name) && !isNull(name)) { "Missing required field '$name'." }
    return getBoolean(name)
}

private fun JSONObject.requiredArray(name: String): JSONArray {
    require(has(name) && !isNull(name)) { "Missing required array '$name'." }
    return getJSONArray(name)
}

private fun JSONObject.nullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else getString(name)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (!has(name) || isNull(name)) null else getDouble(name)

private fun JSONObject.nullableBoolean(name: String): Boolean? =
    if (!has(name) || isNull(name)) null else getBoolean(name)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    buildList(length()) {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

private fun JSONArray.mapStrings(): List<String> =
    buildList(length()) {
        for (index in 0 until length()) add(getString(index))
    }

private fun parseRequiredDate(value: String, field: String): Long {
    val text = value.trim()
    val parsed = runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
        ?: runCatching {
            LocalDate.parse(text.take(10))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    return requireNotNull(parsed) { "Field '$field' contains an invalid date: $value" }
}
