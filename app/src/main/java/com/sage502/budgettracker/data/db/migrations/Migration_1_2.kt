package com.sage502.budgettracker.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove duplicate (monthKey, categoryId) rows, keeping the row with the highest amount
        db.execSQL(
            """
            DELETE FROM budget WHERE id NOT IN (
                SELECT MIN(id) FROM budget b
                WHERE b.amount = (
                    SELECT MAX(amount) FROM budget b2
                    WHERE b2.monthKey = b.monthKey AND b2.categoryId = b.categoryId
                )
                GROUP BY b.monthKey, b.categoryId
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_budget_monthKey_categoryId ON budget(monthKey, categoryId)"
        )
    }
}
