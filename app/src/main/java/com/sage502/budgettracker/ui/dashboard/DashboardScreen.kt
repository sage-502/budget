@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sage502.budgettracker.data.repository.PaymentMethodRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.formatAmount
import com.sage502.budgettracker.util.formatMonthKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedItem by remember { mutableStateOf<CategoryBudgetItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::prevMonth) {
                            Icon(Icons.Filled.ChevronLeft, "이전 달")
                        }
                        Text(formatMonthKey(state.monthKey))
                        IconButton(onClick = viewModel::nextMonth) {
                            Icon(Icons.Filled.ChevronRight, "다음 달")
                        }
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
            item { OverallCard(state.totalSpent, state.totalBudget) }
            items(state.items) { item ->
                CategoryCard(item = item, onClick = { selectedItem = item })
            }
        }
    }

    selectedItem?.let { item ->
        CategoryDetailBottomSheetWrapper(
            item = item,
            monthKey = state.monthKey,
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun CategoryDetailBottomSheetWrapper(
    item: CategoryBudgetItem,
    monthKey: String,
    onDismiss: () -> Unit
) {
    val vm: DashboardHelperViewModel = hiltViewModel()
    CategoryDetailBottomSheet(
        category = item.category,
        budget = item.budget,
        spent = item.spent,
        monthKey = monthKey,
        transactionRepo = vm.transactionRepo,
        paymentMethodRepo = vm.paymentMethodRepo,
        onDismiss = onDismiss
    )
}

@HiltViewModel
class DashboardHelperViewModel @Inject constructor(
    val transactionRepo: TransactionRepository,
    val paymentMethodRepo: PaymentMethodRepository,
) : ViewModel()

@Composable
private fun OverallCard(spent: Long, budget: Long) {
    val progress = if (budget > 0) (spent.toFloat() / budget).coerceIn(0f, 1f) else 0f
    val isOver = spent > budget

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("전체 지출", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatAmount(spent), style = MaterialTheme.typography.bodyLarge)
                Text("/ ${formatAmount(budget)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CategoryCard(item: CategoryBudgetItem, onClick: () -> Unit) {
    val progress = if (item.budget > 0) (item.spent.toFloat() / item.budget).coerceIn(0f, 1f) else 0f
    val isOver = item.spent > item.budget

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.category.name, style = MaterialTheme.typography.titleSmall)
                if (isOver) {
                    Text(
                        "초과",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatAmount(item.spent), style = MaterialTheme.typography.bodySmall)
                Text(
                    "/ ${formatAmount(item.budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
