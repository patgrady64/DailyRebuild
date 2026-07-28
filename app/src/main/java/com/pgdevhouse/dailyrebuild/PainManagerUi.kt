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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.domain.correctDailyPainValue
import com.pgdevhouse.dailyrebuild.domain.recordDailyHighestPain

/**
 * Tracks one daily high for back pain and one daily high for shin-splint pain.
 * Quick logging can only raise those highs. Correction mode intentionally
 * allows an inaccurate value to be lowered or moved to the other pain area.
 */
@Composable
fun DailyPainDialog(
    currentBackPain: Float,
    currentShinPain: Float,
    wasRecordedToday: Boolean,
    onSaveDailyHighs: (backPain: Float, shinPain: Float) -> Unit,
    onCorrectValues: (backPain: Float, shinPain: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBackPain by remember(currentBackPain) {
        mutableFloatStateOf(currentBackPain)
    }
    var selectedShinPain by remember(currentShinPain) {
        mutableFloatStateOf(currentShinPain)
    }
    var correctionMode by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (correctionMode) {
                    "Correct Today's Pain"
                } else {
                    "Pain Today"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when {
                        correctionMode ->
                            "Set the exact values that should be saved for today. " +
                                "Correction mode can lower a mistaken value."

                        wasRecordedToday ->
                            "These are today's highest recorded values. Quick logging " +
                                "can raise either value, but it will not lower one."

                        else ->
                            "Record the highest back pain and shin-splint pain you " +
                                "have experienced so far today."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (wasRecordedToday) {
                    TextButton(
                        onClick = { correctionMode = !correctionMode }
                    ) {
                        Text(
                            if (correctionMode) {
                                "Return to Quick Log"
                            } else {
                                "Correct a Mistaken Value"
                            }
                        )
                    }
                }

                DailyPainSlider(
                    label = "Back pain",
                    value = selectedBackPain,
                    onValueChange = { selectedBackPain = it }
                )

                DailyPainSlider(
                    label = "Shin-splint pain",
                    value = selectedShinPain,
                    onValueChange = { selectedShinPain = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (correctionMode) {
                        onCorrectValues(
                            correctDailyPainValue(selectedBackPain),
                            correctDailyPainValue(selectedShinPain)
                        )
                    } else {
                        onSaveDailyHighs(
                            recordDailyHighestPain(
                                currentBackPain,
                                selectedBackPain
                            ),
                            recordDailyHighestPain(
                                currentShinPain,
                                selectedShinPain
                            )
                        )
                    }
                }
            ) {
                Text(
                    if (correctionMode) {
                        "Save Correction"
                    } else if (wasRecordedToday) {
                        "Update Daily Highs"
                    } else {
                        "Save"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DailyPainSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = painDescription(value),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "${value.toInt()} / 10",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
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
}

private fun painDescription(value: Float): String = when {
    value <= 0f -> "No pain"
    value < 4f -> "Mild"
    value < 7f -> "Moderate"
    value < 9f -> "Severe"
    else -> "Very severe"
}
