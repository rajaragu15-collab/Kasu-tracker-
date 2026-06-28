package com.nanba.financetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    TRANSACTIONS("transactions", "Transactions", Icons.Filled.List),
    BUDGETS("budgets", "Budgets", Icons.Filled.Savings),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}
