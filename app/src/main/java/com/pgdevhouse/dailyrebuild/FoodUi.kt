package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.FoodSourceType
import com.pgdevhouse.dailyrebuild.data.local.NutritionConfidence
import com.pgdevhouse.dailyrebuild.data.local.isPreparedFood
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

private sealed class FuelDisplayItem {

    data class SingleFood(
        val entry: FoodLogEntry
    ) : FuelDisplayItem()

    data class LoggedMeal(
        val mealLogId: String,
        val mealName: String,
        val entries: List<FoodLogEntry>
    ) : FuelDisplayItem()
}

private fun buildFuelDisplayItems(
    entries: List<FoodLogEntry>
): List<FuelDisplayItem> {
    val items = mutableListOf<FuelDisplayItem>()
    val handledMealLogIds = mutableSetOf<String>()

    entries.forEach { entry ->
        val mealLogId = entry.mealLogId

        if (mealLogId.isNullOrBlank()) {
            items += FuelDisplayItem.SingleFood(entry)
            return@forEach
        }

        if (!handledMealLogIds.add(mealLogId)) {
            return@forEach
        }

        val mealEntries =
            entries.filter {
                it.mealLogId == mealLogId
            }

        items +=
            FuelDisplayItem.LoggedMeal(
                mealLogId = mealLogId,
                mealName =
                    entry.mealName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Saved Meal",
                entries = mealEntries
            )
    }

    return items
}

