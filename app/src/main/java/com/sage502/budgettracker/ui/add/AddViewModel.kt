package com.sage502.budgettracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.PaymentMethod
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.data.repository.CategoryRepository
import com.sage502.budgettracker.data.repository.PaymentMethodRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AddUiState(
    val amountText: String = "",
    val selectedCategory: Category? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val dateMillis: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    val memo: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isEditMode: Boolean = false,
    val saved: Boolean = false
) {
    val isValid: Boolean get() = selectedCategory != null && selectedPaymentMethod != null && amountText.isNotBlank()
}

@HiltViewModel
class AddViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val paymentMethodRepo: PaymentMethodRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    private var editingId: Int? = null

    init {
        viewModelScope.launch {
            combine(
                categoryRepo.getActive(),
                paymentMethodRepo.getActive()
            ) { cats, pms -> cats to pms }.collect { (cats, pms) ->
                _state.update { it.copy(categories = cats, paymentMethods = pms) }
            }
        }
    }

    fun loadTransaction(id: Int) {
        viewModelScope.launch {
            val tx = transactionRepo.getById(id) ?: return@launch
            editingId = tx.id
            val cat = state.value.categories.find { it.id == tx.categoryId }
            val pm = state.value.paymentMethods.find { it.id == tx.paymentMethodId }
            _state.update {
                it.copy(
                    amountText = tx.amount.toString(),
                    selectedCategory = cat,
                    selectedPaymentMethod = pm,
                    dateMillis = tx.date,
                    memo = tx.memo,
                    isEditMode = true
                )
            }
        }
    }

    fun setAmount(text: String) = _state.update { it.copy(amountText = text.filter(Char::isDigit)) }
    fun setCategory(cat: Category) = _state.update { it.copy(selectedCategory = cat) }
    fun setPaymentMethod(pm: PaymentMethod) = _state.update { it.copy(selectedPaymentMethod = pm) }
    fun setDate(millis: Long) = _state.update { it.copy(dateMillis = millis) }
    fun setMemo(memo: String) = _state.update { it.copy(memo = memo) }

    fun save() {
        val s = state.value
        if (!s.isValid) return
        val amount = s.amountText.toLongOrNull() ?: return
        viewModelScope.launch {
            val tx = Transaction(
                id = editingId ?: 0,
                amount = amount,
                categoryId = s.selectedCategory!!.id,
                paymentMethodId = s.selectedPaymentMethod!!.id,
                date = s.dateMillis,
                memo = s.memo
            )
            if (editingId != null) transactionRepo.update(tx) else transactionRepo.insert(tx)
            _state.update { it.copy(saved = true) }
        }
    }
}
