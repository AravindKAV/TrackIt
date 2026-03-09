package com.upipulse.ui.screens.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.domain.model.Merchant
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionDirection
import com.upipulse.domain.model.TransactionSource
import com.upipulse.ui.components.formatInr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    when (val uiState = state) {
        TimelineUiState.Loading -> Loading()
        is TimelineUiState.Error -> Error(uiState.message)
        is TimelineUiState.Ready -> TimelineContent(uiState.transactions, modifier)
    }
}

@Composable
private fun TimelineContent(transactions: List<Transaction>, modifier: Modifier) {
    val filter = remember { mutableStateOf(TimelineFilter.ALL) }
    val filtered = transactions.filter { filter.value.allows(it.direction) }
    val hasRealData = filtered.isNotEmpty()
    val listToRender = if (hasRealData) filtered else sampleTransactions()
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TimelineHeader(transactionsCount = transactions.size, filter = filter.value, onFilterChange = { filter.value = it })
        }
        if (!hasRealData) {
            item {
                Text(
                    text = "No transactions yet. Showing demo data until we parse your SMS/notifications.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(listToRender, key = { it.referenceId }) { transaction ->
            TransactionRow(transaction)
        }
        if (!hasRealData) {
            item {
                Text(
                    text = "Grant permissions to replace demo data with your actual UPI history.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineHeader(transactionsCount: Int, filter: TimelineFilter, onFilterChange: (TimelineFilter) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Timeline", style = MaterialTheme.typography.titleLarge)
        Text("$transactionsCount transactions tracked", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimelineFilter.values().forEach { item ->
                AssistChip(
                    onClick = { onFilterChange(item) },
                    label = { Text(item.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (item == filter) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (item == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val color = if (transaction.direction == TransactionDirection.CREDIT)
        MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = transaction.merchant.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = transaction.rawDescription,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatInr(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
            Text(
                text = transaction.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Error(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Unable to load transactions")
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

private enum class TimelineFilter(val label: String) {
    ALL("All"),
    DEBIT("Outgoing"),
    CREDIT("Incoming");

    fun allows(direction: TransactionDirection): Boolean =
        this == ALL ||
            (this == DEBIT && direction == TransactionDirection.DEBIT) ||
            (this == CREDIT && direction == TransactionDirection.CREDIT)
}

private fun sampleTransactions(): List<Transaction> = listOf(
    Transaction(
        referenceId = "demo1",
        merchant = Merchant(id = "swiggy", name = "Swiggy"),
        category = "Food",
        amount = 425.0,
        currency = "INR",
        direction = TransactionDirection.DEBIT,
        timestamp = Instant.now(),
        source = TransactionSource.SMS,
        rawDescription = "Paid via UPI - Swiggy Order",
        metadata = emptyMap()
    ),
    Transaction(
        referenceId = "demo2",
        merchant = Merchant(id = "amazon", name = "Amazon"),
        category = "Shopping",
        amount = 1899.0,
        currency = "INR",
        direction = TransactionDirection.DEBIT,
        timestamp = Instant.now(),
        source = TransactionSource.NOTIFICATION,
        rawDescription = "Order #AB1234",
        metadata = emptyMap()
    ),
    Transaction(
        referenceId = "demo3",
        merchant = Merchant(id = "gpay", name = "Google Pay Cashback"),
        category = "Cashback",
        amount = 75.0,
        currency = "INR",
        direction = TransactionDirection.CREDIT,
        timestamp = Instant.now(),
        source = TransactionSource.NOTIFICATION,
        rawDescription = "Cashback credited",
        metadata = emptyMap()
    )
)