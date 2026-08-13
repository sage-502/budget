package com.sage502.budgettracker.ui.settings.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage502.budgettracker.data.entity.Category
import com.sage502.budgettracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val repo: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deactivate(category: Category) = viewModelScope.launch { repo.deactivate(category) }
    fun restore(category: Category) = viewModelScope.launch { repo.restore(category) }

    fun rename(category: Category, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.update(category.copy(name = newName.trim())) }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val current = categories.value
            repo.insert(
                Category(
                    name = name.trim(),
                    icon = "more_horiz",
                    color = "#9E9E9E",
                    isDefault = false,
                    isActive = true,
                    sortOrder = (current.maxOfOrNull { it.sortOrder } ?: 0) + 1
                )
            )
        }
    }

    fun moveUp(category: Category) {
        val list = categories.value.sortedBy { it.sortOrder }
        val idx = list.indexOfFirst { it.id == category.id }.takeIf { it > 0 } ?: return
        val prev = list[idx - 1]
        viewModelScope.launch {
            repo.update(category.copy(sortOrder = prev.sortOrder))
            repo.update(prev.copy(sortOrder = category.sortOrder))
        }
    }

    fun moveDown(category: Category) {
        val list = categories.value.sortedBy { it.sortOrder }
        val idx = list.indexOfFirst { it.id == category.id }.takeIf { it < list.lastIndex } ?: return
        val next = list[idx + 1]
        viewModelScope.launch {
            repo.update(category.copy(sortOrder = next.sortOrder))
            repo.update(next.copy(sortOrder = category.sortOrder))
        }
    }
}
