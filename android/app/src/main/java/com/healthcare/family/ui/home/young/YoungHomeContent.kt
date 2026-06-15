package com.healthcare.family.ui.home.young

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcare.family.data.remote.api.PatientHomeDto

/**
 * 年轻患者首页：人生阶段、今日待办、趋势图、快捷入口、数据洞察。
 */
@Composable
fun YoungHomeContent(
    onNavigate: (String) -> Unit,
    onAlertClick: ((com.healthcare.family.data.remote.api.AlertSummaryDto) -> Unit)? = null,
    patientHome: PatientHomeDto? = null,
    isLoading: Boolean = false,
) {
    val userName = patientHome?.user?.name ?: "用户"
    val diseases = patientHome?.user?.diseases ?: emptyList()
    val pendingMeds = patientHome?.todayMedications?.size ?: 0
    val bp = patientHome?.latestBp
    val bg = patientHome?.latestBg
    val alerts = patientHome?.activeAlerts ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 问候语 + 病种标签
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "你好，$userName",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (diseases.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            diseases.forEach { disease ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFCE7F3))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = disease,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFBE185D),
                                    )
                                }
                            }
                        }
                    }
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

        // 今日待办
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("今日待办", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (pendingMeds > 0) "${pendingMeds}项待服药" else "暂无待办",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pendingMeds > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 本周血压趋势卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    Text(
                        text = "最新血压",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (bp != null) "${bp.systolic}/${bp.diastolic}" else "--/--",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "mmHg",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = when {
                            bp == null -> "暂无记录，点击快捷入口开始记录"
                            bp.systolic >= 140 || bp.diastolic >= 90 -> "血压偏高，注意休息"
                            else -> "血压控制良好，继续保持"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        // 预警提示
        if (alerts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alerts.firstOrNull()?.let { onAlertClick?.invoke(it) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF7ED),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠ ${alerts.size}条风险预警",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFB45309),
                        )
                        alerts.take(2).forEach { alert ->
                            Text(
                                text = "${alert.triggerType}: ${alert.triggerValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E),
                            )
                        }
                    }
                }
            }
        }

        // 快捷记录入口
        item {
            Text(text = "快捷记录", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickEntryCard(
                    icon = Icons.Default.MonitorHeart,
                    label = "血压",
                    color = Color(0xFFEFF6FF),
                    onClick = { onNavigate("record") },
                    modifier = Modifier.weight(1f),
                )
                QuickEntryCard(
                    icon = Icons.Default.Bloodtype,
                    label = "血糖",
                    color = Color(0xFFFFFBEB),
                    onClick = { onNavigate("record") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 数据洞察
        if (bg != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAF5FF),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "数据洞察",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF312E81),
                            )
                            Text(
                                text = "最近血糖 ${bg.value} mmol/L (${bg.type})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4338CA),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEntryCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
