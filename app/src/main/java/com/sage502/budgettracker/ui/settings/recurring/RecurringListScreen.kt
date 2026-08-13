@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage502.budgettracker.data.entity.RecurringExpense
import com.sage502.budgettracker.util.formatAmount

@Composable
fun RecurringListScreen(
    onBack: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (Int) -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    var deleteTarget by remember { mutableStateOf<RecurringExpense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("반복 지출") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurring) {
                Icon(Icons.Filled.Add, "반복 지출 추가")
            }
        }
    ) { padding ->
        if (expenses.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("반복 지출 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${categories[expense.categoryId]?.name ?: "-"} · 매월 ${expense.dayOfMonth}일",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${formatAmount(expense.amount)} · ${paymentMethods[expense.paymentMethodId]?.name ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (expense.memo.isNotBlank()) {
                                    Text(expense.memo, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = expense.isActive,
                                onCheckedChange = { viewModel.toggleActive(expense) }
                            )
                            IconButton(onClick = { onEditRecurring(expense.id) }) {
                                Icon(Icons.Filled.Edit, "수정", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { deleteTarget = expense }) {
                                Icon(Icons.Filled.Delete, "삭제",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { exp ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("반복 지출 삭제") },
            text = { Text("이 반복 지출을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(exp); deleteTarget = null }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } }
        )
    }
}
