package com.upipulse.ui.screens.transactions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.ui.components.TransactionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionsEvent.Deleted -> onMessage(event.message)
                is TransactionsEvent.Error -> onMessage(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Transactions", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Keep track of all your UPI payments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.categories) { category ->
                    val isSelected = category == state.selectedCategory
                    AssistChip(
                        onClick = { viewModel.updateFilter(category) },
                        label = { Text(category) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            enabled = true
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (state.displayedTransactions.isEmpty()) {
                EmptyState(onAddTransaction)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.displayedTransactions, key = { it.id }) { transaction ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.delete(transaction)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else Color.Transparent
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp)
                                        .background(color, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            },
                            content = {
                                TransactionRow(
                                    transaction = transaction,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onEditTransaction(transaction.id) }
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddTransaction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No transactions yet", 
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Add your first transaction manually or enable detectors to see them here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
