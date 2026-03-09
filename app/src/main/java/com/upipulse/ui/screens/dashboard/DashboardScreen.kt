package com.upipulse.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.ui.components.CategorySpendList
import com.upipulse.ui.components.MetricCard
import com.upipulse.ui.components.MerchantSpendList
import com.upipulse.ui.components.formatInr
import com.upipulse.ui.components.formatInrLabel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        DashboardUiState.Loading -> LoadingState(modifier)
        is DashboardUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Failed to load dashboard")
                Text(state.message, style = MaterialTheme.typography.bodySmall)
            }
        }
        is DashboardUiState.Ready -> DashboardContent(state, modifier)
    }
}

@Composable
private fun DashboardContent(state: DashboardUiState.Ready, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("This month's overview", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Total Outflow",
                    value = formatInr(state.summary.totalOutflow),
                    supportingText = formatInrLabel("Avg ticket", state.summary.avgTicketSize)
                )
                MetricCard(
                    title = "Total Inflow",
                    value = formatInr(state.summary.totalInflow),
                    supportingText = formatInrLabel("Cashback", state.summary.cashbackTotal)
                )
            }
        }
        if (state.summary.topMerchants.isNotEmpty()) {
            item {
                Text("Top Merchants", style = MaterialTheme.typography.titleMedium)
            }
            item {
                MerchantSpendList(state.summary.topMerchants)
            }
        }
        if (state.summary.categoryBreakdown.isNotEmpty()) {
            item {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
            }
            item {
                CategorySpendList(state.summary.categoryBreakdown)
            }
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
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Preparing analytics…")
    }
}