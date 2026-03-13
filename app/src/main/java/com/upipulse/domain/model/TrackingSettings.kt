package com.upipulse.domain.model

data class TrackingSettings(
    val smsDetectionEnabled: Boolean = true,
    val notificationDetectionEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val sampleDataSeeded: Boolean = false
)
