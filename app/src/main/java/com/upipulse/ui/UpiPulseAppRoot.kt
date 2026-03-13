package com.upipulse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.upipulse.ui.navigation.BottomDestination
import com.upipulse.ui.navigation.Destinations
import com.upipulse.ui.navigation.rememberUpiPulseAppState
import com.upipulse.ui.screens.addtransaction.TransactionFormScreen
import com.upipulse.ui.screens.dashboard.DashboardScreen
import com.upipulse.ui.screens.onboarding.OnboardingScreen
import com.upipulse.ui.screens.settings.SettingsScreen
import com.upipulse.ui.screens.splash.SplashEvent
import com.upipulse.ui.screens.splash.SplashScreen
import com.upipulse.ui.screens.transactions.TransactionsScreen
import kotlinx.coroutines.launch

@Composable
fun UpiPulseAppRoot() {
    val appState = rememberUpiPulseAppState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = appState.shouldShowBottomBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp))
                ) {
                    BottomDestination.values().forEach { destination ->
                        val selected = appState.currentDestination?.route == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { appState.navigateToBottom(destination) },
                            icon = { 
                                Icon(
                                    imageVector = destination.icon, 
                                    contentDescription = destination.label,
                                    modifier = Modifier.padding(4.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = appState.navController,
            startDestination = Destinations.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.SPLASH) {
                SplashScreen(onEvent = { event ->
                    when (event) {
                        SplashEvent.NavigateHome -> appState.navController.navigate(Destinations.DASHBOARD) {
                            popUpTo(Destinations.SPLASH) { inclusive = true }
                        }
                        SplashEvent.NavigateOnboarding -> appState.navController.navigate(Destinations.ONBOARDING) {
                            popUpTo(Destinations.SPLASH) { inclusive = true }
                        }
                    }
                })
            }
            composable(Destinations.ONBOARDING) {
                OnboardingScreen(onFinished = {
                    appState.navController.navigate(Destinations.DASHBOARD) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(Destinations.DASHBOARD) {
                DashboardScreen(onAddTransaction = { appState.navController.navigate(Destinations.ADD_TRANSACTION) })
            }
            composable(Destinations.TRANSACTIONS) {
                TransactionsScreen(
                    onAddTransaction = { appState.navController.navigate(Destinations.ADD_TRANSACTION) },
                    onEditTransaction = { id ->
                        appState.navController.navigate("${Destinations.EDIT_TRANSACTION}/$id")
                    },
                    onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } })
            }
            composable(Destinations.ADD_TRANSACTION) {
                TransactionFormScreen(
                    onSaved = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                        appState.navController.popBackStack()
                    },
                    onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                    onManageAccounts = { appState.navigateToBottom(BottomDestination.SETTINGS) }
                )
            }
            composable(
                route = "${Destinations.EDIT_TRANSACTION}/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionFormScreen(
                    onSaved = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                        appState.navController.popBackStack()
                    },
                    onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                    onManageAccounts = { appState.navigateToBottom(BottomDestination.SETTINGS) }
                )
            }
        }
    }
}
