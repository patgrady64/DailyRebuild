package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.PantryEssentialStatus
import com.pgdevhouse.dailyrebuild.domain.WALMART_ORDER_MINIMUM_DOLLARS
import com.pgdevhouse.dailyrebuild.domain.summarizePantryShopping
import java.util.Locale

private val pantryCategories = listOf(
    "Condiments",
    "Spices & seasonings",
    "Sweeteners",
    "Drink flavoring",
    "Cooking ingredients",
    "Meal supplies",
    "Other"
)

@Composable
fun PantryEssentialsSection(
    items: List<PantryEssential>,
    isSaving: Boolean,
    onSave: (PantryEssential) -> Unit,
    onDelete: (PantryEssential) -> Unit,
    onStatusChange: (PantryEssential, String) -> Unit
) {
    var itemBeingEdited by remember {
        mutableStateOf<PantryEssential?>(null)
    }
    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }
    var itemPendingDelete by remember {
        mutableStateOf<PantryEssential?>(null)
    }

    val needed = items.filter { it.isNeeded }
    val have = items.filterNot { it.isNeeded }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildSectionCard(
            title = "Pantry Essentials",
            subtitle = "Condiments, spices, sweeteners, drink flavoring, and supplies are tracked as Have or Need.",
            accentColor = RebuildGreen
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "need",
                    value = needed.size.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                RebuildMetricPill(
                    label = "have",
                    value = have.size.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Button(
                onClick = {
                    itemBeingEdited = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Pantry Essential")
            }
        }

        if (items.isEmpty()) {
            RebuildInsetPanel {
                Text(
                    text = "No pantry essentials yet.",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Add items such as mustard, sweetener, black pepper, drink enhancer, or cooking spray.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (needed.isNotEmpty()) {
                PantryGroup(
                    title = "Need — required on next order",
                    items = needed,
                    onEdit = {
                        itemBeingEdited = it
                        showEditor = true
                    },
                    onDelete = { itemPendingDelete = it },
                    onStatusChange = onStatusChange
                )
            }

            if (have.isNotEmpty()) {
                PantryGroup(
                    title = "Have — never added to an order",
                    items = have,
                    onEdit = {
                        itemBeingEdited = it
                        showEditor = true
                    },
                    onDelete = { itemPendingDelete = it },
                    onStatusChange = onStatusChange
                )
            }
        }
    }

    if (showEditor) {
        PantryEssentialEditorDialog(
            existing = itemBeingEdited,
            isSaving = isSaving,
            onSave = {
                onSave(it)
                showEditor = false
                itemBeingEdited = null
            },
            onDismiss = {
                if (!isSaving) {
                    showEditor = false
                    itemBeingEdited = null
                }
            }
        )
    }

    itemPendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text("Delete Pantry Essential?") },
            text = {
                Text("Delete ${item.name}? It will be removed from Have/Need tracking and future shopping lists.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item)
                        itemPendingDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PantryGroup(
    title: String,
    items: List<PantryEssential>,
    onEdit: (PantryEssential) -> Unit,
    onDelete: (PantryEssential) -> Unit,
    onStatusChange: (PantryEssential, String) -> Unit
) {
    RebuildSectionCard(
        title = title,
        accentColor = if (items.firstOrNull()?.isNeeded == true) {
            RebuildAmber
        } else {
            RebuildBlue
        }
    ) {
        items.forEachIndexed { index, item ->
            PantryEssentialRow(
                item = item,
                onEdit = { onEdit(item) },
                onDelete = { onDelete(item) },
                onStatusChange = { status ->
                    onStatusChange(item, status)
                }
            )
            if (index < items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PantryEssentialRow(
    item: PantryEssential,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buildString {
                        append(item.category)
                        item.expectedPrice?.let {
                            append(" · $")
                            append(String.format(Locale.US, "%.2f", it))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.preferredProduct.isNotBlank()) {
                    Text(
                        text = item.preferredProduct,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            RebuildStatusBadge(
                text = if (item.isNeeded) "Need" else "Have",
                backgroundColor = if (item.isNeeded) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (item.isNeeded) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onStatusChange(
                        if (item.isNeeded) PantryEssentialStatus.HAVE
                        else PantryEssentialStatus.NEED
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (item.isNeeded) "Mark Have" else "Mark Need")
            }
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun PantryEssentialEditorDialog(
    existing: PantryEssential?,
    isSaving: Boolean,
    onSave: (PantryEssential) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.name.orEmpty())
    }
    var category by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.category ?: pantryCategories.first())
    }
    var status by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.status ?: PantryEssentialStatus.HAVE)
    }
    var preferredProduct by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.preferredProduct.orEmpty())
    }
    var brandPreference by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.brandPreference ?: "Any brand")
    }
    var priceText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.expectedPrice?.toString().orEmpty())
    }
    var walmartUrl by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.walmartUrl.orEmpty())
    }
    var notes by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.notes.orEmpty())
    }

    val parsedPrice = priceText.toDoubleOrNull()
    val valid = name.isNotBlank() &&
        (priceText.isBlank() || parsedPrice != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add Pantry Essential" else "Edit Pantry Essential")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = status == PantryEssentialStatus.HAVE,
                        onClick = { status = PantryEssentialStatus.HAVE },
                        label = { Text("Have") }
                    )
                    FilterChip(
                        selected = status == PantryEssentialStatus.NEED,
                        onClick = { status = PantryEssentialStatus.NEED },
                        label = { Text("Need") }
                    )
                }

                Text("Category", fontWeight = FontWeight.SemiBold)
                pantryCategories.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = preferredProduct,
                    onValueChange = { preferredProduct = it },
                    label = { Text("Preferred Walmart product (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brandPreference,
                    onValueChange = { brandPreference = it },
                    label = { Text("Brand preference") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { value ->
                        priceText = value.filter {
                            it.isDigit() || it == '.'
                        }.take(8)
                    },
                    label = { Text("Expected price (optional)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = walmartUrl,
                    onValueChange = { walmartUrl = it },
                    label = { Text("Walmart link (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !isSaving,
                onClick = {
                    onSave(
                        PantryEssential(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            category = category,
                            status = status,
                            preferredProduct = preferredProduct.trim(),
                            brandPreference = brandPreference.trim().ifBlank { "Any brand" },
                            expectedPrice = parsedPrice,
                            walmartUrl = walmartUrl.trim(),
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun PantryShopStagingSection(
    items: List<PantryEssential>,
    onOpenPantry: () -> Unit,
    onMarkAllPurchased: () -> Unit
) {
    val shoppingSummary = summarizePantryShopping(items)
    val needed = shoppingSummary.requiredItems
    val knownTotal = shoppingSummary.knownRequiredCost
    val unknownPriceCount = shoppingSummary.unknownPriceCount

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildSectionCard(
            title = "Next Walmart Order",
            subtitle = "Every Pantry Essential marked Need is a required item. Items marked Have are never added.",
            accentColor = RebuildBlue
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label = "required",
                    value = needed.size.toString(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                RebuildMetricPill(
                    label = "known cost",
                    value = String.format(Locale.US, "$%.2f", knownTotal),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (unknownPriceCount > 0) {
                Text(
                    text = "$unknownPriceCount required item${if (unknownPriceCount == 1) " has" else "s have"} no price yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val remaining = shoppingSummary.dollarsRemainingBeforeMinimum
            Text(
                text = if (needed.isEmpty()) {
                    "No pantry essentials are currently required."
                } else if (remaining > 0.0) {
                    "Known required items leave ${String.format(Locale.US, "$%.2f", remaining)} before the ${String.format(Locale.US, "$%.0f", WALMART_ORDER_MINIMUM_DOLLARS)} order minimum. Meal foods will fill the rest after the optimizer is added."
                } else {
                    "Known required essentials already meet the $35 order minimum."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = onOpenPantry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review Pantry Essentials")
            }
        }

        if (needed.isNotEmpty()) {
            RebuildSectionCard(
                title = "Required Essentials"
            ) {
                needed.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                item.preferredProduct.ifBlank { item.category },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            item.expectedPrice?.let {
                                String.format(Locale.US, "$%.2f", it)
                            } ?: "Price needed"
                        )
                    }
                    if (index < needed.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                }

                Button(
                    onClick = onMarkAllPurchased,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Mark Required Items Purchased")
                }
            }
        }

        RebuildInsetPanel {
            Text(
                text = "Automatic $35 meal-food optimization comes next",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This phase stabilizes Food, Pantry, and Shop first. The next milestone will combine required essentials with weighted meal foods and minimize the amount over $35.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
