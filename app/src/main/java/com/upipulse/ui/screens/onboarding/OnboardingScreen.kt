package com.upipulse.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collect

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        viewModel.updateSmsDetection(granted)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateNotificationDetection(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OnboardingEvent.Finished) onFinished()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Stay on top of your UPI spending", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "UPI Pulse tracks SMS, notifications and manual entries to build a living ledger.",
            style = MaterialTheme.typography.bodyMedium
        )
        FeatureCard(title = "Expense tracking", description = "Add expenses manually or let the app read your UPI alerts.")
        FeatureCard(title = "Auto detection", description = "We parse SMS and notifications from Google Pay, PhonePe, Paytm and more.")
        FeatureCard(title = "Analytics", description = "A fintech-style dashboard shows category and weekly trends.")
        Text(text = "Permissions", style = MaterialTheme.typography.titleMedium)
        PermissionCard(
            title = "SMS detection",
            granted = state.smsEnabled,
            description = "Grant READ and RECEIVE SMS to capture bank alerts.",
            actionLabel = if (state.smsEnabled) "Granted" else "Grant",
            onClick = {
                smsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            },
            forceEnabled = false
        )
        PermissionCard(
            title = "Notification listener",
            granted = state.notificationEnabled,
            description = "Allow notification access for Google Pay / PhonePe alerts.",
            actionLabel = "Open Settings",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            forceEnabled = true
        )
        PermissionCard(
            title = "Notification permission",
            granted = state.notificationEnabled,
            description = "Allow app notifications so we can confirm detections.",
            actionLabel = "Allow",
            onClick = {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            forceEnabled = false
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.completeOnboarding() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start tracking")
        }
    }
}

@Composable
private fun FeatureCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    forceEnabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            Button(onClick = onClick, enabled = forceEnabled || !granted) {
                Text(if (granted && !forceEnabled) "Granted" else actionLabel)
            }
        }
    }
}
