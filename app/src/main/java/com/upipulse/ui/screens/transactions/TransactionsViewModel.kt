package com.upipulse.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.usecase.DeleteTransactionUseCase
import com.upipulse.domain.usecase.ObserveCategoriesUseCase
import com.upipulse.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TransactionsEvent {
    data class Deleted(val message: String) : TransactionsEvent
    data class Error(val message: String) : TransactionsEvent
}

data class TransactionsUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val displayedTransactions: List<Transaction> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    observeTransactionsUseCase: ObserveTransactionsUseCase,
    observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<TransactionsEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                observeTransactionsUseCase(),
                observeCategoriesUseCase()
            ) { transactions, categories ->
                val sorted = transactions.sortedByDescending { it.date }
                val categoryNames = listOf("All") + categories.map { it.name }
                val currentFilter = _uiState.value.selectedCategory
                val filtered = filterTransactions(sorted, currentFilter)
                _uiState.value.copy(
                    allTransactions = sorted,
                    displayedTransactions = filtered,
                    categories = categoryNames,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateFilter(category: String) {
        _uiState.update { state ->
            val filtered = filterTransactions(state.allTransactions, category)
            state.copy(selectedCategory = category, displayedTransactions = filtered)
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            runCatching { deleteTransactionUseCase(transaction) }
                .onSuccess { eventsChannel.send(TransactionsEvent.Deleted("Transaction deleted")) }
                .onFailure { eventsChannel.send(TransactionsEvent.Error(it.message.orEmpty())) }
        }
    }

    private fun filterTransactions(transactions: List<Transaction>, category: String): List<Transaction> =
        if (category == "All" || category.isBlank()) transactions else transactions.filter { it.category == category }
}
