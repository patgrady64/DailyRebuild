package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DataQualitySummaryCard(
    warnings: List<DataQualityWarning>,
    onReview: (DataQualityWarning) -> Unit,
    onKeep: (DataQualityWarning) -> Unit,
    onIgnoreExactValue: (DataQualityWarning) -> Unit,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) return

    var showWarnings by rememberSaveable { mutableStateOf(false) }
    val countText = if (warnings.size == 1) "1 entry to review" else "${warnings.size} entries to review"

    RebuildSectionCard(
        title = "Data checks",
        subtitle = "$countText · Nothing is changed automatically.",
        accentColor = RebuildAmber,
        modifier = modifier
    ) {
        Text(
            text = warnings.first().title,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = warnings.first().summary,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { showWarnings = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (warnings.size == 1) "Review Entry" else "Review All ${warnings.size}")
        }
        Text(
            text = "These checks only look for possible entry mistakes. They are not medical advice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showWarnings) {
        DataQualityWarningsDialog(
            warnings = warnings,
            onReview = { warning ->
                showWarnings = false
                onReview(warning)
            },
            onKeep = onKeep,
            onIgnoreExactValue = onIgnoreExactValue,
            onDismiss = { showWarnings = false }
        )
    }
}

@Composable
private fun DataQualityWarningsDialog(
    warnings: List<DataQualityWarning>,
    onReview: (DataQualityWarning) -> Unit,
    onKeep: (DataQualityWarning) -> Unit,
    onIgnoreExactValue: (DataQualityWarning) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Review Data Checks")
                Text(
                    text = "Daily Rebuild will never correct or delete these entries on its own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                warnings.forEach { warning ->
                    DataQualityWarningCard(
                        warning = warning,
                        onReview = { onReview(warning) },
                        onKeep = { onKeep(warning) },
                        onIgnoreExactValue = { onIgnoreExactValue(warning) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DataQualityWarningCard(
    warning: DataQualityWarning,
    onReview: () -> Unit,
    onKeep: () -> Unit,
    onIgnoreExactValue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = warning.title,
                fontWeight = FontWeight.Bold
            )
            Text(warning.summary)
            Text(
                text = warning.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReview,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Review")
                }
                OutlinedButton(
                    onClick = onKeep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Keep It")
                }
            }
            TextButton(
                onClick = onIgnoreExactValue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don’t warn for this exact value again")
            }
        }
    }
}
