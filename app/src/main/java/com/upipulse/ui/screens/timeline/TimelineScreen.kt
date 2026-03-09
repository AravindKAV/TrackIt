package com.upipulse.ui.screens.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.domain.model.Transaction
import com.upipulse.ui.components.formatInr
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
        is TimelineUiState.Ready -> TransactionList(uiState.transactions, modifier)
    }
}

@Composable
private fun TransactionList(transactions: List<Transaction>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions, key = { it.referenceId }) { transaction ->
            TransactionRow(transaction)
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
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
                style = MaterialTheme.typography.bodyLarge
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