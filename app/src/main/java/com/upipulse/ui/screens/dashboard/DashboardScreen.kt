package com.upipulse.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.domain.model.AccountSpending
import com.upipulse.domain.model.DashboardAnalytics
import com.upipulse.ui.components.CategoryPieChart
import com.upipulse.ui.components.TransactionRow
import com.upipulse.ui.components.WeeklySpendingBarChart
import com.upipulse.util.formatInr
import kotlin.math.absoluteValue

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    when (val uiState = state) {
        DashboardUiState.Loading -> LoadingState(modifier)
        is DashboardUiState.Ready -> DashboardContent(
            analytics = uiState.analytics,
            isDemo = uiState.isDemo,
            onAddTransaction = onAddTransaction,
            modifier = modifier
        )
    }
}

@Composable
private fun DashboardContent(
    analytics: DashboardAnalytics,
    isDemo: Boolean,
    onAddTransaction: () -> Unit,
    modifier: Modifier
) {
    val topCategory = analytics.categoryBreakdown.firstOrNull()
    val peakDay = analytics.weeklyTrend.maxByOrNull { it.amount }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DashboardHeroCard(
                total = analytics.monthlyTotal,
                isDemo = isDemo,
                topCategory = topCategory?.category,
                topCategoryAmount = topCategory?.total ?: 0.0,
                peakDayLabel = peakDay?.day?.name,
                peakDayAmount = peakDay?.amount ?: 0.0,
                onAddTransaction = onAddTransaction
            )
        }
        
        if (analytics.accountSpending.isNotEmpty()) {
            item { 
                SectionHeader(
                    title = "Bank Accounts", 
                    subtitle = "Manage your spending across different banks" 
                ) 
            }
            items(analytics.accountSpending) { account ->
                AccountInsightCard(spending = account)
            }
        }
        
        if (analytics.categoryBreakdown.isNotEmpty() || analytics.weeklyTrend.isNotEmpty()) {
            item { SectionHeader(title = "Analytics & Trends") }
            
            if (analytics.categoryBreakdown.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Category Distribution", 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            CategoryPieChart(data = analytics.categoryBreakdown, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (analytics.weeklyTrend.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Weekly Spending", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Peak spending on ${peakDay?.day?.name ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            WeeklySpendingBarChart(
                                data = analytics.weeklyTrend, 
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                    }
                }
            }
        }

        if (analytics.recentTransactions.isNotEmpty()) {
            item { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Recent Activity")
                    Text(
                        "View All",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            items(analytics.recentTransactions) { transaction ->
                TransactionRow(transaction = transaction, modifier = Modifier.fillMaxWidth())
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun DashboardHeroCard(
    total: Double,
    isDemo: Boolean,
    topCategory: String?,
    topCategoryAmount: Double,
    peakDayLabel: String?,
    peakDayAmount: Double,
    onAddTransaction: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFFA855F7), // Purple
            Color(0xFFEC4899)  // Pink
        )
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        if (isDemo) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "DEMO MODE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            "Total Spent", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            formatInr(total), 
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp
                            ), 
                            color = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = onAddTransaction,
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color(0xFF6366F1)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickStatChip(
                        label = "Top Category",
                        value = topCategory ?: "None",
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatChip(
                        label = "Peak Day",
                        value = peakDayLabel ?: "None",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInsightCard(spending: AccountSpending) {
    val accent = accountAccentColor(spending.account.id)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = spending.account.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    spending.account.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Balance: ${formatInr(spending.balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatInr(spending.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            title, 
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                value, 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

private val AccentPalette = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF0EA5E9), // Sky
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444)  // Red
)

private fun accountAccentColor(id: Long): Color =
    if (AccentPalette.isEmpty()) Color(0xFF6366F1)
    else AccentPalette[(id.toInt().absoluteValue) % AccentPalette.size]
