package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ActivitySection(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    isLoading: Boolean,
    activity: HealthActivityData,
    sourceLabel: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onManageAccess: () -> Unit,
    onInstallOrUpdate: () -> Unit
) {
    RebuildSectionCard(
        title = "Connected Activity",
        subtitle = "Steps, distance, and recorded activity time from Health Connect.",
        accentColor = RebuildBlue
    ) {
        when (availability) {
            HealthConnectAvailability.AVAILABLE -> {
                if (hasPermissions || sourceLabel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RebuildMetricPill(
                            label = "steps",
                            value = String.format(Locale.US, "%,d", activity.steps),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                        RebuildMetricPill(
                            label = "miles",
                            value = String.format(Locale.US, "%.2f", activity.distanceMiles),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        RebuildMetricPill(
                            label = "time",
                            value = connectedActivityTime(activity.activityMinutes),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    sourceLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RebuildSecondaryAction(
                            text = when {
                                isLoading -> "Refreshing…"
                                hasPermissions -> "Refresh"
                                else -> "Reconnect"
                            },
                            onClick = if (hasPermissions) onRefresh else onConnect,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        )
                        RebuildSecondaryAction(
                            text = "Manage access",
                            onClick = onManageAccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = "Connect Health Connect to show activity recorded by Google Fit, your phone, or a compatible wearable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RebuildPrimaryAction(
                        text = "Connect Health Connect",
                        onClick = onConnect,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HealthConnectAvailability.UPDATE_REQUIRED -> {
                Text(
                    text = "Health Connect must be installed or updated before Daily Rebuild can read activity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RebuildPrimaryAction(
                    text = "Install or update",
                    onClick = onInstallOrUpdate,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HealthConnectAvailability.UNAVAILABLE -> {
                Text(
                    text = "Health Connect is not available on this device. Daily Rebuild will continue working without it.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun connectedActivityTime(totalMinutes: Long): String {
    if (totalMinutes <= 0L) return "0m"
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}
