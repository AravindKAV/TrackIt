package com.upipulse.domain.model

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

data class TrackingSettings(
    val smsDetectionEnabled: Boolean = false,
    val notificationDetectionEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
    val sampleDataSeeded: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val lockEnabled: Boolean = false
)
