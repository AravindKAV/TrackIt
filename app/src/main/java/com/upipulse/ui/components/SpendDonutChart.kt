package com.upipulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.upipulse.domain.model.CategorySpend

private val chartColors = listOf(
    Color(0xFF6366F1),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF14B8A6),
    Color(0xFFF97316),
    Color(0xFF22D3EE)
)

@Composable
fun SpendDonutChart(
    breakdown: List<CategorySpend>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp
) {
    if (breakdown.isEmpty() || breakdown.sumOf { it.amount } == 0.0) {
        Text(
            text = "Add transactions to unlock category insights",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        return
    }
    val total = breakdown.sumOf { it.amount }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(220.dp)) {
            var startAngle = -90f
            breakdown.take(chartColors.size).forEachIndexed { index, item ->
                val sweep = ((item.amount / total) * 360f).toFloat()
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            breakdown.take(chartColors.size).forEachIndexed { index, item ->
                LegendRow(
                    label = item.category,
                    amount = formatInr(item.amount),
                    color = chartColors[index % chartColors.size]
                )
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, amount: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(14.dp)) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(text = amount, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun SpendDonutChartPreview() {
    val items = listOf(
        CategorySpend("Food", 5400.0),
        CategorySpend("Transport", 2200.0),
        CategorySpend("Shopping", 3400.0),
        CategorySpend("Bills", 1800.0)
    )
    SpendDonutChart(breakdown = items, modifier = Modifier.padding(16.dp))
}