package com.sage502.budgettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int
)
