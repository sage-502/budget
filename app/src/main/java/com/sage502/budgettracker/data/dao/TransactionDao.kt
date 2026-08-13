package com.sage502.budgettracker.data.dao

import androidx.room.*
import com.sage502.budgettracker.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

data class CategorySum(val categoryId: Int, val total: Long)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction_entry WHERE date BETWEEN :monthStart AND :monthEnd ORDER BY date DESC")
    fun getByMonth(monthStart: Long, monthEnd: Long): Flow<List<Transaction>>

    @Query("SELECT categoryId, SUM(amount) as total FROM transaction_entry WHERE date BETWEEN :monthStart AND :monthEnd GROUP BY categoryId")
    fun getSumByCategory(monthStart: Long, monthEnd: Long): Flow<List<CategorySum>>

    @Query("SELECT SUM(amount) FROM transaction_entry WHERE date BETWEEN :monthStart AND :monthEnd")
    suspend fun getTotalOnce(monthStart: Long, monthEnd: Long): Long?

    @Query("SELECT * FROM transaction_entry WHERE recurringId = :recurringId AND date BETWEEN :monthStart AND :monthEnd LIMIT 1")
    suspend fun findByRecurringAndMonth(recurringId: Int, monthStart: Long, monthEnd: Long): Transaction?

    @Query("SELECT * FROM transaction_entry WHERE id = :id")
    suspend fun getById(id: Int): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transaction_entry WHERE date BETWEEN :monthStart AND :monthEnd")
    suspend fun deleteByMonth(monthStart: Long, monthEnd: Long)
}
