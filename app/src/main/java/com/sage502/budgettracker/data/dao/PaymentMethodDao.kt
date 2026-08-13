package com.sage502.budgettracker.data.dao

import androidx.room.*
import com.sage502.budgettracker.data.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_method ORDER BY sortOrder")
    fun getAll(): Flow<List<PaymentMethod>>

    @Query("SELECT * FROM payment_method WHERE isActive = 1 ORDER BY sortOrder")
    fun getActive(): Flow<List<PaymentMethod>>

    @Insert
    suspend fun insert(paymentMethod: PaymentMethod): Long

    @Update
    suspend fun update(paymentMethod: PaymentMethod)

    @Query("SELECT COUNT(*) FROM payment_method")
    suspend fun count(): Int
}
