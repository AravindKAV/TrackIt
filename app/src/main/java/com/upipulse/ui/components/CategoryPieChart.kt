package com.upipulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.upipulse.domain.model.CategoryBreakdown
import com.upipulse.util.formatInr

private val ChartColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6)  // Violet
)

@Composable
fun CategoryPieChart(
    data: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = data.sumOf { it.total }
    
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val diameter = min(maxWidth, 200.dp)
            
            // Background ring
            Canvas(modifier = Modifier.size(diameter)) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    style = Stroke(width = 40f)
                )
            }

            Canvas(modifier = Modifier.size(diameter)) {
                var startAngle = -90f
                data.take(ChartColors.size).forEachIndexed { index, item ->
                    val sweep = ((item.total / total) * 360f).toFloat()
                    if (sweep > 0) {
                        drawArc(
                            color = ChartColors[index % ChartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 40f, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweep
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatInr(total),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.take(ChartColors.size).chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEachIndexed { index, item ->
                        val colorIndex = data.indexOf(item)
                        LegendItem(
                            item = item,
                            color = ChartColors[colorIndex % ChartColors.size],
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    item: CategoryBreakdown,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Text(
                text = formatInr(item.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
