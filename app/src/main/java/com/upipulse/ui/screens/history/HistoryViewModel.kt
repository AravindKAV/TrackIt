package com.upipulse.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val monthlyHistory: List<MonthlyHistory> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTransactionsUseCase().collectLatest { transactions ->
                _uiState.value = HistoryUiState(
                    monthlyHistory = groupTransactionsByMonth(transactions)
                )
            }
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
