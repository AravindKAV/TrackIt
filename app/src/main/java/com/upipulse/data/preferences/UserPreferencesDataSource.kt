package com.upipulse.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.upipulse.domain.model.TrackingSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("upi_pulse_settings") }
    )

    private object Keys {
        val SMS = booleanPreferencesKey("sms_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val SAMPLE = booleanPreferencesKey("sample_seeded")
    }

    val settings: Flow<TrackingSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            TrackingSettings(
                smsDetectionEnabled = prefs[Keys.SMS] ?: true,
                notificationDetectionEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
                sampleDataSeeded = prefs[Keys.SAMPLE] ?: false
            )
        }

    suspend fun setSmsDetection(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SMS] = enabled }
    }

    suspend fun setNotificationDetection(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setOnboardingComplete(completed: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING] = completed }
    }

    suspend fun setSampleSeeded(seed: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SAMPLE] = seed }
    }
}
