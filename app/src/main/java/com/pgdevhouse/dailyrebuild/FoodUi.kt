package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import java.util.Locale
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill

/*
 * Information prepared by the manual food dialog.
 *
 * MainActivity saves the product first, obtains its generated ID,
 * and then creates the daily food entry.
 */
data class ManualFoodDraft(
    val product: FoodProduct,
    val quantityEaten: Double,
    val unit: String,
    val mealName: String?,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val sodiumMilligrams: Double
)

@Composable
fun FoodSection(
    entries: List<FoodLogEntry>,
    lastScannedBarcode: String?,
    isScanningBarcode: Boolean,
    onScanFood: () -> Unit,
    onAddFoodManually: () -> Unit,
    onDeleteEntry: (FoodLogEntry) -> Unit
) {
    val totalCalories =
        entries.sumOf { it.calories }

    val totalProtein =
        entries.sumOf { it.proteinGrams }

    val totalCarbohydrates =
        entries.sumOf { it.carbohydrateGrams }

    val totalFat =
        entries.sumOf { it.fatGrams }

    val totalSodium =
        entries.sumOf { it.sodiumMilligrams }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Fuel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (entries.isEmpty()) {
                Text("No food recorded yet.")
            } else {
                Text(
                    text = "${formatFoodNumber(totalCalories)} calories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "${formatFoodNumber(totalProtein)} g protein  •  " +
                                "${formatFoodNumber(totalCarbohydrates)} g carbs"
                )

                Text(
                    text =
                        "${formatFoodNumber(totalFat)} g fat  •  " +
                                "${formatFoodNumber(totalSodium)} mg sodium"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 12.dp
                    )
                )

                entries.forEachIndexed { index, entry ->

                    FoodEntryRow(
                        entry = entry,
                        onDelete = {
                            onDeleteEntry(entry)
                        }
                    )

                    if (index < entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                vertical = 8.dp
                            )
                        )
                    }
                }
            }

            if (!lastScannedBarcode.isNullOrBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 12.dp
                    )
                )

                Text(
                    text = "Last Scanned Barcode",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = lastScannedBarcode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onScanFood,
                enabled = !isScanningBarcode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isScanningBarcode) {
                        "Scanning / Looking Up..."
                    } else {
                        "Scan Food Barcode"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onAddFoodManually,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Food Manually")
            }
        }
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodLogEntry,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (!entry.mealName.isNullOrBlank()) {
                Text(
                    text = entry.mealName,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = entry.productNameSnapshot,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text =
                    "${formatFoodNumber(entry.quantity)} ${entry.unit}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "${formatFoodNumber(entry.calories)} calories • " +
                            "${formatFoodNumber(entry.proteinGrams)} g protein",
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(
            onClick = onDelete
        ) {
            Text("Delete")
        }
    }
}

