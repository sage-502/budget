@file:OptIn(ExperimentalMaterial3Api::class)

package com.sage502.budgettracker.ui.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.export(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { pendingImportUri = it } }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is BackupEvent.ExportSuccess -> showSuccessMessage = "내보내기 완료"
                is BackupEvent.ImportSuccess -> { showSuccessMessage = "가져오기 완료"; onBack() }
                is BackupEvent.Error -> errorMessage = event.message
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("백업 / 복원") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "뒤로") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Warning banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "가져오기 시 현재 월 데이터를 덮어씁니다.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = {
                    val fileName = "budget_backup_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("현재 월 데이터 내보내기") }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("백업 파일 가져오기") }

            if (showSuccessMessage.isNotBlank()) {
                Text(showSuccessMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Import confirmation dialog
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("가져오기 확인") },
            text = { Text("현재 월 데이터를 선택한 파일로 덮어씁니다. 계속할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.import(uri)
                    pendingImportUri = null
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("취소") }
            }
        )
    }

    // Error dialog
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("오류") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("확인") }
            }
        )
    }
}
