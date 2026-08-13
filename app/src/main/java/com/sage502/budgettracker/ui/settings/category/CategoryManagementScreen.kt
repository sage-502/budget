@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sage502.budgettracker.data.entity.Category

@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    var editTarget by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("카테고리 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "카테고리 추가")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories.sortedBy { it.sortOrder }, key = { it.id }) { cat ->
                CategoryItem(
                    category = cat,
                    onEdit = { editTarget = cat },
                    onDeactivate = { viewModel.deactivate(cat) },
                    onRestore = { viewModel.restore(cat) },
                    onMoveUp = { viewModel.moveUp(cat) },
                    onMoveDown = { viewModel.moveDown(cat) }
                )
                HorizontalDivider()
            }
        }
    }

    editTarget?.let { cat ->
        RenameDialog(
            initial = cat.name,
            onConfirm = { name -> viewModel.rename(cat, name); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }

    if (showAddDialog) {
        RenameDialog(
            title = "카테고리 추가",
            initial = "",
            onConfirm = { name -> viewModel.addCategory(name); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onRestore: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (category.isActive) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (category.isActive) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, "위로", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, "아래로", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, "이름 변경", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDeactivate, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.VisibilityOff, "비활성화",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.RestoreFromTrash, "복구", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RenameDialog(
    title: String = "이름 변경",
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("카테고리 이름") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
