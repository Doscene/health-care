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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcare.family.data.remote.RetrofitClient
import com.healthcare.family.data.remote.api.MonthlyReportDto
import com.healthcare.family.data.remote.api.MemberMonthlySummaryDto
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    familyId: String,
    onBack: () -> Unit,
) {
    var report by remember { mutableStateOf<MonthlyReportDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now().minusMonths(1)) }

    LaunchedEffect(selectedMonth) {
        isLoading = true
        try {
            val month = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val response = RetrofitClient.api.getMonthlyReport(familyId, month)
            if (response.code == 0) {
                report = response.data
            } else {
                error = response.message
            }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("家庭健康月报") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                }
            }
            report != null -> {
                val data = report!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // 月份选择
                    item {
                        MonthSelector(
                            selectedMonth = selectedMonth,
                            onMonthChange = { selectedMonth = it },
                        )
                    }

                    // 成员月报卡片
                    items(data.members) { member ->
                        MemberMonthlyCard(member)
                    }

                    // 家庭目标
                    if (data.goals.isNotEmpty()) {
                        item {
                            Text(
                                text = "家庭目标",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(data.goals) { goal ->
                            GoalCard(goal)
                        }
                    }

                    // 复诊记录
                    if (data.appointments.isNotEmpty()) {
                        item {
                            Text(
                                text = "复诊记录",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(data.appointments) { apt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(apt.department, fontWeight = FontWeight.Medium)
                                        Text(apt.hospital, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(apt.date, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
            Text("◀", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${selectedMonth.year}年${selectedMonth.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                if (selectedMonth < YearMonth.now()) {
                    onMonthChange(selectedMonth.plusMonths(1))
                }
            },
        ) {
            Text("▶", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MemberMonthlyCard(member: MemberMonthlySummaryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 血压趋势（4周）
            if (member.bpAvg != null) {
                Text("血压均值: ${member.bpAvg.systolic}/${member.bpAvg.diastolic} mmHg", style = MaterialTheme.typography.bodyMedium)
            }

            // 血糖趋势
            if (member.bgAvg != null) {
                Text("血糖均值: ${member.bgAvg} mmol/L", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 统计数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("达标天数", "${member.totalDays - member.abnormalDays}/${member.totalDays}")
                StatItem("服药依从", "${member.adherenceRate}%")
                StatItem("异常天数", "${member.abnormalDays}")
            }
        }
    }
}

@Composable
private fun GoalCard(goal: com.healthcare.family.data.remote.api.FamilyGoalSummaryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(goal.title, fontWeight = FontWeight.Medium)
                if (goal.achieved) {
                    Text("✅ 已达成", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (goal.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${goal.currentValue.toInt()}/${goal.targetValue.toInt()} ${goal.unit}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
