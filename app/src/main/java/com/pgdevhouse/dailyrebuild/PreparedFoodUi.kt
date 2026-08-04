package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.FoodSourceType
import com.pgdevhouse.dailyrebuild.data.local.NutritionConfidence
import com.pgdevhouse.dailyrebuild.data.local.PreparedFoodLeftover
import com.pgdevhouse.dailyrebuild.data.local.isPreparedFood
import com.pgdevhouse.dailyrebuild.domain.PreparedFoodComponent
import com.pgdevhouse.dailyrebuild.domain.PreparedFoodNutrition
import com.pgdevhouse.dailyrebuild.domain.combinePreparedFoodComponents
import com.pgdevhouse.dailyrebuild.domain.normalizedCalorieRange
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

/** Complete prepared-food entry ready for the database workflow. */
data class PreparedFoodDraft(
    val entryId: Long? = null,
    val productId: Long? = null,
    val sourceType: String,
    val sourceName: String,
    val foodName: String,
    val date: String,
    val consumedAt: Long,
    val quantityEaten: Double,
    val unit: String,
    val nutritionPerUnit: PreparedFoodNutrition,
    val nutritionConfidence: String,
    val calorieEstimateLowPerUnit: Double?,
    val calorieEstimateHighPerUnit: Double?,
    val nutritionNotes: String,
    val componentSummary: String,
    val saveAsReusable: Boolean,
    val leftoverQuantity: Double
)

data class LeftoverLogDraft(
    val leftover: PreparedFoodLeftover,
    val date: String,
    val consumedAt: Long,
    val quantityEaten: Double
)

private enum class PreparedFoodDialogPage {
    HOME,
    FORM,
    LEFTOVER
}

