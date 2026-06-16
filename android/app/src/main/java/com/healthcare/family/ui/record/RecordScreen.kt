package com.healthcare.family.ui.record

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.healthcare.family.data.remote.api.BpRecordDto
import com.healthcare.family.data.remote.api.BgRecordDto

/**
 * 快速记录页面：血压/血糖表单 + 最近记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onBack: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
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
                title = { Text("快速记录") },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("血压") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("血糖") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("语音") },
                )
            }

            when (selectedTab) {
                0 -> BloodPressureTab(
                    systolic = uiState.systolic,
                    diastolic = uiState.diastolic,
                    heartRate = uiState.heartRate,
                    isLoading = uiState.isLoading,
                    recentRecords = uiState.recentBpRecords,
                    onSystolicChanged = viewModel::onSystolicChanged,
                    onDiastolicChanged = viewModel::onDiastolicChanged,
                    onHeartRateChanged = viewModel::onHeartRateChanged,
                    onSubmit = viewModel::submitBpRecord,
                    onDeleteBp = viewModel::deleteBpRecord,
                )
                1 -> BloodSugarTab(
                    bgType = uiState.bgType,
                    bgValue = uiState.bgValue,
                    isLoading = uiState.isLoading,
                    recentRecords = uiState.recentBgRecords,
                    onBgTypeChanged = viewModel::onBgTypeChanged,
                    onBgValueChanged = viewModel::onBgValueChanged,
                    onSubmit = viewModel::submitBgRecord,
                    onDeleteBg = viewModel::deleteBgRecord,
                )
                2 -> VoiceTab(
                    recordState = uiState.voiceRecordState,
                    recognizedText = uiState.recognizedText,
                    parsedData = uiState.parsedHealthData,
                    isProcessing = uiState.isVoiceProcessing,
                    onRecordStart = viewModel::onVoiceRecordStart,
                    onRecordStop = { viewModel.onVoiceRecordStop("") },
                    onRecordCancel = viewModel::onVoiceRecordCancel,
                    onConfirm = viewModel::confirmVoiceRecord,
                    onRetry = viewModel::onVoiceRecordCancel,
                )
            }
        }
    }
}

@Composable
private fun BloodPressureTab(
    systolic: String,
    diastolic: String,
    heartRate: String,
    isLoading: Boolean,
    recentRecords: List<BpRecordDto>,
    onSystolicChanged: (String) -> Unit,
    onDiastolicChanged: (String) -> Unit,
    onHeartRateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDeleteBp: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("记录血压", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = systolic,
                    onValueChange = onSystolicChanged,
                    label = { Text("收缩压") },
                    suffix = { Text("mmHg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = diastolic,
                    onValueChange = onDiastolicChanged,
                    label = { Text("舒张压") },
                    suffix = { Text("mmHg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            OutlinedTextField(
                value = heartRate,
                onValueChange = onHeartRateChanged,
                label = { Text("心率（选填）") },
                suffix = { Text("次/分") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                else Text("记录血压")
            }
        }

        if (recentRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("最近记录", style = MaterialTheme.typography.titleMedium)
            }
            items(recentRecords) { record ->
                BpRecordItem(
                    record = record,
                    onDelete = { onDeleteBp(record.id) },
                )
            }
        }
    }
}

@Composable
private fun BloodSugarTab(
    bgType: String,
    bgValue: String,
    isLoading: Boolean,
    recentRecords: List<BgRecordDto>,
    onBgTypeChanged: (String) -> Unit,
    onBgValueChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDeleteBg: (String) -> Unit = {},
) {
    val typeOptions = listOf(
        "fasting" to "空腹",
        "before_meal" to "餐前",
        "after_meal" to "餐后",
        "before_sleep" to "睡前",
        "random" to "随机",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("记录血糖", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Text("测量时间", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeOptions.take(3).forEach { (value, label) ->
                    FilterChip(
                        selected = bgType == value,
                        onClick = { onBgTypeChanged(value) },
                        label = { Text(label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeOptions.drop(3).forEach { (value, label) ->
                    FilterChip(
                        selected = bgType == value,
                        onClick = { onBgTypeChanged(value) },
                        label = { Text(label) },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = bgValue,
                onValueChange = onBgValueChanged,
                label = { Text("血糖值") },
                suffix = { Text("mmol/L") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                else Text("记录血糖")
            }
        }

        if (recentRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("最近记录", style = MaterialTheme.typography.titleMedium)
            }
            items(recentRecords) { record ->
                BgRecordItem(
                    record = record,
                    onDelete = { onDeleteBg(record.id) },
                )
            }
        }
    }
}

@Composable
private fun VoiceTab(
    recordState: VoiceRecordState,
    recognizedText: String,
    parsedData: ParsedHealthData?,
    isProcessing: Boolean,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onRecordCancel: () -> Unit,
    onConfirm: (ParsedHealthData) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("语音记录", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Text(
                text = "说出您的血压或血糖数据，例如：\n“血压135 85” 或 “空腹血糖6.8”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            if (recognizedText.isEmpty()) {
                // 录音按钮
                VoiceRecordButton(
                    recordState = recordState,
                    onRecordStart = onRecordStart,
                    onRecordStop = onRecordStop,
                    onRecordCancel = onRecordCancel,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // 识别结果
                VoiceRecognitionResult(
                    recognizedText = recognizedText,
                    parsedData = parsedData,
                    isProcessing = isProcessing,
                    onConfirm = onConfirm,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun BpRecordItem(record: BpRecordDto, onDelete: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${record.systolic}/${record.diastolic} mmHg",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (record.heartRate != null) {
                Text(
                    text = "心率 ${record.heartRate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = record.recordedAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun BgRecordItem(record: BgRecordDto, onDelete: () -> Unit = {}) {
    val typeLabel = when (record.type) {
        "fasting" -> "空腹"
        "before_meal" -> "餐前"
        "after_meal" -> "餐后"
        "before_sleep" -> "睡前"
        "random" -> "随机"
        else -> record.type
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${record.value} mmol/L",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = record.recordedAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
