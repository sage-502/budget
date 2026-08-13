@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage502.budgettracker.util.formatMonthKey

@Composable
fun BudgetSettingsScreen(
    onBack: () -> Unit,
    viewModel: BudgetSettingsViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val monthDeleted by viewModel.monthDeleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) onBack() }
    LaunchedEffect(monthDeleted) { if (monthDeleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("예산 설정 · ${formatMonthKey(viewModel.monthKey)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.category.id }) { entry ->
                OutlinedTextField(
                    value = entry.budgetText,
                    onValueChange = { viewModel.updateEntry(entry.category.id, it) },
                    label = { Text(entry.category.name) },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::saveAll,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("예산 저장") }
            }
            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("이번 달 데이터 삭제") }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("이번 달 데이터 삭제") },
            text = { Text("이번 달 모든 거래 내역이 삭제됩니다. 계속할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMonthData()
                    showDeleteDialog = false
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }
}
