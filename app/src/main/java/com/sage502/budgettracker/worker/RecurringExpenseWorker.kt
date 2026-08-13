package com.sage502.budgettracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.sage502.budgettracker.data.entity.Transaction
import com.sage502.budgettracker.data.repository.RecurringExpenseRepository
import com.sage502.budgettracker.data.repository.TransactionRepository
import com.sage502.budgettracker.util.monthKeyToRange
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringExpenseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recurringRepo: RecurringExpenseRepository,
    private val transactionRepo: TransactionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val dayOfMonth = today.dayOfMonth
        val monthKey = YearMonth.now().toString()
        val (monthStart, monthEnd) = monthKeyToRange(monthKey)

        val expenses = recurringRepo.getActiveByDay(dayOfMonth)
        expenses.forEach { expense ->
            val alreadyCreated = transactionRepo.findByRecurringAndMonth(expense.id, monthStart, monthEnd)
            if (alreadyCreated == null) {
                transactionRepo.insert(
                    Transaction(
                        amount = expense.amount,
                        categoryId = expense.categoryId,
                        paymentMethodId = expense.paymentMethodId,
                        date = System.currentTimeMillis(),
                        memo = expense.memo,
                        recurringId = expense.id
                    )
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "RecurringExpenseWorker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val zone = java.time.ZoneId.systemDefault()
            val tomorrow = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return (tomorrow - now).coerceAtLeast(0)
        }
    }
}
