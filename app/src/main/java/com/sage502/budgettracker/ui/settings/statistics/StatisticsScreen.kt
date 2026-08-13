@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage502.budgettracker.util.formatAmount

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val rangeMonths by viewModel.rangeMonths.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통계") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(3, 6, 12).forEachIndexed { index, months ->
                    SegmentedButton(
                        selected = rangeMonths == months,
                        onClick = { viewModel.setRange(months) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        label = { Text("${months}개월") }
                    )
                }
            }

            if (stats.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                BarChart(stats = stats, modifier = Modifier.fillMaxWidth().height(260.dp))

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "지출")
                    LegendItem(color = MaterialTheme.colorScheme.error, label = "예산 초과분")
                    LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "예산")
                }

                // Monthly summary list
                stats.forEach { s ->
                    MonthSummaryRow(s)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawRect(color = color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BarChart(stats: List<MonthStats>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val budgetColor = MaterialTheme.colorScheme.surfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val barWidth = (size.width / stats.size) * 0.6f
        val barSpacing = size.width / stats.size
        val maxValue = stats.maxOf { maxOf(it.totalBudget, it.totalSpent) }.coerceAtLeast(1L)
        val chartHeight = size.height - 40f
        val labelStyle = TextStyle(fontSize = 9.sp, color = Color.Gray)

        stats.forEachIndexed { index, s ->
            val x = index * barSpacing + (barSpacing - barWidth) / 2f
            val budgetBarH = (s.totalBudget.toFloat() / maxValue) * chartHeight
            val spentBarH = (s.totalSpent.toFloat() / maxValue) * chartHeight
            val baselineY = chartHeight

            // Budget bar (background)
            drawRect(
                color = budgetColor,
                topLeft = Offset(x, baselineY - budgetBarH),
                size = Size(barWidth, budgetBarH)
            )

            // Spent bar
            val spentWithinBudget = minOf(s.totalSpent, s.totalBudget)
            val spentH = (spentWithinBudget.toFloat() / maxValue) * chartHeight
            if (spentH > 0) {
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, baselineY - spentH),
                    size = Size(barWidth, spentH)
                )
            }

            // Overspent bar (stacked on top)
            if (s.totalSpent > s.totalBudget) {
                val overH = ((s.totalSpent - s.totalBudget).toFloat() / maxValue) * chartHeight
                drawRect(
                    color = errorColor,
                    topLeft = Offset(x, baselineY - spentH - overH),
                    size = Size(barWidth, overH)
                )
            }

            // Month label
            val label = s.monthKey.takeLast(2) + "월"
            val textResult = textMeasurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x + (barWidth - textResult.size.width) / 2f,
                    baselineY + 4f
                )
            )
        }
    }
}

@Composable
private fun MonthSummaryRow(stats: MonthStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stats.monthKey,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("지출 ${formatAmount(stats.totalSpent)}", style = MaterialTheme.typography.bodySmall)
                Text("예산 ${formatAmount(stats.totalBudget)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
