package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val WEEKLY_SHOWER_MINIMUM = 2
private const val WEEKLY_SHOWER_PREFERRED = 3

/**
 * Compact hydration entry point for the Food tab.
 *
 * It deliberately opens the same water dialog as Home so both locations use
 * one source of truth and one set of bottle counters.
 */
@Composable
fun FoodHydrationCard(
    totalWaterOunces: Double,
    totalBottleCount: Int,
    onAddWater: () -> Unit
) {
    RebuildSectionCard(
        title = "Hydration today",
        subtitle =
            "Water is available here because it is part of today’s intake.",
        accentColor = RebuildBlue,
        trailing = {
            TextButton(
                onClick = onAddWater
            ) {
                Text("Add")
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            RebuildMetricPill(
                label = "water",
                value =
                    "${formatHydrationOunces(totalWaterOunces)} oz",
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .primaryContainer
            )

            RebuildMetricPill(
                label = "bottles",
                value = totalBottleCount.toString(),
                modifier = Modifier.weight(1f),
                color =
                    MaterialTheme.colorScheme
                        .secondaryContainer,
                contentColor =
                    MaterialTheme.colorScheme
                        .onSecondaryContainer
            )
        }

        Button(
            onClick = onAddWater,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Add Water")
        }
    }
}

/**
 * Weekly shower progress shown inside Home → More Today.
 *
 * A shower is not a daily anchor. Two logs reach the weekly minimum; the third
 * is shown as the preferred upper target. More than three is still accepted.
 */
@Composable
fun WeeklyShowerControls(
    showerDates: List<String>,
    showeredToday: Boolean,
    onLogToday: () -> Unit,
    onRemoveToday: () -> Unit
) {
    val showerCount =
        showerDates.distinct().size

    val progress =
        showerCount
            .toFloat()
            .div(WEEKLY_SHOWER_MINIMUM.toFloat())
            .coerceIn(0f, 1f)

    val statusText =
        when {
            showerCount <= 0 ->
                "No showers logged this week. Two reaches your minimum."

            showerCount == 1 ->
                "One logged. One more reaches your weekly minimum."

            showerCount == 2 ->
                "Weekly minimum reached. A third shower is optional."

            showerCount == 3 ->
                "Preferred weekly range reached."

            else ->
                "$showerCount showers logged this week."
        }

    val lastShowerText =
        showerDates
            .maxOrNull()
            ?.let(::formatShowerDate)
            ?: "Not recorded this week"

    RebuildInsetPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Weekly shower goal",
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text =
                        "$showerCount logged · goal $WEEKLY_SHOWER_MINIMUM–$WEEKLY_SHOWER_PREFERRED",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            RebuildStatusBadge(
                text =
                    when {
                        showerCount >= WEEKLY_SHOWER_PREFERRED ->
                            "Goal reached"

                        showerCount >= WEEKLY_SHOWER_MINIMUM ->
                            "Minimum reached"

                        else ->
                            "$showerCount / $WEEKLY_SHOWER_MINIMUM"
                    }
            )
        }

        LinearProgressIndicator(
            progress = progress,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Last shower: $lastShowerText",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        if (showeredToday) {
            OutlinedButton(
                onClick = onRemoveToday,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Undo Today’s Shower")
            }
        } else {
            Button(
                onClick = onLogToday,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Today’s Shower")
            }
        }
    }
}

private fun formatHydrationOunces(
    ounces: Double
): String {
    return if (ounces % 1.0 == 0.0) {
        ounces.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.1f",
            ounces
        )
    }
}

private fun formatShowerDate(
    dateText: String
): String {
    return runCatching {
        LocalDate.parse(dateText)
            .format(
                DateTimeFormatter.ofPattern(
                    "EEE, MMM d",
                    Locale.US
                )
            )
    }.getOrDefault(dateText)
}
