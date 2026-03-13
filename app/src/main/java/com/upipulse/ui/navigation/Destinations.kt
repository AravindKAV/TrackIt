package com.upipulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.ui.graphics.vector.ImageVector

object Destinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction"
}

enum class BottomDestination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD(Destinations.DASHBOARD, "Dashboard", Icons.Default.BarChart),
    TRANSACTIONS(Destinations.TRANSACTIONS, "Transactions", Icons.Default.TableRows),
    SETTINGS(Destinations.SETTINGS, "Settings", Icons.Default.Settings)
}
