package com.upipulse.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.domain.model.AppTheme
import com.upipulse.domain.model.CategoryType
import com.upipulse.util.formatInr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMessage: (String) -> Unit,
    onNavigateToAccountTransactions: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) viewModel.toggleSms(true)
        else {
            viewModel.toggleSms(false)
            onMessage("SMS permissions are required for this feature")
        }
    }

    var selectedStartDate by remember { mutableStateOf(LocalDate.now().minusMonths(1)) }
    var selectedEndDate by remember { mutableStateOf(LocalDate.now()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportTransactions(context, selectedStartDate, selectedEndDate, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> onMessage(event.text)
                is SettingsEvent.ExportReady -> { /* handled via launcher */ }
            }
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
            SettingsSection(title = "Appearance") {
                SettingItem(
                    title = "App Theme",
                    subtitle = when (state.settings.theme) {
                        AppTheme.LIGHT -> "Light Mode"
                        AppTheme.DARK -> "Dark Mode"
                        AppTheme.SYSTEM -> "System Default"
                    },
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }
        }

        item {
            SettingsSection(title = "Security") {
                SettingToggle(
                    title = "App Lock",
                    subtitle = "Require biometric or device lock to open app",
                    icon = Icons.Default.Lock,
                    checked = state.settings.lockEnabled,
                    onCheckedChange = viewModel::toggleLock
                )
            }
        }

        item {
            SettingsSection(title = "Detectors") {
                SettingToggle(
                    title = "SMS Detection",
                    subtitle = "Automatically parse UPI debit SMS alerts",
                    icon = Icons.Default.Sms,
                    checked = state.settings.smsDetectionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            if (hasSms) viewModel.toggleSms(true)
                            else smsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        } else {
                            viewModel.toggleSms(false)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingToggle(
                    title = "Notification Detection",
                    subtitle = "Watch GPay, PhonePe, and Paytm notifications",
                    icon = Icons.Default.Notifications,
                    checked = state.settings.notificationDetectionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val isListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                            if (isListenerEnabled) {
                                viewModel.toggleNotifications(true)
                            } else {
                                viewModel.toggleNotifications(false) // Keep it off until granted
                                onMessage("Please enable TrackIt in Notification Access settings")
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        } else {
                            viewModel.toggleNotifications(false)
                        }
                    }
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
                            onDelete = { viewModel.deleteAccount(account.id) },
                            onClick = { onNavigateToAccountTransactions(account.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
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
                        "Categories", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showCategoryDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Category")
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "Browse Existing Categories",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        if (state.categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No custom categories added") },
                                onClick = { categoryDropdownExpanded = false }
                            )
                        } else {
                            val uniqueCategories = state.categories.distinctBy { it.name }
                            val debitCategories = uniqueCategories.filter { it.type == CategoryType.DEBIT || it.type == CategoryType.BOTH }
                            val creditCategories = uniqueCategories.filter { it.type == CategoryType.CREDIT || it.type == CategoryType.BOTH }

                            if (debitCategories.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("DEBIT CATEGORIES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                    onClick = {},
                                    enabled = false
                                )
                                debitCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(category.name, modifier = Modifier.padding(start = 8.dp))
                                        },
                                        onClick = { categoryDropdownExpanded = false }
                                    )
                                }
                            }

                            if (creditCategories.isNotEmpty()) {
                                if (debitCategories.isNotEmpty()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("CREDIT CATEGORIES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) },
                                    onClick = {},
                                    enabled = false
                                )
                                creditCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(category.name, modifier = Modifier.padding(start = 8.dp))
                                        },
                                        onClick = { categoryDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Data Management") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export to CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Download your transaction history.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = { showExportDialog = true }, 
                        enabled = !state.isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (state.isExporting) "Exporting..." else "Export Data")
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Reset All Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Clear all transactions and bank accounts.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = { showResetDialog = true }, 
                        enabled = !state.isResetting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text(if (state.isResetting) "Resetting..." else "Reset")
                    }
                }
            }
        }

        item {
            val privacyGradient = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer
                )
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.background(privacyGradient).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Privacy First", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "TrackIt keeps all computation on-device. Your financial data never leaves your phone. No banking credentials are ever requested.",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (showExportDialog) {
        ExportRangeDialog(
            initialStartDate = selectedStartDate,
            initialEndDate = selectedEndDate,
            onDismiss = { showExportDialog = false },
            onExport = { start, end ->
                selectedStartDate = start
                selectedEndDate = end
                showExportDialog = false
                val fileName = "TrackIt_Export_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                createDocumentLauncher.launch(fileName)
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently delete all your transactions and bank accounts. Core categories will remain.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.resetData()
                    showResetDialog = false 
                }) {
                    Text("Confirm Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = state.settings.theme,
            onThemeSelected = {
                viewModel.updateTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
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

    if (showCategoryDialog) {
        CategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name, type ->
                viewModel.addCategory(name, type)
                showCategoryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportRangeDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onExport: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Range", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(onClick = { onExport(startDate, endDate) }) {
                Text("Confirm Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Select the period for transaction export:", style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("From") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = { showStartDatePicker = true }) {
                            Text("Change")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = endDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("To") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = { showEndDatePicker = true }) {
                            Text("Change")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )

    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun CategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, CategoryType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CategoryType.BOTH) }
    val canSave = name.isNotBlank()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            Button(
                onClick = { onSave(name, type) }, 
                enabled = canSave,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { 
            Text(
                "Add Custom Category", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Category Name (e.g. Subscriptions)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text("Show category in:", style = MaterialTheme.typography.labelMedium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == CategoryType.DEBIT,
                        onClick = { type = CategoryType.DEBIT },
                        label = { Text("Debit") }
                    )
                    FilterChip(
                        selected = type == CategoryType.CREDIT,
                        onClick = { type = CategoryType.CREDIT },
                        label = { Text("Credit") }
                    )
                    FilterChip(
                        selected = type == CategoryType.BOTH,
                        onClick = { type = CategoryType.BOTH },
                        label = { Text("Both") }
                    )
                }
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme", fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(28.dp),
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    title = "System Default",
                    icon = Icons.Default.SettingsSuggest,
                    selected = currentTheme == AppTheme.SYSTEM,
                    onClick = { onThemeSelected(AppTheme.SYSTEM) }
                )
                ThemeOption(
                    title = "Light Mode",
                    icon = Icons.Default.LightMode,
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = { onThemeSelected(AppTheme.LIGHT) }
                )
                ThemeOption(
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    selected = currentTheme == AppTheme.DARK,
                    onClick = { onThemeSelected(AppTheme.DARK) }
                )
            }
        }
    )
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = null)
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
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
private fun AccountRow(
    account: com.upipulse.domain.model.Account, 
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val accent = accountAccentColor(account.id)
    val accountGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            accent.copy(alpha = 0.1f)
        )
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .background(accountGradient)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = accent)
            }
            
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    account.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${account.bankName}${account.numberSuffix?.let { " • ****$it" } ?: ""}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatInr(account.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.End
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
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

private val AccentPalette = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF0EA5E9), // Sky
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444)  // Red
)

private fun accountAccentColor(id: Long): Color =
    if (AccentPalette.isEmpty()) Color(0xFF6366F1)
    else AccentPalette[(id.toInt().absoluteValue) % AccentPalette.size]
