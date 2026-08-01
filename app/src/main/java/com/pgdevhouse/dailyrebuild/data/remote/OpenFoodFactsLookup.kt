package com.pgdevhouse.dailyrebuild.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ScannedFoodPrefill(
    /*
     * Present when loading a product already stored
     * in Daily Rebuild's local food library.
     */
    val productId: Long? = null,

    /*
     * Null for manually entered products without
     * a UPC or EAN barcode.
     */
    val barcode: String? = null,

    val name: String = "",
    val brand: String = "",

    val caloriesPerServing: Double = 0.0,
    val proteinGramsPerServing: Double = 0.0,
    val carbohydrateGramsPerServing: Double = 0.0,
    val fatGramsPerServing: Double = 0.0,
    val sodiumMilligramsPerServing: Double = 0.0,

    val servingQuantity: Double = 1.0,
    val servingUnit: String = "serving",

    val packageQuantity: Double? = null,
    val packageUnit: String = "",

    val isFavorite: Boolean = false,
    val isCondiment: Boolean = false,

    val originalServingSize: String = ""
)

sealed interface FoodLookupResult {

    data class Found(
        val food: ScannedFoodPrefill
    ) : FoodLookupResult

    data object NotFound : FoodLookupResult

    data class Failed(
        val message: String
    ) : FoodLookupResult
}

object OpenFoodFactsLookup {

    private const val USER_AGENT =
        "DailyRebuild/0.1 Android " +
                "(https://github.com/patgrady64)"

    suspend fun findProduct(
        barcode: String
    ): FoodLookupResult = withContext(
        Dispatchers.IO
    ) {
        var connection: HttpURLConnection? = null

        try {
            val fields =
                "code," +
                        "product_name," +
                        "brands," +
                        "serving_size," +
                        "serving_quantity," +
                        "nutriments"

            val url = URL(
                "https://world.openfoodfacts.org/" +
                        "api/v2/product/$barcode.json" +
                        "?fields=$fields"
            )

            connection =
                url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true

            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode =
                connection.responseCode

            if (responseCode == 404) {
                return@withContext FoodLookupResult.NotFound
            }

            if (responseCode !in 200..299) {
                return@withContext FoodLookupResult.Failed(
                    message =
                        "Open Food Facts returned " +
                                "error $responseCode."
                )
            }

            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val root =
                JSONObject(responseText)

            if (root.optInt("status", 0) != 1) {
                return@withContext FoodLookupResult.NotFound
            }

            val product =
                root.optJSONObject("product")
                    ?: return@withContext FoodLookupResult.NotFound

            val productName =
                product.stringOrEmpty(
                    "product_name"
                )

            val brand =
                product.stringOrEmpty(
                    "brands"
                )

            val originalServingSize =
                product.stringOrEmpty(
                    "serving_size"
                )

            val servingQuantityGrams =
                product.nullableDouble(
                    "serving_quantity"
                )

            val parsedServing =
                parseServingSize(
                    originalServingSize
                )

            val servingQuantity =
                parsedServing?.first ?: 1.0

            val servingUnit =
                parsedServing?.second ?: "serving"

            val nutriments =
                product.optJSONObject(
                    "nutriments"
                )

            val calories =
                nutrientPerServing(
                    nutriments = nutriments,
                    nutrientName =
                        "energy-kcal",
                    servingQuantityGrams =
                        servingQuantityGrams
                )

            val protein =
                nutrientPerServing(
                    nutriments = nutriments,
                    nutrientName =
                        "proteins",
                    servingQuantityGrams =
                        servingQuantityGrams
                )

            val carbohydrates =
                nutrientPerServing(
                    nutriments = nutriments,
                    nutrientName =
                        "carbohydrates",
                    servingQuantityGrams =
                        servingQuantityGrams
                )

            val fat =
                nutrientPerServing(
                    nutriments = nutriments,
                    nutrientName =
                        "fat",
                    servingQuantityGrams =
                        servingQuantityGrams
                )

            /*
             * Open Food Facts provides sodium in grams.
             * Daily Rebuild stores it in milligrams.
             */
            val sodiumMilligrams =
                nutrientPerServing(
                    nutriments = nutriments,
                    nutrientName =
                        "sodium",
                    servingQuantityGrams =
                        servingQuantityGrams
                ) * 1_000.0

            FoodLookupResult.Found(
                food = ScannedFoodPrefill(
                    barcode = barcode,
                    name = productName,
                    brand = brand,

                    caloriesPerServing =
                        calories,

                    proteinGramsPerServing =
                        protein,

                    carbohydrateGramsPerServing =
                        carbohydrates,

                    fatGramsPerServing =
                        fat,

                    sodiumMilligramsPerServing =
                        sodiumMilligrams,

                    servingQuantity =
                        servingQuantity,

                    servingUnit =
                        servingUnit,

                    originalServingSize =
                        originalServingSize
                )
            )
        } catch (exception: Exception) {
            FoodLookupResult.Failed(
                message =
                    exception.message
                        ?: "Product lookup failed."
            )
        } finally {
            connection?.disconnect()
        }
    }

    /*
     * Prefer nutrition already calculated per serving.
     *
     * If it is unavailable, calculate it from the
     * per-100-gram value and serving weight.
     */
    private fun nutrientPerServing(
        nutriments: JSONObject?,
        nutrientName: String,
        servingQuantityGrams: Double?
    ): Double {
        if (nutriments == null) {
            return 0.0
        }

        val directServingValue =
            nutriments.nullableDouble(
                "${nutrientName}_serving"
            )

        if (directServingValue != null) {
            return directServingValue
        }

        val per100Grams =
            nutriments.nullableDouble(
                "${nutrientName}_100g"
            )

        if (
            per100Grams != null &&
            servingQuantityGrams != null &&
            servingQuantityGrams > 0.0
        ) {
            return per100Grams *
                    servingQuantityGrams /
                    100.0
        }

        return 0.0
    }

    /*
     * Examples this understands:
     *
     * 2 slices
     * 1 patty
     * 30 g
     * 1 sandwich
     *
     * More complicated labels remain "1 serving"
     * and can be corrected in the dialog.
     */
    private fun parseServingSize(
        servingSize: String
    ): Pair<Double, String>? {
        if (servingSize.isBlank()) {
            return null
        }

        val simplified =
            servingSize
                .substringBefore("(")
                .trim()

        val match = Regex(
            pattern =
                """^(\d+(?:\.\d+)?)\s*(.+)$"""
        ).find(simplified)
            ?: return null

        val quantity =
            match.groupValues[1]
                .toDoubleOrNull()
                ?: return null

        val unit =
            match.groupValues[2]
                .trim()
                .trim(',', ';')

        if (unit.isBlank()) {
            return null
        }

        return quantity to unit
    }

    private fun JSONObject.stringOrEmpty(
        fieldName: String
    ): String {
        if (
            !has(fieldName) ||
            isNull(fieldName)
        ) {
            return ""
        }

        return optString(fieldName)
            .trim()
    }

    private fun JSONObject.nullableDouble(
        fieldName: String
    ): Double? {
        if (
            !has(fieldName) ||
            isNull(fieldName)
        ) {
            return null
        }

        return when (
            val value = get(fieldName)
        ) {
            is Number ->
                value.toDouble()

            is String ->
                value.toDoubleOrNull()

            else ->
                null
        }
    }
}