package com.sage502.budgettracker.data.repository

import com.sage502.budgettracker.data.DefaultData
import com.sage502.budgettracker.data.dao.BudgetDao
import com.sage502.budgettracker.data.dao.CategoryDao
import com.sage502.budgettracker.data.entity.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
) {
    fun getByMonth(monthKey: String): Flow<List<Budget>> = budgetDao.getByMonth(monthKey)
    fun getTotalByMonth(monthKey: String): Flow<Long?> = budgetDao.getTotalByMonth(monthKey)
    suspend fun getTotalByMonthOnce(monthKey: String): Long? = budgetDao.getTotalByMonthOnce(monthKey)

    suspend fun setAmount(monthKey: String, categoryId: Int, amount: Long) {
        budgetDao.insertOrReplace(Budget(monthKey = monthKey, categoryId = categoryId, amount = amount))
    }

    suspend fun ensureMonth(monthKey: String) {
        val existing = budgetDao.getByMonthOnce(monthKey).map { it.categoryId }.toSet()
        val activeCategories = categoryDao.getActive().first()
        val missing = activeCategories.filter { it.id !in existing }
        if (missing.isEmpty()) return

        val prevKey = YearMonth.parse(monthKey).minusMonths(1).toString()
        val prevAmounts = budgetDao.getByMonthOnce(prevKey).associate { it.categoryId to it.amount }
        val defaultByName = DefaultData.DEFAULT_CATEGORIES.associateBy { it.name }

        missing.forEach { category ->
            val amount = prevAmounts[category.id]
                ?: defaultByName[category.name]?.defaultBudget
                ?: 0L
            budgetDao.insertIgnore(Budget(monthKey = monthKey, categoryId = category.id, amount = amount))
        }
    }

    suspend fun deleteByMonth(monthKey: String) = budgetDao.deleteByMonth(monthKey)
}
