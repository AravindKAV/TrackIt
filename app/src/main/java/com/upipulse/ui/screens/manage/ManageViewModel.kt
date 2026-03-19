package com.upipulse.ui.screens.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Account
import com.upipulse.domain.model.Category
import com.upipulse.domain.model.CategoryType
import com.upipulse.domain.usecase.ObserveAccountsUseCase
import com.upipulse.domain.usecase.ObserveCategoriesUseCase
import com.upipulse.domain.usecase.UpsertAccountUseCase
import com.upipulse.domain.usecase.DeleteAccountUseCase
import com.upipulse.domain.usecase.UpsertCategoryUseCase
import com.upipulse.domain.usecase.DeleteCategoryUseCase
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
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class ManageViewModel @Inject constructor(
    observeAccountsUseCase: ObserveAccountsUseCase,
    observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val upsertAccountUseCase: UpsertAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val upsertCategoryUseCase: UpsertCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ManageUiState())
    val state: StateFlow<ManageUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<ManageEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeAccountsUseCase().collectLatest { accounts ->
                _state.value = _state.value.copy(accounts = accounts)
            }
        }
        viewModelScope.launch {
            observeCategoriesUseCase().collectLatest { categories ->
                _state.value = _state.value.copy(categories = categories)
            }
        }
    }

    fun addCategory(name: String, type: CategoryType) {
        viewModelScope.launch {
            val category = Category(id = 0, name = name.trim(), icon = "ic_custom", type = type)
            runCatching { upsertCategoryUseCase(category) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Category added")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            runCatching { deleteCategoryUseCase(category) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Category removed")) }
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
            runCatching { upsertAccountUseCase(account) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Account added")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }

    fun deleteAccount(accountId: Long) {
        val target = _state.value.accounts.firstOrNull { it.id == accountId } ?: return
        viewModelScope.launch {
            runCatching { deleteAccountUseCase(target) }
                .onSuccess { eventsChannel.send(ManageEvent.Message("Account removed")) }
                .onFailure { eventsChannel.send(ManageEvent.Message(it.message.orEmpty())) }
        }
    }
}
