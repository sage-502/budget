@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

data class RecurringEditState(
    val amountText: String = "",
    val selectedCategory: Category? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val dayText: String = "",
    val memo: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isEditMode: Boolean = false,
    val saved: Boolean = false
) {
    val isValid get() = selectedCategory != null && selectedPaymentMethod != null
            && amountText.isNotBlank() && dayText.toIntOrNull()?.let { it in 1..31 } == true
}

@HiltViewModel
class RecurringEditViewModel @Inject constructor(
    private val repo: RecurringExpenseRepository,
    private val categoryRepo: CategoryRepository,
    private val paymentMethodRepo: PaymentMethodRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringEditState())
    val state: StateFlow<RecurringEditState> = _state.asStateFlow()
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

    fun load(id: Int) {
        viewModelScope.launch {
            val exp = repo.getById(id) ?: return@launch
            editingId = exp.id
            val cat = state.value.categories.find { it.id == exp.categoryId }
            val pm = state.value.paymentMethods.find { it.id == exp.paymentMethodId }
            _state.update {
                it.copy(
                    amountText = exp.amount.toString(),
                    selectedCategory = cat,
                    selectedPaymentMethod = pm,
                    dayText = exp.dayOfMonth.toString(),
                    memo = exp.memo,
                    isEditMode = true
                )
            }
        }
    }

    fun setAmount(v: String) = _state.update { it.copy(amountText = v.filter(Char::isDigit)) }
    fun setCategory(c: Category) = _state.update { it.copy(selectedCategory = c) }
    fun setPaymentMethod(pm: PaymentMethod) = _state.update { it.copy(selectedPaymentMethod = pm) }
    fun setDay(v: String) = _state.update { it.copy(dayText = v.filter(Char::isDigit).take(2)) }
    fun setMemo(v: String) = _state.update { it.copy(memo = v) }

    fun save() {
        val s = state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val exp = RecurringExpense(
                id = editingId ?: 0,
                categoryId = s.selectedCategory!!.id,
                paymentMethodId = s.selectedPaymentMethod!!.id,
                amount = s.amountText.toLong(),
                dayOfMonth = s.dayText.toInt(),
                memo = s.memo,
                isActive = true
            )
            if (editingId != null) repo.update(exp) else repo.insert(exp)
            _state.update { it.copy(saved = true) }
        }
    }
}

@Composable
fun RecurringEditScreen(
    recurringId: Int?,
    onSaved: () -> Unit,
    viewModel: RecurringEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recurringId) { if (recurringId != null) viewModel.load(recurringId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "반복 지출 수정" else "반복 지출 추가") },
                navigationIcon = {
                    IconButton(onClick = onSaved) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                label = { Text("금액 (원)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(
                    value = state.selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("카테고리") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { viewModel.setCategory(cat); catExpanded = false }
                        )
                    }
                }
            }

            var pmExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = pmExpanded, onExpandedChange = { pmExpanded = !pmExpanded }) {
                OutlinedTextField(
                    value = state.selectedPaymentMethod?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("결제수단") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pmExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = pmExpanded, onDismissRequest = { pmExpanded = false }) {
                    state.paymentMethods.forEach { pm ->
                        DropdownMenuItem(
                            text = { Text(pm.name) },
                            onClick = { viewModel.setPaymentMethod(pm); pmExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.dayText,
                onValueChange = viewModel::setDay,
                label = { Text("결제일 (1~31)") },
                suffix = { Text("일") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.memo,
                onValueChange = viewModel::setMemo,
                label = { Text("메모 (선택)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.isEditMode) "수정 저장" else "저장") }
        }
    }
}
