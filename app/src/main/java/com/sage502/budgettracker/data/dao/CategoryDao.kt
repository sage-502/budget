package com.sage502.budgettracker.data.dao

import androidx.room.*
import com.sage502.budgettracker.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sortOrder")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE isActive = 1 ORDER BY sortOrder")
    fun getActive(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE isActive = 1 ORDER BY sortOrder")
    suspend fun getActiveOnce(): List<Category>

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int
}
