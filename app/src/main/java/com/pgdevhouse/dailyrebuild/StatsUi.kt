package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pgdevhouse.dailyrebuild.ui.stats.StatsChart
import com.pgdevhouse.dailyrebuild.ui.stats.StatsChartType
import com.pgdevhouse.dailyrebuild.ui.stats.StatsFilter
import com.pgdevhouse.dailyrebuild.ui.stats.StatsMetric
import com.pgdevhouse.dailyrebuild.ui.stats.StatsPoint
import com.pgdevhouse.dailyrebuild.ui.stats.StatsRange
import com.pgdevhouse.dailyrebuild.ui.stats.StatsUiState

@Composable
fun StatsScreen(
    state: StatsUiState,
    onRangeSelected: (StatsRange) -> Unit,
    onFilterSelected: (StatsFilter) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHistoryDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubScreenHeader(
            title = "Stats",
            subtitle = "Averages, comparisons, trends, goals, and data coverage without treating missing days as zero.",
            onOpenHistory = onOpenHistory
        )

        StatsChipGrid(
            labels = StatsRange.entries.map { it.label },
            selectedIndex = state.selectedRange.ordinal,
            onSelected = { onRangeSelected(StatsRange.entries[it]) }
        )

        StatsPeriodNavigator(
            label = state.periodLabel,
            canMoveNext = state.canMoveNext,
            onPrevious = onPreviousPeriod,
            onNext = onNextPeriod
        )

        StatsChipGrid(
            labels = StatsFilter.entries.map { it.label },
            selectedIndex = state.selectedFilter.ordinal,
            columns = 3,
            onSelected = { onFilterSelected(StatsFilter.entries[it]) }
        )

        when {
            state.isLoading -> StatsLoadingPanel()
            state.errorMessage != null -> StatsErrorPanel(
                message = state.errorMessage,
                onRetry = onRefresh
            )
            else -> {
                StatsMetricGrid(state.selectedSection.metrics)

                state.selectedSection.charts.forEach { chart ->
                    InteractiveStatsChart(
                        chart = chart,
                        onOpenHistoryDate = onOpenHistoryDate
                    )
                }

                state.selectedSection.notes.forEach { note ->
                    RebuildInsetPanel {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatsPeriodNavigator(
    label: String,
    canMoveNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onPrevious) {
                Text("‹ Previous")
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onNext,
                enabled = canMoveNext
            ) {
                Text("Next ›")
            }
        }
    }
}

@Composable
private fun StatsMetricGrid(
    metrics: List<StatsMetric>
) {
    metrics.chunked(2).forEach { rowMetrics ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowMetrics.forEach { metric ->
                StatsMetricCard(
                    metric = metric,
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowMetrics.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatsMetricCard(
    metric: StatsMetric,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (metric.detail.isNotBlank()) {
                Text(
                    text = metric.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric.comparison.isNotBlank()) {
                Text(
                    text = metric.comparison,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InteractiveStatsChart(
    chart: StatsChart,
    onOpenHistoryDate: (String) -> Unit
) {
    var selectedPoint by remember(chart.title, chart.points) {
        mutableStateOf<StatsPoint?>(null)
    }

    RebuildSectionCard(
        title = chart.title,
        subtitle = chart.subtitle,
        accentColor = if (chart.type == StatsChartType.LINE) RebuildBlue else RebuildGreen
    ) {
        val availablePoints = chart.points.filter { it.hasData }

        if (availablePoints.isEmpty()) {
            Text(
                text = "No data was recorded for this chart in the selected period.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedPoint?.let { point ->
                StatsPointDetail(
                    point = point,
                    onOpenHistoryDate = onOpenHistoryDate
                )
            }

            StatsChartCanvas(
                chart = chart,
                selectedPoint = selectedPoint,
                onPointSelected = { selectedPoint = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    chart.points.firstOrNull()?.label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap a point or bar for its exact value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    chart.points.lastOrNull()?.label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsPointDetail(
    point: StatsPoint,
    onOpenHistoryDate: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(point.label, fontWeight = FontWeight.SemiBold)
                Text(
                    point.valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            point.historyDate?.let { date ->
                OutlinedButton(onClick = { onOpenHistoryDate(date) }) {
                    Text("Open Date")
                }
            }
        }
    }
}

@Composable
private fun StatsChartCanvas(
    chart: StatsChart,
    selectedPoint: StatsPoint?,
    onPointSelected: (StatsPoint) -> Unit
) {
    val points = chart.points
    val chartWidth = maxOf(330, points.size * 34).dp
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val missing = MaterialTheme.colorScheme.surfaceVariant
    val selected = MaterialTheme.colorScheme.tertiary
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(chartWidth)
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isEmpty()) return@detectTapGestures
                        val segmentWidth = size.width / points.size.coerceAtLeast(1)
                        val index = (offset.x / segmentWidth)
                            .toInt()
                            .coerceIn(0, points.lastIndex)
                        onPointSelected(points[index])
                    }
                }
        ) {
            val left = 12.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val usableWidth = (right - left).coerceAtLeast(1f)
            val usableHeight = (bottom - top).coerceAtLeast(1f)
            val validValues = points.filter { it.hasData }.map { it.value }
            val rawMaximum = validValues.maxOrNull() ?: 1.0
            val rawMinimum = validValues.minOrNull() ?: 0.0
            val minimumValue = if (chart.type == StatsChartType.LINE && rawMinimum > 0.0) {
                val spread = (rawMaximum - rawMinimum).coerceAtLeast(1.0)
                (rawMinimum - spread * 0.15).coerceAtLeast(0.0)
            } else {
                0.0
            }
            val valueRange = (rawMaximum - minimumValue).coerceAtLeast(1.0)

            repeat(4) { lineIndex ->
                val y = top + usableHeight * lineIndex / 3f
                drawLine(
                    color = grid,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (chart.type == StatsChartType.BAR) {
                val slotWidth = usableWidth / points.size.coerceAtLeast(1)
                val barWidth = (slotWidth * 0.62f).coerceAtLeast(4.dp.toPx())
                points.forEachIndexed { index, point ->
                    val xCenter = left + slotWidth * (index + 0.5f)
                    val barHeight = if (point.hasData) {
                        (point.value / rawMaximum.coerceAtLeast(1.0) * usableHeight)
                            .toFloat()
                            .coerceAtLeast(2.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                    drawRoundRect(
                        color = when {
                            point == selectedPoint -> selected
                            point.hasData -> secondary
                            else -> missing
                        },
                        topLeft = Offset(xCenter - barWidth / 2f, bottom - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                    )
                }
            } else {
                val spacing = if (points.size <= 1) 0f else usableWidth / (points.size - 1)
                var previous: Pair<Offset, StatsPoint>? = null
                points.forEachIndexed { index, point ->
                    val x = left + spacing * index
                    if (!point.hasData) {
                        previous = null
                        return@forEachIndexed
                    }
                    val y = bottom - ((point.value - minimumValue) / valueRange * usableHeight).toFloat()
                    val current = Offset(x, y)
                    previous?.let { (previousOffset, _) ->
                        drawLine(
                            color = primary,
                            start = previousOffset,
                            end = current,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    drawCircle(
                        color = if (point == selectedPoint) selected else primary,
                        radius = if (point == selectedPoint) 6.dp.toPx() else 4.dp.toPx(),
                        center = current
                    )
                    if (point == selectedPoint) {
                        drawCircle(
                            color = selected,
                            radius = 9.dp.toPx(),
                            center = current,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    previous = current to point
                }
            }
        }
    }
}

@Composable
private fun StatsLoadingPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text("Calculating statistics…")
    }
}

@Composable
private fun StatsErrorPanel(
    message: String,
    onRetry: () -> Unit
) {
    RebuildSectionCard(
        title = "Stats could not load",
        accentColor = MaterialTheme.colorScheme.error
    ) {
        Text(message)
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun StatsChipGrid(
    labels: List<String>,
    selectedIndex: Int,
    columns: Int = 2,
    onSelected: (Int) -> Unit
) {
    labels.chunked(columns).forEachIndexed { rowIndex, rowLabels ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowLabels.forEachIndexed { itemIndex, label ->
                val index = rowIndex * columns + itemIndex
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(columns - rowLabels.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
