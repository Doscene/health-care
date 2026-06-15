package com.healthcare.family.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthcare.family.data.remote.api.FirstAidGuideDto
import com.healthcare.family.data.remote.api.FirstAidKitDto

/**
 * 急救包管理页面：急救物品列表 + 添加弹窗 + 急救指南。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstAidKitScreen(
    onBack: () -> Unit,
    viewModel: FirstAidKitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var selectedGuide by remember { mutableStateOf<FirstAidGuideDto?>(null) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("急救包管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.loadGuides(); showGuideDialog = true }) {
                        Text("急救指南")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加物品")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 即将过期提醒
            val expiringItems = uiState.items.filter { item ->
                item.expireDate != null && try {
                    java.time.LocalDate.parse(item.expireDate.take(10))
                        .isBefore(java.time.LocalDate.now().plusDays(30))
                } catch (_: Exception) { false }
            }
            if (expiringItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(
                                0xFFFFF7ED,
                            ),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚠️", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${expiringItems.size}件物品即将过期",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFFB45309),
                                )
                                Text(
                                    text = expiringItems.joinToString("、") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color(0xFF92400E),
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "急救包为空",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "添加常备药品和急救用品",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(uiState.items) { item ->
                    KitItemCard(
                        item = item,
                        onDelete = { viewModel.deleteItem(item.id) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddKitItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, qty, expire, notes ->
                viewModel.addItem(name, type, qty, expire, notes)
                showAddDialog = false
            },
        )
    }

    // 急救指南弹窗
    if (showGuideDialog) {
        GuideListDialog(
            guides = uiState.guides,
            selectedGuide = selectedGuide,
            onSelectGuide = { selectedGuide = it },
            onBack = { selectedGuide = null },
            onDismiss = { showGuideDialog = false; selectedGuide = null },
        )
    }
}

@Composable
private fun KitItemCard(item: FirstAidKitDto, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val isExpiring = item.expireDate != null && try {
        java.time.LocalDate.parse(item.expireDate.take(10))
            .isBefore(java.time.LocalDate.now().plusDays(30))
    } catch (_: Exception) { false }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpiring)
                androidx.compose.ui.graphics.Color(0xFFFFFBEB)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (item.type == "medicine") Icons.Default.MedicalServices
                    else Icons.Default.LocalHospital,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.type == "medicine") "药品" else "用品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.quantity > 1) {
                    Text(
                        text = "数量: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (item.expireDate != null) {
                    Text(
                        text = "有效期: ${item.expireDate.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExpiring)
                            androidx.compose.ui.graphics.Color(0xFFDC2626)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.notes != null) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${item.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AddKitItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, quantity: Int, expireDate: String?, notes: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("medicine") }
    var quantity by remember { mutableStateOf("1") }
    var expireDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加急救物品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("物品名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "medicine",
                        onClick = { type = "medicine" },
                        label = { Text("药品") },
                    )
                    FilterChip(
                        selected = type == "supply",
                        onClick = { type = "supply" },
                        label = { Text("用品") },
                    )
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = expireDate,
                    onValueChange = { expireDate = it },
                    label = { Text("有效期（如 2027-06-15）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（选填）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, type, quantity.toIntOrNull() ?: 1, expireDate.ifBlank { null }, notes.ifBlank { null })
                },
                enabled = name.isNotBlank(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun GuideListDialog(
    guides: List<FirstAidGuideDto>,
    selectedGuide: FirstAidGuideDto?,
    onSelectGuide: (FirstAidGuideDto) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedGuide != null) selectedGuide.title else "急救指南",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            if (selectedGuide != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = selectedGuide.content,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val steps = selectedGuide.steps
                    if (steps is List<*>) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("操作步骤:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        steps.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else if (guides.isEmpty()) {
                Text("暂无急救指南", style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    guides.forEach { guide ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            TextButton(
                                onClick = { onSelectGuide(guide) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(guide.title, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedGuide != null) {
                TextButton(onClick = onBack) { Text("返回列表") }
            } else {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}
