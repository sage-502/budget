@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.PaymentMethod
import com.sage502.budgettracker.util.formatDate
import java.time.Instant

@Composable
fun AddScreen(
    transactionId: Int?,
    onSaved: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(transactionId) {
        if (transactionId != null) viewModel.loadTransaction(transactionId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (state.isEditMode) "내역 수정" else "내역 추가",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = state.amountText,
            onValueChange = viewModel::setAmount,
            label = { Text("금액 (원)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        CategoryDropdown(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = viewModel::setCategory
        )

        PaymentMethodDropdown(
            paymentMethods = state.paymentMethods,
            selected = state.selectedPaymentMethod,
            onSelect = viewModel::setPaymentMethod
        )

        OutlinedTextField(
            value = formatDate(state.dateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("날짜") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("변경") }
            }
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
        ) {
            Text(if (state.isEditMode) "수정 저장" else "저장")
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("카테고리") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = { onSelect(cat); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodDropdown(
    paymentMethods: List<PaymentMethod>,
    selected: PaymentMethod?,
    onSelect: (PaymentMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("결제수단") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            paymentMethods.forEach { pm ->
                DropdownMenuItem(
                    text = { Text(pm.name) },
                    onClick = { onSelect(pm); expanded = false }
                )
            }
        }
    }
}
