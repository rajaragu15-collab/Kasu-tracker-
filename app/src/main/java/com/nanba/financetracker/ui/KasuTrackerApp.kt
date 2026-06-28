package com.nanba.financetracker.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nanba.financetracker.ui.navigation.AppDestination
import com.nanba.financetracker.ui.screens.BudgetsScreen
import com.nanba.financetracker.ui.screens.HomeScreen
import com.nanba.financetracker.ui.screens.SettingsScreen
import com.nanba.financetracker.ui.screens.TransactionsScreen
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory

@Composable
fun KasuTrackerApp(
    viewModelFactory: ViewModelFactory,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit
) {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.HOME.route) {
                HomeScreen(viewModelFactory)
            }
            composable(AppDestination.TRANSACTIONS.route) {
                TransactionsScreen(viewModelFactory)
            }
            composable(AppDestination.BUDGETS.route) {
                BudgetsScreen(viewModelFactory)
            }
            composable(AppDestination.SETTINGS.route) {
                SettingsScreen(
                    viewModelFactory = viewModelFactory,
                    hasSmsPermission = hasSmsPermission,
                    onRequestSmsPermission = onRequestSmsPermission
                )
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        AppDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