@Composable
fun ManualFoodDialog(
    initialFood: ScannedFoodPrefill? = null,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ManualFoodDraft) -> Unit
) {
    var productName by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood?.name.orEmpty()
        )
    }

    var brand by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood?.brand.orEmpty()
        )
    }

    var caloriesPerServingText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.caloriesPerServing
                .toInitialFieldText()
        )
    }

    var proteinPerServingText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.proteinGramsPerServing
                .toInitialFieldText()
        )
    }

    var carbohydratePerServingText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.carbohydrateGramsPerServing
                .toInitialFieldText()
        )
    }

    var fatPerServingText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.fatGramsPerServing
                .toInitialFieldText()
        )
    }

    var sodiumPerServingText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.sodiumMilligramsPerServing
                .toInitialFieldText()
        )
    }

    var servingQuantityText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.servingQuantity
                ?.let {
                    formatFoodNumber(it)
                }
                ?: "1"
        )
    }

    var servingUnit by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.servingUnit
                ?.ifBlank {
                    "serving"
                }
                ?: "piece"
        )
    }

    var packageQuantityText by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.packageQuantity
                ?.let {
                    formatFoodNumber(it)
                }
                .orEmpty()
        )
    }

    var packageUnit by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood
                ?.packageUnit
                .orEmpty()
        )
    }

    var quantityEatenText by rememberSaveable {
        mutableStateOf("1")
    }

    var mealName by rememberSaveable {
        mutableStateOf("")
    }

    var saveAsFavorite by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            initialFood?.isFavorite ?: false
        )
    }

    var enterAsLabelServings by rememberSaveable(
        initialFood?.barcode
    ) {
        mutableStateOf(
            /*
             * Fractional serving sizes are usually easier
             * to enter as a number of servings.
             */
            initialFood != null &&
                    initialFood.servingQuantity < 1.0
        )
    }

    val caloriesPerServing =
        caloriesPerServingText.toDoubleOrNull() ?: 0.0

    val proteinPerServing =
        proteinPerServingText.toDoubleOrNull() ?: 0.0

    val carbohydratePerServing =
        carbohydratePerServingText.toDoubleOrNull() ?: 0.0

    val fatPerServing =
        fatPerServingText.toDoubleOrNull() ?: 0.0

    val sodiumPerServing =
        sodiumPerServingText.toDoubleOrNull() ?: 0.0

    val servingQuantity =
        parseFoodAmount(
            servingQuantityText
        ) ?: 0.0

    val packageQuantity =
        parseFoodAmount(
            packageQuantityText
        )

    val amountEntered =
        parseFoodAmount(
            quantityEatenText
        ) ?: 0.0

    /*
     * Label-servings mode:
     *
     * Entering 4 means four servings.
     *
     * Measured-amount mode:
     *
     * Entering 1 means one cup, slice, patty, etc.
     */
    val servingsEaten =
        if (enterAsLabelServings) {
            amountEntered
        } else if (servingQuantity > 0.0) {
            amountEntered / servingQuantity
        } else {
            0.0
        }

    val measuredAmountEaten =
        servingsEaten * servingQuantity

    val foodEntryUnit =
        if (enterAsLabelServings) {
            "servings"
        } else {
            servingUnit.trim()
        }

    val calculatedCalories =
        caloriesPerServing * servingsEaten

    val calculatedProtein =
        proteinPerServing * servingsEaten

    val calculatedCarbohydrates =
        carbohydratePerServing * servingsEaten

    val calculatedFat =
        fatPerServing * servingsEaten

    val calculatedSodium =
        sodiumPerServing * servingsEaten

    val canSave =
        productName.isNotBlank() &&
                servingUnit.isNotBlank() &&
                servingQuantity > 0.0 &&
                amountEntered > 0.0 &&
                caloriesPerServing >= 0.0 &&
                proteinPerServing >= 0.0 &&
                carbohydratePerServing >= 0.0 &&
                fatPerServing >= 0.0 &&
                sodiumPerServing >= 0.0

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Food Manually",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (initialFood != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Scanned Product",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Barcode: ${initialFood.barcode}"
                            )

                            if (
                                initialFood
                                    .originalServingSize
                                    .isNotBlank()
                            ) {
                                Text(
                                    text =
                                        "Database serving: " +
                                                initialFood
                                                    .originalServingSize
                                )
                            }

                            Text(
                                text =
                                    "Review and correct the label " +
                                            "information before adding it.",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text(
                    text =
                        "Enter the serving shown on the nutrition label, " +
                                "then enter how much you actually ate."
                )

                NumberField(
                    value = servingQuantityText,

                    onValueChange = {
                        servingQuantityText = it
                    },

                    label = "Serving amount",

                    allowFractions = true,

                    modifier = Modifier.weight(0.4f)
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = {
                        brand = it
                    },
                    label = {
                        Text("Brand — optional")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Nutrition per label serving",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                NumberField(
                    value = caloriesPerServingText,
                    onValueChange = {
                        caloriesPerServingText = it
                    },
                    label = "Calories"
                )

                NumberField(
                    value = proteinPerServingText,
                    onValueChange = {
                        proteinPerServingText = it
                    },
                    label = "Protein grams — optional"
                )

                NumberField(
                    value = carbohydratePerServingText,
                    onValueChange = {
                        carbohydratePerServingText = it
                    },
                    label = "Carbohydrate grams — optional"
                )

                NumberField(
                    value = fatPerServingText,
                    onValueChange = {
                        fatPerServingText = it
                    },
                    label = "Fat grams — optional"
                )

                NumberField(
                    value = sodiumPerServingText,
                    onValueChange = {
                        sodiumPerServingText = it
                    },
                    label = "Sodium milligrams — optional"
                )

                HorizontalDivider()

                Text(
                    text = "Label serving size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Example: if the label says 2 slices, enter " +
                                "2 and slices."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = servingQuantityText,
                        onValueChange = {
                            servingQuantityText = it
                        },
                        label = {
                            Text("Amount")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(0.4f)
                    )

                    OutlinedTextField(
                        value = servingUnit,
                        onValueChange = {
                            servingUnit = it
                        },
                        label = {
                            Text("Unit")
                        },
                        placeholder = {
                            Text("slices")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Package information — optional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "This records details such as 12 patties in a pack. " +
                                "Nutrition is still calculated from the label serving."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    NumberField(
                        value = packageQuantityText,

                        onValueChange = {
                            packageQuantityText = it
                        },

                        label = "Package amount",

                        allowFractions = true,

                        modifier = Modifier.weight(0.4f)
                    )

                    OutlinedTextField(
                        value = packageUnit,
                        onValueChange = {
                            packageUnit = it
                        },
                        label = {
                            Text("Package unit")
                        },
                        placeholder = {
                            Text("patties")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Amount eaten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "How do you want to enter this amount?"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (enterAsLabelServings) {
                        Button(
                            onClick = {
                                enterAsLabelServings = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Label Servings")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                enterAsLabelServings = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Label Servings")
                        }
                    }

                    if (!enterAsLabelServings) {
                        Button(
                            onClick = {
                                enterAsLabelServings = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Measured Amount")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                enterAsLabelServings = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Measured Amount")
                        }
                    }
                }

                Text(
                    text =
                        if (enterAsLabelServings) {
                            "Enter how many nutrition-label servings you ate."
                        } else {
                            "Enter how many $servingUnit you ate."
                        },
                    style = MaterialTheme.typography.bodySmall
                )

                NumberField(
                    value = quantityEatenText,

                    onValueChange = {
                        quantityEatenText = it
                    },

                    label =
                        if (enterAsLabelServings) {
                            "Number of label servings"
                        } else {
                            "Amount in $servingUnit"
                        },

                    allowFractions = true
                )

                OutlinedTextField(
                    value = mealName,
                    onValueChange = {
                        mealName = it
                    },
                    label = {
                        Text("Meal name — optional")
                    },
                    placeholder = {
                        Text("Four Hamburgers")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveAsFavorite,
                        onCheckedChange = {
                            saveAsFavorite = it
                        }
                    )

                    Text(
                        text = "Save as favorite food"
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Calculated amount",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                if (enterAsLabelServings) {
                                    "${formatFoodNumber(amountEntered)} " +
                                            "label servings = " +
                                            "${formatFoodNumber(measuredAmountEaten)} " +
                                            servingUnit
                                } else {
                                    "${formatFoodNumber(amountEntered)} " +
                                            "$servingUnit = " +
                                            "${formatFoodNumber(servingsEaten)} " +
                                            "label servings"
                                }
                        )

                        Text(
                            text =
                                "${formatFoodNumber(calculatedCalories)} " +
                                        "calories",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "${formatFoodNumber(calculatedProtein)} g protein  •  " +
                                        "${formatFoodNumber(calculatedCarbohydrates)} g carbs"
                        )

                        Text(
                            text =
                                "${formatFoodNumber(calculatedFat)} g fat  •  " +
                                        "${formatFoodNumber(calculatedSodium)} mg sodium"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val product = FoodProduct(
                                barcode = initialFood?.barcode,
                                name = productName.trim(),
                                brand = brand.trim(),

                                caloriesPerServing =
                                    caloriesPerServing,

                                proteinGramsPerServing =
                                    proteinPerServing,

                                carbohydrateGramsPerServing =
                                    carbohydratePerServing,

                                fatGramsPerServing =
                                    fatPerServing,

                                sodiumMilligramsPerServing =
                                    sodiumPerServing,

                                servingQuantity =
                                    servingQuantity,

                                servingUnit =
                                    servingUnit.trim(),

                                packageQuantity =
                                    packageQuantity,

                                packageUnit =
                                    packageUnit
                                        .trim()
                                        .ifBlank {
                                            null
                                        },

                                isFavorite =
                                    saveAsFavorite
                            )

                            val draft = ManualFoodDraft(
                                product = product,

                                quantityEaten =
                                    amountEntered,

                                unit =
                                    foodEntryUnit,

                                mealName =
                                    mealName
                                        .trim()
                                        .ifBlank {
                                            null
                                        },

                                calories =
                                    calculatedCalories,

                                proteinGrams =
                                    calculatedProtein,

                                carbohydrateGrams =
                                    calculatedCarbohydrates,

                                fatGrams =
                                    calculatedFat,

                                sodiumMilligrams =
                                    calculatedSodium
                            )

                            onSave(draft)
                        },
                        enabled =
                            canSave && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isSaving) {
                                "Saving..."
                            } else {
                                "Add Food"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    allowFractions: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,

        onValueChange = { newValue ->

            val filteredValue =
                if (allowFractions) {
                    newValue.filter {
                        it.isDigit() ||
                                it == '.' ||
                                it == '/' ||
                                it == ' '
                    }
                } else {
                    newValue.filter {
                        it.isDigit() ||
                                it == '.'
                    }
                }

            val decimalCount =
                filteredValue.count {
                    it == '.'
                }

            val slashCount =
                filteredValue.count {
                    it == '/'
                }

            val isAllowed =
                if (allowFractions) {
                    decimalCount <= 2 &&
                            slashCount <= 1
                } else {
                    decimalCount <= 1
                }

            if (isAllowed) {
                onValueChange(
                    filteredValue
                )
            }
        },

        label = {
            Text(label)
        },

        singleLine = true,

        keyboardOptions = KeyboardOptions(
            keyboardType =
                KeyboardType.Decimal
        ),

        modifier = modifier
    )
}

private fun parseFoodAmount(
    text: String
): Double? {
    val cleaned =
        text
            .trim()
            .replace(
                Regex("""\s+"""),
                " "
            )

    if (cleaned.isBlank()) {
        return null
    }

    /*
     * Mixed number:
     *
     * 1 1/2
     */
    val mixedNumberMatch =
        Regex(
            """^(\d+(?:\.\d+)?)\s+""" +
                    """(\d+(?:\.\d+)?)/""" +
                    """(\d+(?:\.\d+)?)$"""
        ).matchEntire(cleaned)

    if (mixedNumberMatch != null) {
        val whole =
            mixedNumberMatch
                .groupValues[1]
                .toDoubleOrNull()
                ?: return null

        val numerator =
            mixedNumberMatch
                .groupValues[2]
                .toDoubleOrNull()
                ?: return null

        val denominator =
            mixedNumberMatch
                .groupValues[3]
                .toDoubleOrNull()
                ?: return null

        if (denominator == 0.0) {
            return null
        }

        return whole +
                numerator / denominator
    }

    /*
     * Simple fraction:
     *
     * 1/4
     */
    val fractionMatch =
        Regex(
            """^(\d+(?:\.\d+)?)/""" +
                    """(\d+(?:\.\d+)?)$"""
        ).matchEntire(cleaned)

    if (fractionMatch != null) {
        val numerator =
            fractionMatch
                .groupValues[1]
                .toDoubleOrNull()
                ?: return null

        val denominator =
            fractionMatch
                .groupValues[2]
                .toDoubleOrNull()
                ?: return null

        if (denominator == 0.0) {
            return null
        }

        return numerator / denominator
    }

    return cleaned.toDoubleOrNull()
}

private fun formatFoodNumber(
    value: Double
): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            value
        )
    }
}

private fun Double?.toInitialFieldText():
        String {

    if (
        this == null ||
        this == 0.0
    ) {
        return ""
    }

    return formatFoodNumber(this)
}