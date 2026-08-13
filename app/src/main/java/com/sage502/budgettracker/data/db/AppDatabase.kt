package com.sage502.budgettracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sage502.budgettracker.data.DefaultData
import com.sage502.budgettracker.data.dao.*
import com.sage502.budgettracker.data.db.migrations.MIGRATION_1_2
import com.sage502.budgettracker.data.entity.*

@Database(
    entities = [
        Category::class,
        PaymentMethod::class,
        Budget::class,
        Transaction::class,
        RecurringExpense::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        val MIGRATIONS = arrayOf(MIGRATION_1_2)

        fun seedCallback() = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DefaultData.DEFAULT_CATEGORIES.forEach { info ->
                    db.execSQL(
                        "INSERT INTO category (name, icon, color, isDefault, isActive, sortOrder) VALUES (?, ?, ?, 1, 1, ?)",
                        arrayOf(info.name, info.icon, info.color, info.sortOrder)
                    )
                }
                DefaultData.DEFAULT_PAYMENT_METHODS.forEach { pm ->
                    db.execSQL(
                        "INSERT INTO payment_method (name, isDefault, isActive, sortOrder) VALUES (?, 1, 1, ?)",
                        arrayOf(pm.name, pm.sortOrder)
                    )
                }
            }
        }
    }
}
