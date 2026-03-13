package com.upipulse.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.model.Account
import com.upipulse.domain.model.TrackingSettings
import com.upipulse.domain.usecase.ObserveTrackingSettingsUseCase
import com.upipulse.domain.usecase.ObserveAccountsUseCase
import com.upipulse.domain.usecase.ResetSampleDataUseCase
import com.upipulse.domain.usecase.UpsertAccountUseCase
import com.upipulse.domain.usecase.DeleteAccountUseCase
import com.upipulse.domain.usecase.UpdateNotificationDetectionUseCase
import com.upipulse.domain.usecase.UpdateSmsDetectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
}

data class SettingsUiState(
    val settings: TrackingSettings = TrackingSettings(),
    val accounts: List<Account> = emptyList(),
    val isResetting: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeTrackingSettingsUseCase: ObserveTrackingSettingsUseCase,
    observeAccountsUseCase: ObserveAccountsUseCase,
    private val updateSmsDetectionUseCase: UpdateSmsDetectionUseCase,
    private val updateNotificationDetectionUseCase: UpdateNotificationDetectionUseCase,
    private val resetSampleDataUseCase: ResetSampleDataUseCase,
    private val upsertAccountUseCase: UpsertAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeTrackingSettingsUseCase().collectLatest { trackingSettings ->
                _state.value = _state.value.copy(settings = trackingSettings)
            }
        }
        viewModelScope.launch {
            observeAccountsUseCase().collectLatest { accounts ->
                _state.value = _state.value.copy(accounts = accounts)
            }
        }
    }

    fun toggleSms(enabled: Boolean) {
        viewModelScope.launch { updateSmsDetectionUseCase(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { updateNotificationDetectionUseCase(enabled) }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResetting = true)
            runCatching { resetSampleDataUseCase() }
                .onSuccess { eventsChannel.send(SettingsEvent.Message("Sample data reloaded")) }
                .onFailure { eventsChannel.send(SettingsEvent.Message(it.message.orEmpty())) }
            _state.value = _state.value.copy(isResetting = false)
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
                .onSuccess { eventsChannel.send(SettingsEvent.Message("Account added")) }
                .onFailure { eventsChannel.send(SettingsEvent.Message(it.message.orEmpty())) }
        }
    }

    fun deleteAccount(accountId: Long) {
        val target = _state.value.accounts.firstOrNull { it.id == accountId } ?: return
        viewModelScope.launch {
            runCatching { deleteAccountUseCase(target) }
                .onSuccess { eventsChannel.send(SettingsEvent.Message("Account removed")) }
                .onFailure { eventsChannel.send(SettingsEvent.Message(it.message.orEmpty())) }
        }
    }
}
