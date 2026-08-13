package com.sage502.budgettracker.data

import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.entity.PaymentMethod

data class DefaultCategoryInfo(
    val name: String,
    val icon: String,
    val color: String,
    val defaultBudget: Long,
    val sortOrder: Int
)

object DefaultData {
    val DEFAULT_CATEGORIES = listOf(
        DefaultCategoryInfo("식비",  "restaurant",      "#F44336", 300_000L, 0),
        DefaultCategoryInfo("주거",  "home",             "#2196F3", 400_000L, 1),
        DefaultCategoryInfo("교통",  "directions_car",   "#4CAF50", 100_000L, 2),
        DefaultCategoryInfo("건강",  "health_and_safety","#E91E63",  30_000L, 3),
        DefaultCategoryInfo("쇼핑",  "shopping_cart",    "#9C27B0", 150_000L, 4),
        DefaultCategoryInfo("여가",  "sports_esports",   "#FF9800",  80_000L, 5),
        DefaultCategoryInfo("기타",  "more_horiz",       "#9E9E9E",  50_000L, 6),
    )

    val DEFAULT_PAYMENT_METHODS = listOf(
        PaymentMethod(name = "카드", isDefault = true, isActive = true, sortOrder = 0),
        PaymentMethod(name = "현금", isDefault = true, isActive = true, sortOrder = 1),
        PaymentMethod(name = "계좌", isDefault = true, isActive = true, sortOrder = 2),
    )

    fun toCategories(): List<Category> = DEFAULT_CATEGORIES.mapIndexed { index, info ->
        Category(
            id = index + 1,
            name = info.name,
            icon = info.icon,
            color = info.color,
            isDefault = true,
            isActive = true,
            sortOrder = info.sortOrder
        )
    }
}
