package com.upipulse.ui.screens.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val paymentMethods = listOf("UPI", "Cash", "Debit Card", "Credit Card", "Net Banking")
private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onSaved: (String) -> Unit,
    onError: (String) -> Unit,
    onManageAccounts: (() -> Unit)? = null,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionFormEvent.Saved -> onSaved(event.message)
                is TransactionFormEvent.Error -> if (event.message.isNotBlank()) onError(event.message)
            }
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(gradientBrush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = if (state.isEdit) "Edit Entry" else "New Entry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.isCredit && !state.isTransfer,
                    onClick = { viewModel.updateType(isCredit = false, isTransfer = false) },
                    label = { Text("Debit") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                FilterChip(
                    selected = state.isCredit,
                    onClick = { viewModel.updateType(isCredit = true, isTransfer = false) },
                    label = { Text("Credit") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF059669)
                    )
                )
                FilterChip(
                    selected = state.isTransfer,
                    onClick = { viewModel.updateType(isCredit = false, isTransfer = true) },
                    label = { Text("Transfer") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Amount Field
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = viewModel::updateAmount,
                        label = { Text("Amount") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isTransfer) {
                        // Source Account
                        AccountDropdownField(
                            label = "From Account",
                            accounts = accounts,
                            selectedId = state.accountId,
                            onAccountSelected = viewModel::updateAccount
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Target Account
                        AccountDropdownField(
                            label = "To Account",
                            accounts = accounts,
                            selectedId = state.targetAccountId,
                            onAccountSelected = viewModel::updateTargetAccount
                        )
                    } else {
                        // Merchant / Sender Field
                        FormTextField(
                            value = state.merchant,
                            onValueChange = viewModel::updateMerchant,
                            label = if (state.isCredit) "From (Sender)" else "Merchant / To",
                            icon = Icons.Default.Storefront
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Single Account Selection
                        AccountDropdownField(
                            label = "Account",
                            accounts = accounts,
                            selectedId = state.accountId,
                            onAccountSelected = viewModel::updateAccount
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category
                    DropdownField(
                        label = "Category",
                        value = state.category,
                        options = categories,
                        icon = Icons.Default.Category,
                        onValueSelected = viewModel::updateCategory,
                        enabled = !state.isTransfer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Method
                    DropdownField(
                        label = "Payment Method",
                        value = state.paymentMethod,
                        options = paymentMethods,
                        icon = Icons.Default.Payments,
                        onValueSelected = viewModel::updatePaymentMethod
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Field
                    OutlinedTextField(
                        value = state.date.format(dateFormatter),
                        onValueChange = {},
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) {
                                Text("Change")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes Field
                    FormTextField(
                        value = state.notes,
                        onValueChange = viewModel::updateNotes,
                        label = "Notes (Optional)",
                        icon = Icons.Default.Description
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val saveButtonGradient = Brush.horizontalGradient(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )

            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                val buttonBgModifier = if (state.isSaving) {
                    Modifier.background(Color.Gray.copy(alpha = 0.5f))
                } else {
                    Modifier.background(saveButtonGradient)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(buttonBgModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.isEdit) "Update Entry" else if (state.isTransfer) "Complete Transfer" else "Save Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showDatePicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.updateDate(newDate)
                        }
                        showDatePicker = false
                    }) { Text("Select") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownField(
    label: String,
    accounts: List<com.upipulse.domain.model.Account>,
    selectedId: Long?,
    onAccountSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.bankName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { 
                        Column {
                            Text(account.bankName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (account.name != account.bankName) {
                                Text(account.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    icon: ImageVector,
    onValueSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
