package com.upipulse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class TrackItAppState(val navController: NavHostController) {
    private val bottomDestinations = BottomDestination.values().map { it.route }

    val currentDestination: NavDestination?
        @Composable get() {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            return navBackStackEntry?.destination
        }

    val shouldShowBottomBar: Boolean
        @Composable get() =
            currentDestination?.route in bottomDestinations

    fun navigateToBottom(destination: BottomDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberTrackItAppState(
    navController: NavHostController = rememberNavController()
): TrackItAppState = remember(navController) { TrackItAppState(navController) }
