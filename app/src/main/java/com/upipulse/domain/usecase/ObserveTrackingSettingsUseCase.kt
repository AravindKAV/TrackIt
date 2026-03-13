package com.upipulse.domain.usecase

import com.upipulse.data.preferences.UserPreferencesDataSource
import com.upipulse.domain.model.TrackingSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTrackingSettingsUseCase @Inject constructor(
    private val preferences: UserPreferencesDataSource
) {
    operator fun invoke(): Flow<TrackingSettings> = preferences.settings
}
