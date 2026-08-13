package com.sage502.budgettracker.ui.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.PaymentMethod
import com.sage502.budgettracker.data.entity.RecurringExpense
import com.sage502.budgettracker.data.repository.CategoryRepository
import com.sage502.budgettracker.data.repository.PaymentMethodRepository
import com.sage502.budgettracker.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val repo: RecurringExpenseRepository,
    private val categoryRepo: CategoryRepository,
    private val paymentMethodRepo: PaymentMethodRepository,
) : ViewModel() {

    val expenses: StateFlow<List<RecurringExpense>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<Map<Int, Category>> = categoryRepo.getAll()
        .map { it.associateBy { c -> c.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val paymentMethods: StateFlow<Map<Int, PaymentMethod>> = paymentMethodRepo.getAll()
        .map { it.associateBy { pm -> pm.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun toggleActive(expense: RecurringExpense) = viewModelScope.launch { repo.toggleActive(expense) }
    fun delete(expense: RecurringExpense) = viewModelScope.launch { repo.delete(expense) }
}
