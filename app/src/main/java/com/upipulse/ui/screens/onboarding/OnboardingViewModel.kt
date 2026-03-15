package com.upipulse.ui.screens.onboarding

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.usecase.CompleteOnboardingUseCase
import com.upipulse.domain.usecase.ObserveTrackingSettingsUseCase
import com.upipulse.domain.usecase.UpdateNotificationDetectionUseCase
import com.upipulse.domain.usecase.UpdateSmsDetectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface OnboardingEvent {
    data object Finished : OnboardingEvent
}

data class OnboardingUiState(
    val smsEnabled: Boolean = false,
    val notificationEnabled: Boolean = false,
    val systemNotificationListenerEnabled: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeTrackingSettingsUseCase: ObserveTrackingSettingsUseCase,
    private val updateSmsDetectionUseCase: UpdateSmsDetectionUseCase,
    private val updateNotificationDetectionUseCase: UpdateNotificationDetectionUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeTrackingSettingsUseCase().collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    smsEnabled = settings.smsDetectionEnabled,
                    notificationEnabled = settings.notificationDetectionEnabled,
                    systemNotificationListenerEnabled = isNotificationListenerEnabled()
                )
            }
        }
    }

    fun refreshSystemStates() {
        _uiState.value = _uiState.value.copy(
            systemNotificationListenerEnabled = isNotificationListenerEnabled()
        )
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(packageName)
    }

    fun updateSmsDetection(enabled: Boolean) {
        viewModelScope.launch { updateSmsDetectionUseCase(enabled) }
    }

    fun updateNotificationDetection(enabled: Boolean) {
        viewModelScope.launch { updateNotificationDetectionUseCase(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            completeOnboardingUseCase()
            eventsChannel.send(OnboardingEvent.Finished)
        }
    }
}
