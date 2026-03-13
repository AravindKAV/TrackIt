package com.upipulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upipulse.domain.model.Transaction
import com.upipulse.util.formatInr
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransactionIcon(category = transaction.category)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.account.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatInr(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = transaction.date.atZone(ZoneId.systemDefault()).format(dateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionIcon(category: String) {
    val (icon, color) = when (category.lowercase()) {
        "food", "dining" -> Icons.Default.Fastfood to Color(0xFFF59E0B)
        "shopping" -> Icons.Default.ShoppingBag to Color(0xFFEC4899)
        "bills", "utilities" -> Icons.Default.Receipt to Color(0xFF3B82F6)
        "transport" -> Icons.Default.CreditCard to Color(0xFF10B981)
        else -> Icons.Default.Category to Color(0xFF6366F1)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}
