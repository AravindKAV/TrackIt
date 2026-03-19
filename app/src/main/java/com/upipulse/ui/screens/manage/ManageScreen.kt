package com.upipulse.ui.screens.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.upipulse.domain.model.CategoryType
import com.upipulse.domain.model.Mandate
import com.upipulse.domain.model.MandateType
import com.upipulse.util.formatInr
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onMessage: (String) -> Unit,
    onNavigateToAccountTransactions: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMandateDialog by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var mandateToDelete by remember { mutableStateOf<Mandate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ManageEvent.Message) onMessage(event.text)
        }
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(modifier = modifier.fillMaxSize().background(bgGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    "Manage", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Bank Accounts Section
            item {
                SectionHeader(
                    title = "Bank Accounts",
                    onAddClick = { showAccountDialog = true },
                    addLabel = "Add Account"
                )
                
                if (state.accounts.isEmpty()) {
                    EmptyStateCard("No bank accounts added. Tap 'Add Account' to start tracking your balances.")
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

            // Mandates / EMI Section
            item {
                SectionHeader(
                    title = "Mandates & EMIs",
                    onAddClick = { showMandateDialog = true },
                    addLabel = "Add Mandate"
                )
                
                if (state.mandates.isEmpty()) {
                    EmptyStateCard("No mandates added. Tracking recurring payments helps you stay on top of your commitments.")
                } else {
                    state.mandates.forEach { mandate ->
                        MandateRow(
                            mandate = mandate,
                            onDelete = { mandateToDelete = mandate }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Categories Section
            item {
                SectionHeader(
                    title = "Categories",
                    onAddClick = { showCategoryDialog = true },
                    addLabel = "Add Category"
                )
                
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
                                        text = { Text(category.name, modifier = Modifier.padding(start = 8.dp)) },
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
                                        text = { Text(category.name, modifier = Modifier.padding(start = 8.dp)) },
                                        onClick = { categoryDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
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

    if (showMandateDialog) {
        MandateDialog(
            categories = state.categories.map { it.name }.distinct(),
            onDismiss = { showMandateDialog = false },
            onSave = { name, amount, day, type, category ->
                viewModel.addMandate(name, amount, day, type, category)
                showMandateDialog = false
            }
        )
    }

    if (mandateToDelete != null) {
        AlertDialog(
            onDismissRequest = { mandateToDelete = null },
            title = { Text("Delete Mandate?") },
            text = { Text("This will stop tracking this recurring payment. Existing transactions will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    mandateToDelete?.let { viewModel.deleteMandate(it) }
                    mandateToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mandateToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, onAddClick: () -> Unit, addLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(
            onClick = onAddClick,
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(addLabel)
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MandateRow(
    mandate: Mandate,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (mandate.type == MandateType.LOAN) Icons.Default.AccountBalance else Icons.Default.Subscriptions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                Text(mandate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Due on day ${mandate.dueDay} • ${mandate.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(formatInr(mandate.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MandateDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, MandateType, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MandateType.LOAN) }
    var category by remember { mutableStateOf(if (categories.isNotEmpty()) categories.first() else "EMI") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mandate / EMI", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(name, amount.toDoubleOrNull() ?: 0.0, dueDay.toIntOrNull() ?: 1, type, category) 
                },
                enabled = name.isNotBlank() && amount.isNotEmpty() && dueDay.isNotEmpty()
            ) {
                Text("Save Mandate")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Mandate Name (e.g. Home Loan)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount, 
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, 
                        label = { Text("Amount") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dueDay, 
                        onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) }, 
                        label = { Text("Due Day (1-31)") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == MandateType.LOAN, onClick = { type = MandateType.LOAN }, label = { Text("Loan EMI") })
                    FilterChip(selected = type == MandateType.SUBSCRIPTION, onClick = { type = MandateType.SUBSCRIPTION }, label = { Text("Subscription") })
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                        }
                    }
                }
            }
        }
    )
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

private fun accountAccentColor(id: Long): Color {
    val AccentPalette = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF0EA5E9), // Sky
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444)  // Red
    )
    return if (AccentPalette.isEmpty()) Color(0xFF6366F1)
    else AccentPalette[(id.toInt().absoluteValue) % AccentPalette.size]
}
