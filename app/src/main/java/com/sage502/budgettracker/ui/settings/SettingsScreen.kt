@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onNavigateBudget: () -> Unit,
    onNavigateCategory: () -> Unit,
    onNavigateRecurring: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateStatistics: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("설정") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SettingsItem(Icons.Filled.Wallet, "예산 설정", onNavigateBudget)
            HorizontalDivider()
            SettingsItem(Icons.Filled.Category, "카테고리 관리", onNavigateCategory)
            HorizontalDivider()
            SettingsItem(Icons.Filled.Repeat, "반복 지출", onNavigateRecurring)
            HorizontalDivider()
            SettingsItem(Icons.Filled.Backup, "백업 / 복원", onNavigateBackup)
            HorizontalDivider()
            SettingsItem(Icons.Filled.BarChart, "통계", onNavigateStatistics)
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
