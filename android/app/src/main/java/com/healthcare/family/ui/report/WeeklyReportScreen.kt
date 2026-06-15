package com.healthcare.family.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthcare.family.data.remote.api.BpRecordDto
import com.healthcare.family.data.remote.api.BgRecordDto
import com.healthcare.family.data.repository.RecordRepository

/**
 * 健康周报页面：近7天血压/血糖记录汇总。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReportScreen(
    onBack: () -> Unit,
) {
    var bpRecords by remember { mutableStateOf<List<BpRecordDto>>(emptyList()) }
    var bgRecords by remember { mutableStateOf<List<BgRecordDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 使用 Hilt 注入 Repository
    val recordRepository: RecordRepository = androidx.hilt.navigation.compose.hiltViewModel<WeeklyReportViewModel>().recordRepository

    LaunchedEffect(Unit) {
        recordRepository.getBpRecords(20).onSuccess { bpRecords = it }
        recordRepository.getBgRecords(20).onSuccess { bgRecords = it }
        isLoading = false
    }

    val avgSystolic = bpRecords.map { it.systolic }.average().takeIf { !it.isNaN() }?.toInt()
    val avgDiastolic = bpRecords.map { it.diastolic }.average().takeIf { !it.isNaN() }?.toInt()
    val avgBg = bgRecords.map { it.value }.average().takeIf { !it.isNaN() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健康周报") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 概览卡片
                item {
                    Text(
                        text = "健康概览",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryCard(
                            title = "血压均值",
                            value = if (avgSystolic != null) "$avgSystolic/$avgDiastolic" else "--/--",
                            unit = "mmHg",
                            count = bpRecords.size,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            title = "血糖均值",
                            value = avgBg?.let { String.format("%.1f", it) } ?: "--",
                            unit = "mmol/L",
                            count = bgRecords.size,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 血压趋势
                if (bpRecords.isNotEmpty()) {
                    item {
                        Text(
                            text = "血压记录",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(bpRecords.take(7)) { record ->
                        BpRecordRow(record)
                    }
                }

                // 血糖趋势
                if (bgRecords.isNotEmpty()) {
                    item {
                        Text(
                            text = "血糖记录",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(bgRecords.take(7)) { record ->
                        BgRecordRow(record)
                    }
                }

                // 空状态
                if (bpRecords.isEmpty() && bgRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = "暂无健康记录数据，请先记录血压或血糖",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    unit: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text("共${count}条记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun BpRecordRow(record: BpRecordDto) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${record.systolic}/${record.diastolic}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (record.heartRate != null) {
                Text("心率 ${record.heartRate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(record.recordedAt.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun BgRecordRow(record: BgRecordDto) {
    val typeLabel = when (record.type) {
        "fasting" -> "空腹"
        "before_meal" -> "餐前"
        "after_meal" -> "餐后"
        "before_sleep" -> "睡前"
        else -> record.type
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${record.value} mmol/L", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(typeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(record.recordedAt.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
