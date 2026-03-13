package com.upipulse.domain.usecase

import com.upipulse.data.preferences.UserPreferencesDataSource
import javax.inject.Inject

class UpdateNotificationDetectionUseCase @Inject constructor(
    private val preferences: UserPreferencesDataSource
) {
    suspend operator fun invoke(enabled: Boolean) {
        preferences.setNotificationDetection(enabled)
    }
}
