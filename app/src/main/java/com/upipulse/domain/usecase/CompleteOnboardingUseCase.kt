package com.upipulse.domain.usecase

import com.upipulse.data.preferences.UserPreferencesDataSource
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val preferences: UserPreferencesDataSource
) {
    suspend operator fun invoke() {
        preferences.setOnboardingComplete(true)
    }
}
