package com.upipulse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.upipulse.ui.screens.history.HistoryScreen
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
                Box(contentAlignment = Alignment.BottomCenter) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        val items = BottomDestination.values()
                        // Ensure we have exactly 4 items for the split layout
                        val leftItems = items.take(2)
                        val rightItems = items.takeLast(2)

                        leftItems.forEach { destination ->
                            NavigationItem(appState, destination)
                        }

                        // Spacer to make room for the central FAB
                        Spacer(modifier = Modifier.weight(0.6f))

                        rightItems.forEach { destination ->
                            NavigationItem(appState, destination)
                        }
                    }

                    // Centered Floating Action Button
                    val fabGradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
                    FloatingActionButton(
                        onClick = { appState.navController.navigate(Destinations.ADD_TRANSACTION) },
                        shape = CircleShape,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier
                            .offset(y = (-32).dp)
                            .size(60.dp)
                            .background(fabGradient, CircleShape)
                            .shadow(8.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = appState.navController,
            startDestination = Destinations.SPLASH,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 2 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 2 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 2 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 2 } }
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
                DashboardScreen()
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
            composable(Destinations.HISTORY) {
                HistoryScreen(onEditTransaction = { id ->
                    appState.navController.navigate("${Destinations.EDIT_TRANSACTION}/$id")
                })
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } })
            }
            composable(
                route = Destinations.ADD_TRANSACTION,
                enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
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
            composable(
                route = "${Destinations.EDIT_TRANSACTION}/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
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

@Composable
private fun RowScope.NavigationItem(appState: com.upipulse.ui.navigation.UpiPulseAppState, destination: BottomDestination) {
    val selected = appState.currentDestination?.route == destination.route
    NavigationBarItem(
        selected = selected,
        onClick = { appState.navigateToBottom(destination) },
        icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
        label = { Text(text = destination.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    )
}
