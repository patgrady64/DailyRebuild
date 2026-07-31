package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pgdevhouse.dailyrebuild.data.local.CalorieGoalChange
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurementType
import com.pgdevhouse.dailyrebuild.data.local.HealthProfile
import com.pgdevhouse.dailyrebuild.data.local.MedicationEntry
import com.pgdevhouse.dailyrebuild.data.repository.DailyRebuildRepositories
import com.pgdevhouse.dailyrebuild.data.repository.FoodRepository
import com.pgdevhouse.dailyrebuild.data.repository.HealthProfileRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private data class HealthFeatureData(
    val profile: HealthProfile,
    val measurements: List<HealthMeasurement>,
    val medications: List<MedicationEntry>,
    val goalChanges: List<CalorieGoalChange>,
    val averageCalories: Int?,
    val loggedCalorieDays: Int,
    val latestWeight: Double?,
    val weeklyWeightChange: Double?
)

private val healthDisplayDateFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun HealthProfileFeature(
    repositories: DailyRebuildRepositories
) {
    val healthRepository = repositories.healthProfile
    val foodRepository = repositories.food
    val scope = rememberCoroutineScope()

    var showProfile by rememberSaveable {
        mutableStateOf(false)
    }
    var refreshKey by remember {
        mutableIntStateOf(0)
    }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var data by remember {
        mutableStateOf<HealthFeatureData?>(null)
    }

    fun reload() {
        scope.launch {
            isLoading = true
            data = loadHealthFeatureData(
                healthRepository = healthRepository,
                foodRepository = foodRepository
            )
            isLoading = false
        }
    }

    LaunchedEffect(refreshKey) {
        reload()
    }

    RebuildSectionCard(
        title = "Health profile & goals",
        subtitle =
            "Private local tracking for goals, measurements, daily highest pain, and your medication reference list.",
        accentColor = RebuildAmber
    ) {
        if (isLoading || data == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val current = data!!

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RebuildMetricPill(
                    label =
                        if (current.loggedCalorieDays == 1) {
                            "1 logged day"
                        } else {
                            "${current.loggedCalorieDays} logged days"
                        },
                    value =
                        current.averageCalories
                            ?.let { "$it avg" }
                            ?: "—",
                    modifier = Modifier.weight(1f),
                    color =
                        MaterialTheme.colorScheme.primaryContainer
                )

                RebuildMetricPill(
                    label = "calorie goal",
                    value =
                        current.profile.currentCalorieGoal
                            ?.toString()
                            ?: "Not set",
                    modifier = Modifier.weight(1f),
                    color =
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor =
                        MaterialTheme.colorScheme.onSecondaryContainer
                )

                RebuildMetricPill(
                    label = "latest weight",
                    value =
                        current.latestWeight
                            ?.let { "${formatHealthNumber(it)} lb" }
                            ?: "—",
                    modifier = Modifier.weight(1f),
                    color =
                        MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor =
                        MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            if (current.latestWeight == null) {
                RebuildInsetPanel {
                    Text(
                        text = "Weight tracking is ready whenever you get a scale.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = {
                    showProfile = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open health profile")
            }
        }
    }

    if (showProfile && data != null) {
        HealthProfileDialog(
            initialData = data!!,
            healthRepository = healthRepository,
            foodRepository = foodRepository,
            onDismiss = {
                showProfile = false
                refreshKey++
            },
            onDataChanged = {
                refreshKey++
            }
        )
    }
}

private suspend fun loadHealthFeatureData(
    healthRepository: HealthProfileRepository,
    foodRepository: FoodRepository
): HealthFeatureData {
    var profile = healthRepository.getProfile()
        ?: HealthProfile().also {
            healthRepository.saveProfile(it)
        }

    if (!profile.medicationImportCompleted) {
        if (healthRepository.countMedications() == 0) {
            healthRepository.saveMedications(
                defaultMedicationEntries()
            )
        }

        profile = profile.copy(
            medicationImportCompleted = true,
            updatedAt = System.currentTimeMillis()
        )
        healthRepository.saveProfile(profile)
    }

    val measurements = healthRepository.getAllMeasurements()
    val medications = healthRepository.getMedications()
    val goalChanges = healthRepository.getCalorieGoalChanges()

    var totalCalories = 0.0
    var loggedDays = 0
    val today = LocalDate.now()

    for (offset in 0L until 7L) {
        val date = today.minusDays(offset).toString()
        val entries = foodRepository.getEntriesForDate(date)

        if (entries.isNotEmpty()) {
            loggedDays++
            totalCalories += entries.sumOf { it.calories }
        }
    }

    val averageCalories =
        if (loggedDays > 0) {
            (totalCalories / loggedDays).roundToInt()
        } else {
            null
        }

    val weights = measurements.filter {
        it.type == HealthMeasurementType.WEIGHT
    }
    val latestWeight = weights.firstOrNull()?.primaryValue
    val weeklyWeightChange = calculateWeeklyWeightChange(weights)

    return HealthFeatureData(
        profile = profile,
        measurements = measurements,
        medications = medications,
        goalChanges = goalChanges,
        averageCalories = averageCalories,
        loggedCalorieDays = loggedDays,
        latestWeight = latestWeight,
        weeklyWeightChange = weeklyWeightChange
    )
}

private fun calculateWeeklyWeightChange(
    weights: List<HealthMeasurement>
): Double? {
    val latest = weights.firstOrNull() ?: return null
    val latestDate = runCatching {
        LocalDate.parse(latest.recordedDate)
    }.getOrNull() ?: return null

    val comparison = weights
        .drop(1)
        .filter {
            val date = runCatching {
                LocalDate.parse(it.recordedDate)
            }.getOrNull()

            date != null &&
                !date.isBefore(latestDate.minusDays(7)) &&
                !date.isAfter(latestDate)
        }
        .lastOrNull()
        ?: return null

    return latest.primaryValue - comparison.primaryValue
}

@Composable
private fun HealthProfileDialog(
    initialData: HealthFeatureData,
    healthRepository: HealthProfileRepository,
    foodRepository: FoodRepository,
    onDismiss: () -> Unit,
    onDataChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var data by remember {
        mutableStateOf(initialData)
    }
    var isWorking by remember {
        mutableStateOf(false)
    }
    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showMeasurementType by remember {
        mutableStateOf<String?>(null)
    }
    var medicationBeingEdited by remember {
        mutableStateOf<MedicationEntry?>(null)
    }
    var showMedicationEditor by remember {
        mutableStateOf(false)
    }
    var medicationPendingDelete by remember {
        mutableStateOf<MedicationEntry?>(null)
    }
    var measurementPendingDelete by remember {
        mutableStateOf<HealthMeasurement?>(null)
    }

    var birthDate by rememberSaveable {
        mutableStateOf(initialData.profile.birthDate)
    }
    var heightInches by rememberSaveable {
        mutableStateOf(initialData.profile.heightInches.toString())
    }
    var startingWeight by rememberSaveable {
        mutableStateOf(
            initialData.profile.approximateStartingWeightPounds
                ?.let(::formatHealthNumber)
                ?: ""
        )
    }
    var goalWeight by rememberSaveable {
        mutableStateOf(
            initialData.profile.weightGoalPounds
                ?.let(::formatHealthNumber)
                ?: ""
        )
    }
    var conditionsSummary by rememberSaveable {
        mutableStateOf(initialData.profile.conditionsSummary)
    }
    var foodConstraints by rememberSaveable {
        mutableStateOf(initialData.profile.foodConstraints)
    }
    var movementLimitations by rememberSaveable {
        mutableStateOf(initialData.profile.movementLimitations)
    }
    var seatedDefault by rememberSaveable {
        mutableStateOf(initialData.profile.mobilitySeatedDefault)
    }
    var bedDefault by rememberSaveable {
        mutableStateOf(initialData.profile.mobilityBedDefault)
    }
    var floorDefault by rememberSaveable {
        mutableStateOf(initialData.profile.mobilityFloorDefault)
    }
    var standingDefault by rememberSaveable {
        mutableStateOf(initialData.profile.mobilityStandingDefault)
    }

    fun reload(message: String? = null) {
        scope.launch {
            isWorking = true
            data = loadHealthFeatureData(
                healthRepository = healthRepository,
                foodRepository = foodRepository
            )
            statusMessage = message
            isWorking = false
            onDataChanged()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Health profile & goals",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Stored only in Daily Rebuild on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    statusMessage?.let {
                        RebuildInsetPanel {
                            Text(it)
                        }
                    }

                    ProfileEditorSection(
                        birthDate = birthDate,
                        onBirthDateChange = { birthDate = it },
                        heightInches = heightInches,
                        onHeightInchesChange = { heightInches = it },
                        startingWeight = startingWeight,
                        onStartingWeightChange = { startingWeight = it },
                        goalWeight = goalWeight,
                        onGoalWeightChange = { goalWeight = it },
                        conditionsSummary = conditionsSummary,
                        onConditionsSummaryChange = { conditionsSummary = it },
                        foodConstraints = foodConstraints,
                        onFoodConstraintsChange = { foodConstraints = it },
                        movementLimitations = movementLimitations,
                        onMovementLimitationsChange = { movementLimitations = it },
                        seatedDefault = seatedDefault,
                        onSeatedDefaultChange = { seatedDefault = it },
                        bedDefault = bedDefault,
                        onBedDefaultChange = { bedDefault = it },
                        floorDefault = floorDefault,
                        onFloorDefaultChange = { floorDefault = it },
                        standingDefault = standingDefault,
                        onStandingDefaultChange = { standingDefault = it },
                        isWorking = isWorking,
                        onSave = {
                            val parsedHeight = heightInches.toIntOrNull()
                            val parsedBirthDate = runCatching {
                                LocalDate.parse(birthDate)
                            }.getOrNull()

                            if (parsedHeight == null || parsedHeight <= 0) {
                                statusMessage = "Enter height as total inches."
                            } else if (parsedBirthDate == null) {
                                statusMessage = "Use YYYY-MM-DD for the birth date."
                            } else {
                                scope.launch {
                                    isWorking = true
                                    healthRepository.saveProfile(
                                        data.profile.copy(
                                            birthDate = birthDate,
                                            heightInches = parsedHeight,
                                            approximateStartingWeightPounds =
                                                startingWeight.toDoubleOrNull(),
                                            weightGoalPounds =
                                                goalWeight.toDoubleOrNull(),
                                            conditionsSummary = conditionsSummary.trim(),
                                            foodConstraints = foodConstraints.trim(),
                                            movementLimitations = movementLimitations.trim(),
                                            mobilitySeatedDefault = seatedDefault,
                                            mobilityBedDefault = bedDefault,
                                            mobilityFloorDefault = floorDefault,
                                            mobilityStandingDefault = standingDefault,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                    isWorking = false
                                    reload("Profile saved.")
                                }
                            }
                        }
                    )

                    CalorieGoalSection(
                        averageCalories = data.averageCalories,
                        loggedDays = data.loggedCalorieDays,
                        currentGoal = data.profile.currentCalorieGoal,
                        goalChanges = data.goalChanges,
                        isWorking = isWorking,
                        onSetGoal = { newGoal, reason ->
                            if (newGoal <= 0) {
                                statusMessage = "The calorie goal must be greater than zero."
                            } else {
                                scope.launch {
                                    isWorking = true
                                    val previous = data.profile.currentCalorieGoal
                                    healthRepository.saveProfile(
                                        data.profile.copy(
                                            currentCalorieGoal = newGoal,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                    healthRepository.addCalorieGoalChange(
                                        CalorieGoalChange(
                                            changedDate = LocalDate.now().toString(),
                                            previousGoal = previous,
                                            newGoal = newGoal,
                                            reason = reason
                                        )
                                    )
                                    isWorking = false
                                    reload("Calorie goal updated in a recorded step.")
                                }
                            }
                        }
                    )

                    MeasurementSection(
                        measurements = data.measurements,
                        latestWeight = data.latestWeight,
                        weeklyWeightChange = data.weeklyWeightChange,
                        onAddMeasurement = {
                            showMeasurementType = it
                        },
                        onDeleteMeasurement = {
                            measurementPendingDelete = it
                        }
                    )

                    RebuildSectionCard(
                        title = "Daily Highest Pain",
                        subtitle = "Pain is now managed as one daily value: the highest pain experienced that day.",
                        accentColor = RebuildAmber
                    ) {
                        Text(
                            text = "Use Highest Pain from Home or the Health quick-log area. If pain increases later, update the same daily value. The former before-and-after activity form is no longer part of the interface.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MedicationReferenceSection(
                        medications = data.medications,
                        onAdd = {
                            medicationBeingEdited = null
                            showMedicationEditor = true
                        },
                        onEdit = {
                            medicationBeingEdited = it
                            showMedicationEditor = true
                        },
                        onDelete = {
                            medicationPendingDelete = it
                        }
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    showMeasurementType?.let { type ->
        MeasurementEditorDialog(
            type = type,
            onDismiss = {
                showMeasurementType = null
            },
            onSave = { measurement ->
                scope.launch {
                    healthRepository.addMeasurement(measurement)
                    showMeasurementType = null
                    reload("Measurement saved.")
                }
            }
        )
    }

    if (showMedicationEditor) {
        MedicationEditorDialog(
            initial = medicationBeingEdited,
            onDismiss = {
                showMedicationEditor = false
                medicationBeingEdited = null
            },
            onSave = { medication ->
                scope.launch {
                    healthRepository.saveMedication(medication)
                    showMedicationEditor = false
                    medicationBeingEdited = null
                    reload("Medication reference saved.")
                }
            }
        )
    }

    medicationPendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = {
                medicationPendingDelete = null
            },
            title = {
                Text("Delete medication reference?")
            },
            text = {
                Text(
                    "This only deletes the local reference entry. It does not advise stopping or changing anything."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            healthRepository.deleteMedication(medication)
                            medicationPendingDelete = null
                            reload("Medication reference deleted.")
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        medicationPendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    measurementPendingDelete?.let { measurement ->
        AlertDialog(
            onDismissRequest = {
                measurementPendingDelete = null
            },
            title = {
                Text("Delete measurement?")
            },
            text = {
                Text("This removes the selected local measurement.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            healthRepository.deleteMeasurement(measurement)
                            measurementPendingDelete = null
                            reload("Measurement deleted.")
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        measurementPendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


}

@Composable
private fun ProfileEditorSection(
    birthDate: String,
    onBirthDateChange: (String) -> Unit,
    heightInches: String,
    onHeightInchesChange: (String) -> Unit,
    startingWeight: String,
    onStartingWeightChange: (String) -> Unit,
    goalWeight: String,
    onGoalWeightChange: (String) -> Unit,
    conditionsSummary: String,
    onConditionsSummaryChange: (String) -> Unit,
    foodConstraints: String,
    onFoodConstraintsChange: (String) -> Unit,
    movementLimitations: String,
    onMovementLimitationsChange: (String) -> Unit,
    seatedDefault: Boolean,
    onSeatedDefaultChange: (Boolean) -> Unit,
    bedDefault: Boolean,
    onBedDefaultChange: (Boolean) -> Unit,
    floorDefault: Boolean,
    onFloorDefaultChange: (Boolean) -> Unit,
    standingDefault: Boolean,
    onStandingDefaultChange: (Boolean) -> Unit,
    isWorking: Boolean,
    onSave: () -> Unit
) {
    val age = runCatching {
        Period.between(
            LocalDate.parse(birthDate),
            LocalDate.now()
        ).years
    }.getOrNull()

    RebuildSectionCard(
        title = "Personal health profile",
        subtitle =
            "This is descriptive information for Daily Rebuild, not a diagnosis or treatment plan.",
        accentColor = RebuildBlue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = birthDate,
                onValueChange = onBirthDateChange,
                label = { Text("Birth date") },
                supportingText = {
                    Text(
                        age?.let { "Age $it • YYYY-MM-DD" }
                            ?: "Use YYYY-MM-DD"
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = heightInches,
                onValueChange = onHeightInchesChange,
                label = { Text("Height inches") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = startingWeight,
                onValueChange = onStartingWeightChange,
                label = { Text("Approx. start lb") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )

            OutlinedTextField(
                value = goalWeight,
                onValueChange = onGoalWeightChange,
                label = { Text("Long-term goal lb") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )
        }

        OutlinedTextField(
            value = conditionsSummary,
            onValueChange = onConditionsSummaryChange,
            label = { Text("Health context") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = foodConstraints,
            onValueChange = onFoodConstraintsChange,
            label = { Text("Food and kitchen constraints") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        OutlinedTextField(
            value = movementLimitations,
            onValueChange = onMovementLimitationsChange,
            label = { Text("Movement limitations") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Text(
            text = "Default mobility positions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        MobilityDefaultCheckbox(
            label = "Seated",
            checked = seatedDefault,
            onCheckedChange = onSeatedDefaultChange
        )
        MobilityDefaultCheckbox(
            label = "Lying on bed",
            checked = bedDefault,
            onCheckedChange = onBedDefaultChange
        )
        MobilityDefaultCheckbox(
            label = "Lying on floor",
            checked = floorDefault,
            onCheckedChange = onFloorDefaultChange
        )
        MobilityDefaultCheckbox(
            label = "Standing",
            checked = standingDefault,
            onCheckedChange = onStandingDefaultChange
        )

        Button(
            onClick = onSave,
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save profile and mobility defaults")
        }
    }
}

@Composable
private fun MobilityDefaultCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(label)
    }
}

@Composable
private fun CalorieGoalSection(
    averageCalories: Int?,
    loggedDays: Int,
    currentGoal: Int?,
    goalChanges: List<CalorieGoalChange>,
    isWorking: Boolean,
    onSetGoal: (Int, String) -> Unit
) {
    RebuildSectionCard(
        title = "Seven-day calories",
        subtitle =
            "The average uses only days that contain food entries, so missing days do not count as zero.",
        accentColor = RebuildGreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "$loggedDays of 7 days logged",
                value = averageCalories?.toString() ?: "—",
                modifier = Modifier.weight(1f)
            )
            RebuildMetricPill(
                label = "current goal",
                value = currentGoal?.toString() ?: "Not set",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        if (averageCalories != null && currentGoal == null) {
            OutlinedButton(
                onClick = {
                    onSetGoal(
                        averageCalories,
                        "Started from the current seven-day logged average."
                    )
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start goal at current average")
            }
        }

        if (currentGoal != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSetGoal(
                            currentGoal - 100,
                            "Lowered gradually by 100 calories."
                        )
                    },
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lower 100")
                }

                OutlinedButton(
                    onClick = {
                        onSetGoal(
                            currentGoal + 100,
                            "Raised gradually by 100 calories."
                        )
                    },
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Raise 100")
                }
            }
        }

        if (goalChanges.isNotEmpty()) {
            Text(
                text = "Recent goal changes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            goalChanges.take(5).forEach { change ->
                RebuildInsetPanel {
                    Text(
                        text =
                            "${displayHealthDate(change.changedDate)} • ${change.previousGoal ?: "Not set"} → ${change.newGoal}",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (change.reason.isNotBlank()) {
                        Text(
                            text = change.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementSection(
    measurements: List<HealthMeasurement>,
    latestWeight: Double?,
    weeklyWeightChange: Double?,
    onAddMeasurement: (String) -> Unit,
    onDeleteMeasurement: (HealthMeasurement) -> Unit
) {
    RebuildSectionCard(
        title = "Measurements",
        subtitle =
            "Record values only when they are available. No scale or lab result is treated as zero.",
        accentColor = RebuildTeal
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "latest weight",
                value = latestWeight?.let {
                    "${formatHealthNumber(it)} lb"
                } ?: "Not recorded",
                modifier = Modifier.weight(1f)
            )

            RebuildMetricPill(
                label = "7-day change",
                value = weeklyWeightChange?.let {
                    val sign = if (it > 0) "+" else ""
                    "$sign${formatHealthNumber(it)} lb"
                } ?: "Not enough data",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onAddMeasurement(HealthMeasurementType.WEIGHT)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Weight")
            }
            OutlinedButton(
                onClick = {
                    onAddMeasurement(HealthMeasurementType.A1C)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("A1C")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onAddMeasurement(HealthMeasurementType.BLOOD_PRESSURE)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Blood pressure")
            }
            OutlinedButton(
                onClick = {
                    onAddMeasurement(HealthMeasurementType.CHOLESTEROL)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cholesterol")
            }
        }

        if (measurements.isEmpty()) {
            Text(
                text = "No measurements recorded yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Recent measurements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            measurements.take(12).forEach { measurement ->
                RebuildInsetPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = measurementDisplayValue(measurement),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = displayHealthDate(measurement.recordedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (measurement.notes.isNotBlank()) {
                                Text(
                                    text = measurement.notes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                onDeleteMeasurement(measurement)
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationReferenceSection(
    medications: List<MedicationEntry>,
    onAdd: () -> Unit,
    onEdit: (MedicationEntry) -> Unit,
    onDelete: (MedicationEntry) -> Unit
) {
    RebuildSectionCard(
        title = "Medication reference",
        subtitle =
            "Reference only. Daily Rebuild does not recommend starting, stopping, combining, or changing medications or supplements.",
        accentColor = RebuildDanger
    ) {
        RebuildInsetPanel {
            Text(
                text =
                    "The initial entries were imported from your uploaded CSV. Verify the list with the bottle labels or a pharmacist.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text =
                    "“Acetamitophen” is preserved exactly from the CSV and marked for spelling review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add medication or supplement")
        }

        medications.forEach { medication ->
            RebuildInsetPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = medication.name,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = medicationScheduleText(medication),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (medication.purchaseSource.isNotBlank()) {
                            Text(
                                text = "Source: ${medication.purchaseSource}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (medication.notes.isNotBlank()) {
                            Text(
                                text = medication.notes,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        TextButton(
                            onClick = {
                                onEdit(medication)
                            }
                        ) {
                            Text("Edit")
                        }
                        TextButton(
                            onClick = {
                                onDelete(medication)
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun MeasurementEditorDialog(
    type: String,
    onDismiss: () -> Unit,
    onSave: (HealthMeasurement) -> Unit
) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var primary by rememberSaveable { mutableStateOf("") }
    var secondary by rememberSaveable { mutableStateOf("") }
    var tertiary by rememberSaveable { mutableStateOf("") }
    var quaternary by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (type) {
        HealthMeasurementType.WEIGHT -> "Record weight"
        HealthMeasurementType.A1C -> "Record A1C"
        HealthMeasurementType.BLOOD_PRESSURE -> "Record blood pressure"
        else -> "Record cholesterol"
    }

    RebuildInputDialog(
        title = title,
        subtitle = "Add a dated result to your personal health history.",
        onDismissRequest = onDismiss,
        primaryActionText = "Save measurement",
        onPrimaryAction = save@{
            val validDate = runCatching { LocalDate.parse(date) }.getOrNull()
            val primaryValue = primary.toDoubleOrNull()
            val secondaryValue = secondary.toDoubleOrNull()

            when {
                validDate == null -> error = "Use YYYY-MM-DD for the date."
                primaryValue == null -> error = "Enter the main measurement value."
                type == HealthMeasurementType.BLOOD_PRESSURE && secondaryValue == null ->
                    error = "Enter both systolic and diastolic values."
                else -> onSave(
                    HealthMeasurement(
                        recordedDate = date,
                        type = type,
                        primaryValue = primaryValue,
                        secondaryValue = secondaryValue,
                        tertiaryValue = tertiary.toDoubleOrNull(),
                        quaternaryValue = quaternary.toDoubleOrNull(),
                        notes = notes.trim()
                    )
                )
            }
        }
    ) {
        OutlinedTextField(
            value = date,
            onValueChange = { date = it; error = null },
            label = { Text("Date") },
            supportingText = { Text("Use YYYY-MM-DD") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        when (type) {
            HealthMeasurementType.WEIGHT -> NumericField(primary, { primary = it; error = null }, "Weight (lb)")
            HealthMeasurementType.A1C -> NumericField(primary, { primary = it; error = null }, "A1C (%)")
            HealthMeasurementType.BLOOD_PRESSURE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = primary,
                        onValueChange = { primary = it; error = null },
                        label = { Text("Systolic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondary,
                        onValueChange = { secondary = it; error = null },
                        label = { Text("Diastolic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            HealthMeasurementType.CHOLESTEROL -> {
                NumericField(primary, { primary = it; error = null }, "Total cholesterol")
                NumericField(secondary, { secondary = it }, "LDL (optional)")
                NumericField(tertiary, { tertiary = it }, "HDL (optional)")
                NumericField(quaternary, { quaternary = it }, "Triglycerides (optional)")
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        )
    )
}
@Composable
private fun MedicationEditorDialog(
    initial: MedicationEntry?,
    onDismiss: () -> Unit,
    onSave: (MedicationEntry) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var milligrams by rememberSaveable { mutableStateOf(initial?.milligrams?.let(::formatHealthNumber) ?: "") }
    var numberPerDose by rememberSaveable { mutableStateOf(initial?.numberPerDose?.let(::formatHealthNumber) ?: "") }
    var timesPerDay by rememberSaveable { mutableStateOf(initial?.timesPerDay?.let(::formatHealthNumber) ?: "") }
    var purchaseSource by rememberSaveable { mutableStateOf(initial?.purchaseSource ?: "") }
    var pricePerBottle by rememberSaveable { mutableStateOf(initial?.pricePerBottle ?: "") }
    var numberPerBottle by rememberSaveable { mutableStateOf(initial?.numberPerBottle?.toString() ?: "") }
    var pricePerPill by rememberSaveable { mutableStateOf(initial?.pricePerPill ?: "") }
    var purchasesPerYear by rememberSaveable { mutableStateOf(initial?.purchasesPerYear?.let(::formatHealthNumber) ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var isActive by rememberSaveable { mutableStateOf(initial?.isActive ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    RebuildInputDialog(
        title = if (initial == null) "Add medication reference" else "Edit medication reference",
        subtitle = "This is a personal reference entry, not a medication reminder or treatment recommendation.",
        onDismissRequest = onDismiss,
        primaryActionText = if (initial == null) "Add reference" else "Save changes",
        onPrimaryAction = {
            if (name.trim().isBlank()) {
                error = "Enter a medication name."
            } else {
                onSave(
                    MedicationEntry(
                        id = initial?.id ?: 0L,
                        name = name.trim(),
                        milligrams = milligrams.toDoubleOrNull(),
                        numberPerDose = numberPerDose.toDoubleOrNull(),
                        timesPerDay = timesPerDay.toDoubleOrNull(),
                        purchaseSource = purchaseSource.trim(),
                        pricePerBottle = pricePerBottle.trim(),
                        numberPerBottle = numberPerBottle.toIntOrNull(),
                        pricePerPill = pricePerPill.trim(),
                        purchasesPerYear = purchasesPerYear.toDoubleOrNull(),
                        notes = notes.trim(),
                        isActive = isActive,
                        sortOrder = initial?.sortOrder ?: 1000
                    )
                )
            }
        }
    ) {
        Text("Medication details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text("Name *") },
            singleLine = true,
            isError = error != null && name.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = milligrams,
                onValueChange = { milligrams = it },
                label = { Text("Milligrams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = numberPerDose,
                onValueChange = { numberPerDose = it },
                label = { Text("Per dose") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        NumericField(timesPerDay, { timesPerDay = it }, "Times per day")

        Text("Purchase reference", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(purchaseSource, { purchaseSource = it }, label = { Text("Purchase source") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(pricePerBottle, { pricePerBottle = it }, label = { Text("Bottle price") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(numberPerBottle, { numberPerBottle = it.filter(Char::isDigit) }, label = { Text("Pills per bottle") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(pricePerPill, { pricePerPill = it }, label = { Text("Price per pill") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(purchasesPerYear, { purchasesPerYear = it }, label = { Text("Purchases/year") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }

        OutlinedTextField(notes, { notes = it }, label = { Text("Reference notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())

        RebuildInsetPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Column {
                    Text("Currently on my reference list", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Turning this off keeps the old entry without showing it as current.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun measurementDisplayValue(
    measurement: HealthMeasurement
): String {
    return when (measurement.type) {
        HealthMeasurementType.WEIGHT ->
            "Weight: ${formatHealthNumber(measurement.primaryValue)} lb"
        HealthMeasurementType.A1C ->
            "A1C: ${formatHealthNumber(measurement.primaryValue)}%"
        HealthMeasurementType.BLOOD_PRESSURE ->
            "Blood pressure: ${formatHealthNumber(measurement.primaryValue)}/${formatHealthNumber(measurement.secondaryValue ?: 0.0)}"
        HealthMeasurementType.CHOLESTEROL -> {
            val parts = mutableListOf(
                "Total ${formatHealthNumber(measurement.primaryValue)}"
            )
            measurement.secondaryValue?.let {
                parts += "LDL ${formatHealthNumber(it)}"
            }
            measurement.tertiaryValue?.let {
                parts += "HDL ${formatHealthNumber(it)}"
            }
            measurement.quaternaryValue?.let {
                parts += "Triglycerides ${formatHealthNumber(it)}"
            }
            "Cholesterol: ${parts.joinToString(" • ")}"
        }
        else -> "Measurement: ${formatHealthNumber(measurement.primaryValue)}"
    }
}

private fun medicationScheduleText(
    medication: MedicationEntry
): String {
    val parts = mutableListOf<String>()

    medication.milligrams?.let {
        parts += "${formatHealthNumber(it)} mg"
    }
    medication.numberPerDose?.let {
        parts += "${formatHealthNumber(it)} per dose"
    }
    medication.timesPerDay?.let {
        parts += "${formatHealthNumber(it)}×/day"
    }

    return if (parts.isEmpty()) {
        "Dose or schedule not entered"
    } else {
        parts.joinToString(" • ")
    }
}

private fun displayHealthDate(date: String): String {
    return runCatching {
        LocalDate.parse(date).format(healthDisplayDateFormatter)
    }.getOrDefault(date)
}

private fun formatHealthNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun defaultMedicationEntries(): List<MedicationEntry> {
    val importedNote = "Imported from Meds - Sheet1(1).csv. Verify against the bottle label."

    return listOf(
        MedicationEntry(
            name = "St John's Wort",
            milligrams = 500.0,
            numberPerDose = 1.0,
            timesPerDay = 2.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$11.95",
            numberPerBottle = 120,
            pricePerPill = "\$0.10",
            purchasesPerYear = 3.04,
            notes = importedNote,
            sortOrder = 1
        ),
        MedicationEntry(
            name = "Ibuprofen",
            milligrams = 200.0,
            numberPerDose = 2.0,
            timesPerDay = 2.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$6.00",
            numberPerBottle = 250,
            pricePerPill = "\$0.02",
            purchasesPerYear = 1.46,
            notes = importedNote,
            sortOrder = 2
        ),
        MedicationEntry(
            name = "Once-Daily Multi",
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Amazon",
            pricePerBottle = "\$14.99",
            numberPerBottle = 180,
            pricePerPill = "\$0.08",
            purchasesPerYear = 2.03,
            notes = importedNote,
            sortOrder = 3
        ),
        MedicationEntry(
            name = "Melatonin",
            milligrams = 10.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$10.68",
            numberPerBottle = 240,
            pricePerPill = "\$0.04",
            purchasesPerYear = 1.52,
            notes = importedNote,
            sortOrder = 4
        ),
        MedicationEntry(
            name = "Valerian Root",
            milligrams = 500.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$5.94",
            numberPerBottle = 100,
            pricePerPill = "\$0.06",
            purchasesPerYear = 3.65,
            notes = importedNote,
            sortOrder = 5
        ),
        MedicationEntry(
            name = "Acetamitophen",
            milligrams = 500.0,
            numberPerDose = 2.0,
            timesPerDay = 2.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$3.94",
            numberPerBottle = 200,
            pricePerPill = "\$0.02",
            purchasesPerYear = 1.83,
            notes =
                "$importedNote The spelling is preserved from the CSV and should be reviewed.",
            sortOrder = 6
        ),
        MedicationEntry(
            name = "Magnesium Glycinate",
            milligrams = 400.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Amazon",
            pricePerBottle = "\$9.97",
            numberPerBottle = 180,
            pricePerPill = "\$0.06",
            purchasesPerYear = 2.03,
            notes = importedNote,
            sortOrder = 7
        ),
        MedicationEntry(
            name = "Omega-3",
            milligrams = 500.0,
            numberPerDose = 1.0,
            timesPerDay = 2.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$8.32",
            numberPerBottle = 60,
            pricePerPill = "\$0.14",
            purchasesPerYear = 6.08,
            notes = importedNote,
            sortOrder = 8
        ),
        MedicationEntry(
            name = "Aspirin",
            milligrams = 325.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$4.18",
            numberPerBottle = 500,
            pricePerPill = "\$0.01",
            purchasesPerYear = 0.73,
            notes = importedNote,
            sortOrder = 9
        ),
        MedicationEntry(
            name = "Naproxen Sodium",
            milligrams = 220.0,
            numberPerDose = 1.0,
            timesPerDay = 2.0,
            purchaseSource = "Amazon",
            pricePerBottle = "\$9.01",
            numberPerBottle = 200,
            pricePerPill = "\$0.05",
            purchasesPerYear = 1.83,
            notes = importedNote,
            sortOrder = 10
        ),
        MedicationEntry(
            name = "Doxylamine Succinate",
            milligrams = 25.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$4.53",
            numberPerBottle = 32,
            pricePerPill = "\$0.14",
            purchasesPerYear = 11.41,
            notes = importedNote,
            sortOrder = 11
        ),
        MedicationEntry(
            name = "Diphenhydramine",
            milligrams = 25.0,
            numberPerDose = 1.0,
            timesPerDay = 1.0,
            purchaseSource = "Walmart",
            pricePerBottle = "\$7.72",
            numberPerBottle = 365,
            pricePerPill = "\$0.02",
            purchasesPerYear = 1.0,
            notes = importedNote,
            sortOrder = 12
        )
    )
}
