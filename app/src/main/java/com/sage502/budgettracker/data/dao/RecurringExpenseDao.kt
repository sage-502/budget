package com.sage502.budgettracker.data.dao

import androidx.room.*
import com.sage502.budgettracker.data.entity.RecurringExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expense ORDER BY id")
    fun getAll(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expense WHERE isActive = 1 AND dayOfMonth = :day")
    suspend fun getActiveByDay(day: Int): List<RecurringExpense>

    @Query("SELECT * FROM recurring_expense WHERE id = :id")
    suspend fun getById(id: Int): RecurringExpense?

    @Insert
    suspend fun insert(expense: RecurringExpense): Long

    @Update
    suspend fun update(expense: RecurringExpense)

    @Delete
    suspend fun delete(expense: RecurringExpense)
}
