package com.upipulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upipulse.domain.model.WeeklySpendingPoint
import com.upipulse.util.formatInr

@Composable
fun WeeklySpendingBarChart(
    data: List<WeeklySpendingPoint>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 160.dp
) {
    val maxValue = data.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { point ->
                val fraction = (point.amount / maxValue).toFloat()
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (point.amount > 0) {
                        Text(
                            text = if (point.amount >= 1000) "${(point.amount / 1000).toInt()}k" else point.amount.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = point.day.name.take(1),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (fraction >= 1f) FontWeight.Black else FontWeight.Medium,
                        color = if (fraction >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
