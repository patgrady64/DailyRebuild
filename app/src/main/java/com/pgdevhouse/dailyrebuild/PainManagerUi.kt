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
    var selectedBackPain by remember(currentBackPain) { mutableFloatStateOf(currentBackPain) }
    var selectedShinPain by remember(currentShinPain) { mutableFloatStateOf(currentShinPain) }
    var correctionMode by remember { mutableStateOf(false) }

    RebuildInputDialog(
        title = if (correctionMode) "Correct today’s pain" else "Pain today",
        subtitle = when {
            correctionMode -> "Set the exact values that should be saved for today."
            wasRecordedToday -> "Quick logging can raise today’s high values but will not lower them."
            else -> "Record the highest level experienced so far today."
        },
        onDismissRequest = onDismiss,
        primaryActionText = when {
            correctionMode -> "Save correction"
            wasRecordedToday -> "Update daily highs"
            else -> "Save pain levels"
        },
        onPrimaryAction = {
            if (correctionMode) {
                onCorrectValues(
                    correctDailyPainValue(selectedBackPain),
                    correctDailyPainValue(selectedShinPain)
                )
            } else {
                onSaveDailyHighs(
                    recordDailyHighestPain(currentBackPain, selectedBackPain),
                    recordDailyHighestPain(currentShinPain, selectedShinPain)
                )
            }
        }
    ) {
        if (wasRecordedToday) {
            RebuildDialogInfoPanel {
                Text(
                    text = if (correctionMode) {
                        "Correction mode may lower a value that was entered incorrectly."
                    } else {
                        "Already recorded today. Move either slider higher to update the daily high."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { correctionMode = !correctionMode }) {
                    Text(if (correctionMode) "Return to quick log" else "Correct a mistaken value")
                }
            }
        }

        DailyPainSlider("Back pain", selectedBackPain) { selectedBackPain = it }
        DailyPainSlider("Shin-splint pain", selectedShinPain) { selectedShinPain = it }
    }
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
