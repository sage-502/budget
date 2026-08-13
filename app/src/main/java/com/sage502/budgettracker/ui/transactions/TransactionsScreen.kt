@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.transactions

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
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.util.formatAmount
import com.sage502.budgettracker.util.formatDate
import com.sage502.budgettracker.util.formatMonthKey

@Composable
fun TransactionsScreen(
    onEditTransaction: (Int) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val monthKey by viewModel.monthKey.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::prevMonth) {
                            Icon(Icons.Filled.ChevronLeft, "이전 달")
                        }
                        Text(formatMonthKey(monthKey))
                        IconButton(onClick = viewModel::nextMonth) {
                            Icon(Icons.Filled.ChevronRight, "다음 달")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("이번 달 내역 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        categoryName = categories[tx.categoryId]?.name ?: "-",
                        paymentName = paymentMethods[tx.paymentMethodId]?.name ?: "-",
                        onEdit = { onEditTransaction(tx.id) },
                        onDelete = { deleteTarget = tx }
                    )
                }
            }
        }
    }

    deleteTarget?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("내역 삭제") },
            text = { Text("이 내역을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tx)
                    deleteTarget = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    categoryName: String,
    paymentName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(categoryName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "· $paymentName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (transaction.memo.isBlank()) "(메모 없음)" else transaction.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (transaction.memo.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(formatAmount(transaction.amount), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, "수정", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete, "삭제",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
