package com.sage502.budgettracker.di

import android.content.Context
import androidx.room.Room
import com.sage502.budgettracker.data.dao.*
import com.sage502.budgettracker.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "budget_tracker.db")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .addCallback(AppDatabase.seedCallback())
            .build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePaymentMethodDao(db: AppDatabase): PaymentMethodDao = db.paymentMethodDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideRecurringExpenseDao(db: AppDatabase): RecurringExpenseDao = db.recurringExpenseDao()
}