@Composable
fun FoodSection(
    entries: List<FoodLogEntry>,
    drinkCalories: Double = 0.0,
    drinkProteinGrams: Double = 0.0,
    drinkCarbohydrateGrams: Double = 0.0,
    drinkSugarGrams: Double = 0.0,
    lastScannedBarcode: String?,
    isScanningBarcode: Boolean,
    savedFoodCount: Int,
    savedMealCount: Int,
    preparedFoodCount: Int = 0,
    leftoverCount: Int = 0,
    onScanFood: () -> Unit,
    onAddFoodManually: () -> Unit,
    onOpenPreparedFood: () -> Unit,
    onOpenSavedFoods: () -> Unit,
    onBuildMeal: () -> Unit,
    onOpenSavedMeals: () -> Unit,
    onUpdateQuantity: (FoodLogEntry, Double) -> Unit,
    onEditPreparedEntry: (FoodLogEntry) -> Unit,
    onUpdateMealQuantity: (String, Double) -> Unit,
    onDeleteEntry: (FoodLogEntry) -> Unit,
    onDeleteMealLog: (String) -> Unit
) {
    val totalCalories = entries.sumOf { it.calories } + drinkCalories
    val totalProtein = entries.sumOf { it.proteinGrams } + drinkProteinGrams
    val totalCarbohydrates =
        entries.sumOf { it.carbohydrateGrams } + drinkCarbohydrateGrams
    val totalFat = entries.sumOf { it.fatGrams }
    val totalSodium = entries.sumOf { it.sodiumMilligrams }
    val unknownPreparedNutritionCount = entries.count {
        it.isPreparedFood() &&
            it.nutritionConfidenceSnapshot == NutritionConfidence.UNKNOWN
    }
    val displayItems = buildFuelDisplayItems(entries)

    var quantityEditorEntryId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var mealQuantityEditorLogId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val quantityEditorEntry =
        entries.firstOrNull {
            it.id == quantityEditorEntryId
        }
    val mealQuantityEditorEntries =
        mealQuantityEditorLogId?.let { selectedMealLogId ->
            entries.filter { it.mealLogId == selectedMealLogId }
        }.orEmpty()

    RebuildSectionCard(
        title = "Food and nutrition",
        subtitle = "Log quickly now; review the useful detail when you need it.",
        accentColor = RebuildGreen,
        trailing = {
            RebuildStatusBadge(
                text = "${formatFoodNumber(totalCalories)} cal",
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "protein",
                value = "${formatFoodNumber(totalProtein)} g",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            RebuildMetricPill(
                label = "carbs",
                value = "${formatFoodNumber(totalCarbohydrates)} g",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            RebuildMetricPill(
                label = "fat",
                value = "${formatFoodNumber(totalFat)} g",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Text(
            text = buildString {
                append("${formatFoodNumber(totalSodium)} mg sodium")
                if (drinkSugarGrams > 0.0) {
                    append(" · ${formatFoodNumber(drinkSugarGrams)} g sugar from drinks")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (unknownPreparedNutritionCount > 0) {
            Text(
                text = "$unknownPreparedNutritionCount prepared-food " +
                    if (unknownPreparedNutritionCount == 1) {
                        "entry has unknown nutrition and is not included in these totals."
                    } else {
                        "entries have unknown nutrition and are not included in these totals."
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (
            drinkCalories > 0.0 || drinkProteinGrams > 0.0 ||
            drinkCarbohydrateGrams > 0.0 || drinkSugarGrams > 0.0
        ) {
            Text(
                text = "These nutrition totals include recorded drinks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RebuildPrimaryAction(
                text = if (isScanningBarcode) "Scanning…" else "Scan barcode",
                onClick = onScanFood,
                enabled = !isScanningBarcode,
                modifier = Modifier.weight(1f)
            )
            RebuildSecondaryAction(
                text = "Add manually",
                onClick = onAddFoodManually,
                modifier = Modifier.weight(1f)
            )
        }

        RebuildPrimaryAction(
            text = buildString {
                append("Takeout / delivery")
                if (preparedFoodCount > 0 || leftoverCount > 0) {
                    append(" · $preparedFoodCount saved")
                    if (leftoverCount > 0) append(" · $leftoverCount leftover")
                }
            },
            onClick = onOpenPreparedFood,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RebuildSecondaryAction(
                text = "Foods & condiments · $savedFoodCount",
                onClick = onOpenSavedFoods,
                modifier = Modifier.weight(1f)
            )
            RebuildPrimaryAction(
                text = "Build meal",
                onClick = onBuildMeal,
                modifier = Modifier.weight(1f)
            )
        }

        RebuildSecondaryAction(
            text = "Saved meals · $savedMealCount",
            onClick = onOpenSavedMeals,
            modifier = Modifier.fillMaxWidth()
        )

        if (!lastScannedBarcode.isNullOrBlank()) {
            RebuildInsetPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Last scanned barcode",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = lastScannedBarcode,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    RebuildStatusBadge(text = "Verified")
                }
            }
        }

        if (entries.isEmpty()) {
            RebuildInsetPanel {
                Text(
                    text = "Nothing logged yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Scan a package, choose a saved food, or add a meal to begin today’s nutrition record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Today’s entries",
                    style = MaterialTheme.typography.titleMedium
                )
                displayItems.forEach { item ->
                    when (item) {
                        is FuelDisplayItem.SingleFood -> FoodEntryRow(
                            entry = item.entry,
                            onEditQuantity = {
                                if (item.entry.isPreparedFood()) {
                                    onEditPreparedEntry(item.entry)
                                } else {
                                    quantityEditorEntryId = item.entry.id
                                }
                            },
                            onDelete = { onDeleteEntry(item.entry) }
                        )
                        is FuelDisplayItem.LoggedMeal -> LoggedMealCard(
                            meal = item,
                            onEditQuantity = {
                                mealQuantityEditorLogId = item.mealLogId
                            },
                            onDeleteMeal = { onDeleteMealLog(item.mealLogId) }
                        )
                    }
                }
            }
        }
    }

    if (quantityEditorEntry != null) {
        FoodQuantityEditDialog(
            entry = quantityEditorEntry,
            onDismiss = {
                quantityEditorEntryId = null
            },
            onSave = { newQuantity ->
                quantityEditorEntryId = null
                onUpdateQuantity(
                    quantityEditorEntry,
                    newQuantity
                )
            }
        )
    }

    if (mealQuantityEditorEntries.isNotEmpty()) {
        LoggedMealQuantityEditDialog(
            mealName = mealQuantityEditorEntries.first().mealName
                ?.takeIf { it.isNotBlank() }
                ?: "Saved meal",
            currentQuantity = mealQuantityEditorEntries
                .maxOfOrNull { it.mealQuantity }
                ?.takeIf { it > 0.0 }
                ?: 1.0,
            onDismiss = { mealQuantityEditorLogId = null },
            onSave = { newQuantity ->
                val selectedMealLogId = mealQuantityEditorLogId
                mealQuantityEditorLogId = null
                selectedMealLogId?.let {
                    onUpdateMealQuantity(it, newQuantity)
                }
            }
        )
    }
}

@Composable
private fun LoggedMealCard(
    meal: FuelDisplayItem.LoggedMeal,
    onEditQuantity: () -> Unit,
    onDeleteMeal: () -> Unit
) {
    var expanded by rememberSaveable(meal.mealLogId) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(meal.mealLogId) { mutableStateOf(false) }
    val calories = meal.entries.sumOf { it.calories }
    val protein = meal.entries.sumOf { it.proteinGrams }
    val carbohydrates = meal.entries.sumOf { it.carbohydrateGrams }
    val fat = meal.entries.sumOf { it.fatGrams }
    val sodium = meal.entries.sumOf { it.sodiumMilligrams }
    val mealQuantity =
        meal.entries.maxOfOrNull { it.mealQuantity }
            ?.takeIf { it > 0.0 }
            ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = meal.mealName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text =
                            "Quantity ${formatFoodNumber(mealQuantity)} · " +
                                "${meal.entries.size} ingredients · " +
                                "${formatFoodNumber(calories)} calories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RebuildStatusBadge(
                    text = "Meal",
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "protein", value = "${formatFoodNumber(protein)} g",
                    modifier = Modifier.weight(1f)
                )
                RebuildMetricPill(
                    label = "carbs", value = "${formatFoodNumber(carbohydrates)} g",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface
                )
            }
            Text(
                text = "${formatFoodNumber(fat)} g fat · ${formatFoodNumber(sodium)} mg sodium",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                RebuildInsetPanel {
                    meal.entries.forEach { entry ->
                        MealIngredientRow(entry = entry)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildSecondaryAction(
                    text = if (expanded) "Hide ingredients" else "Show ingredients",
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onEditQuantity,
                    modifier = Modifier.weight(0.55f)
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(0.55f)
                ) {
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete logged meal?") },
            text = {
                Text(
                    "This removes every ingredient from this particular ${meal.mealName} entry. The reusable Saved Meal remains."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteMeal()
                    }
                ) { Text("Delete meal") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LoggedMealQuantityEditDialog(
    mealName: String,
    currentQuantity: Double,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var quantityText by rememberSaveable(mealName, currentQuantity) {
        mutableStateOf(currentQuantity.toEditableFoodAmount())
    }
    val parsedQuantity = parseFoodAmount(quantityText)
    val isValid = parsedQuantity != null && parsedQuantity > 0.0

    RebuildInputDialog(
        title = "Update meal quantity",
        subtitle = mealName,
        onDismissRequest = { if (!isSaving) onDismiss() },
        primaryActionText = if (isSaving) "Updating…" else "Update meal",
        onPrimaryAction = { parsedQuantity?.let(onSave) },
        primaryActionEnabled = isValid && !isSaving,
        secondaryActionEnabled = !isSaving
    ) {
        RebuildDialogInfoPanel {
            Text(
                text = "Current quantity",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatFoodNumber(currentQuantity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Every ingredient amount and nutrition total will scale together.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("New meal quantity") },
            supportingText = {
                Text(
                    if (quantityText.isBlank() || isValid) {
                        "Whole numbers, decimals, and fractions are accepted."
                    } else {
                        "Enter a quantity greater than zero."
                    }
                )
            },
            isError = quantityText.isNotBlank() && !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MealIngredientRow(
    entry: FoodLogEntry
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = entry.productNameSnapshot,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text =
                "${formatFoodNumber(entry.quantity)} ${entry.unit}  •  " +
                    "${formatFoodNumber(entry.calories)} calories",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodLogEntry,
    onEditQuantity: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (!entry.mealName.isNullOrBlank()) {
                    RebuildStatusBadge(
                        text = entry.mealName,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = entry.productNameSnapshot,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${formatFoodNumber(entry.quantity)} ${entry.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.isPreparedFood()) {
                    Text(
                        text = listOf(
                            entry.sourceNameSnapshot,
                            FoodSourceType.label(entry.sourceTypeSnapshot),
                            NutritionConfidence.label(entry.nutritionConfidenceSnapshot)
                        ).filter(String::isNotBlank).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.calorieEstimateLow != null || entry.calorieEstimateHigh != null) {
                        Text(
                            text = "Likely ${formatFoodNumber(entry.calorieEstimateLow ?: entry.calories)}–" +
                                "${formatFoodNumber(entry.calorieEstimateHigh ?: entry.calories)} calories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (
                        entry.isPreparedFood() &&
                        entry.nutritionConfidenceSnapshot == NutritionConfidence.UNKNOWN
                    ) {
                        "Nutrition not estimated"
                    } else {
                        "${formatFoodNumber(entry.calories)} calories · " +
                            "${formatFoodNumber(entry.proteinGrams)} g protein"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                TextButton(onClick = onEditQuantity) {
                    Text(if (entry.isPreparedFood()) "Edit details" else "Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}
@Composable
fun FoodQuantityEditDialog(
    entry: FoodLogEntry,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var quantityText by rememberSaveable(entry.id) {
        mutableStateOf(entry.quantity.toEditableFoodAmount())
    }

    val parsedQuantity = parseFoodAmount(quantityText)
    val isValid = parsedQuantity != null && parsedQuantity > 0.0

    RebuildInputDialog(
        title = "Update food quantity",
        subtitle = entry.productNameSnapshot,
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        primaryActionText = if (isSaving) "Updating…" else "Update quantity",
        onPrimaryAction = {
            parsedQuantity?.let(onSave)
        },
        primaryActionEnabled = isValid && !isSaving,
        secondaryActionEnabled = !isSaving
    ) {
        RebuildDialogInfoPanel {
            Text(
                text = "Current amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatFoodNumber(entry.quantity)} ${entry.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Calories and every nutrition total will be recalculated automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("New quantity") },
            suffix = { Text(entry.unit) },
            supportingText = {
                Text(
                    if (quantityText.isBlank() || isValid) {
                        "Whole numbers, decimals, and fractions are accepted."
                    } else {
                        "Enter a quantity greater than zero."
                    }
                )
            },
            isError = quantityText.isNotBlank() && !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
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
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood?.isFavorite ?: false
        )
    }

    var treatAsCondiment by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            initialFood?.isCondiment ?: false
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

    var showOptionalProductDetails by rememberSaveable(
        initialFoodKey
    ) {
        mutableStateOf(
            !initialFood?.barcode.isNullOrBlank() ||
                initialFood?.packageQuantity != null ||
                !initialFood?.packageUnit.isNullOrBlank()
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

        treatAsCondiment =
            initialFood?.isCondiment ?: false

        enterAsLabelServings =
            initialFood != null &&
                initialFood.servingQuantity < 1.0

        showOptionalProductDetails =
            !initialFood?.barcode.isNullOrBlank() ||
                initialFood?.packageQuantity != null ||
                !initialFood?.packageUnit.isNullOrBlank()

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

    val validationMessage =
        when {
            productName.isBlank() ->
                "Enter a food name."

            servingQuantity <= 0.0 ->
                "Enter a serving amount greater than zero."

            servingUnit.isBlank() ->
                "Enter the serving unit shown on the label."

            !productOnlyMode && amountEntered <= 0.0 ->
                "Enter how much you ate."

            else ->
                if (productOnlyMode) {
                    "Review the food details before saving."
                } else {
                    "Review the calculated nutrition before adding this food."
                }
        }

    val submitFood: () -> Unit = {
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
            caloriesPerServing = caloriesPerServing,
            proteinGramsPerServing = proteinPerServing,
            carbohydrateGramsPerServing = carbohydratePerServing,
            fatGramsPerServing = fatPerServing,
            sodiumMilligramsPerServing = sodiumPerServing,
            servingQuantity = servingQuantity,
            servingUnit = servingUnit.trim(),
            packageQuantity = packageQuantity,
            packageUnit =
                packageUnit
                    .trim()
                    .ifBlank {
                        null
                    },
            isFavorite = saveAsFavorite,
            isCondiment = treatAsCondiment
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
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            RebuildStatusBadge(
                                text =
                                    if (productOnlyMode) {
                                        "Saved food"
                                    } else {
                                        "Food log"
                                    },
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text =
                                    dialogTitle
                                        ?: if (productOnlyMode) {
                                            "Create or edit food"
                                        } else {
                                            "Add food"
                                        },
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Text(
                                text =
                                    if (productOnlyMode) {
                                        "Create a clean, reusable food record from the nutrition label."
                                    } else {
                                        "Enter the food, choose the amount eaten, and check the totals before adding it."
                                    },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving
                        ) {
                            Text("Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FoodFormSectionCard(
                        title = "Food details",
                        subtitle = "Start with the name. Barcode and package details are optional."
                    ) {
                        if (initialFood != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text =
                                            if (initialFood.productId != null) {
                                                "Saved food loaded"
                                            } else {
                                                "Barcode result loaded"
                                            },
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    if (barcodeText.isNotBlank()) {
                                        Text(
                                            text = "Barcode $barcodeText",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    if (initialFood.originalServingSize.isNotBlank()) {
                                        Text(
                                            text =
                                                "Source serving: " +
                                                    initialFood.originalServingSize,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        text = "Review the fields below before saving.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (onScanBarcode != null) {
                            OutlinedButton(
                                onClick = onScanBarcode,
                                enabled = !isScanningBarcode && !isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    if (isScanningBarcode) {
                                        "Opening scanner..."
                                    } else if (barcodeText.isBlank()) {
                                        "Scan a barcode"
                                    } else {
                                        "Scan a different barcode"
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = productName,
                            onValueChange = {
                                productName = it
                            },
                            label = {
                                Text("Food name")
                            },
                            placeholder = {
                                Text("Example: Mashed Potatoes")
                            },
                            supportingText = {
                                Text(
                                    if (productName.isBlank()) {
                                        "Required"
                                    } else {
                                        "This is the name shown in your food log."
                                    }
                                )
                            },
                            isError = productName.isBlank(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = brand,
                            onValueChange = {
                                brand = it
                            },
                            label = {
                                Text("Brand")
                            },
                            placeholder = {
                                Text("Optional")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = saveAsFavorite,
                                    onCheckedChange = {
                                        saveAsFavorite = it
                                    }
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Favorite food",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Keep it easy to find next time.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (treatAsCondiment) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = treatAsCondiment,
                                    onCheckedChange = {
                                        treatAsCondiment = it
                                    }
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Treat as a condiment",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Counts in nutrition totals, but stays separate from regular foods and does not complete the Food anchor by itself.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                showOptionalProductDetails =
                                    !showOptionalProductDetails
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showOptionalProductDetails) {
                                    "Hide optional product details"
                                } else {
                                    "Show barcode and package details"
                                }
                            )
                        }

                        if (showOptionalProductDetails) {
                            OutlinedTextField(
                                value = barcodeText,
                                onValueChange = { newValue ->
                                    barcodeText =
                                        newValue.filter {
                                            it.isDigit()
                                        }
                                },
                                label = {
                                    Text("Barcode")
                                },
                                placeholder = {
                                    Text("Optional")
                                },
                                supportingText = {
                                    Text("Use the digits printed under the barcode.")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Package size",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text =
                                    "Optional. Example: 12 patties. Nutrition still uses the label serving below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NumberField(
                                    value = packageQuantityText,
                                    onValueChange = {
                                        packageQuantityText = it
                                    },
                                    label = "Amount",
                                    allowFractions = true,
                                    modifier = Modifier.weight(0.4f)
                                )

                                OutlinedTextField(
                                    value = packageUnit,
                                    onValueChange = {
                                        packageUnit = it
                                    },
                                    label = {
                                        Text("Unit")
                                    },
                                    placeholder = {
                                        Text("patties")
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                    }

                    FoodFormSectionCard(
                        title = "Nutrition label",
                        subtitle = "Enter the values for one label serving. Leave an optional nutrient blank when it is unknown."
                    ) {
                        NumberField(
                            value = caloriesPerServingText,
                            onValueChange = {
                                caloriesPerServingText = it
                            },
                            label = "Calories per serving"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumberField(
                                value = proteinPerServingText,
                                onValueChange = {
                                    proteinPerServingText = it
                                },
                                label = "Protein (g)",
                                modifier = Modifier.weight(1f)
                            )

                            NumberField(
                                value = carbohydratePerServingText,
                                onValueChange = {
                                    carbohydratePerServingText = it
                                },
                                label = "Carbs (g)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumberField(
                                value = fatPerServingText,
                                onValueChange = {
                                    fatPerServingText = it
                                },
                                label = "Fat (g)",
                                modifier = Modifier.weight(1f)
                            )

                            NumberField(
                                value = sodiumPerServingText,
                                onValueChange = {
                                    sodiumPerServingText = it
                                },
                                label = "Sodium (mg)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    FoodFormSectionCard(
                        title = "Label serving size",
                        subtitle = "Match the serving written on the package. Example: 2 slices or 1/4 cup."
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumberField(
                                value = servingQuantityText,
                                onValueChange = {
                                    servingQuantityText = it
                                },
                                label = "Amount",
                                allowFractions = true,
                                supportingText =
                                    if (servingQuantity > 0.0) {
                                        null
                                    } else {
                                        "Enter a value greater than zero."
                                    },
                                isError = servingQuantity <= 0.0,
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
                                supportingText = {
                                    if (servingUnit.isBlank()) {
                                        Text("Required")
                                    }
                                },
                                isError = servingUnit.isBlank(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }

                    if (!productOnlyMode) {
                        FoodFormSectionCard(
                            title = "How much did you eat?",
                            subtitle = "Choose the entry style that matches how you measured it."
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (enterAsLabelServings) {
                                        Button(
                                            onClick = {
                                                enterAsLabelServings = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Label servings")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                enterAsLabelServings = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Label servings")
                                        }
                                    }

                                    if (!enterAsLabelServings) {
                                        Button(
                                            onClick = {
                                                enterAsLabelServings = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Measured amount")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                enterAsLabelServings = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Measured amount")
                                        }
                                    }
                                }
                            }

                            Text(
                                text =
                                    if (enterAsLabelServings) {
                                        "Enter the number of complete or partial label servings you ate."
                                    } else {
                                        "Enter the amount in ${servingUnit.ifBlank { "units" }}."
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            NumberField(
                                value = quantityEatenText,
                                onValueChange = {
                                    quantityEatenText = it
                                },
                                label =
                                    if (enterAsLabelServings) {
                                        "Servings eaten"
                                    } else {
                                        "Amount eaten (${servingUnit.ifBlank { "units" }})"
                                    },
                                allowFractions = true,
                                supportingText =
                                    if (amountEntered > 0.0) {
                                        "Fractions such as 1/2 and 1 1/2 are accepted."
                                    } else {
                                        "Enter a value greater than zero."
                                    },
                                isError = amountEntered <= 0.0
                            )

                            Text(
                                text = "Quick amount",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                foodQuickAmounts(
                                    isCondiment = treatAsCondiment,
                                    enterAsLabelServings = enterAsLabelServings,
                                    servingUnit = servingUnit
                                ).forEach { quickAmount ->
                                    OutlinedButton(
                                        onClick = {
                                            quantityEatenText = quickAmount.value
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(quickAmount.label)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = mealName,
                                onValueChange = {
                                    mealName = it
                                },
                                label = {
                                    Text("Meal label")
                                },
                                placeholder = {
                                    Text("Optional, such as Breakfast")
                                },
                                supportingText = {
                                    Text("Use this only when a label helps organize the day.")
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        FoodNutritionPreview(
                            amountDescription =
                                if (enterAsLabelServings) {
                                    "${formatFoodNumber(amountEntered)} label servings = " +
                                        "${formatFoodNumber(measuredAmountEaten)} " +
                                        servingUnit.ifBlank { "units" }
                                } else {
                                    "${formatFoodNumber(amountEntered)} " +
                                        servingUnit.ifBlank { "units" } +
                                        " = ${formatFoodNumber(servingsEaten)} label servings"
                                },
                            calories = calculatedCalories,
                            protein = calculatedProtein,
                            carbohydrates = calculatedCarbohydrates,
                            fat = calculatedFat,
                            sodium = calculatedSodium
                        )
                    } else {
                        FoodNutritionPreview(
                            title = "Saved food preview",
                            amountDescription =
                                "Per label serving: ${formatFoodNumber(servingQuantity)} " +
                                    servingUnit.ifBlank { "units" },
                            calories = caloriesPerServing,
                            protein = proteinPerServing,
                            carbohydrates = carbohydratePerServing,
                            fat = fatPerServing,
                            sodium = sodiumPerServing
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (canSave) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = submitFood,
                                enabled = canSave && !isSaving,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    if (isSaving) {
                                        "Saving..."
                                    } else if (productOnlyMode) {
                                        "Save food"
                                    } else {
                                        "Add food"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun FoodFormSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            content()
        }
    }
}

@Composable
private fun FoodNutritionPreview(
    amountDescription: String,
    calories: Double,
    protein: Double,
    carbohydrates: Double,
    fat: Double,
    sodium: Double,
    title: String = "Nutrition for this entry"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "${formatFoodNumber(calories)} calories",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = amountDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FoodPreviewMetric(
                    label = "Protein",
                    value = "${formatFoodNumber(protein)} g",
                    modifier = Modifier.weight(1f)
                )

                FoodPreviewMetric(
                    label = "Carbs",
                    value = "${formatFoodNumber(carbohydrates)} g",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FoodPreviewMetric(
                    label = "Fat",
                    value = "${formatFoodNumber(fat)} g",
                    modifier = Modifier.weight(1f)
                )

                FoodPreviewMetric(
                    label = "Sodium",
                    value = "${formatFoodNumber(sodium)} mg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FoodPreviewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    allowFractions: Boolean = false,
    supportingText: String? = null,
    isError: Boolean = false,
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

        supportingText =
            supportingText?.let { message ->
                {
                    Text(message)
                }
            },

        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),

        keyboardOptions = KeyboardOptions(
            keyboardType =
                if (allowFractions) {
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

private data class FoodQuickAmount(
    val label: String,
    val value: String
)

private fun foodQuickAmounts(
    isCondiment: Boolean,
    enterAsLabelServings: Boolean,
    servingUnit: String
): List<FoodQuickAmount> {
    if (!isCondiment || enterAsLabelServings) {
        return listOf(
            FoodQuickAmount("½", "0.5"),
            FoodQuickAmount("1", "1"),
            FoodQuickAmount("2", "2")
        )
    }

    val unit = servingUnit.trim().ifBlank { "unit" }
    val shortUnit = when (unit.lowercase(Locale.US)) {
        "teaspoon", "teaspoons" -> "tsp"
        "tablespoon", "tablespoons" -> "tbsp"
        "packet", "packets" -> "packet"
        else -> unit
    }
    return listOf(
        FoodQuickAmount("½ $shortUnit", "0.5"),
        FoodQuickAmount("1 $shortUnit", "1"),
        FoodQuickAmount("2 $shortUnit", "2")
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