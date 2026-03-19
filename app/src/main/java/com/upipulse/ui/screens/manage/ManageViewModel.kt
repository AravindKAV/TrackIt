package com.upipulse.ui.screens.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Account
import com.upipulse.domain.model.Category
import com.upipulse.domain.model.CategoryType
import com.upipulse.domain.model.Mandate
import com.upipulse.domain.model.MandateType
import com.upipulse.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ManageEvent {
    data class Message(val text: String) : ManageEvent
}

data class ManageUiState(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val mandates: List<Mandate> = emptyList()
)

@HiltViewModel
class ManageViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManageUiState())
    val state: StateFlow<ManageUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<ManageEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeAccounts().collectLatest { accounts ->
                _state.value = _state.value.copy(accounts = accounts)
            }
        }
        viewModelScope.launch {
            repository.observeCategories().collectLatest { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
        viewModelScope.launch {
            repository.observeMandates().collectLatest { mandates ->
                _state.value = _state.value.copy(mandates = mandates)
            }
        }
    }

    fun addCategory(name: String, type: CategoryType) {
        viewModelScope.launch {
            val category = Category(id = 0, name = name.trim(), icon = "ic_custom", type = type)
            runCatching { repository.upsertCategory(category) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Category added")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun addAccount(name: String, bank: String, numberSuffix: String?, initialBalance: Double) {
        viewModelScope.launch {
            val account = Account(
                name = name.trim(),
                bankName = bank.trim().ifBlank { name },
                numberSuffix = numberSuffix?.ifBlank { null },
                balance = initialBalance
            )
            runCatching { repository.upsertAccount(account) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Account added")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun deleteAccount(accountId: Long) {
        val target = _state.value.accounts.firstOrNull { it.id == accountId } ?: return
        viewModelScope.launch {
            runCatching { repository.deleteAccount(target) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Account removed")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun addMandate(name: String, amount: Double, dueDay: Int, type: MandateType, category: String) {
        viewModelScope.launch {
            val mandate = Mandate(
                name = name.trim(),
                amount = amount,
                dueDay = dueDay,
                type = type,
                category = category
            )
            runCatching { repository.upsertMandate(mandate) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("${type.name} added")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun deleteMandate(mandate: Mandate) {
        viewModelScope.launch {
            runCatching { repository.deleteMandate(mandate) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Mandate removed")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }
}
