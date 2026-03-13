package com.upipulse.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upipulse.domain.usecase.ObserveTrackingSettingsUseCase
import com.upipulse.domain.usecase.SeedSampleDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SplashEvent {
    data object NavigateOnboarding : SplashEvent
    data object NavigateHome : SplashEvent
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeTrackingSettings: ObserveTrackingSettingsUseCase,
    private val seedSampleDataUseCase: SeedSampleDataUseCase
) : ViewModel() {

    private val eventsChannel = Channel<SplashEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            seedSampleDataUseCase()
            val settings = observeTrackingSettings().first()
            if (settings.onboardingComplete) {
                eventsChannel.send(SplashEvent.NavigateHome)
            } else {
                eventsChannel.send(SplashEvent.NavigateOnboarding)
            }
        }
    }
}
