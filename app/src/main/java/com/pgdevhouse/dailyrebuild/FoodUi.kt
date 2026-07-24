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
import androidx.compose.runtime.LaunchedEffect
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
import java.math.BigDecimal
import kotlin.math.abs

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
    savedFoodCount: Int,
    savedMealCount: Int,
    onScanFood: () -> Unit,
    onAddFoodManually: () -> Unit,
    onOpenSavedFoods: () -> Unit,
    onBuildMeal: () -> Unit,
    onOpenSavedMeals: () -> Unit,
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
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onOpenSavedFoods,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Saved Foods ($savedFoodCount)"
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = onBuildMeal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Build Meal")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onOpenSavedMeals,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Saved Meals ($savedMealCount)"
                )
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
    productOnlyMode: Boolean = false,
    dialogTitle: String? = null,
    isScanningBarcode: Boolean = false,
    onScanBarcode: (() -> Unit)? = null,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ManualFoodDraft) -> Unit
) {
    val initialFoodKey =
        when {
            initialFood?.productId != null ->
                "product-${initialFood.productId}"

            !initialFood?.barcode.isNullOrBlank() ->
                "barcode-${initialFood?.barcode}"

            else ->
                "new-manual-food"
        }

    var barcodeText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood?.barcode.orEmpty()
        )
    }

    var productName by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood?.name.orEmpty()
        )
    }

    var brand by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood?.brand.orEmpty()
        )
    }

    var caloriesPerServingText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.caloriesPerServing
                .toInitialFieldText()
        )
    }

    var proteinPerServingText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.proteinGramsPerServing
                .toInitialFieldText()
        )
    }

    var carbohydratePerServingText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.carbohydrateGramsPerServing
                .toInitialFieldText()
        )
    }

    var fatPerServingText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.fatGramsPerServing
                .toInitialFieldText()
        )
    }

    var sodiumPerServingText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.sodiumMilligramsPerServing
                .toInitialFieldText()
        )
    }

    var servingQuantityText by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.servingQuantity
                ?.toEditableFoodAmount()
                ?: "1"
        )
    }

    var servingUnit by rememberSaveable(
        initialFoodKey
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
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood
                ?.packageQuantity
                ?.toEditableFoodAmount()
                .orEmpty()
        )
    }

    var packageUnit by rememberSaveable(
        initialFoodKey
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

    /*
     * The food form may already be open when a barcode scan finishes.
     * In that case, rememberSaveable can retain the blank values that
     * existed before the scan. Explicitly copy every newly loaded
     * product value into the visible form whenever initialFood changes.
     */
    LaunchedEffect(initialFood) {
        barcodeText =
            initialFood?.barcode.orEmpty()

        productName =
            initialFood?.name.orEmpty()

        brand =
            initialFood?.brand.orEmpty()

        caloriesPerServingText =
            initialFood
                ?.caloriesPerServing
                .toInitialFieldText()

        proteinPerServingText =
            initialFood
                ?.proteinGramsPerServing
                .toInitialFieldText()

        carbohydratePerServingText =
            initialFood
                ?.carbohydrateGramsPerServing
                .toInitialFieldText()

        fatPerServingText =
            initialFood
                ?.fatGramsPerServing
                .toInitialFieldText()

        sodiumPerServingText =
            initialFood
                ?.sodiumMilligramsPerServing
                .toInitialFieldText()

        servingQuantityText =
            initialFood
                ?.servingQuantity
                ?.toEditableFoodAmount()
                ?: "1"

        servingUnit =
            initialFood
                ?.servingUnit
                ?.ifBlank {
                    "serving"
                }
                ?: "piece"

        packageQuantityText =
            initialFood
                ?.packageQuantity
                ?.toEditableFoodAmount()
                .orEmpty()

        packageUnit =
            initialFood
                ?.packageUnit
                .orEmpty()

        saveAsFavorite =
            initialFood?.isFavorite ?: false

        enterAsLabelServings =
            initialFood != null &&
                initialFood.servingQuantity < 1.0

        quantityEatenText = "1"

        if (productOnlyMode) {
            mealName = ""
        }
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
                (
                    productOnlyMode ||
                        amountEntered > 0.0
                    ) &&
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
                    text =
                        dialogTitle
                            ?: if (productOnlyMode) {
                                "Create Saved Food"
                            } else {
                                "Add Food Manually"
                            },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (productOnlyMode && onScanBarcode != null) {
                    Button(
                        onClick = onScanBarcode,
                        enabled =
                            !isSaving &&
                                !isScanningBarcode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text =
                                if (isScanningBarcode) {
                                    "Scanning / Looking Up..."
                                } else {
                                    "Scan New Food Barcode"
                                }
                        )
                    }

                    Text(
                        text =
                            "Scan the package to fill in available " +
                                "product information, or enter it manually.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (initialFood != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text =
                                    if (initialFood.productId != null) {
                                        "Saved Product"
                                    } else {
                                        "Scanned Product"
                                    },
                                fontWeight = FontWeight.Bold
                            )

                            if (barcodeText.isNotBlank()) {
                                Text(
                                    text =
                                        "Barcode found: $barcodeText"
                                )
                            }

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

                OutlinedTextField(
                    value = barcodeText,

                    onValueChange = { newValue ->
                        barcodeText =
                            newValue.filter {
                                it.isDigit()
                            }
                    },

                    label = {
                        Text("Barcode — optional")
                    },

                    supportingText = {
                        Text(
                            "Correct this if the scanner did not " +
                                "match the digits printed on the package."
                        )
                    },

                    singleLine = true,

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    text =
                        if (productOnlyMode) {
                            "Enter the product name and the serving shown " +
                                "on its nutrition label."
                        } else {
                            "Enter the serving shown on the nutrition label, " +
                                "then enter how much you actually ate."
                        }
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                    },
                    label = {
                        Text("Food name")
                    },
                    placeholder = {
                        Text("Example: Whole Wheat Bread")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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

                        onValueChange = { newValue ->
                            val filteredValue =
                                newValue.filter {
                                    it.isDigit() ||
                                            it == '.' ||
                                            it == '/' ||
                                            it == ' '
                                }

                            if (
                                filteredValue.count { it == '/' } <= 1 &&
                                filteredValue.count { it == '.' } <= 1
                            ) {
                                servingQuantityText = filteredValue
                            }
                        },

                        label = {
                            Text("Serving amount")
                        },

                        placeholder = {
                            Text("Example: 1/4")
                        },

                        singleLine = true,

                        keyboardOptions = KeyboardOptions(
                            /*
                             * Do not use Decimal here.
                             * URI requests a full keyboard suited to
                             * text containing characters such as "/".
                             */
                            keyboardType = KeyboardType.Uri
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

                if (!productOnlyMode) {
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

                }

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
                                id = initialFood?.productId ?: 0,
                                barcode =
                                    barcodeText
                                        .trim()
                                        .ifBlank {
                                            null
                                        },
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
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        amountEntered
                                    },

                                unit =
                                    if (productOnlyMode) {
                                        servingUnit.trim()
                                    } else {
                                        foodEntryUnit
                                    },

                                mealName =
                                    if (productOnlyMode) {
                                        null
                                    } else {
                                        mealName
                                            .trim()
                                            .ifBlank {
                                                null
                                            }
                                    },

                                calories =
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        calculatedCalories
                                    },

                                proteinGrams =
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        calculatedProtein
                                    },

                                carbohydrateGrams =
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        calculatedCarbohydrates
                                    },

                                fatGrams =
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        calculatedFat
                                    },

                                sodiumMilligrams =
                                    if (productOnlyMode) {
                                        0.0
                                    } else {
                                        calculatedSodium
                                    }
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
                            } else if (productOnlyMode) {
                                "Save Food"
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
                onValueChange(filteredValue)
            }
        },

        label = {
            Text(label)
        },

        singleLine = true,

        keyboardOptions = KeyboardOptions(
            keyboardType =
                if (allowFractions) {
                    /*
                     * Decimal keyboards usually do not
                     * provide a slash key.
                     */
                    KeyboardType.Text
                } else {
                    KeyboardType.Decimal
                }
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

private fun Double.toEditableFoodAmount():
        String {

    /*
     * Preserve common cooking fractions instead of
     * displaying rounded decimal values.
     */
    val wholeNumber =
        toInt()

    val fraction =
        this - wholeNumber

    val fractionText =
        when {
            nearlyEquals(
                fraction,
                0.125
            ) -> "1/8"

            nearlyEquals(
                fraction,
                0.25
            ) -> "1/4"

            nearlyEquals(
                fraction,
                1.0 / 3.0
            ) -> "1/3"

            nearlyEquals(
                fraction,
                0.5
            ) -> "1/2"

            nearlyEquals(
                fraction,
                2.0 / 3.0
            ) -> "2/3"

            nearlyEquals(
                fraction,
                0.75
            ) -> "3/4"

            else -> null
        }

    return when {
        fractionText != null &&
                wholeNumber > 0 -> {

            "$wholeNumber $fractionText"
        }

        fractionText != null -> {
            fractionText
        }

        else -> {
            /*
             * Avoid scientific notation and remove
             * unnecessary trailing zeroes.
             */
            BigDecimal
                .valueOf(this)
                .stripTrailingZeros()
                .toPlainString()
        }
    }
}

private fun nearlyEquals(
    first: Double,
    second: Double
): Boolean {
    return abs(
        first - second
    ) < 0.0001
}