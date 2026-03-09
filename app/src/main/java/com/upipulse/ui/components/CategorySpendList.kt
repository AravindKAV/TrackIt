package com.upipulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.upipulse.domain.model.CategorySpend

@Composable
fun CategorySpendList(
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    val max = categories.maxOfOrNull { it.amount } ?: 0.0
    Column(modifier = modifier) {
        categories.forEach { spend ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(spend.category, style = MaterialTheme.typography.bodyLarge)
                    Text(formatInr(spend.amount), style = MaterialTheme.typography.bodyMedium)
                }
                LinearProgressIndicator(
                    progress = if (max == 0.0) 0f else (spend.amount / max).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth()
                )
                spend.budget?.let { budget ->
                    Text(
                        text = "Budget ${formatInr(budget)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}