package com.upipulse.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.upipulse.ui.common.openAppDetails
import com.upipulse.ui.common.openBatteryOptimizationSettings
import com.upipulse.ui.common.openNotificationListenerSettings
import com.upipulse.ui.common.openSmsPermissions

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val actions = listOf(
        SettingAction(
            title = "SMS access",
            description = "Allow SMS read access so we can parse bank alerts.",
            icon = Icons.Outlined.Message,
            onClick = { openSmsPermissions(context) }
        ),
        SettingAction(
            title = "Notification listener",
            description = "Required to watch Google Pay, PhonePe, Paytm pushes.",
            icon = Icons.Outlined.Notifications,
            onClick = { openNotificationListenerSettings(context) }
        ),
        SettingAction(
            title = "Battery optimization",
            description = "Exclude UPI Pulse from battery optimizations for reliable ingestion.",
            icon = Icons.Outlined.BatteryChargingFull,
            onClick = { openBatteryOptimizationSettings(context) }
        )
    )
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Permissions", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Keep these toggles enabled for the best tracking experience.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        items(actions) { action ->
            SettingCard(action)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Advanced", style = MaterialTheme.typography.titleMedium)
                    Text("Open app info for manual permission review.")
                    Button(onClick = { openAppDetails(context) }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Open App Details")
                    }
                }
            }
        }
    }
}

private data class SettingAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SettingCard(action: SettingAction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(action.icon, contentDescription = action.title)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium)
                    Text(action.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = action.onClick, modifier = Modifier.padding(top = 12.dp)) {
                Text("Open")
            }
        }
    }
}