@Composable
fun PreparedFoodDialog(
    date: String,
    preparedFoods: List<FoodProduct>,
    leftovers: List<PreparedFoodLeftover>,
    initialEntry: FoodLogEntry? = null,
    initialProduct: FoodProduct? = null,
    isWorking: Boolean,
    onSave: (PreparedFoodDraft) -> Unit,
    onLogLeftover: (LeftoverLogDraft) -> Unit,
    onDeleteLeftover: (PreparedFoodLeftover) -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember(initialEntry?.id, initialProduct?.id) {
        mutableStateOf(
            if (initialEntry != null || initialProduct != null) {
                PreparedFoodDialogPage.FORM
            } else {
                PreparedFoodDialogPage.HOME
            }
        )
    }
    var selectedTemplate by remember(initialEntry?.id, initialProduct?.id) {
        mutableStateOf(initialProduct)
    }
    var selectedEntry by remember(initialEntry?.id) {
        mutableStateOf(initialEntry)
    }
    var selectedLeftover by remember {
        mutableStateOf<PreparedFoodLeftover?>(null)
    }
    var newSourceType by rememberSaveable {
        mutableStateOf(FoodSourceType.TAKEOUT)
    }

    when (page) {
        PreparedFoodDialogPage.HOME -> PreparedFoodHomeDialog(
            preparedFoods = preparedFoods,
            leftovers = leftovers,
            isWorking = isWorking,
            onNewPreparedFood = { sourceType ->
                newSourceType = sourceType
                selectedTemplate = null
                selectedEntry = null
                page = PreparedFoodDialogPage.FORM
            },
            onUseTemplate = { product ->
                selectedTemplate = product
                selectedEntry = null
                newSourceType = product.sourceType
                page = PreparedFoodDialogPage.FORM
            },
            onLogLeftover = { leftover ->
                selectedLeftover = leftover
                page = PreparedFoodDialogPage.LEFTOVER
            },
            onDeleteLeftover = onDeleteLeftover,
            onDismiss = onDismiss
        )

        PreparedFoodDialogPage.FORM -> key(
            selectedEntry?.id,
            selectedTemplate?.id,
            newSourceType,
            date
        ) {
            PreparedFoodFormDialog(
                date = date,
                initialSourceType = newSourceType,
                template = selectedTemplate,
                entry = selectedEntry,
                isWorking = isWorking,
                onSave = onSave,
                onBack = {
                    if (initialEntry != null || initialProduct != null) {
                        onDismiss()
                    } else {
                        page = PreparedFoodDialogPage.HOME
                    }
                },
                onDismiss = onDismiss
            )
        }

        PreparedFoodDialogPage.LEFTOVER -> {
            selectedLeftover?.let { leftover ->
                LeftoverLogDialog(
                    leftover = leftover,
                    date = date,
                    isWorking = isWorking,
                    onSave = onLogLeftover,
                    onBack = { page = PreparedFoodDialogPage.HOME },
                    onDismiss = onDismiss
                )
            } ?: PreparedFoodHomeDialog(
                preparedFoods = preparedFoods,
                leftovers = leftovers,
                isWorking = isWorking,
                onNewPreparedFood = { sourceType ->
                    newSourceType = sourceType
                    selectedTemplate = null
                    selectedEntry = null
                    page = PreparedFoodDialogPage.FORM
                },
                onUseTemplate = { product ->
                    selectedTemplate = product
                    selectedEntry = null
                    newSourceType = product.sourceType
                    page = PreparedFoodDialogPage.FORM
                },
                onLogLeftover = { leftover ->
                    selectedLeftover = leftover
                    page = PreparedFoodDialogPage.LEFTOVER
                },
                onDeleteLeftover = onDeleteLeftover,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PreparedFoodHomeDialog(
    preparedFoods: List<FoodProduct>,
    leftovers: List<PreparedFoodLeftover>,
    isWorking: Boolean,
    onNewPreparedFood: (String) -> Unit,
    onUseTemplate: (FoodProduct) -> Unit,
    onLogLeftover: (PreparedFoodLeftover) -> Unit,
    onDeleteLeftover: (PreparedFoodLeftover) -> Unit,
    onDismiss: () -> Unit
) {
    val reusablePreparedFoods = preparedFoods
        .filter { it.isReusable && it.isPreparedFood() }
        .sortedWith(
            compareByDescending<FoodProduct> { it.isFavorite }
                .thenByDescending { it.updatedAt }
        )
    val recentPreparedFoods = preparedFoods
        .filter { !it.isReusable && it.isPreparedFood() }
        .sortedByDescending(FoodProduct::updatedAt)
        .take(8)

    RebuildInputDialog(
        title = "Takeout, delivery, and prepared food",
        subtitle = "Log food without a barcode, reuse frequent orders, or finish saved leftovers.",
        onDismissRequest = onDismiss,
        primaryActionText = "New takeout",
        onPrimaryAction = { onNewPreparedFood(FoodSourceType.TAKEOUT) },
        primaryActionEnabled = !isWorking,
        secondaryActionText = "Close",
        secondaryActionEnabled = !isWorking
    ) {
        RebuildDialogInfoPanel {
            Text(
                text = "Nutrition can be exact, estimated, or unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daily Rebuild records what you ate even when a restaurant does not publish nutrition. Estimates stay visibly labeled instead of being presented as exact.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RebuildSecondaryAction(
                text = "Delivery",
                onClick = { onNewPreparedFood(FoodSourceType.DELIVERY) },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            )
            RebuildSecondaryAction(
                text = "Other food",
                onClick = { onNewPreparedFood(FoodSourceType.MANUAL) },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Available leftovers · ${leftovers.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (leftovers.isEmpty()) {
            Text(
                text = "No prepared-food leftovers are waiting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            leftovers.forEach { leftover ->
                LeftoverSummaryRow(
                    leftover = leftover,
                    enabled = !isWorking,
                    onLog = { onLogLeftover(leftover) },
                    onDelete = { onDeleteLeftover(leftover) }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Frequent orders · ${reusablePreparedFoods.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (reusablePreparedFoods.isEmpty()) {
            Text(
                text = "Save an order while logging it and it will appear here next time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            reusablePreparedFoods.take(12).forEach { product ->
                PreparedFoodTemplateRow(
                    product = product,
                    enabled = !isWorking,
                    actionLabel = "Log",
                    onLog = { onUseTemplate(product) }
                )
            }
        }

        if (recentPreparedFoods.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "Recent orders · ${recentPreparedFoods.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One-off orders remain available here without cluttering Saved Foods.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            recentPreparedFoods.forEach { product ->
                PreparedFoodTemplateRow(
                    product = product,
                    enabled = !isWorking,
                    actionLabel = "Log again",
                    onLog = { onUseTemplate(product) }
                )
            }
        }
    }
}

@Composable
private fun PreparedFoodTemplateRow(
    product: FoodProduct,
    enabled: Boolean,
    actionLabel: String,
    onLog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listOf(
                        product.sourceName,
                        FoodSourceType.label(product.sourceType),
                        NutritionConfidence.label(product.nutritionConfidence),
                        "${formatPreparedNumber(product.caloriesPerServing)} cal / ${formatPreparedNumber(product.servingQuantity)} ${product.servingUnit}"
                    ).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onLog, enabled = enabled) { Text(actionLabel) }
        }
    }
}

@Composable
private fun LeftoverSummaryRow(
    leftover: PreparedFoodLeftover,
    enabled: Boolean,
    onLog: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(leftover.foodName, fontWeight = FontWeight.SemiBold)
            Text(
                text = listOf(
                    leftover.sourceName,
                    "${formatPreparedNumber(leftover.remainingQuantity)} ${leftover.portionUnit} left",
                    NutritionConfidence.label(leftover.nutritionConfidence)
                ).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onLog, enabled = enabled) { Text("Log some") }
                TextButton(onClick = onDelete, enabled = enabled) { Text("Discard") }
            }
        }
    }
}

@Composable
private fun PreparedFoodFormDialog(
    date: String,
    initialSourceType: String,
    template: FoodProduct?,
    entry: FoodLogEntry?,
    isWorking: Boolean,
    onSave: (PreparedFoodDraft) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val sourceFromExisting = entry?.sourceTypeSnapshot
        ?.takeIf(FoodSourceType::isPrepared)
        ?: template?.sourceType
        ?: initialSourceType
    val quantityBase = entry?.quantity?.takeIf { it > 0.0 } ?: 1.0
    val entryNutritionPerUnit = entry?.let {
        PreparedFoodNutrition(
            calories = it.calories / quantityBase,
            proteinGrams = it.proteinGrams / quantityBase,
            carbohydrateGrams = it.carbohydrateGrams / quantityBase,
            fatGrams = it.fatGrams / quantityBase,
            sodiumMilligrams = it.sodiumMilligrams / quantityBase
        )
    }

    var sourceType by rememberSaveable { mutableStateOf(sourceFromExisting) }
    var sourceName by rememberSaveable {
        mutableStateOf(entry?.sourceNameSnapshot ?: template?.sourceName.orEmpty())
    }
    var foodName by rememberSaveable {
        mutableStateOf(entry?.productNameSnapshot ?: template?.name.orEmpty())
    }
    var quantityText by rememberSaveable {
        mutableStateOf(formatPreparedEditable(entry?.quantity ?: 1.0))
    }
    var unit by rememberSaveable {
        mutableStateOf(entry?.unit ?: template?.servingUnit?.takeIf(String::isNotBlank) ?: "meal")
    }
    var dateText by rememberSaveable {
        mutableStateOf(entry?.date ?: date)
    }
    var timeText by rememberSaveable {
        mutableStateOf(formatPreparedTime(entry?.createdAt ?: System.currentTimeMillis()))
    }
    var confidence by rememberSaveable {
        mutableStateOf(
            entry?.nutritionConfidenceSnapshot
                ?: template?.nutritionConfidence
                ?: NutritionConfidence.ROUGH_ESTIMATE
        )
    }

    val initialNutrition = entryNutritionPerUnit ?: PreparedFoodNutrition(
        calories = template?.caloriesPerServing ?: 0.0,
        proteinGrams = template?.proteinGramsPerServing ?: 0.0,
        carbohydrateGrams = template?.carbohydrateGramsPerServing ?: 0.0,
        fatGrams = template?.fatGramsPerServing ?: 0.0,
        sodiumMilligrams = template?.sodiumMilligramsPerServing ?: 0.0
    )
    var caloriesText by rememberSaveable { mutableStateOf(formatPreparedEditable(initialNutrition.calories)) }
    var proteinText by rememberSaveable { mutableStateOf(formatPreparedEditable(initialNutrition.proteinGrams)) }
    var carbsText by rememberSaveable { mutableStateOf(formatPreparedEditable(initialNutrition.carbohydrateGrams)) }
    var fatText by rememberSaveable { mutableStateOf(formatPreparedEditable(initialNutrition.fatGrams)) }
    var sodiumText by rememberSaveable { mutableStateOf(formatPreparedEditable(initialNutrition.sodiumMilligrams)) }
    var lowCaloriesText by rememberSaveable {
        mutableStateOf(formatPreparedEditableNullable(
            entry?.calorieEstimateLow?.let { it / quantityBase } ?: template?.calorieEstimateLow
        ))
    }
    var highCaloriesText by rememberSaveable {
        mutableStateOf(formatPreparedEditableNullable(
            entry?.calorieEstimateHigh?.let { it / quantityBase } ?: template?.calorieEstimateHigh
        ))
    }
    var notes by rememberSaveable {
        mutableStateOf(entry?.nutritionNotes ?: template?.nutritionNotes.orEmpty())
    }
    var saveAsReusable by rememberSaveable {
        mutableStateOf(template?.isReusable ?: entry?.let { false } ?: true)
    }
    var saveLeftovers by rememberSaveable { mutableStateOf(false) }
    var leftoverQuantityText by rememberSaveable { mutableStateOf("") }
    var showComponents by rememberSaveable { mutableStateOf(false) }

    val components = remember { mutableStateListOf<PreparedFoodComponent>() }
    var componentName by rememberSaveable { mutableStateOf("") }
    var componentCalories by rememberSaveable { mutableStateOf("") }
    var componentProtein by rememberSaveable { mutableStateOf("") }
    var componentCarbs by rememberSaveable { mutableStateOf("") }
    var componentFat by rememberSaveable { mutableStateOf("") }
    var componentSodium by rememberSaveable { mutableStateOf("") }

    val componentTotal = combinePreparedFoodComponents(components)
    val manualNutrition = PreparedFoodNutrition(
        calories = parsePreparedNumber(caloriesText) ?: 0.0,
        proteinGrams = parsePreparedNumber(proteinText) ?: 0.0,
        carbohydrateGrams = parsePreparedNumber(carbsText) ?: 0.0,
        fatGrams = parsePreparedNumber(fatText) ?: 0.0,
        sodiumMilligrams = parsePreparedNumber(sodiumText) ?: 0.0
    )
    val nutritionPerUnit = if (components.isNotEmpty()) componentTotal else manualNutrition
    val parsedQuantity = parsePreparedNumber(quantityText)
    val parsedLeftovers = parsePreparedNumber(leftoverQuantityText) ?: 0.0
    val parsedDate = parsePreparedDate(dateText)
    val consumedAt = parsedDate?.let { parsePreparedDateTime(it, timeText) }
    val range = normalizedCalorieRange(
        estimate = nutritionPerUnit.calories,
        low = parsePreparedNumber(lowCaloriesText),
        high = parsePreparedNumber(highCaloriesText)
    )
    val nutritionKnown = confidence != NutritionConfidence.UNKNOWN
    val formValid = foodName.isNotBlank() &&
        !unit.isBlank() &&
        parsedQuantity != null && parsedQuantity > 0.0 &&
        parsedDate != null && consumedAt != null &&
        (!saveLeftovers || parsedLeftovers > 0.0)

    RebuildInputDialog(
        title = if (entry == null) "Log prepared food" else "Edit prepared food",
        subtitle = "Record the meal first. Add as much nutrition detail as you can support.",
        onDismissRequest = { if (!isWorking) onDismiss() },
        primaryActionText = if (isWorking) "Saving…" else if (entry == null) "Log food" else "Save changes",
        primaryActionEnabled = formValid && !isWorking,
        secondaryActionText = "Close",
        secondaryActionEnabled = !isWorking,
        onPrimaryAction = {
            val safeDate = parsedDate ?: return@RebuildInputDialog
            val safeConsumedAt = consumedAt ?: return@RebuildInputDialog
            val safeQuantity = parsedQuantity ?: return@RebuildInputDialog
            onSave(
                PreparedFoodDraft(
                    entryId = entry?.id,
                    productId = template?.id ?: entry?.productId,
                    sourceType = sourceType,
                    sourceName = sourceName.trim(),
                    foodName = foodName.trim(),
                    date = safeDate.toString(),
                    consumedAt = safeConsumedAt,
                    quantityEaten = safeQuantity,
                    unit = unit.trim(),
                    nutritionPerUnit = if (nutritionKnown) nutritionPerUnit else PreparedFoodNutrition(),
                    nutritionConfidence = confidence,
                    calorieEstimateLowPerUnit = if (nutritionKnown) range.first else null,
                    calorieEstimateHighPerUnit = if (nutritionKnown) range.second else null,
                    nutritionNotes = notes.trim(),
                    componentSummary = components.joinToString("; ") { component ->
                        buildString {
                            append(component.name)
                            if (component.calories > 0.0) append(" ${formatPreparedNumber(component.calories)} cal")
                        }
                    },
                    saveAsReusable = saveAsReusable,
                    leftoverQuantity = if (saveLeftovers) parsedLeftovers else 0.0
                )
            )
        }
    ) {
        // RebuildInputDialog's secondary action always calls onDismissRequest.
        // A compact Back button inside the form preserves the hub workflow.
        if (entry == null) {
            TextButton(onClick = onBack, enabled = !isWorking) { Text("‹ Back to prepared foods") }
        }

        FormSectionTitle("Where did it come from?")
        SourceTypeSelector(selected = sourceType, onSelected = { sourceType = it })
        OutlinedTextField(
            value = sourceName,
            onValueChange = { sourceName = it },
            label = { Text(sourceLabel(sourceType)) },
            placeholder = { Text("Restaurant, store, person, or service") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Meal or item name") },
            placeholder = { Text("Cheesesteak and fries") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FormSectionTitle("Amount eaten")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreparedNumberField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = "Quantity",
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Unit") },
                placeholder = { Text("meal, slice, piece") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                isError = dateText.isNotBlank() && parsedDate == null,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it },
                label = { Text("Time") },
                placeholder = { Text("6:30 PM") },
                isError = timeText.isNotBlank() && consumedAt == null,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        FormSectionTitle("How reliable is the nutrition?")
        ConfidenceSelector(selected = confidence, onSelected = { confidence = it })
        Text(
            text = NutritionConfidence.explanation(confidence),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (confidence != NutritionConfidence.UNKNOWN) {
            RebuildDialogInfoPanel {
                Text(
                    text = "Nutrition for one $unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (components.isEmpty()) {
                        "Enter a best estimate below, or reconstruct the meal from components."
                    } else {
                        "The nutrition fields are being calculated from ${components.size} component${if (components.size == 1) "" else "s"}."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = { showComponents = !showComponents },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showComponents) "Hide component estimator" else "Build estimate from components")
            }

            if (showComponents) {
                ComponentEstimator(
                    components = components,
                    name = componentName,
                    calories = componentCalories,
                    protein = componentProtein,
                    carbs = componentCarbs,
                    fat = componentFat,
                    sodium = componentSodium,
                    onNameChange = { componentName = it },
                    onCaloriesChange = { componentCalories = it },
                    onProteinChange = { componentProtein = it },
                    onCarbsChange = { componentCarbs = it },
                    onFatChange = { componentFat = it },
                    onSodiumChange = { componentSodium = it },
                    onAdd = {
                        if (componentName.isNotBlank()) {
                            components += PreparedFoodComponent(
                                name = componentName.trim(),
                                calories = parsePreparedNumber(componentCalories) ?: 0.0,
                                proteinGrams = parsePreparedNumber(componentProtein) ?: 0.0,
                                carbohydrateGrams = parsePreparedNumber(componentCarbs) ?: 0.0,
                                fatGrams = parsePreparedNumber(componentFat) ?: 0.0,
                                sodiumMilligrams = parsePreparedNumber(componentSodium) ?: 0.0
                            )
                            componentName = ""
                            componentCalories = ""
                            componentProtein = ""
                            componentCarbs = ""
                            componentFat = ""
                            componentSodium = ""
                        }
                    },
                    onRemove = { index -> components.removeAt(index) }
                )
            }

            if (components.isEmpty()) {
                PreparedNutritionFields(
                    caloriesText = caloriesText,
                    proteinText = proteinText,
                    carbsText = carbsText,
                    fatText = fatText,
                    sodiumText = sodiumText,
                    onCaloriesChange = { caloriesText = it },
                    onProteinChange = { proteinText = it },
                    onCarbsChange = { carbsText = it },
                    onFatChange = { fatText = it },
                    onSodiumChange = { sodiumText = it }
                )
            } else {
                NutritionPreview(nutrition = componentTotal, unit = unit)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PreparedNumberField(
                    value = lowCaloriesText,
                    onValueChange = { lowCaloriesText = it },
                    label = "Likely low calories",
                    modifier = Modifier.weight(1f),
                    optional = true
                )
                PreparedNumberField(
                    value = highCaloriesText,
                    onValueChange = { highCaloriesText = it },
                    label = "Likely high calories",
                    modifier = Modifier.weight(1f),
                    optional = true
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            placeholder = { Text("Ate about 3/4, extra sauce, no fries…") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        CheckboxRow(
            checked = saveAsReusable,
            onCheckedChange = { saveAsReusable = it },
            title = "Save as a frequent food",
            supporting = "Makes this order available for one-tap reuse."
        )

        if (entry == null) {
            CheckboxRow(
                checked = saveLeftovers,
                onCheckedChange = { saveLeftovers = it },
                title = "I saved leftovers",
                supporting = "Track remaining slices, portions, or pieces separately."
            )
            if (saveLeftovers) {
                PreparedNumberField(
                    value = leftoverQuantityText,
                    onValueChange = { leftoverQuantityText = it },
                    label = "Quantity remaining ($unit)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val totalNutrition = nutritionPerUnit.scaled(parsedQuantity ?: 0.0)
        RebuildDialogInfoPanel(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ) {
            Text(
                text = "Entry preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${formatPreparedNumber(parsedQuantity ?: 0.0)} $unit")
            if (confidence == NutritionConfidence.UNKNOWN) {
                Text("Nutrition not estimated")
            } else {
                Text("${formatPreparedNumber(totalNutrition.calories)} calories")
                Text(
                    "${formatPreparedNumber(totalNutrition.proteinGrams)} g protein · " +
                        "${formatPreparedNumber(totalNutrition.carbohydrateGrams)} g carbs · " +
                        "${formatPreparedNumber(totalNutrition.fatGrams)} g fat"
                )
            }
            Text(
                text = NutritionConfidence.label(confidence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeftoverLogDialog(
    leftover: PreparedFoodLeftover,
    date: String,
    isWorking: Boolean,
    onSave: (LeftoverLogDraft) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by rememberSaveable(leftover.id) { mutableStateOf("1") }
    var dateText by rememberSaveable(leftover.id) { mutableStateOf(date) }
    var timeText by rememberSaveable(leftover.id) { mutableStateOf(formatPreparedTime(System.currentTimeMillis())) }
    val quantity = parsePreparedNumber(quantityText)
    val parsedDate = parsePreparedDate(dateText)
    val consumedAt = parsedDate?.let { parsePreparedDateTime(it, timeText) }
    val isValid = quantity != null && quantity > 0.0 &&
        quantity <= leftover.remainingQuantity + 0.0001 && consumedAt != null

    RebuildInputDialog(
        title = "Log leftovers",
        subtitle = leftover.foodName,
        onDismissRequest = { if (!isWorking) onDismiss() },
        primaryActionText = if (isWorking) "Logging…" else "Log leftovers",
        onPrimaryAction = {
            val safeQuantity = quantity ?: return@RebuildInputDialog
            val safeDate = parsedDate ?: return@RebuildInputDialog
            val safeTime = consumedAt ?: return@RebuildInputDialog
            onSave(
                LeftoverLogDraft(
                    leftover = leftover,
                    date = safeDate.toString(),
                    consumedAt = safeTime,
                    quantityEaten = safeQuantity
                )
            )
        },
        primaryActionEnabled = isValid && !isWorking,
        secondaryActionText = "Close",
        secondaryActionEnabled = !isWorking
    ) {
        TextButton(onClick = onBack, enabled = !isWorking) { Text("‹ Back to prepared foods") }
        RebuildDialogInfoPanel {
            Text(
                text = "${formatPreparedNumber(leftover.remainingQuantity)} ${leftover.portionUnit} available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = listOf(
                    leftover.sourceName,
                    NutritionConfidence.label(leftover.nutritionConfidence),
                    "${formatPreparedNumber(leftover.caloriesPerUnit)} calories per ${leftover.portionUnit}"
                ).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall
            )
        }
        PreparedNumberField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = "Quantity eaten (${leftover.portionUnit})",
            modifier = Modifier.fillMaxWidth(),
            isError = quantityText.isNotBlank() && !isValid
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Date") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = timeText,
                onValueChange = { timeText = it },
                label = { Text("Time") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SourceTypeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val types = listOf(
        FoodSourceType.TAKEOUT,
        FoodSourceType.DELIVERY,
        FoodSourceType.RESTAURANT,
        FoodSourceType.MANUAL,
        FoodSourceType.PROVIDED
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(2).forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTypes.forEach { type ->
                    FilterChip(
                        selected = selected == type,
                        onClick = { onSelected(type) },
                        label = { Text(FoodSourceType.label(type)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTypes.size == 1) {
                    Column(Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun ConfidenceSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val values = listOf(
        NutritionConfidence.EXACT,
        NutritionConfidence.GOOD_ESTIMATE,
        NutritionConfidence.ROUGH_ESTIMATE,
        NutritionConfidence.UNKNOWN
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowValues.forEach { value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelected(value) },
                        label = { Text(NutritionConfidence.label(value)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreparedNutritionFields(
    caloriesText: String,
    proteinText: String,
    carbsText: String,
    fatText: String,
    sodiumText: String,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSodiumChange: (String) -> Unit
) {
    PreparedNumberField(
        value = caloriesText,
        onValueChange = onCaloriesChange,
        label = "Calories",
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PreparedNumberField(proteinText, onProteinChange, "Protein (g)", Modifier.weight(1f), optional = true)
        PreparedNumberField(carbsText, onCarbsChange, "Carbs (g)", Modifier.weight(1f), optional = true)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PreparedNumberField(fatText, onFatChange, "Fat (g)", Modifier.weight(1f), optional = true)
        PreparedNumberField(sodiumText, onSodiumChange, "Sodium (mg)", Modifier.weight(1f), optional = true)
    }
}

@Composable
private fun ComponentEstimator(
    components: List<PreparedFoodComponent>,
    name: String,
    calories: String,
    protein: String,
    carbs: String,
    fat: String,
    sodium: String,
    onNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSodiumChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    FormSectionTitle("Component estimator")
    Text(
        text = "Add the roll, meat, cheese, sauce, fries, oil, or similar comparison items. Daily Rebuild adds them together.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Component name") },
        placeholder = { Text("Large roll, 6 oz steak, fries…") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PreparedNumberField(calories, onCaloriesChange, "Calories", Modifier.weight(1f), optional = true)
        PreparedNumberField(protein, onProteinChange, "Protein (g)", Modifier.weight(1f), optional = true)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PreparedNumberField(carbs, onCarbsChange, "Carbs (g)", Modifier.weight(1f), optional = true)
        PreparedNumberField(fat, onFatChange, "Fat (g)", Modifier.weight(1f), optional = true)
    }
    PreparedNumberField(sodium, onSodiumChange, "Sodium (mg)", Modifier.fillMaxWidth(), optional = true)
    OutlinedButton(
        onClick = onAdd,
        enabled = name.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Add component") }

    components.forEachIndexed { index, component ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(component.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatPreparedNumber(component.calories)} cal · " +
                            "${formatPreparedNumber(component.proteinGrams)} g protein · " +
                            "${formatPreparedNumber(component.fatGrams)} g fat",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { onRemove(index) }) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun NutritionPreview(
    nutrition: PreparedFoodNutrition,
    unit: String
) {
    RebuildDialogInfoPanel(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
    ) {
        Text(
            text = "Calculated for one $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text("${formatPreparedNumber(nutrition.calories)} calories")
        Text(
            "${formatPreparedNumber(nutrition.proteinGrams)} g protein · " +
                "${formatPreparedNumber(nutrition.carbohydrateGrams)} g carbs · " +
                "${formatPreparedNumber(nutrition.fatGrams)} g fat"
        )
        Text("${formatPreparedNumber(nutrition.sodiumMilligrams)} mg sodium")
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    supporting: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PreparedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    optional: Boolean = false,
    isError: Boolean = false
) {
    val parsed = parsePreparedNumber(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = if (optional) {
            { Text("Optional") }
        } else {
            null
        },
        isError = isError || (value.isNotBlank() && parsed == null),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun sourceLabel(sourceType: String): String = when (sourceType) {
    FoodSourceType.DELIVERY -> "Restaurant or delivery source"
    FoodSourceType.PROVIDED -> "Who prepared or served it?"
    FoodSourceType.MANUAL -> "Store, event, or source"
    else -> "Restaurant or source"
}

private fun parsePreparedNumber(text: String): Double? {
    val cleaned = text.trim().replace(",", "")
    if (cleaned.isBlank()) return null
    if ('/' in cleaned) {
        val parts = cleaned.split('/').map(String::trim)
        if (parts.size == 2) {
            val numerator = parts[0].toDoubleOrNull()
            val denominator = parts[1].toDoubleOrNull()
            if (numerator != null && denominator != null && abs(denominator) > 0.000001) {
                return numerator / denominator
            }
        }
    }
    return cleaned.toDoubleOrNull()
}

private fun parsePreparedDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim())
} catch (_: DateTimeParseException) {
    null
}

private fun parsePreparedDateTime(date: LocalDate, text: String): Long? {
    val normalized = text.trim().uppercase(Locale.US).replace(".", "")
    val formats = listOf(
        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
        DateTimeFormatter.ofPattern("h a", Locale.US),
        DateTimeFormatter.ofPattern("H:mm", Locale.US),
        DateTimeFormatter.ofPattern("HHmm", Locale.US)
    )
    val time = formats.firstNotNullOfOrNull { formatter ->
        runCatching { LocalTime.parse(normalized, formatter) }.getOrNull()
    } ?: return null
    return LocalDateTime.of(date, time)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun formatPreparedTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

private fun formatPreparedNumber(value: Double): String =
    if (abs(value - value.toLong()) < 0.0001) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
    }

private fun formatPreparedEditable(value: Double): String =
    if (abs(value) < 0.0001) "" else formatPreparedNumber(value)

private fun formatPreparedEditableNullable(value: Double?): String =
    value?.let(::formatPreparedEditable).orEmpty()
