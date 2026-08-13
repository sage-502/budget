package com.sage502.budgettracker.data.repository

import com.sage502.budgettracker.data.dao.CategorySum
import com.sage502.budgettracker.data.dao.TransactionDao
import com.sage502.budgettracker.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {
    fun getByMonth(monthStart: Long, monthEnd: Long): Flow<List<Transaction>> =
        dao.getByMonth(monthStart, monthEnd)

    fun getSumByCategory(monthStart: Long, monthEnd: Long): Flow<List<CategorySum>> =
        dao.getSumByCategory(monthStart, monthEnd)

    suspend fun getTotalOnce(monthStart: Long, monthEnd: Long): Long? =
        dao.getTotalOnce(monthStart, monthEnd)

    suspend fun findByRecurringAndMonth(recurringId: Int, monthStart: Long, monthEnd: Long): Transaction? =
        dao.findByRecurringAndMonth(recurringId, monthStart, monthEnd)

    suspend fun getById(id: Int): Transaction? = dao.getById(id)
    suspend fun insert(transaction: Transaction): Long = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    suspend fun deleteByMonth(monthStart: Long, monthEnd: Long) = dao.deleteByMonth(monthStart, monthEnd)
}
