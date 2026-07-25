package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill
import java.util.Locale

@Composable
fun SavedFoodsDialog(
    products: List<FoodProduct>,
    onDismiss: () -> Unit,
    onUseProduct: (FoodProduct) -> Unit,
    onEditProduct: (FoodProduct) -> Unit,
    onDeleteProduct: (FoodProduct) -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val visibleProducts = products.filter { product ->
        searchText.isBlank() ||
            product.name.contains(searchText, ignoreCase = true) ||
            product.brand.contains(searchText, ignoreCase = true) ||
            product.barcode.orEmpty().contains(searchText)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        RebuildStatusBadge(text = "Library · ${products.size}")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Saved foods",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Reuse, correct, or safely remove food records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search saved foods") },
                    placeholder = { Text("Name, brand, or barcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (products.isEmpty()) {
                    RebuildInsetPanel {
                        Text(
                            text = "No saved foods yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Scan or manually enter a food first. It will appear here for quick reuse.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (visibleProducts.isEmpty()) {
                    RebuildInsetPanel {
                        Text(
                            text = "No matches",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Try a different name, brand, or barcode.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = visibleProducts,
                            key = { it.id }
                        ) { product ->
                            SavedFoodRow(
                                product = product,
                                onUse = { onUseProduct(product) },
                                onEdit = { onEditProduct(product) },
                                onDelete = { onDeleteProduct(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFoodRow(
    product: FoodProduct,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (product.brand.isNotBlank()) {
                        Text(
                            text = product.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (product.isFavorite) {
                    RebuildStatusBadge(
                        text = "Favorite",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "per serving",
                    value = "${formatSavedNumber(product.caloriesPerServing)} cal",
                    modifier = Modifier.weight(1f)
                )
                RebuildMetricPill(
                    label = "serving",
                    value = "${formatSavedNumber(product.servingQuantity)} ${product.servingUnit}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (product.packageQuantity != null && !product.packageUnit.isNullOrBlank()) {
                Text(
                    text = "Package: ${formatSavedNumber(product.packageQuantity)} ${product.packageUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!product.barcode.isNullOrBlank()) {
                Text(
                    text = "Barcode ${product.barcode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildPrimaryAction(
                    text = "Use",
                    onClick = onUse,
                    modifier = Modifier.weight(1f)
                )
                RebuildSecondaryAction(
                    text = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(0.7f)
                ) { Text("Delete") }
            }
        }
    }
}

fun FoodProduct.toFoodPrefill():
    ScannedFoodPrefill {

    return ScannedFoodPrefill(
        productId = id,
        barcode = barcode,
        name = name,
        brand = brand,
        caloriesPerServing =
            caloriesPerServing,
        proteinGramsPerServing =
            proteinGramsPerServing,
        carbohydrateGramsPerServing =
            carbohydrateGramsPerServing,
        fatGramsPerServing =
            fatGramsPerServing,
        sodiumMilligramsPerServing =
            sodiumMilligramsPerServing,
        servingQuantity =
            servingQuantity,
        servingUnit =
            servingUnit,
        packageQuantity =
            packageQuantity,
        packageUnit =
            packageUnit.orEmpty(),
        isFavorite =
            isFavorite,
        originalServingSize =
            "${formatSavedNumber(servingQuantity)} " +
                servingUnit
    )
}

private fun formatSavedNumber(
    value: Double
): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.2f",
            value
        ).trimEnd('0')
            .trimEnd('.')
    }
}
