package com.sage502.budgettracker.data.dao

import androidx.room.*
import com.sage502.budgettracker.data.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE monthKey = :monthKey ORDER BY categoryId")
    fun getByMonth(monthKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE monthKey = :monthKey ORDER BY categoryId")
    suspend fun getByMonthOnce(monthKey: String): List<Budget>

    @Query("SELECT SUM(amount) FROM budget WHERE monthKey = :monthKey")
    fun getTotalByMonth(monthKey: String): Flow<Long?>

    @Query("SELECT SUM(amount) FROM budget WHERE monthKey = :monthKey")
    suspend fun getTotalByMonthOnce(monthKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(budget: Budget)

    @Query("DELETE FROM budget WHERE monthKey = :monthKey")
    suspend fun deleteByMonth(monthKey: String)
}
