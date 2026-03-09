package com.upipulse.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.ui.common.openAppDetails
import com.upipulse.ui.components.CategorySpendList
import com.upipulse.ui.components.MetricCard
import com.upipulse.ui.components.MerchantSpendList
import com.upipulse.ui.components.SpendDonutChart
import com.upipulse.ui.components.formatInr
import com.upipulse.ui.components.formatInrLabel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    when (val state = uiState) {
        DashboardUiState.Loading -> LoadingState(modifier)
        is DashboardUiState.Error -> ErrorState(message = state.message, modifier = modifier)
        is DashboardUiState.Ready -> {
            val hasData = state.summary.totalInflow > 0 ||
                state.summary.totalOutflow > 0 ||
                state.summary.categoryBreakdown.isNotEmpty()
            if (!hasData) {
                DashboardEmptyState(onGrantPermissions = { openAppDetails(context) }, modifier = modifier)
            } else {
                DashboardContent(state, modifier)
            }
        }
    }
}

@Composable
private fun DashboardContent(state: DashboardUiState.Ready, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            OverviewHero(state)
        }
        if (state.summary.categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Spending mix",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SpendDonutChart(
                        breakdown = state.summary.categoryBreakdown,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
        if (state.summary.topMerchants.isNotEmpty()) {
            item {
                Text(
                    text = "Top merchants",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                MerchantSpendList(
                    merchants = state.summary.topMerchants,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        if (state.summary.categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                CategorySpendList(
                    categories = state.summary.categoryBreakdown,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun OverviewHero(state: DashboardUiState.Ready) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFF9333EA))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This month", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Text(
                    text = formatInr(state.summary.totalOutflow),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Text(
                    text = "Spent so far",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickMetricChip(label = "Avg ticket", value = formatInr(state.summary.avgTicketSize))
                    QuickMetricChip(label = "Cashback", value = formatInr(state.summary.cashbackTotal))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Outflow",
                value = formatInr(state.summary.totalOutflow),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Inflow",
                value = formatInr(state.summary.totalInflow),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickMetricChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DashboardEmptyState(onGrantPermissions: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No data yet",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Grant SMS and notification access to start tracking UPI payments.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(onClick = onGrantPermissions) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Syncing your wallet...")
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Unable to load dashboard")
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}
