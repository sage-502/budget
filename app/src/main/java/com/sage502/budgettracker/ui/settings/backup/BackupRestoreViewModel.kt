package com.sage502.budgettracker.ui.settings.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sage502.budgettracker.data.entity.Budget
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.data.repository.BudgetRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.currentMonthKey
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupEvent {
    object ExportSuccess : BackupEvent()
    object ImportSuccess : BackupEvent()
    data class Error(val message: String) : BackupEvent()
}

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    application: Application,
    private val transactionRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
) : AndroidViewModel(application) {

    private val _event = MutableSharedFlow<BackupEvent>()
    val event: SharedFlow<BackupEvent> = _event.asSharedFlow()

    private val gson = Gson()
    private val monthKey = currentMonthKey()

    fun export(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (start, end) = monthKeyToRange(monthKey)
                val transactions = transactionRepo.getByMonth(start, end).first()
                val budgets = budgetRepo.getByMonth(monthKey).first()

                val json = JsonObject().apply {
                    addProperty("version", 1)
                    addProperty("monthKey", monthKey)
                    addProperty("exportedAt", System.currentTimeMillis())
                    add("budgets", gson.toJsonTree(budgets))
                    add("transactions", gson.toJsonTree(transactions))
                }

                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(gson.toJson(json).toByteArray())
                }
                _event.emit(BackupEvent.ExportSuccess)
            } catch (e: Exception) {
                _event.emit(BackupEvent.Error("내보내기 실패: ${e.message}"))
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonText = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception("파일을 읽을 수 없습니다")

                val root = gson.fromJson(jsonText, JsonObject::class.java)
                val importMonthKey = root.get("monthKey")?.asString ?: throw Exception("monthKey 없음")
                val budgets = gson.fromJson(root.get("budgets"), Array<Budget>::class.java).toList()
                val transactions = gson.fromJson(root.get("transactions"), Array<Transaction>::class.java).toList()

                val (start, end) = monthKeyToRange(importMonthKey)
                transactionRepo.deleteByMonth(start, end)
                budgetRepo.deleteByMonth(importMonthKey)

                budgets.forEach { b -> budgetRepo.setAmount(importMonthKey, b.categoryId, b.amount) }
                transactions.forEach { t -> transactionRepo.insert(t.copy(id = 0)) }

                _event.emit(BackupEvent.ImportSuccess)
            } catch (e: Exception) {
                _event.emit(BackupEvent.Error("가져오기 실패: ${e.message}"))
            }
        }
    }
}
