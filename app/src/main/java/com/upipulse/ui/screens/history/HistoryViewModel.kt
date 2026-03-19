package com.upipulse.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.usecase.DeleteTransactionUseCase
import com.upipulse.domain.usecase.ObserveCategoriesUseCase
import com.upipulse.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MonthlyHistory(
    val monthName: String,
    val totalSpent: Double,
    val totalEarned: Double,
    val transactions: List<Transaction>
)

data class HistoryUiState(
    val monthlyHistory: List<MonthlyHistory> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val selectedAccountId: Long? = null,
    val isLoading: Boolean = true
)

sealed interface HistoryEvent {
    data class Deleted(val message: String) : HistoryEvent
    data class Error(val message: String) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val initialAccountId: Long? = savedStateHandle.get<Long>("accountId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(HistoryUiState(selectedAccountId = initialAccountId))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<HistoryEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                observeTransactionsUseCase(),
                observeCategoriesUseCase()
            ) { transactions, categories ->
                val categoryNames = (listOf("All") + categories.map { it.name }).distinct()
                val currentCategory = _uiState.value.selectedCategory
                val currentAccountId = _uiState.value.selectedAccountId
                
                val filtered = filterTransactions(transactions, currentCategory, currentAccountId)
                
                _uiState.value.copy(
                    monthlyHistory = groupTransactionsByMonth(filtered),
                    categories = categoryNames,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateFilter(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        // The combined flow in init will react to this change if we use a state flow for filters
        // but here we are just updating the UI state directly. 
        // Let's fix the init block to use StateFlows for filters for better reactivity.
    }

    // Better approach for reactivity
    private val _selectedCategory = MutableStateFlow("All")
    private val _selectedAccountId = MutableStateFlow<Long?>(initialAccountId)

    init {
        // Redo the combined flow properly
        viewModelScope.launch {
            combine(
                observeTransactionsUseCase(),
                observeCategoriesUseCase(),
                _selectedCategory,
                _selectedAccountId
            ) { transactions, categories, selCat, selAccId ->
                val categoryNames = (listOf("All") + categories.map { it.name }).distinct()
                val filtered = filterTransactions(transactions, selCat, selAccId)
                
                HistoryUiState(
                    monthlyHistory = groupTransactionsByMonth(filtered),
                    categories = categoryNames,
                    selectedCategory = selCat,
                    selectedAccountId = selAccId,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            runCatching { deleteTransactionUseCase(transaction) }
                .onSuccess { eventsChannel.send(HistoryEvent.Deleted("Transaction deleted")) }
                .onFailure { eventsChannel.send(HistoryEvent.Error(it.message.orEmpty())) }
        }
    }

    private fun filterTransactions(
        transactions: List<Transaction>, 
        category: String, 
        accountId: Long?
    ): List<Transaction> {
        return transactions.filter { txn ->
            val matchesCategory = if (category == "All" || category.isBlank()) true else txn.category == category
            val matchesAccount = if (accountId == null) true else txn.account.id == accountId
            matchesCategory && matchesAccount
        }
    }

    private fun groupTransactionsByMonth(transactions: List<Transaction>): List<MonthlyHistory> {
        val zoneId = ZoneId.systemDefault()
        return transactions.groupBy {
            val date = it.date.atZone(zoneId)
            "${date.year}-${date.monthValue}"
        }.map { (key, txns) ->
            val date = txns.first().date.atZone(zoneId)
            val monthName = "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
            val totalSpent = txns.filter { it.amount < 0 }.sumOf { it.amount.absoluteValue }
            val totalEarned = txns.filter { it.amount > 0 }.sumOf { it.amount }
            
            MonthlyHistory(
                monthName = monthName,
                totalSpent = totalSpent,
                totalEarned = totalEarned,
                transactions = txns.sortedByDescending { it.date }
            )
        }.sortedByDescending { it.transactions.first().date }
    }
}

private val Double.absoluteValue: Double
    get() = if (this < 0) -this else this
