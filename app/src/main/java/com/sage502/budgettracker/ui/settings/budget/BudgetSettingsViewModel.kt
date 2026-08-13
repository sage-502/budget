package com.sage502.budgettracker.ui.settings.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.repository.BudgetRepository
import com.sage502.budgettracker.data.repository.CategoryRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.currentMonthKey
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetEntry(val category: Category, val budgetText: String)

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val categoryRepo: CategoryRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    val monthKey = currentMonthKey()

    private val _entries = MutableStateFlow<List<BudgetEntry>>(emptyList())
    val entries: StateFlow<List<BudgetEntry>> = _entries.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _monthDeleted = MutableStateFlow(false)
    val monthDeleted: StateFlow<Boolean> = _monthDeleted.asStateFlow()

    init {
        viewModelScope.launch {
            budgetRepo.ensureMonth(monthKey)
            combine(
                categoryRepo.getActive(),
                budgetRepo.getByMonth(monthKey)
            ) { cats, budgets ->
                val budgetMap = budgets.associate { it.categoryId to it.amount }
                cats.map { cat ->
                    BudgetEntry(cat, (budgetMap[cat.id] ?: 0L).toString())
                }
            }.collect { _entries.value = it }
        }
    }

    fun updateEntry(categoryId: Int, text: String) {
        _entries.update { list ->
            list.map { if (it.category.id == categoryId) it.copy(budgetText = text.filter(Char::isDigit)) else it }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _entries.value.forEach { entry ->
                val amount = entry.budgetText.toLongOrNull() ?: 0L
                budgetRepo.setAmount(monthKey, entry.category.id, amount)
            }
            _saved.value = true
        }
    }

    fun deleteMonthData() {
        viewModelScope.launch {
            val (start, end) = monthKeyToRange(monthKey)
            transactionRepo.deleteByMonth(start, end)
            _monthDeleted.value = true
        }
    }
}
