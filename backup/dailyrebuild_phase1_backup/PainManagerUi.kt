package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun HighestPainDialog(
    currentHighestPain: Float,
    wasRecordedToday: Boolean,
    onSave: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPain by remember(currentHighestPain) {
        mutableFloatStateOf(currentHighestPain)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highest Pain Today") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (wasRecordedToday) {
                        "Today's highest recorded pain is ${currentHighestPain.toInt()} / 10. Raise it if your pain became worse. A lower selection will not reduce today's highest value."
                    } else {
                        "Record the highest pain you have experienced so far today. You can raise it later if something causes more pain."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Selected highest pain",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = painDescription(selectedPain),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${selectedPain.toInt()} / 10",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Slider(
                    value = selectedPain,
                    onValueChange = { selectedPain = it },
                    valueRange = 0f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("No pain", style = MaterialTheme.typography.bodySmall)
                    Text("Worst pain", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(max(currentHighestPain, selectedPain))
                }
            ) {
                Text(if (wasRecordedToday) "Update Highest" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun painDescription(value: Float): String = when {
    value <= 0f -> "No pain recorded"
    value < 4f -> "Mild"
    value < 7f -> "Moderate"
    value < 9f -> "Severe"
    else -> "Very severe"
}
