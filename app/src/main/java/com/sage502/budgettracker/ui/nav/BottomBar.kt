package com.sage502.budgettracker.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomTab(val route: String, val label: String) {
    object Dashboard : BottomTab("dashboard", "대시보드")
    object Add : BottomTab("add", "추가")
    object Transactions : BottomTab("transactions", "내역")
    object Settings : BottomTab("settings", "설정")
}

private val tabs = listOf(
    BottomTab.Dashboard,
    BottomTab.Add,
    BottomTab.Transactions,
    BottomTab.Settings
)

@Composable
fun BottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(BottomTab.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    when (tab) {
                        is BottomTab.Dashboard -> Icon(Icons.Filled.Dashboard, tab.label)
                        is BottomTab.Add -> Icon(Icons.Filled.AddCircle, tab.label)
                        is BottomTab.Transactions -> Icon(Icons.Filled.List, tab.label)
                        is BottomTab.Settings -> Icon(Icons.Filled.Settings, tab.label)
                    }
                },
                label = { Text(tab.label) }
            )
        }
    }
}
