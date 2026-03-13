package com.upipulse.ui.screens.addtransaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = if (state.isEdit) "Edit transaction" else "Add transaction")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::updateAmount,
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.merchant,
            onValueChange = viewModel::updateMerchant,
            label = { Text("Merchant") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        if (accounts.isEmpty()) {
            Text(
                text = "Add a bank account in Settings to start tagging expenses.",
                modifier = Modifier.padding(top = 12.dp)
            )
            Button(
                onClick = { onManageAccounts?.invoke() ?: onError("Open Settings > Bank accounts to add one.") },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Add bank account")
            }
        } else {
            AccountDropdownField(
                label = "Bank account",
                accounts = accounts,
                selectedId = state.accountId,
                onAccountSelected = viewModel::updateAccount,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        DropdownField(
            label = "Category",
            value = state.category,
            options = categories,
            onValueSelected = viewModel::updateCategory,
            modifier = Modifier.padding(top = 12.dp)
        )
        DropdownField(
            label = "Payment method",
            value = state.paymentMethod,
            options = paymentMethods,
            onValueSelected = viewModel::updatePaymentMethod,
            modifier = Modifier.padding(top = 12.dp)
        )
        OutlinedTextField(
            value = state.date.format(dateFormatter),
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            readOnly = true,
            trailingIcon = {
                Button(onClick = { showDatePicker = true }) { Text("Pick") }
            }
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::updateNotes,
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.save() },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isEdit) "Save changes" else "Save transaction")
        }
        if (showDatePicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.updateDate(newDate)
                        }
                        showDatePicker = false
                    }) { Text("Select") }
                },
                dismissButton = {
                    Button(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownField(
    label: String,
    accounts: List<com.upipulse.domain.model.Account>,
    selectedId: Long?,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Select account") }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name} - ${account.bankName}") },
                        onClick = {
                            onAccountSelected(account.id)
                            expanded = false
                        }
                    )
                }
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
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label)
        val hasOptions = options.isNotEmpty()
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text(if (hasOptions) "Select" else "No categories yet") }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (!hasOptions) {
                    DropdownMenuItem(
                        text = { Text("No categories yet") },
                        enabled = false,
                        onClick = {}
                    )
                } else {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
