package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.ui.theme.DailyRebuildTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyRebuildTheme {
                DailyRebuildApp()
            }
        }
    }
}

@Composable
fun DailyRebuildApp() {
    var foodRecorded by remember { mutableStateOf(false) }
    var walkCompleted by remember { mutableStateOf(false) }
    var painRecorded by remember { mutableStateOf(false) }
    var mobilityCompleted by remember { mutableStateOf(false) }

    var backPain by remember { mutableFloatStateOf(0f) }
    var shinPain by remember { mutableFloatStateOf(0f) }

    var notes by remember { mutableStateOf("") }

    val completedTasks = listOf(
        foodRecorded,
        walkCompleted,
        painRecorded,
        mobilityCompleted
    ).count { it }

    val progressPercent = completedTasks * 25

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            HeaderSection()

            ProgressSection(
                completedTasks = completedTasks,
                progressPercent = progressPercent
            )

            DailyTasksSection(
                foodRecorded = foodRecorded,
                onFoodRecordedChange = { foodRecorded = it },

                walkCompleted = walkCompleted,
                onWalkCompletedChange = { walkCompleted = it },

                painRecorded = painRecorded,
                onPainRecordedChange = { painRecorded = it },

                mobilityCompleted = mobilityCompleted,
                onMobilityCompletedChange = { mobilityCompleted = it }
            )

            FoodSection()

            PainSection(
                backPain = backPain,
                onBackPainChange = {
                    backPain = it
                    painRecorded = true
                },

                shinPain = shinPain,
                onShinPainChange = {
                    shinPain = it
                    painRecorded = true
                }
            )

            MedicationSection()

            NotesSection(
                notes = notes,
                onNotesChange = { notes = it }
            )

            Button(
                onClick = {
                    /*
                     * Saving will be added after we create
                     * the Room database.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Today")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderSection() {
    val today = LocalDate.now()

    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    Column {
        Text(
            text = "Daily Rebuild",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = today.format(dayFormatter),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = today.format(dateFormatter),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ProgressSection(
    completedTasks: Int,
    progressPercent: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completedTasks of 4 tasks completed"
            )

            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyTasksSection(
    foodRecorded: Boolean,
    onFoodRecordedChange: (Boolean) -> Unit,

    walkCompleted: Boolean,
    onWalkCompletedChange: (Boolean) -> Unit,

    painRecorded: Boolean,
    onPainRecordedChange: (Boolean) -> Unit,

    mobilityCompleted: Boolean,
    onMobilityCompletedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Next Steps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TaskCheckbox(
                text = "Record food and drinks",
                checked = foodRecorded,
                onCheckedChange = onFoodRecordedChange
            )

            TaskCheckbox(
                text = "Complete today's walk or movement",
                checked = walkCompleted,
                onCheckedChange = onWalkCompletedChange
            )

            TaskCheckbox(
                text = "Record pain levels",
                checked = painRecorded,
                onCheckedChange = onPainRecordedChange
            )

            TaskCheckbox(
                text = "Complete mobility routine",
                checked = mobilityCompleted,
                onCheckedChange = onMobilityCompletedChange
            )
        }
    }
}

@Composable
private fun TaskCheckbox(
    text: String,
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

        Text(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FoodSection() {
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

            Spacer(modifier = Modifier.height(8.dp))

            Text("No food or drinks recorded yet.")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    /*
                     * Barcode scanning will be added soon.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Food Barcode")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    /*
                     * Manual food entry will be added next.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Food Manually")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    /*
                     * Water logging will be added next.
                     */
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Water")
            }
        }
    }
}

@Composable
private fun PainSection(
    backPain: Float,
    onBackPainChange: (Float) -> Unit,

    shinPain: Float,
    onShinPainChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pain",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PainSlider(
                label = "Lower Back Pain",
                painValue = backPain,
                onPainValueChange = onBackPainChange
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp)
            )

            PainSlider(
                label = "Shin Pain",
                painValue = shinPain,
                onPainValueChange = onShinPainChange
            )
        }
    }
}

@Composable
private fun PainSlider(
    label: String,
    painValue: Float,
    onPainValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = painValue.toInt().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = painValue,
            onValueChange = onPainValueChange,
            valueRange = 0f..10f,
            steps = 9
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("No pain")
            Text("Worst pain")
        }
    }
}

@Composable
private fun MedicationSection() {
    var aspirinTaken by remember { mutableStateOf(true) }
    var ibuprofenTaken by remember { mutableStateOf(true) }
    var naproxenTaken by remember { mutableStateOf(true) }
    var acetaminophenTaken by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pain Relievers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Prefilled from your normal pill organizer. " +
                        "Uncheck anything you did not take today.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            MedicationCheckbox(
                name = "Aspirin",
                dose = "325 mg",
                checked = aspirinTaken,
                onCheckedChange = { aspirinTaken = it }
            )

            MedicationCheckbox(
                name = "Ibuprofen",
                dose = "800 mg total",
                checked = ibuprofenTaken,
                onCheckedChange = { ibuprofenTaken = it }
            )

            MedicationCheckbox(
                name = "Naproxen sodium",
                dose = "440 mg total",
                checked = naproxenTaken,
                onCheckedChange = { naproxenTaken = it }
            )

            MedicationCheckbox(
                name = "Acetaminophen",
                dose = "2,000 mg total",
                checked = acetaminophenTaken,
                onCheckedChange = { acetaminophenTaken = it }
            )
        }
    }
}

@Composable
private fun MedicationCheckbox(
    name: String,
    dose: String,
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

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = dose,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = {
                    Text("How did today feel?")
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}