package com.sage502.budgettracker.data.repository

import com.sage502.budgettracker.data.dao.CategoryDao
import com.sage502.budgettracker.data.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = dao.getAll()
    fun getActive(): Flow<List<Category>> = dao.getActive()
    suspend fun getActiveOnce(): List<Category> = dao.getActiveOnce()
    suspend fun insert(category: Category): Long = dao.insert(category)
    suspend fun update(category: Category) = dao.update(category)
    suspend fun deactivate(category: Category) = dao.update(category.copy(isActive = false))
    suspend fun restore(category: Category) = dao.update(category.copy(isActive = true))
}
