package com.pgdevhouse.dailyrebuild.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.remote.ScannedFoodPrefill
import java.util.Locale

@Composable
fun LocalSavedFoodChoiceDialog(
    product: FoodProduct,
    isLookingUp: Boolean,
    onUseSavedFood: () -> Unit,
    onLookupAnyway: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLookingUp) onCancel()
        },
        title = { Text("Saved Food Found") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                if (product.brand.isNotBlank()) {
                    Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${formatBarcodeFoodNumber(product.servingQuantity)} ${product.servingUnit} · " +
                        "${formatBarcodeFoodNumber(product.caloriesPerServing)} calories"
                )
                Text(
                    "This barcode is already in Saved Foods. Using the local copy is faster and works offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUseSavedFood,
                enabled = !isLookingUp
            ) { Text("Use Local") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onCancel,
                    enabled = !isLookingUp
                ) { Text("Cancel") }
                TextButton(
                    onClick = onLookupAnyway,
                    enabled = !isLookingUp
                ) { Text(if (isLookingUp) "Looking Up…" else "Look Up Anyway") }
            }
        }
    )
}

@Composable
fun OnlineSavedFoodDecisionDialog(
    local: FoodProduct,
    online: ScannedFoodPrefill,
    allowOnlineOnce: Boolean,
    onUseOnlineOnce: () -> Unit,
    onUpdateSavedFood: () -> Unit,
    onKeepLocal: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Choose Which Food Data to Use") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BarcodeFoodComparison(
                    title = "Saved local copy",
                    name = local.name,
                    brand = local.brand,
                    calories = local.caloriesPerServing,
                    servingQuantity = local.servingQuantity,
                    servingUnit = local.servingUnit
                )
                HorizontalDivider()
                BarcodeFoodComparison(
                    title = "Online result",
                    name = online.name,
                    brand = online.brand,
                    calories = online.caloriesPerServing,
                    servingQuantity = online.servingQuantity,
                    servingUnit = online.servingUnit
                )
                Text(
                    if (allowOnlineOnce) {
                        "Using the online result once keeps your Saved Food unchanged. Updating replaces it only after you review and save the form."
                    } else {
                        "Meal templates use Saved Foods. Keep the local copy or update it after reviewing the online values."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (allowOnlineOnce) {
                TextButton(onClick = onUseOnlineOnce) {
                    Text("Use Online Once")
                }
            }
        },
        dismissButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onKeepLocal) { Text("Keep Local") }
                    TextButton(onClick = onUpdateSavedFood) { Text("Update Saved Food") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    )
}

@Composable
private fun BarcodeFoodComparison(
    title: String,
    name: String,
    brand: String,
    calories: Double,
    servingQuantity: Double,
    servingUnit: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(name.ifBlank { "Unnamed product" }, fontWeight = FontWeight.SemiBold)
        if (brand.isNotBlank()) {
            Text(brand, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "${formatBarcodeFoodNumber(servingQuantity)} $servingUnit · " +
                "${formatBarcodeFoodNumber(calories)} calories",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatBarcodeFoodNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
