@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.data.repository.PaymentMethodRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.formatAmount
import com.sage502.budgettracker.util.formatDate
import com.sage502.budgettracker.util.monthKeyToRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun CategoryDetailBottomSheet(
    category: Category,
    budget: Long,
    spent: Long,
    monthKey: String,
    transactionRepo: TransactionRepository,
    paymentMethodRepo: PaymentMethodRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var paymentNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(category.id, monthKey) {
        val (start, end) = monthKeyToRange(monthKey)
        transactions = transactionRepo.getByMonth(start, end).first()
            .filter { it.categoryId == category.id }
            .sortedByDescending { it.date }
        paymentNames = paymentMethodRepo.getActive().first().associate { it.id to it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("지출: ${formatAmount(spent)}", style = MaterialTheme.typography.bodyMedium)
                Text("예산: ${formatAmount(budget)}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("이번 달 내역 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatDate(tx.date), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    paymentNames[tx.paymentMethodId] ?: "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.memo.isNotBlank()) {
                                    Text(tx.memo, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                formatAmount(tx.amount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "조회 전용 · 수정/삭제는 내역 화면에서",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
