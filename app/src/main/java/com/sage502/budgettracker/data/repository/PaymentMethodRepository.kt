package com.sage502.budgettracker.data.repository

import com.sage502.budgettracker.data.dao.PaymentMethodDao
import com.sage502.budgettracker.data.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentMethodRepository @Inject constructor(private val dao: PaymentMethodDao) {
    fun getAll(): Flow<List<PaymentMethod>> = dao.getAll()
    fun getActive(): Flow<List<PaymentMethod>> = dao.getActive()
    suspend fun insert(pm: PaymentMethod): Long = dao.insert(pm)
    suspend fun update(pm: PaymentMethod) = dao.update(pm)
    suspend fun deactivate(pm: PaymentMethod) = dao.update(pm.copy(isActive = false))
    suspend fun restore(pm: PaymentMethod) = dao.update(pm.copy(isActive = true))
}
