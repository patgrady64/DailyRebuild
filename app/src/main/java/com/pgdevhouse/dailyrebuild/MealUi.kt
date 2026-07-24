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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.MealAmountMode
import com.pgdevhouse.dailyrebuild.data.local.SavedMealWithIngredients
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.abs

data class MealIngredientDraft(
    val product: FoodProduct,
    val amountMode: String,
    val amount: Double
)

data class MealBuilderDraft(
    val name: String,
    val isFavorite: Boolean,
    val ingredients: List<MealIngredientDraft>
)

@Composable
fun MealBuilderDialog(
    products: List<FoodProduct>,
    initialMeal: SavedMealWithIngredients? = null,
    isSaving: Boolean,
    onCreateFood: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (MealBuilderDraft) -> Unit
) {
    val initialMealId =
        initialMeal?.meal?.id ?: 0L

    var mealName by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf(
            initialMeal?.meal?.name.orEmpty()
        )
    }

    var isFavorite by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf(
            initialMeal?.meal?.isFavorite
                ?: false
        )
    }

    /*
     * summary:
     * Shows the complete meal.
     *
     * choose_product:
     * Shows the Saved Foods library.
     *
     * enter_amount:
     * Asks how much of the selected food is used.
     */
    var currentPage by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf("summary")
    }

    var selectedProductId by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf<Long?>(null)
    }

    var amountMode by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf(
            MealAmountMode.MEASURED_AMOUNT
        )
    }

    var amountText by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf("1")
    }

    var editingIngredientIndex by rememberSaveable(
        initialMealId
    ) {
        mutableStateOf(-1)
    }

    /*
     * The key is only the meal ID. Updating the Saved Foods
     * list while this dialog is open must not erase the
     * ingredients already added to the draft.
     */
    val ingredients =
        remember(initialMealId) {
            mutableStateListOf<MealIngredientDraft>()
                .apply {
                    initialMeal
                        ?.ingredients
                        ?.sortedBy {
                            it.sortOrder
                        }
                        ?.mapNotNull { ingredient ->
                            products
                                .firstOrNull {
                                    it.id ==
                                        ingredient.productId
                                }
                                ?.let { product ->
                                    MealIngredientDraft(
                                        product = product,
                                        amountMode =
                                            ingredient.amountMode,
                                        amount =
                                            ingredient.amount
                                    )
                                }
                        }
                        ?.let {
                            addAll(it)
                        }
                }
        }

    val selectedProduct =
        products.firstOrNull {
            it.id == selectedProductId
        }

    val totalCalories =
        ingredients.sumOf {
            calculateIngredientCalories(it)
        }

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
            when (currentPage) {
                "choose_product" -> {
                    ChooseMealProductPage(
                        products = products,

                        onBack = {
                            currentPage = "summary"
                        },

                        onCreateFood = onCreateFood,

                        onChooseProduct = { product ->
                            selectedProductId =
                                product.id

                            amountMode =
                                if (
                                    product
                                        .servingQuantity < 1.0
                                ) {
                                    MealAmountMode
                                        .LABEL_SERVINGS
                                } else {
                                    MealAmountMode
                                        .MEASURED_AMOUNT
                                }

                            amountText =
                                if (
                                    amountMode ==
                                    MealAmountMode
                                        .LABEL_SERVINGS
                                ) {
                                    "1"
                                } else {
                                    product
                                        .servingQuantity
                                        .toMealEditableAmount()
                                }

                            editingIngredientIndex =
                                -1

                            currentPage =
                                "enter_amount"
                        }
                    )
                }

                "enter_amount" -> {
                    if (selectedProduct != null) {
                        MealIngredientAmountPage(
                            product = selectedProduct,

                            amountMode = amountMode,

                            onAmountModeChange = {
                                amountMode = it

                                amountText =
                                    if (
                                        it ==
                                        MealAmountMode
                                            .LABEL_SERVINGS
                                    ) {
                                        "1"
                                    } else {
                                        selectedProduct
                                            .servingQuantity
                                            .toMealEditableAmount()
                                    }
                            },

                            amountText = amountText,

                            onAmountTextChange = {
                                amountText = it
                            },

                            onCancel = {
                                selectedProductId =
                                    null

                                editingIngredientIndex =
                                    -1

                                currentPage =
                                    "summary"
                            },

                            onAddIngredient = {
                                val parsedAmount =
                                    parseMealAmount(
                                        amountText
                                    )

                                if (
                                    parsedAmount != null &&
                                    parsedAmount > 0.0
                                ) {
                                    val draft =
                                        MealIngredientDraft(
                                            product =
                                                selectedProduct,

                                            amountMode =
                                                amountMode,

                                            amount =
                                                parsedAmount
                                        )

                                    if (
                                        editingIngredientIndex
                                        in ingredients.indices
                                    ) {
                                        ingredients[
                                            editingIngredientIndex
                                        ] = draft
                                    } else {
                                        ingredients.add(
                                            draft
                                        )
                                    }

                                    selectedProductId =
                                        null

                                    editingIngredientIndex =
                                        -1

                                    currentPage =
                                        "summary"
                                }
                            }
                        )
                    }
                }

                else -> {
                    MealBuilderSummaryPage(
                        mealName = mealName,

                        onMealNameChange = {
                            mealName = it
                        },

                        isFavorite = isFavorite,

                        onFavoriteChange = {
                            isFavorite = it
                        },

                        ingredients = ingredients,

                        totalCalories =
                            totalCalories,

                        productsAvailable =
                            products.isNotEmpty(),

                        isEditing =
                            initialMeal != null,

                        isSaving = isSaving,

                        onAddIngredient = {
                            currentPage =
                                "choose_product"
                        },

                        onEditIngredient = { index ->
                            val ingredient =
                                ingredients[index]

                            selectedProductId =
                                ingredient.product.id

                            amountMode =
                                ingredient.amountMode

                            amountText =
                                ingredient.amount
                                    .toMealEditableAmount()

                            editingIngredientIndex =
                                index

                            currentPage =
                                "enter_amount"
                        },

                        onRemoveIngredient = { index ->
                            ingredients.removeAt(
                                index
                            )
                        },

                        onCancel = onDismiss,

                        onSave = {
                            if (
                                mealName.isNotBlank() &&
                                ingredients.isNotEmpty()
                            ) {
                                onSave(
                                    MealBuilderDraft(
                                        name =
                                            mealName.trim(),

                                        isFavorite =
                                            isFavorite,

                                        ingredients =
                                            ingredients.toList()
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MealBuilderSummaryPage(
    mealName: String,
    onMealNameChange: (String) -> Unit,
    isFavorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit,
    ingredients: List<MealIngredientDraft>,
    totalCalories: Double,
    productsAvailable: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    onAddIngredient: () -> Unit,
    onEditIngredient: (Int) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .heightIn(max = 730.dp)
    ) {
        Text(
            text =
                if (isEditing) {
                    "Edit Meal"
                } else {
                    "Build a Meal"
                },
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text =
                if (isEditing) {
                    "Update the meal name or ingredients."
                } else {
                    "Combine foods from your Saved Foods library."
                }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = mealName,

            onValueChange =
                onMealNameChange,

            label = {
                Text("Meal name")
            },

            placeholder = {
                Text("PBJ Sandwich")
            },

            singleLine = true,

            modifier =
                Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isFavorite,
                onCheckedChange =
                    onFavoriteChange
            )

            Text("Favorite meal")
        }

        HorizontalDivider()

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Ingredients",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text =
                    "${formatMealNumber(totalCalories)} calories",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (ingredients.isEmpty()) {
            Text(
                text =
                    if (productsAvailable) {
                        "No ingredients added yet."
                    } else {
                        "Create or scan at least one Saved Food before building a meal."
                    }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 330.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = ingredients.size,

                    key = { index ->
                        "$index-" +
                                ingredients[index]
                                    .product.id
                    }
                ) { index ->
                    MealIngredientRow(
                        ingredient =
                            ingredients[index],

                        onEdit = {
                            onEditIngredient(
                                index
                            )
                        },

                        onRemove = {
                            onRemoveIngredient(
                                index
                            )
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onAddIngredient,

            enabled =
                productsAvailable &&
                        !isSaving,

            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("Add Ingredient")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,

                enabled = !isSaving,

                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onSave,

                enabled =
                    mealName.isNotBlank() &&
                            ingredients.isNotEmpty() &&
                            !isSaving,

                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isSaving) {
                        "Saving..."
                    } else if (isEditing) {
                        "Update Meal"
                    } else {
                        "Save Meal"
                    }
                )
            }
        }
    }
}

@Composable
private fun MealIngredientRow(
    ingredient: MealIngredientDraft,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text =
                    ingredient.product.name,

                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    ingredientAmountDescription(
                        ingredient
                    ),

                style =
                    MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "${formatMealNumber(
                        calculateIngredientCalories(
                            ingredient
                        )
                    )} calories",

                style =
                    MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit
                ) {
                    Text("Edit")
                }

                TextButton(
                    onClick = onRemove
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun ChooseMealProductPage(
    products: List<FoodProduct>,
    onBack: () -> Unit,
    onCreateFood: () -> Unit,
    onChooseProduct: (FoodProduct) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Choose Ingredient",
                    style =
                        MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Select from Saved Foods."
                )
            }

            TextButton(
                onClick = onBack
            ) {
                Text("Back")
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onCreateFood,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Saved Food")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (products.isEmpty()) {
            Text(
                text =
                    "No Saved Foods are available."
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier.heightIn(max = 620.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = products,
                    key = {
                        it.id
                    }
                ) { product ->
                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    text =
                                        product.name,

                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    text =
                                        "Serving: " +
                                                product
                                                    .servingQuantity
                                                    .toMealEditableAmount() +
                                                " " +
                                                product
                                                    .servingUnit,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )

                                Text(
                                    text =
                                        "${formatMealNumber(
                                            product
                                                .caloriesPerServing
                                        )} calories per serving",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            TextButton(
                                onClick = {
                                    onChooseProduct(
                                        product
                                    )
                                }
                            ) {
                                Text("Choose")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealIngredientAmountPage(
    product: FoodProduct,
    amountMode: String,
    onAmountModeChange: (String) -> Unit,
    amountText: String,
    onAmountTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAddIngredient: () -> Unit
) {
    val parsedAmount =
        parseMealAmount(
            amountText
        ) ?: 0.0

    val servings =
        when (amountMode) {
            MealAmountMode.LABEL_SERVINGS ->
                parsedAmount

            else -> {
                if (
                    product.servingQuantity > 0
                ) {
                    parsedAmount /
                            product.servingQuantity
                } else {
                    0.0
                }
            }
        }

    val calories =
        product.caloriesPerServing *
                servings

    val measuredAmount =
        servings *
                product.servingQuantity

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = product.name,
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text =
                "Label serving: " +
                        product.servingQuantity
                            .toMealEditableAmount() +
                        " " +
                        product.servingUnit
        )

        Text(
            text =
                "${formatMealNumber(
                    product.caloriesPerServing
                )} calories per label serving"
        )

        HorizontalDivider()

        Text(
            text = "How are you entering it?",
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            if (
                amountMode ==
                MealAmountMode.LABEL_SERVINGS
            ) {
                Button(
                    onClick = {
                        onAmountModeChange(
                            MealAmountMode
                                .LABEL_SERVINGS
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Label Servings")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onAmountModeChange(
                            MealAmountMode
                                .LABEL_SERVINGS
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Label Servings")
                }
            }

            if (
                amountMode ==
                MealAmountMode.MEASURED_AMOUNT
            ) {
                Button(
                    onClick = {
                        onAmountModeChange(
                            MealAmountMode
                                .MEASURED_AMOUNT
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Measured Amount")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onAmountModeChange(
                            MealAmountMode
                                .MEASURED_AMOUNT
                        )
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Measured Amount")
                }
            }
        }

        OutlinedTextField(
            value = amountText,

            onValueChange = { newValue ->
                val filtered =
                    newValue.filter {
                        it.isDigit() ||
                                it == '.' ||
                                it == '/' ||
                                it == ' '
                    }

                if (
                    filtered.count {
                        it == '/'
                    } <= 1
                ) {
                    onAmountTextChange(
                        filtered
                    )
                }
            },

            label = {
                Text(
                    if (
                        amountMode ==
                        MealAmountMode.LABEL_SERVINGS
                    ) {
                        "Number of label servings"
                    } else {
                        "Amount in ${product.servingUnit}"
                    }
                )
            },

            placeholder = {
                Text("Examples: 2, 1/4, 1 1/2")
            },

            singleLine = true,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Uri
                ),

            modifier =
                Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Ingredient Total",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        if (
                            amountMode ==
                            MealAmountMode
                                .LABEL_SERVINGS
                        ) {
                            "${formatMealNumber(parsedAmount)} " +
                                    "label servings = " +
                                    "${formatMealNumber(measuredAmount)} " +
                                    product.servingUnit
                        } else {
                            "${formatMealNumber(parsedAmount)} " +
                                    product.servingUnit +
                                    " = " +
                                    "${formatMealNumber(servings)} " +
                                    "label servings"
                        }
                )

                Text(
                    text =
                        "${formatMealNumber(calories)} calories",

                    style =
                        MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onAddIngredient,

                enabled =
                    parsedAmount > 0.0,

                modifier = Modifier.weight(1f)
            ) {
                Text("Save Ingredient")
            }
        }
    }
}

