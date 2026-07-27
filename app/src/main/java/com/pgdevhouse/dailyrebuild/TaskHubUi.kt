package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment

@Composable
fun TaskHubFrame(
    title: String,
    subtitle: String,
    labels: List<String>,
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HubScreenHeader(
            title = title,
            subtitle = subtitle,
            onOpenHistory = onOpenHistory
        )
        TaskSectionTabs(
            labels = labels,
            selected = selectedSection,
            onSelected = onSectionSelected
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
    }
}

@Composable
fun HealthQuickLogScreen(
    onLogHighestPain: () -> Unit,
    onLogMigraine: () -> Unit,
    onLogCareVisit: () -> Unit,
    onLogShower: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RebuildSectionCard(
            title = "Health Quick Log",
            subtitle = "Record occasional health and self-care events without opening the full records hub.",
            accentColor = RebuildAmber
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLogHighestPain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Pain") }
                OutlinedButton(
                    onClick = onLogMigraine,
                    modifier = Modifier.weight(1f)
                ) { Text("Migraine") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLogCareVisit,
                    modifier = Modifier.weight(1f)
                ) { Text("Care Visit") }
                OutlinedButton(
                    onClick = onLogShower,
                    modifier = Modifier.weight(1f)
                ) { Text("Shower") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMeasurements,
                    modifier = Modifier.weight(1f)
                ) { Text("Measurement") }
                OutlinedButton(
                    onClick = onOpenHealth,
                    modifier = Modifier.weight(1f)
                ) { Text("Health Records") }
            }
        }

        RebuildInsetPanel {
            Text(
                text = "Measurements, appointments, medications, care history, and connected activity remain in the Health tab.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenHealth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Health Records")
            }
        }
    }
}

@Composable
fun AppointmentPlanningScreen(
    appointments: List<CareAppointment>,
    onSchedule: () -> Unit,
    onOpenHistory: () -> Unit,
    onViewAppointment: (CareAppointment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CareAppointmentTrackerCard(
            appointments = appointments,
            onSchedule = onSchedule,
            onOpenHistory = onOpenHistory,
            onViewAppointment = onViewAppointment
        )
        RebuildInsetPanel {
            Text(
                text = "Plan transportation, questions, documents, reminders, and follow-up before the appointment.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskSectionTabs(
    labels: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    labels.chunked(2).forEachIndexed { rowIndex, rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEachIndexed { itemIndex, label ->
                val index = rowIndex * 2 + itemIndex
                FilterChip(
                    selected = selected == index,
                    onClick = { onSelected(index) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowItems.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
