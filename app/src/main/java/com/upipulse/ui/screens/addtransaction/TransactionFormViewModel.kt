package com.upipulse.ui.screens.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Account
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import com.upipulse.domain.usecase.InsertTransactionUseCase
import com.upipulse.domain.usecase.ObserveAccountsUseCase
import com.upipulse.domain.usecase.ObserveCategoriesUseCase
import com.upipulse.domain.usecase.ObserveTransactionUseCase
import com.upipulse.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TransactionFormEvent {
    data class Saved(val message: String) : TransactionFormEvent
    data class Error(val message: String) : TransactionFormEvent
}

data class TransactionFormState(
    val amount: String = "",
    val merchant: String = "",
    val category: String = "",
    val paymentMethod: String = "UPI",
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val accountId: Long? = null,
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCategoriesUseCase: ObserveCategoriesUseCase,
    observeAccountsUseCase: ObserveAccountsUseCase,
    observeTransactionUseCase: ObserveTransactionUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(TransactionFormState(isEdit = transactionId != null))
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val eventsChannel = Channel<TransactionFormEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCategoriesUseCase().collect { list ->
                _categories.value = list.map { it.name }
                if (_state.value.category.isEmpty() && list.isNotEmpty()) {
                    _state.update { it.copy(category = list.first().name) }
                }
            }
        }
        viewModelScope.launch {
            observeAccountsUseCase().collect { list ->
                _accounts.value = list
                if (_state.value.accountId == null && list.isNotEmpty()) {
                    _state.update { it.copy(accountId = list.first().id) }
                } else if (list.none { it.id == _state.value.accountId }) {
                    _state.update { it.copy(accountId = list.firstOrNull()?.id) }
                }
            }
        }
        transactionId?.let { id ->
            viewModelScope.launch {
                observeTransactionUseCase(id).filterNotNull().first().also { transaction ->
                    _state.value = _state.value.copy(
                        amount = transaction.amount.toString(),
                        merchant = transaction.merchant,
                        category = transaction.category,
                        paymentMethod = transaction.paymentMethod,
                        date = transaction.date.atZone(zoneId).toLocalDate(),
                        notes = transaction.notes.orEmpty(),
                        accountId = transaction.account.id,
                        source = transaction.source,
                        isEdit = true
                    )
                }
            }
        }
    }

    fun updateAmount(value: String) {
        _state.update { it.copy(amount = value.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun updateMerchant(value: String) {
        _state.update { it.copy(merchant = value) }
    }

    fun updateCategory(value: String) {
        _state.update { it.copy(category = value) }
    }

    fun updatePaymentMethod(value: String) {
        _state.update { it.copy(paymentMethod = value) }
    }

    fun updateDate(date: LocalDate) {
        _state.update { it.copy(date = date) }
    }

    fun updateNotes(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun updateAccount(accountId: Long) {
        _state.update { it.copy(accountId = accountId) }
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val validationError = validate(current)
            if (validationError != null) {
                _state.update { it.copy(errorMessage = validationError) }
                eventsChannel.send(TransactionFormEvent.Error(validationError))
                return@launch
            }
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val selectedAccount = _accounts.value.firstOrNull { it.id == current.accountId }
                ?: run {
                    val message = "Add a bank account in Settings before saving"
                    _state.update { it.copy(isSaving = false, errorMessage = message) }
                    eventsChannel.send(TransactionFormEvent.Error(message))
                    return@launch
                }
            val transaction = Transaction(
                id = transactionId ?: 0L,
                amount = current.amount.toDouble(),
                merchant = current.merchant.trim(),
                category = current.category,
                paymentMethod = current.paymentMethod,
                date = current.date.atStartOfDay(zoneId).toInstant(),
                notes = current.notes.ifBlank { null },
                source = current.source,
                account = selectedAccount.toSummary()
            )
            runCatching {
                if (current.isEdit) updateTransactionUseCase(transaction) else insertTransactionUseCase(transaction)
            }.onSuccess {
                eventsChannel.send(
                    TransactionFormEvent.Saved(
                        if (current.isEdit) "Transaction updated" else "Transaction added"
                    )
                )
                _state.update { it.copy(isSaving = false) }
            }.onFailure { throwable ->
                _state.update { it.copy(isSaving = false, errorMessage = throwable.message) }
                eventsChannel.send(TransactionFormEvent.Error(throwable.message.orEmpty()))
            }
        }
    }

    private fun validate(state: TransactionFormState): String? {
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) return "Enter an amount greater than 0"
        if (state.merchant.isBlank()) return "Merchant is required"
        if (state.category.isBlank()) return "Select a category"
        if (state.paymentMethod.isBlank()) return "Select a payment method"
        if (state.accountId == null) return "Select a bank account"
        return null
    }
}
