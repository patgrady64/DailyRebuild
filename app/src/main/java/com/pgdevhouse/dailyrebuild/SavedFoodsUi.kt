package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill
import java.util.Locale

@Composable
fun SavedFoodsDialog(
    products: List<FoodProduct>,
    onDismiss: () -> Unit,
    onSelectProduct: (FoodProduct) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Saved Foods",
                            style =
                                MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "Choose a product, then enter " +
                                        "how much you ate.",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (products.isEmpty()) {
                    Text(
                        text =
                            "No foods have been saved yet. " +
                                    "Scan or manually enter a food first."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = products,
                            key = {
                                it.id
                            }
                        ) { product ->
                            SavedFoodRow(
                                product = product,
                                onSelect = {
                                    onSelectProduct(
                                        product
                                    )
                                }
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
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
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
                        text = product.name,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    if (product.brand.isNotBlank()) {
                        Text(
                            text = product.brand,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }

                TextButton(
                    onClick = onSelect
                ) {
                    Text("Use")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 8.dp
                )
            )

            Text(
                text =
                    "Serving: " +
                            "${formatSavedNumber(product.servingQuantity)} " +
                            product.servingUnit,
                style =
                    MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "${formatSavedNumber(product.caloriesPerServing)} " +
                            "calories per serving",
                style =
                    MaterialTheme.typography.bodySmall
            )

            if (
                product.packageQuantity != null &&
                !product.packageUnit.isNullOrBlank()
            ) {
                Text(
                    text =
                        "Package: " +
                                "${formatSavedNumber(product.packageQuantity)} " +
                                product.packageUnit,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            if (!product.barcode.isNullOrBlank()) {
                Text(
                    text =
                        "Barcode: ${product.barcode}",
                    style =
                        MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/*
 * Converts a reusable local product into the same
 * prefilled form used after scanning a barcode.
 */
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