@Composable
fun SavedMealsDialog(
    meals: List<SavedMealWithIngredients>,
    products: List<FoodProduct>,
    isAddingMeal: Boolean,
    onAddToToday: (
        SavedMealWithIngredients,
        Double
    ) -> Unit,
    onEdit: (SavedMealWithIngredients) -> Unit,
    onDelete: (SavedMealWithIngredients) -> Unit,
    onDismiss: () -> Unit
) {
    val productsById =
        products.associateBy {
            it.id
        }

    var mealPendingDelete by remember {
        mutableStateOf<
            SavedMealWithIngredients?
        >(null)
    }

    var mealPendingAdd by remember {
        mutableStateOf<
            SavedMealWithIngredients?
        >(null)
    }

    var mealMultiplierText by rememberSaveable {
        mutableStateOf("1")
    }

    Dialog(
        onDismissRequest = {
            if (!isAddingMeal) {
                onDismiss()
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Saved Meals",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Meal templates stored locally."
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        enabled = !isAddingMeal
                    ) {
                        Text("Close")
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (meals.isEmpty()) {
                    Text(
                        text =
                            "No saved meals yet."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = meals,
                            key = {
                                it.meal.id
                            }
                        ) { savedMeal ->
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text =
                                            savedMeal.meal.name,

                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,

                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    savedMeal.ingredients
                                        .sortedBy {
                                            it.sortOrder
                                        }
                                        .forEach {
                                                ingredient ->

                                            val product =
                                                productsById[
                                                    ingredient
                                                        .productId
                                                ]

                                            val amountText =
                                                when (
                                                    ingredient
                                                        .amountMode
                                                ) {
                                                    MealAmountMode
                                                        .LABEL_SERVINGS -> {
                                                        "${formatMealNumber(ingredient.amount)} label servings"
                                                    }

                                                    else -> {
                                                        "${formatMealNumber(ingredient.amount)} " +
                                                            (
                                                                product
                                                                    ?.servingUnit
                                                                    ?: "units"
                                                                )
                                                    }
                                                }

                                            Text(
                                                text =
                                                    "• " +
                                                        (
                                                            product
                                                                ?.name
                                                                ?: "Unknown food"
                                                            ) +
                                                        " — " +
                                                        amountText,

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall
                                            )
                                        }

                                    Spacer(
                                        modifier =
                                            Modifier.height(6.dp)
                                    )

                                    Text(
                                        text =
                                            "${savedMeal.ingredients.size} ingredients",

                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall
                                    )

                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                mealMultiplierText =
                                                    "1"
                                                mealPendingAdd =
                                                    savedMeal
                                            },
                                            enabled =
                                                !isAddingMeal
                                        ) {
                                            Text("Add to Today")
                                        }

                                        TextButton(
                                            onClick = {
                                                onEdit(
                                                    savedMeal
                                                )
                                            },
                                            enabled =
                                                !isAddingMeal
                                        ) {
                                            Text("Edit")
                                        }

                                        TextButton(
                                            onClick = {
                                                mealPendingDelete =
                                                    savedMeal
                                            },
                                            enabled =
                                                !isAddingMeal
                                        ) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingAddMeal =
        mealPendingAdd

    if (pendingAddMeal != null) {
        val multiplier =
            parseMealAmount(
                mealMultiplierText
            ) ?: 0.0

        AlertDialog(
            onDismissRequest = {
                if (!isAddingMeal) {
                    mealPendingAdd = null
                }
            },

            title = {
                Text("Add meal to today?")
            },

            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text =
                            pendingAddMeal.meal.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "How many of this meal should be added?"
                    )

                    OutlinedTextField(
                        value =
                            mealMultiplierText,

                        onValueChange = {
                            mealMultiplierText = it
                        },

                        label = {
                            Text("Meal amount")
                        },

                        supportingText = {
                            Text(
                                "Examples: 1, 2, or 1/2"
                            )
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Uri
                            ),

                        singleLine = true,
                        enabled = !isAddingMeal,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    if (
                        mealMultiplierText.isNotBlank() &&
                        multiplier <= 0.0
                    ) {
                        Text(
                            text =
                                "Enter an amount greater than zero.",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        mealPendingAdd = null
                        onAddToToday(
                            pendingAddMeal,
                            multiplier
                        )
                    },
                    enabled =
                        !isAddingMeal &&
                        multiplier > 0.0
                ) {
                    Text(
                        if (isAddingMeal) {
                            "Adding..."
                        } else {
                            "Add to Today"
                        }
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        mealPendingAdd = null
                    },
                    enabled =
                        !isAddingMeal
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val pendingMeal =
        mealPendingDelete

    if (pendingMeal != null) {
        AlertDialog(
            onDismissRequest = {
                mealPendingDelete = null
            },

            title = {
                Text("Delete saved meal?")
            },

            text = {
                Text(
                    text =
                        "\"${pendingMeal.meal.name}\" " +
                            "will be removed. Its Saved Foods " +
                            "and past daily entries will remain."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        mealPendingDelete = null
                        onDelete(pendingMeal)
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        mealPendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun calculateIngredientCalories(
    ingredient: MealIngredientDraft
): Double {
    val servings =
        when (ingredient.amountMode) {
            MealAmountMode.LABEL_SERVINGS ->
                ingredient.amount

            else -> {
                if (
                    ingredient.product
                        .servingQuantity > 0
                ) {
                    ingredient.amount /
                            ingredient.product
                                .servingQuantity
                } else {
                    0.0
                }
            }
        }

    return ingredient.product
        .caloriesPerServing * servings
}

private fun ingredientAmountDescription(
    ingredient: MealIngredientDraft
): String {
    return when (ingredient.amountMode) {
        MealAmountMode.LABEL_SERVINGS -> {
            "${formatMealNumber(ingredient.amount)} " +
                    "label servings"
        }

        else -> {
            "${formatMealNumber(ingredient.amount)} " +
                    ingredient.product.servingUnit
        }
    }
}

private fun parseMealAmount(
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

    val mixedNumber =
        Regex(
            """^(\d+(?:\.\d+)?)\s+""" +
                    """(\d+(?:\.\d+)?)/""" +
                    """(\d+(?:\.\d+)?)$"""
        ).matchEntire(cleaned)

    if (mixedNumber != null) {
        val whole =
            mixedNumber.groupValues[1]
                .toDoubleOrNull()
                ?: return null

        val numerator =
            mixedNumber.groupValues[2]
                .toDoubleOrNull()
                ?: return null

        val denominator =
            mixedNumber.groupValues[3]
                .toDoubleOrNull()
                ?: return null

        if (denominator == 0.0) {
            return null
        }

        return whole +
                numerator / denominator
    }

    val fraction =
        Regex(
            """^(\d+(?:\.\d+)?)/""" +
                    """(\d+(?:\.\d+)?)$"""
        ).matchEntire(cleaned)

    if (fraction != null) {
        val numerator =
            fraction.groupValues[1]
                .toDoubleOrNull()
                ?: return null

        val denominator =
            fraction.groupValues[2]
                .toDoubleOrNull()
                ?: return null

        if (denominator == 0.0) {
            return null
        }

        return numerator / denominator
    }

    return cleaned.toDoubleOrNull()
}

private fun Double.toMealEditableAmount():
        String {

    val whole =
        toInt()

    val fraction =
        this - whole

    val fractionText =
        when {
            nearlyEqual(
                fraction,
                0.125
            ) -> "1/8"

            nearlyEqual(
                fraction,
                0.25
            ) -> "1/4"

            nearlyEqual(
                fraction,
                1.0 / 3.0
            ) -> "1/3"

            nearlyEqual(
                fraction,
                0.5
            ) -> "1/2"

            nearlyEqual(
                fraction,
                2.0 / 3.0
            ) -> "2/3"

            nearlyEqual(
                fraction,
                0.75
            ) -> "3/4"

            else -> null
        }

    return when {
        fractionText != null &&
                whole > 0 -> {

            "$whole $fractionText"
        }

        fractionText != null ->
            fractionText

        else ->
            BigDecimal
                .valueOf(this)
                .stripTrailingZeros()
                .toPlainString()
    }
}

private fun nearlyEqual(
    first: Double,
    second: Double
): Boolean {
    return abs(
        first - second
    ) < 0.0001
}

private fun formatMealNumber(
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