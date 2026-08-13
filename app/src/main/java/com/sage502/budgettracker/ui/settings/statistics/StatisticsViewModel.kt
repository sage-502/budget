package com.sage502.budgettracker.ui.settings.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.repository.BudgetRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.currentMonthKey
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class MonthStats(
    val monthKey: String,
    val totalBudget: Long,
    val totalSpent: Long
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    private val _rangeMonths = MutableStateFlow(6)
    val rangeMonths: StateFlow<Int> = _rangeMonths.asStateFlow()

    private val _stats = MutableStateFlow<List<MonthStats>>(emptyList())
    val stats: StateFlow<List<MonthStats>> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            _rangeMonths.collect { months -> loadStats(months) }
        }
    }

    fun setRange(months: Int) { _rangeMonths.value = months }

    private suspend fun loadStats(months: Int) {
        val current = YearMonth.parse(currentMonthKey())
        val result = (months - 1 downTo 0).map { offset ->
            val ym = current.minusMonths(offset.toLong())
            val key = ym.toString()
            val (start, end) = monthKeyToRange(key)
            val budget = budgetRepo.getTotalByMonthOnce(key) ?: 0L
            val spent = transactionRepo.getTotalOnce(start, end) ?: 0L
            MonthStats(key, budget, spent)
        }
        _stats.value = result
    }
}
