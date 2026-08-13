package com.sage502.budgettracker.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sage502.budgettracker.ui.add.AddScreen
import com.sage502.budgettracker.ui.dashboard.DashboardScreen
import com.sage502.budgettracker.ui.settings.SettingsScreen
import com.sage502.budgettracker.ui.settings.backup.BackupRestoreScreen
import com.sage502.budgettracker.ui.settings.budget.BudgetSettingsScreen
import com.sage502.budgettracker.ui.settings.category.CategoryManagementScreen
import com.sage502.budgettracker.ui.settings.recurring.RecurringEditScreen
import com.sage502.budgettracker.ui.settings.recurring.RecurringListScreen
import com.sage502.budgettracker.ui.settings.statistics.StatisticsScreen
import com.sage502.budgettracker.ui.transactions.TransactionsScreen

@Composable
fun BudgetNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomTab.Dashboard.route) {
                DashboardScreen()
            }
            composable(
                route = "add?transactionId={transactionId}",
                arguments = listOf(navArgument("transactionId") {
                    type = NavType.IntType; defaultValue = -1
                })
            ) { backStack ->
                val txId = backStack.arguments?.getInt("transactionId") ?: -1
                AddScreen(
                    transactionId = if (txId == -1) null else txId,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(BottomTab.Transactions.route) {
                TransactionsScreen(
                    onEditTransaction = { id ->
                        navController.navigate("add?transactionId=$id")
                    }
                )
            }
            composable(BottomTab.Settings.route) {
                SettingsScreen(
                    onNavigateBudget = { navController.navigate("settings/budget") },
                    onNavigateCategory = { navController.navigate("settings/category") },
                    onNavigateRecurring = { navController.navigate("settings/recurring") },
                    onNavigateBackup = { navController.navigate("settings/backup") },
                    onNavigateStatistics = { navController.navigate("settings/statistics") }
                )
            }
            composable("settings/budget") {
                BudgetSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/category") {
                CategoryManagementScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/recurring") {
                RecurringListScreen(
                    onBack = { navController.popBackStack() },
                    onAddRecurring = { navController.navigate("settings/recurring/edit") },
                    onEditRecurring = { id -> navController.navigate("settings/recurring/edit?recurringId=$id") }
                )
            }
            composable(
                route = "settings/recurring/edit?recurringId={recurringId}",
                arguments = listOf(navArgument("recurringId") {
                    type = NavType.IntType; defaultValue = -1
                })
            ) { backStack ->
                val rid = backStack.arguments?.getInt("recurringId") ?: -1
                RecurringEditScreen(
                    recurringId = if (rid == -1) null else rid,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("settings/backup") {
                BackupRestoreScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/statistics") {
                StatisticsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
