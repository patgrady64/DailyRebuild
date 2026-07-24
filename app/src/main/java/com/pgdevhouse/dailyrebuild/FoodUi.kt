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

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onScanFood,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Food Barcode")
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
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ManualFoodDraft) -> Unit
) {
    var productName by rememberSaveable {
        mutableStateOf("")
    }

    var brand by rememberSaveable {
        mutableStateOf("")
    }

    var caloriesPerServingText by rememberSaveable {
        mutableStateOf("")
    }

    var proteinPerServingText by rememberSaveable {
        mutableStateOf("")
    }

    var carbohydratePerServingText by rememberSaveable {
        mutableStateOf("")
    }

    var fatPerServingText by rememberSaveable {
        mutableStateOf("")
    }

    var sodiumPerServingText by rememberSaveable {
        mutableStateOf("")
    }

    var servingQuantityText by rememberSaveable {
        mutableStateOf("1")
    }

    var servingUnit by rememberSaveable {
        mutableStateOf("piece")
    }

    var packageQuantityText by rememberSaveable {
        mutableStateOf("")
    }

    var packageUnit by rememberSaveable {
        mutableStateOf("")
    }

    var quantityEatenText by rememberSaveable {
        mutableStateOf("1")
    }

    var mealName by rememberSaveable {
        mutableStateOf("")
    }

    var saveAsFavorite by rememberSaveable {
        mutableStateOf(false)
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
        servingQuantityText.toDoubleOrNull() ?: 0.0

    val packageQuantity =
        packageQuantityText.toDoubleOrNull()

    val quantityEaten =
        quantityEatenText.toDoubleOrNull() ?: 0.0

    val servingsEaten =
        if (servingQuantity > 0.0) {
            quantityEaten / servingQuantity
        } else {
            0.0
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
                quantityEaten > 0.0 &&
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

                Text(
                    text =
                        "Enter the serving shown on the nutrition label, " +
                                "then enter how much you actually ate."
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                    },
                    label = {
                        Text("Product name")
                    },
                    placeholder = {
                        Text("Hamburger patties")
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
                    OutlinedTextField(
                        value = packageQuantityText,
                        onValueChange = {
                            packageQuantityText = it
                        },
                        label = {
                            Text("Package amount")
                        },
                        placeholder = {
                            Text("12")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        ),
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

                NumberField(
                    value = quantityEatenText,
                    onValueChange = {
                        quantityEatenText = it
                    },
                    label = "How many $servingUnit did you eat?"
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
                                "${formatFoodNumber(quantityEaten)} " +
                                        "$servingUnit = " +
                                        "${formatFoodNumber(servingsEaten)} " +
                                        "label servings"
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
                                    quantityEaten,

                                unit =
                                    servingUnit.trim(),

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
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filteredValue =
                newValue.filter {
                    it.isDigit() || it == '.'
                }

            /*
             * Prevent more than one decimal point.
             */
            if (
                filteredValue.count {
                    it == '.'
                } <= 1
            ) {
                onValueChange(filteredValue)
            }
        },
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth()
    )
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