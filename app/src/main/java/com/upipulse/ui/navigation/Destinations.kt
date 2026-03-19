package com.upipulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Destinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val MANAGE = "manage"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction"
    const val TRANSACTIONS = "transactions" // Kept for legacy routes if any
}

enum class BottomDestination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD(Destinations.DASHBOARD, "Dashboard", Icons.Default.BarChart),
    HISTORY(Destinations.HISTORY, "History", Icons.Default.History),
    MANAGE(Destinations.MANAGE, "Accounts", Icons.Default.AccountBalanceWallet),
    SETTINGS(Destinations.SETTINGS, "Settings", Icons.Default.Settings)
}
