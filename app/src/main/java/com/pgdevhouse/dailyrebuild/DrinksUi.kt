package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.DrinkCategory
import com.pgdevhouse.dailyrebuild.data.local.DrinkDefinition
import com.pgdevhouse.dailyrebuild.data.local.DrinkEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val quickWaterSizes = listOf(8.0, 12.0, 16.9, 20.0, 24.0, 32.0)
private val drinkTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

@Composable
fun DrinksAndHydrationDialog(
    date: String,
    entries: List<DrinkEntry>,
    definitions: List<DrinkDefinition>,
    isWorking: Boolean,
    onLogDefinition: (DrinkDefinition, Double) -> Unit,
    onLogCustom: (DrinkEntry, Boolean) -> Unit,
    onUpdateEntry: (DrinkEntry) -> Unit,
    onDeleteEntry: (DrinkEntry) -> Unit,
    onSaveDefinition: (DrinkDefinition) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDrinkPicker by rememberSaveable { mutableStateOf(false) }
    var showCustomDrink by rememberSaveable { mutableStateOf(false) }
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var definitionBeingEdited by remember { mutableStateOf<DrinkDefinition?>(null) }
    var entryBeingEdited by remember { mutableStateOf<DrinkEntry?>(null) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    val waterTotal = entries.filter(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
    val otherTotal = entries.filterNot(DrinkEntry::countsAsWater).sumOf(DrinkEntry::amountFlOz)
    val total = waterTotal + otherTotal
    val waterDefinition = definitions.firstOrNull {
        it.category == DrinkCategory.WATER && it.isActive
    } ?: DrinkDefinition(
        name = "Water",
        category = DrinkCategory.WATER,
        defaultAmountFlOz = 24.0,
        countsAsWater = true,
        isFavorite = true
    )

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Drinks & Hydration") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 590.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${formatDrinkAmount(total)} fl oz total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Water ${formatDrinkAmount(waterTotal)} oz · Other drinks ${formatDrinkAmount(otherTotal)} oz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()
                Text("Quick water", style = MaterialTheme.typography.titleMedium)
                quickWaterSizes.chunked(3).forEach { sizes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizes.forEach { amount ->
                            OutlinedButton(
                                onClick = { onLogDefinition(waterDefinition, amount) },
                                enabled = !isWorking,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("${formatDrinkAmount(amount)} oz")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showDrinkPicker = true },
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f)
                    ) { Text("Other Drink") }
                    OutlinedButton(
                        onClick = { showCustomDrink = true },
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f)
                    ) { Text("Custom") }
                }

                val favorites = definitions
                    .filter { it.isActive && it.isFavorite && it.category != DrinkCategory.WATER }
                    .take(6)
                if (favorites.isNotEmpty()) {
                    Text("Favorites", style = MaterialTheme.typography.titleMedium)
                    favorites.forEach { definition ->
                        DrinkShortcutRow(
                            definition = definition,
                            enabled = !isWorking,
                            onLog = { onLogDefinition(definition, definition.defaultAmountFlOz) }
                        )
                    }
                }

                TextButton(
                    onClick = { showLibrary = true },
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Manage beverage library") }

                HorizontalDivider()
                Text("Today’s drinks", style = MaterialTheme.typography.titleMedium)
                if (entries.isEmpty()) {
                    Text(
                        "No drinks recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    entries.sortedByDescending(DrinkEntry::consumedAt).forEach { entry ->
                        DrinkEntryRow(
                            entry = entry,
                            enabled = !isWorking,
                            onEdit = { entryBeingEdited = entry },
                            onDelete = { onDeleteEntry(entry) }
                        )
                    }
                    TextButton(
                        onClick = { showClearConfirmation = true },
                        enabled = !isWorking,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Clear today’s drinks", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Done") }
        }
    )

    if (showDrinkPicker) {
        DrinkPickerDialog(
            definitions = definitions.filter(DrinkDefinition::isActive),
            onSelect = { definition ->
                showDrinkPicker = false
                onLogDefinition(definition, definition.defaultAmountFlOz)
            },
            onCustom = {
                showDrinkPicker = false
                showCustomDrink = true
            },
            onDismiss = { showDrinkPicker = false }
        )
    }

    if (showCustomDrink) {
        CustomDrinkDialog(
            date = date,
            onSave = { entry, saveShortcut ->
                showCustomDrink = false
                onLogCustom(entry, saveShortcut)
            },
            onDismiss = { showCustomDrink = false }
        )
    }

    entryBeingEdited?.let { entry ->
        DrinkEntryEditorDialog(
            entry = entry,
            onSave = {
                entryBeingEdited = null
                onUpdateEntry(it)
            },
            onDismiss = { entryBeingEdited = null }
        )
    }

    if (showLibrary) {
        DrinkLibraryDialog(
            definitions = definitions,
            onAdd = {
                definitionBeingEdited = DrinkDefinition(
                    name = "",
                    category = DrinkCategory.OTHER,
                    defaultAmountFlOz = 12.0
                )
            },
            onEdit = { definitionBeingEdited = it },
            onToggleActive = {
                onSaveDefinition(it.copy(isActive = !it.isActive, updatedAt = System.currentTimeMillis()))
            },
            onDismiss = { showLibrary = false }
        )
    }

    definitionBeingEdited?.let { definition ->
        DrinkDefinitionEditorDialog(
            initial = definition,
            onSave = {
                definitionBeingEdited = null
                onSaveDefinition(it)
            },
            onDismiss = { definitionBeingEdited = null }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear today’s drinks?") },
            text = { Text("Every drink entry for this date will be removed. You can restore them immediately with Undo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearAll()
                    }
                ) { Text("Clear drinks", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DrinkShortcutRow(
    definition: DrinkDefinition,
    enabled: Boolean,
    onLog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onLog)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(definition.name, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(
                    definition.containerName,
                    "${formatDrinkAmount(definition.defaultAmountFlOz)} fl oz",
                    DrinkCategory.label(definition.category)
                ).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("Log", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DrinkEntryRow(
    entry: DrinkEntry,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.drinkNameSnapshot, fontWeight = FontWeight.SemiBold)
            Text(
                "${formatDrinkAmount(entry.amountFlOz)} fl oz · ${formatDrinkTime(entry.consumedAt)}" +
                    if (entry.calories > 0.0) " · ${formatDrinkAmount(entry.calories)} cal" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onEdit, enabled = enabled) { Text("Edit") }
        TextButton(onClick = onDelete, enabled = enabled) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DrinkPickerDialog(
    definitions: List<DrinkDefinition>,
    onSelect: (DrinkDefinition) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Drink") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                definitions.sortedWith(
                    compareByDescending<DrinkDefinition> { it.isFavorite }
                        .thenBy { it.name.lowercase(Locale.US) }
                ).forEach { definition ->
                    DrinkShortcutRow(definition, true) { onSelect(definition) }
                }
                if (definitions.isEmpty()) Text("No saved beverages yet.")
            }
        },
        confirmButton = { TextButton(onClick = onCustom) { Text("Custom drink") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomDrinkDialog(
    date: String,
    onSave: (DrinkEntry, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(DrinkCategory.OTHER) }
    var amount by rememberSaveable { mutableStateOf("12") }
    var calories by rememberSaveable { mutableStateOf("0") }
    var carbs by rememberSaveable { mutableStateOf("0") }
    var sugar by rememberSaveable { mutableStateOf("0") }
    var protein by rememberSaveable { mutableStateOf("0") }
    var caffeine by rememberSaveable { mutableStateOf("0") }
    var notes by rememberSaveable { mutableStateOf("") }
    var container by rememberSaveable { mutableStateOf("") }
    var countsAsWater by rememberSaveable { mutableStateOf(false) }
    var countsAsFood by rememberSaveable { mutableStateOf(false) }
    var saveShortcut by rememberSaveable { mutableStateOf(false) }

    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Drink") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Drink name") }, modifier = Modifier.fillMaxWidth())
                DrinkCategoryPicker(category = category, onSelect = { selected ->
                    category = selected
                    if (selected == DrinkCategory.WATER || selected == DrinkCategory.FLAVORED_WATER) countsAsWater = true
                    if (selected == DrinkCategory.PROTEIN_DRINK) countsAsFood = true
                })
                NumericDrinkField("Amount (fl oz)", amount) { amount = it }
                OutlinedTextField(container, { container = it }, label = { Text("Container name, optional") }, modifier = Modifier.fillMaxWidth())
                NumericDrinkField("Calories", calories) { calories = it }
                NumericDrinkField("Carbohydrates (g)", carbs) { carbs = it }
                NumericDrinkField("Sugar (g)", sugar) { sugar = it }
                NumericDrinkField("Protein (g)", protein) { protein = it }
                NumericDrinkField("Caffeine (mg), optional", caffeine) { caffeine = it }
                LabeledCheckbox("Count toward water total", countsAsWater) { countsAsWater = it }
                LabeledCheckbox("Can complete Food recorded", countsAsFood) { countsAsFood = it }
                LabeledCheckbox("Save as reusable shortcut", saveShortcut) { saveShortcut = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        DrinkEntry(
                            date = date,
                            consumedAt = now,
                            drinkNameSnapshot = name.trim(),
                            categorySnapshot = category,
                            amountFlOz = parsedAmount ?: 0.0,
                            calories = calories.toDoubleOrNull() ?: 0.0,
                            carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                            sugarGrams = sugar.toDoubleOrNull() ?: 0.0,
                            proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                            caffeineMilligrams = caffeine.toDoubleOrNull() ?: 0.0,
                            countsAsWater = countsAsWater,
                            countsAsFood = countsAsFood,
                            notes = buildString {
                                if (container.isNotBlank()) append("Container: ${container.trim()}")
                                if (notes.isNotBlank()) {
                                    if (isNotBlank()) append(" · ")
                                    append(notes.trim())
                                }
                            }
                        ),
                        saveShortcut
                    )
                }
            ) { Text("Log Drink") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrinkEntryEditorDialog(
    entry: DrinkEntry,
    onSave: (DrinkEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val initialTime = Instant.ofEpochMilli(entry.consumedAt).atZone(ZoneId.systemDefault())
    var name by rememberSaveable(entry.id) { mutableStateOf(entry.drinkNameSnapshot) }
    var amount by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.amountFlOz)) }
    var date by rememberSaveable(entry.id) { mutableStateOf(entry.date) }
    var time by rememberSaveable(entry.id) { mutableStateOf(initialTime.format(drinkTimeFormatter)) }
    var calories by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.calories)) }
    var carbs by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.carbohydrateGrams)) }
    var sugar by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.sugarGrams)) }
    var protein by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.proteinGrams)) }
    var caffeine by rememberSaveable(entry.id) { mutableStateOf(formatDrinkAmount(entry.caffeineMilligrams)) }
    var notes by rememberSaveable(entry.id) { mutableStateOf(entry.notes) }
    var countsAsWater by rememberSaveable(entry.id) { mutableStateOf(entry.countsAsWater) }
    var countsAsFood by rememberSaveable(entry.id) { mutableStateOf(entry.countsAsFood) }

    val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    val parsedTime = runCatching { LocalTime.parse(time.trim().uppercase(Locale.US), drinkTimeFormatter) }.getOrNull()
    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && parsedDate != null && parsedTime != null && parsedAmount != null && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Drink") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Drink name") }, modifier = Modifier.fillMaxWidth())
                NumericDrinkField("Amount (fl oz)", amount) { amount = it }
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(time, { time = it }, label = { Text("Time (for example 4:30 PM)") }, modifier = Modifier.fillMaxWidth())
                NumericDrinkField("Calories", calories) { calories = it }
                NumericDrinkField("Carbohydrates (g)", carbs) { carbs = it }
                NumericDrinkField("Sugar (g)", sugar) { sugar = it }
                NumericDrinkField("Protein (g)", protein) { protein = it }
                NumericDrinkField("Caffeine (mg)", caffeine) { caffeine = it }
                LabeledCheckbox("Count toward water total", countsAsWater) { countsAsWater = it }
                LabeledCheckbox("Can complete Food recorded", countsAsFood) { countsAsFood = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val newDate = parsedDate ?: return@TextButton
                    val newTime = parsedTime ?: return@TextButton
                    val newTimestamp = ZonedDateTime.of(newDate, newTime, ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onSave(
                        entry.copy(
                            date = newDate.toString(),
                            consumedAt = newTimestamp,
                            drinkNameSnapshot = name.trim(),
                            amountFlOz = parsedAmount ?: entry.amountFlOz,
                            calories = calories.toDoubleOrNull() ?: 0.0,
                            carbohydrateGrams = carbs.toDoubleOrNull() ?: 0.0,
                            sugarGrams = sugar.toDoubleOrNull() ?: 0.0,
                            proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                            caffeineMilligrams = caffeine.toDoubleOrNull() ?: 0.0,
                            countsAsWater = countsAsWater,
                            countsAsFood = countsAsFood,
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrinkLibraryDialog(
    definitions: List<DrinkDefinition>,
    onAdd: () -> Unit,
    onEdit: (DrinkDefinition) -> Unit,
    onToggleActive: (DrinkDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beverage Library") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                definitions.sortedWith(compareByDescending<DrinkDefinition> { it.isActive }.thenByDescending { it.isFavorite }.thenBy { it.name.lowercase(Locale.US) }).forEach { definition ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(definition.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(
                                    DrinkCategory.label(definition.category),
                                    definition.containerName,
                                    "${formatDrinkAmount(definition.defaultAmountFlOz)} fl oz",
                                    if (definition.isFavorite) "Favorite" else "",
                                    if (!definition.isActive) "Hidden" else ""
                                ).filter(String::isNotBlank).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onEdit(definition) }) { Text("Edit") }
                        TextButton(onClick = { onToggleActive(definition) }) {
                            Text(if (definition.isActive) "Hide" else "Show")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text("Add Beverage") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun DrinkDefinitionEditorDialog(
    initial: DrinkDefinition,
    onSave: (DrinkDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var category by rememberSaveable(initial.id) { mutableStateOf(initial.category) }
    var amount by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.defaultAmountFlOz)) }
    var calories by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.caloriesPerDefaultAmount)) }
    var carbs by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.carbohydrateGramsPerDefaultAmount)) }
    var sugar by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.sugarGramsPerDefaultAmount)) }
    var protein by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.proteinGramsPerDefaultAmount)) }
    var caffeine by rememberSaveable(initial.id) { mutableStateOf(formatDrinkAmount(initial.caffeineMilligramsPerDefaultAmount)) }
    var container by rememberSaveable(initial.id) { mutableStateOf(initial.containerName) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes) }
    var countsAsWater by rememberSaveable(initial.id) { mutableStateOf(initial.countsAsWater) }
    var countsAsFood by rememberSaveable(initial.id) { mutableStateOf(initial.countsAsFood) }
    var favorite by rememberSaveable(initial.id) { mutableStateOf(initial.isFavorite) }

    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add Beverage" else "Edit Beverage") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                DrinkCategoryPicker(category, { selected ->
                    category = selected
                    if (selected == DrinkCategory.WATER || selected == DrinkCategory.FLAVORED_WATER) countsAsWater = true
                    if (selected == DrinkCategory.PROTEIN_DRINK) countsAsFood = true
                })
                NumericDrinkField("Default amount (fl oz)", amount) { amount = it }
                OutlinedTextField(container, { container = it }, label = { Text("Container name") }, modifier = Modifier.fillMaxWidth())
                NumericDrinkField("Calories per default amount", calories) { calories = it }
                NumericDrinkField("Carbohydrates (g)", carbs) { carbs = it }
                NumericDrinkField("Sugar (g)", sugar) { sugar = it }
                NumericDrinkField("Protein (g)", protein) { protein = it }
                NumericDrinkField("Caffeine (mg)", caffeine) { caffeine = it }
                LabeledCheckbox("Count toward water total", countsAsWater) { countsAsWater = it }
                LabeledCheckbox("Can complete Food recorded", countsAsFood) { countsAsFood = it }
                LabeledCheckbox("Favorite shortcut", favorite) { favorite = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            category = category,
                            defaultAmountFlOz = parsedAmount ?: 12.0,
                            caloriesPerDefaultAmount = calories.toDoubleOrNull() ?: 0.0,
                            carbohydrateGramsPerDefaultAmount = carbs.toDoubleOrNull() ?: 0.0,
                            sugarGramsPerDefaultAmount = sugar.toDoubleOrNull() ?: 0.0,
                            proteinGramsPerDefaultAmount = protein.toDoubleOrNull() ?: 0.0,
                            caffeineMilligramsPerDefaultAmount = caffeine.toDoubleOrNull() ?: 0.0,
                            countsAsWater = countsAsWater,
                            countsAsFood = countsAsFood,
                            containerName = container.trim(),
                            notes = notes.trim(),
                            isFavorite = favorite,
                            updatedAt = now,
                            createdAt = initial.createdAt.takeIf { initial.id > 0L } ?: now
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrinkCategoryPicker(
    category: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(DrinkCategory.label(category))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DrinkCategory.all.forEach { option ->
                DropdownMenuItem(
                    text = { Text(DrinkCategory.label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun NumericDrinkField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            if (updated.isEmpty() || updated.matches(Regex("\\d{0,5}(\\.\\d{0,2})?"))) {
                onValueChange(updated)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun formatDrinkAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')

private fun formatDrinkTime(epochMillis: Long): String = runCatching {
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(drinkTimeFormatter)
}.getOrDefault("")
