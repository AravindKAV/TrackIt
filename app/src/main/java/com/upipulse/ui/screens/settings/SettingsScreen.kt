package com.upipulse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.util.formatInr

@Composable
fun SettingsScreen(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.Message) onMessage(event.text)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "Settings", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            SettingsSection(title = "Detectors") {
                SettingToggle(
                    title = "SMS Detection",
                    subtitle = "Automatically parse UPI debit SMS alerts",
                    icon = Icons.Default.Sms,
                    checked = state.settings.smsDetectionEnabled,
                    onCheckedChange = viewModel::toggleSms
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingToggle(
                    title = "Notification Detection",
                    subtitle = "Watch GPay, PhonePe, and Paytm notifications",
                    icon = Icons.Default.Notifications,
                    checked = state.settings.notificationDetectionEnabled,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bank Accounts", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAccountDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account")
                    }
                }
                
                if (state.accounts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "No bank accounts added. Tap 'Add Account' to start tracking your balances.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    state.accounts.forEach { account ->
                        AccountRow(
                            account = account, 
                            onDelete = { viewModel.deleteAccount(account.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Data Management") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sample Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Reset demo entries to preview the app features.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = viewModel::resetSampleData, 
                        enabled = !state.isResetting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text(if (state.isResetting) "Resetting..." else "Reset Sample Data")
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Privacy First", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "UPI Pulse keeps all computation on-device. Your financial data never leaves your phone. No banking credentials are ever requested.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (showAccountDialog) {
        AccountDialog(
            onDismiss = { showAccountDialog = false },
            onSave = { name, bank, suffix, amount ->
                viewModel.addAccount(name, bank, suffix, amount)
                showAccountDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title, 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AccountRow(account: com.upipulse.domain.model.Account, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "${account.bankName}${account.numberSuffix?.let { " • ****$it" } ?: ""}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Balance: ${formatInr(account.balance)}", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val canSave = name.isNotBlank() && amount.isNotEmpty()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            Button(
                onClick = { onSave(name, bank.ifBlank { name }, suffix.ifBlank { null }, amountValue) }, 
                enabled = canSave,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { 
            Text(
                "Add Bank Account", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Account Nickname (e.g. My Savings)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = bank, 
                    onValueChange = { bank = it }, 
                    label = { Text("Bank Name (e.g. HDFC)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = suffix, 
                    onValueChange = { suffix = it }, 
                    label = { Text("Last 4 Digits (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Current Balance") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}
