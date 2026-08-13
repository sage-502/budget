package com.sage502.budgettracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["monthKey", "categoryId"], unique = true)
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthKey: String,
    val categoryId: Int,
    val amount: Long
)
