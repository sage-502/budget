package com.sage502.budgettracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.PaymentMethod
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.data.repository.CategoryRepository
import com.sage502.budgettracker.data.repository.PaymentMethodRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.currentMonthKey
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val paymentMethodRepo: PaymentMethodRepository,
) : ViewModel() {

    private val _monthKey = MutableStateFlow(currentMonthKey())
    val monthKey: StateFlow<String> = _monthKey.asStateFlow()

    val categories: StateFlow<Map<Int, Category>> = categoryRepo.getAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val paymentMethods: StateFlow<Map<Int, PaymentMethod>> = paymentMethodRepo.getAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val transactions: StateFlow<List<Transaction>> = _monthKey.flatMapLatest { key ->
        val (start, end) = monthKeyToRange(key)
        transactionRepo.getByMonth(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prevMonth() = _monthKey.update { YearMonth.parse(it).minusMonths(1).toString() }
    fun nextMonth() = _monthKey.update { YearMonth.parse(it).plusMonths(1).toString() }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { transactionRepo.delete(transaction) }
    }
}
