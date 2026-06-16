package com.healthcare.family.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 添加药品页面
 * 支持拍照OCR识别和手动录入两种方式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 表单状态
    var name by remember { mutableStateOf("") }
    var specification by remember { mutableStateOf("") }
    var dosagePerTime by remember { mutableStateOf("") }
    var frequencyPerDay by remember { mutableStateOf("3") }
    var selectedTimeSlots by remember { mutableStateOf(listOf("morning", "noon", "evening")) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加药品") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Tab切换
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("手动录入") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("拍照识别") },
                )
            }

            when (selectedTab) {
                0 -> ManualEntryTab(
                    name = name,
                    onNameChange = { name = it },
                    specification = specification,
                    onSpecificationChange = { specification = it },
                    dosagePerTime = dosagePerTime,
                    onDosagePerTimeChange = { dosagePerTime = it },
                    frequencyPerDay = frequencyPerDay,
                    onFrequencyPerDayChange = { frequencyPerDay = it },
                    selectedTimeSlots = selectedTimeSlots,
                    onTimeSlotsChange = { selectedTimeSlots = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    isLoading = uiState.isLoading,
                    onSubmit = {
                        val dosage = dosagePerTime.toIntOrNull()
                        val frequency = frequencyPerDay.toIntOrNull()
                        if (name.isBlank()) {
                            viewModel.updateError("请输入药品名称")
                            return@ManualEntryTab
                        }
                        if (dosage == null || dosage <= 0) {
                            viewModel.updateError("请输入有效的每次用量")
                            return@ManualEntryTab
                        }
                        if (frequency == null || frequency <= 0) {
                            viewModel.updateError("请输入有效的每日次数")
                            return@ManualEntryTab
                        }
                        viewModel.addMedication(
                            name = name,
                            specification = specification,
                            dosagePerTime = dosage,
                            frequencyPerDay = frequency,
                            timeSlots = selectedTimeSlots,
                            startDate = java.time.LocalDate.now().toString(),
                            notes = notes.takeIf { it.isNotBlank() },
                        )
                    },
                )
                1 -> OcrEntryTab(
                    onNavigateToCamera = onNavigateToCamera,
                    recognizedName = uiState.ocrRecognizedName,
                    onNameChange = { viewModel.updateOcrName(it) },
                    onSubmit = {
                        if (uiState.ocrRecognizedName.isNotBlank()) {
                            name = uiState.ocrRecognizedName
                            selectedTab = 0
                        }
                    },
                )
            }
        }
    }
}

/**
 * 手动录入Tab
 */
@Composable
private fun ManualEntryTab(
    name: String,
    onNameChange: (String) -> Unit,
    specification: String,
    onSpecificationChange: (String) -> Unit,
    dosagePerTime: String,
    onDosagePerTimeChange: (String) -> Unit,
    frequencyPerDay: String,
    onFrequencyPerDayChange: (String) -> Unit,
    selectedTimeSlots: List<String>,
    onTimeSlotsChange: (List<String>) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
) {
    val timeSlotOptions = listOf(
        "morning" to "早晨",
        "noon" to "中午",
        "evening" to "晚上",
        "before_sleep" to "睡前",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 药品名称
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("药品名称 *") },
            placeholder = { Text("请输入药品名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // 规格
        OutlinedTextField(
            value = specification,
            onValueChange = onSpecificationChange,
            label = { Text("规格") },
            placeholder = { Text("如：500mg/片") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // 每次用量和每日次数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = dosagePerTime,
                onValueChange = onDosagePerTimeChange,
                label = { Text("每次用量 *") },
                placeholder = { Text("1") },
                suffix = { Text("片/粒") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = frequencyPerDay,
                onValueChange = onFrequencyPerDayChange,
                label = { Text("每日次数 *") },
                placeholder = { Text("3") },
                suffix = { Text("次") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        // 服药时间
        Text(
            text = "服药时间",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            timeSlotOptions.take(3).forEach { (value, label) ->
                FilterChip(
                    selected = selectedTimeSlots.contains(value),
                    onClick = {
                        onTimeSlotsChange(
                            if (selectedTimeSlots.contains(value)) {
                                selectedTimeSlots - value
                            } else {
                                selectedTimeSlots + value
                            }
                        )
                    },
                    label = { Text(label) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            timeSlotOptions.drop(3).forEach { (value, label) ->
                FilterChip(
                    selected = selectedTimeSlots.contains(value),
                    onClick = {
                        onTimeSlotsChange(
                            if (selectedTimeSlots.contains(value)) {
                                selectedTimeSlots - value
                            } else {
                                selectedTimeSlots + value
                            }
                        )
                    },
                    label = { Text(label) },
                )
            }
        }

        // 备注
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("备注") },
            placeholder = { Text("可选，如：饭后服用") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 提交按钮
        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text("添加药品")
            }
        }
    }
}

/**
 * 拍照识别Tab
 */
@Composable
private fun OcrEntryTab(
    onNavigateToCamera: () -> Unit,
    recognizedName: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "拍照识别药品",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "拍摄药盒正面，系统将自动识别药品名称和规格",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // 拍照按钮
        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("打开相机拍照")
        }

        // 识别结果
        if (recognizedName.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "识别结果",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recognizedName,
                        onValueChange = onNameChange,
                        label = { Text("药品名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("使用此名称")
            }
        }
    }
}
