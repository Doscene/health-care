package com.healthcare.family.ui.home.elderly

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcare.family.data.remote.api.AlertSummaryDto
import com.healthcare.family.data.remote.api.PatientHomeDto

/**
 * 老人首页：问候语、今日待办、语音记录大按钮、最近指标。
 */
@Composable
fun ElderlyHomeContent(
    onNavigate: (String) -> Unit,
    onAlertClick: ((AlertSummaryDto) -> Unit)? = null,
    patientHome: PatientHomeDto? = null,
    isLoading: Boolean = false,
) {
    val userName = patientHome?.user?.name ?: "用户"
    val diseases = patientHome?.user?.diseases ?: emptyList()
    val greeting = when {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 12 -> "早上好"
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 18 -> "下午好"
        else -> "晚上好"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 问候语
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "$greeting，$userName",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (diseases.isNotEmpty()) diseases.joinToString("、") else "今天也要照顾好自己哦",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "通知",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // 加载中
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // 语音记录大按钮
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { onNavigate("record") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "语音记录",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "按住说话，记录今天的身体状况",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        // 今日待办
        val meds = patientHome?.todayMedications ?: emptyList()
        val alerts = patientHome?.activeAlerts ?: emptyList()

        // 预警提示（老人端）
        if (alerts.isNotEmpty()) {
            item {
                val redAlerts = alerts.filter { it.level == "red" }
                val orangeAlerts = alerts.filter { it.level == "orange" }
                val highestLevel = when {
                    redAlerts.isNotEmpty() -> "red"
                    orangeAlerts.isNotEmpty() -> "orange"
                    else -> "yellow"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { redAlerts.firstOrNull()?.let { onAlertClick?.invoke(it) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (highestLevel) {
                            "red" -> Color(0xFFFEE2E2)
                            "orange" -> Color(0xFFFFF7ED)
                            else -> Color(0xFFFEFCE8)
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = when (highestLevel) {
                                "red" -> Color(0xFFDC2626)
                                "orange" -> Color(0xFFB45309)
                                else -> Color(0xFFCA8A04)
                            },
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (highestLevel) {
                                    "red" -> "⚠ ${redAlerts.size}条危险预警！请立即关注"
                                    "orange" -> "⚠ ${alerts.size}条健康提醒"
                                    else -> "💡 ${alerts.size}条健康建议"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            alerts.take(2).forEach { alert ->
                                Text(
                                    text = alert.triggerValue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 今日待办

        if (meds.isEmpty() && !isLoading) {
            item {
                Text(
                    text = "暂无待服药物",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(meds) { med ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${med.dosagePerTime}片/次，${med.frequencyPerDay}次/天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 最近指标
        item {
            Text(
                text = "最近指标",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val bp = patientHome?.latestBp
                val bg = patientHome?.latestBg
                MetricCard(
                    title = "血压",
                    value = if (bp != null) "${bp.systolic}/${bp.diastolic}" else "--/--",
                    unit = "mmHg",
                    status = when {
                        bp == null -> "暂无数据"
                        bp.systolic >= 140 || bp.diastolic >= 90 -> "偏高"
                        bp.systolic < 90 || bp.diastolic < 60 -> "偏低"
                        else -> "正常"
                    },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "血糖",
                    value = bg?.value?.toString() ?: "--",
                    unit = "mmol/L",
                    status = when {
                        bg == null -> "暂无数据"
                        bg.value > 7.0 -> "偏高"
                        bg.value < 3.9 -> "偏低"
                        else -> "正常"
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
