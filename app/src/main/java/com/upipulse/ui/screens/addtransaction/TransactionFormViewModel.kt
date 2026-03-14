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
    val targetAccountId: Long? = null,
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val isCredit: Boolean = false,
    val isTransfer: Boolean = false,
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

    private val _allCategories = MutableStateFlow<List<String>>(emptyList())
    private val _filteredCategories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _filteredCategories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val eventsChannel = Channel<TransactionFormEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCategoriesUseCase().collect { list ->
                val distinct = list.map { it.name }.distinct()
                _allCategories.value = distinct
                updateFilteredCategories()
            }
        }
        viewModelScope.launch {
            observeAccountsUseCase().collect { list ->
                _accounts.value = list
                if (_state.value.accountId == null && list.isNotEmpty()) {
                    _state.update { it.copy(accountId = list.first().id) }
                }
            }
        }
        transactionId?.let { id ->
            viewModelScope.launch {
                observeTransactionUseCase(id).filterNotNull().first().also { transaction ->
                    _state.value = _state.value.copy(
                        amount = transaction.amount.absoluteValue.toString(),
                        merchant = transaction.merchant,
                        category = transaction.category,
                        paymentMethod = transaction.paymentMethod,
                        date = transaction.date.atZone(zoneId).toLocalDate(),
                        notes = transaction.notes.orEmpty(),
                        accountId = transaction.account.id,
                        source = transaction.source,
                        isEdit = true,
                        isCredit = transaction.amount > 0,
                        isTransfer = transaction.category == "Transfer"
                    )
                    updateFilteredCategories()
                }
            }
        }
    }

    private fun updateFilteredCategories() {
        val s = _state.value
        val creditNames = listOf("Salary", "Refunds", "Cashback", "Interest", "Gift Received", "Rental Income", "Others")
        val filtered = if (s.isTransfer) {
            listOf("Transfer")
        } else if (s.isCredit) {
            _allCategories.value.filter { it in creditNames }
        } else {
            _allCategories.value.filter { it !in creditNames && it != "Transfer" || it == "Others" }
        }
        _filteredCategories.value = filtered
        if (s.category !in filtered && filtered.isNotEmpty()) {
            _state.update { it.copy(category = filtered.first()) }
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

    fun updateTargetAccount(accountId: Long) {
        _state.update { it.copy(targetAccountId = accountId) }
    }

    fun updateType(isCredit: Boolean, isTransfer: Boolean) {
        _state.update { it.copy(isCredit = isCredit, isTransfer = isTransfer) }
        if (isTransfer) _state.update { it.copy(category = "Transfer") }
        updateFilteredCategories()
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val validationError = validate(current)
            if (validationError != null) {
                eventsChannel.send(TransactionFormEvent.Error(validationError))
                return@launch
            }
            _state.update { it.copy(isSaving = true) }
            
            if (current.isTransfer) {
                handleTransfer(current)
            } else {
                handleNormalSave(current)
            }
        }
    }

    private suspend fun handleTransfer(s: TransactionFormState) {
        val amount = s.amount.toDouble()
        val fromAcc = _accounts.value.first { it.id == s.accountId }
        val toAcc = _accounts.value.first { it.id == s.targetAccountId }
        
        val debit = Transaction(
            amount = -amount,
            merchant = "Transfer to ${toAcc.name}",
            category = "Transfer",
            paymentMethod = s.paymentMethod,
            date = s.date.atStartOfDay(zoneId).toInstant(),
            notes = s.notes,
            source = TransactionSource.MANUAL,
            account = fromAcc.toSummary()
        )
        val credit = Transaction(
            amount = amount,
            merchant = "Transfer from ${fromAcc.name}",
            category = "Transfer",
            paymentMethod = s.paymentMethod,
            date = s.date.atStartOfDay(zoneId).toInstant(),
            notes = s.notes,
            source = TransactionSource.MANUAL,
            account = toAcc.toSummary()
        )
        
        runCatching {
            insertTransactionUseCase(debit)
            insertTransactionUseCase(credit)
        }.onSuccess {
            eventsChannel.send(TransactionFormEvent.Saved("Transfer completed"))
        }.onFailure {
            eventsChannel.send(TransactionFormEvent.Error(it.message ?: "Transfer failed"))
        }
        _state.update { it.copy(isSaving = false) }
    }

    private suspend fun handleNormalSave(s: TransactionFormState) {
        val amount = s.amount.toDouble().let { if (s.isCredit) it else -it }
        val acc = _accounts.value.first { it.id == s.accountId }
        val txn = Transaction(
            id = transactionId ?: 0L,
            amount = amount,
            merchant = s.merchant.trim(),
            category = s.category,
            paymentMethod = s.paymentMethod,
            date = s.date.atStartOfDay(zoneId).toInstant(),
            notes = s.notes.ifBlank { null },
            source = s.source,
            account = acc.toSummary()
        )
        runCatching {
            if (s.isEdit) updateTransactionUseCase(txn) else insertTransactionUseCase(txn)
        }.onSuccess {
            eventsChannel.send(TransactionFormEvent.Saved("Transaction saved"))
        }.onFailure {
            eventsChannel.send(TransactionFormEvent.Error(it.message ?: "Failed to save"))
        }
        _state.update { it.copy(isSaving = false) }
    }

    private fun validate(s: TransactionFormState): String? {
        if (s.amount.toDoubleOrNull() == null || s.amount.toDouble() <= 0) return "Invalid amount"
        if (s.accountId == null) return "Source account required"
        if (s.isTransfer) {
            if (s.targetAccountId == null) return "Destination account required"
            if (s.accountId == s.targetAccountId) return "Cannot transfer to same account"
        } else {
            if (s.merchant.isBlank()) return "Merchant required"
        }
        return null
    }
}

private val Double.absoluteValue: Double
    get() = if (this < 0) -this else this
