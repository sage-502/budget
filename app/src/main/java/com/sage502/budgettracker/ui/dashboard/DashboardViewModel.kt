package com.sage502.budgettracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.repository.BudgetRepository
import com.sage502.budgettracker.data.repository.CategoryRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.currentMonthKey
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class CategoryBudgetItem(
    val category: Category,
    val budget: Long,
    val spent: Long
)

data class DashboardState(
    val monthKey: String = currentMonthKey(),
    val items: List<CategoryBudgetItem> = emptyList(),
    val totalBudget: Long = 0L,
    val totalSpent: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val categoryRepo: CategoryRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    private val _monthKey = MutableStateFlow(currentMonthKey())
    val monthKey: StateFlow<String> = _monthKey.asStateFlow()

    val state: StateFlow<DashboardState> = _monthKey.flatMapLatest { key ->
        val (start, end) = monthKeyToRange(key)
        combine(
            categoryRepo.getActive(),
            budgetRepo.getByMonth(key),
            transactionRepo.getSumByCategory(start, end)
        ) { categories, budgets, sums ->
            val budgetMap = budgets.associate { it.categoryId to it.amount }
            val sumMap = sums.associate { it.categoryId to it.total }
            val items = categories.map { cat ->
                CategoryBudgetItem(
                    category = cat,
                    budget = budgetMap[cat.id] ?: 0L,
                    spent = sumMap[cat.id] ?: 0L
                )
            }
            DashboardState(
                monthKey = key,
                items = items,
                totalBudget = items.sumOf { it.budget },
                totalSpent = items.sumOf { it.spent }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    init {
        viewModelScope.launch {
            _monthKey.collect { key ->
                budgetRepo.ensureMonth(key)
            }
        }
    }

    fun prevMonth() {
        _monthKey.update { YearMonth.parse(it).minusMonths(1).toString() }
    }

    fun nextMonth() {
        _monthKey.update { YearMonth.parse(it).plusMonths(1).toString() }
    }
}
