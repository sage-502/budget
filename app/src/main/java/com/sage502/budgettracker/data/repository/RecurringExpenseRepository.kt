package com.sage502.budgettracker.data.repository

import com.sage502.budgettracker.data.dao.RecurringExpenseDao
import com.sage502.budgettracker.data.entity.RecurringExpense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(private val dao: RecurringExpenseDao) {
    fun getAll(): Flow<List<RecurringExpense>> = dao.getAll()
    suspend fun getActiveByDay(day: Int): List<RecurringExpense> = dao.getActiveByDay(day)
    suspend fun getById(id: Int): RecurringExpense? = dao.getById(id)
    suspend fun insert(expense: RecurringExpense): Long = dao.insert(expense)
    suspend fun update(expense: RecurringExpense) = dao.update(expense)
    suspend fun delete(expense: RecurringExpense) = dao.delete(expense)
    suspend fun toggleActive(expense: RecurringExpense) = dao.update(expense.copy(isActive = !expense.isActive))
}
