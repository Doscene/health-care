package com.healthcare.family.ui.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.healthcare.family.data.remote.api.AlertDto

/**
 * 四级风险弹窗组件
 *
 * 黄色 🟡 → 底部Sheet，温和提示 + 健康科普
 * 橙色 🟠 → 全屏弹窗，问询卡片 + 通知紧急联系人
 * 红色 🔴 → 全屏弹窗，无法关闭 + 一键呼救按钮
 */
@Composable
fun RiskAlertDialog(
    alert: AlertDto,
    onAcknowledge: (String) -> Unit,
    onDismiss: () -> Unit,
    onEmergencyCall: (() -> Unit)? = null,
) {
    when (alert.level) {
        "yellow" -> YellowAlertContent(
            alert = alert,
            onAcknowledge = { onAcknowledge("acknowledged") },
            onDismiss = { onDismiss(); onAcknowledge("dismissed") },
        )
        "orange" -> OrangeAlertContent(
            alert = alert,
            onAcknowledge = { onAcknowledge("acknowledged") },
            onDismiss = { onDismiss(); onAcknowledge("dismissed") },
        )
        "red" -> RedAlertContent(
            alert = alert,
            onAcknowledge = { onAcknowledge("acknowledged") },
            onEmergencyCall = onEmergencyCall ?: {},
        )
        else -> {} // green — no dialog
    }
}

/**
 * 黄色弹窗：底部Sheet，温和科普提示
 */
@Composable
private fun YellowAlertContent(
    alert: AlertDto,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text("温馨提示", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Text(
                    text = when (alert.triggerType) {
                        "bp_high" -> "血压偏高，建议休息15分钟后复测。如果复测仍偏高，请注意清淡饮食，减少盐摄入。"
                        "bg_high" -> "血糖偏高，建议记录最近饮食情况。控制主食摄入，适量运动有助于血糖管理。"
                        "bg_low" -> "血糖偏低，请注意及时进食。随身携带糖果或饼干以备不时之需。"
                        "missed_dose" -> alert.triggerValue
                        else -> alert.triggerValue
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📋 健康小贴士",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 保持规律作息，每天测量并记录\n• 如有持续不适，建议咨询家庭医生\n• 保持心情舒畅，避免情绪波动",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAcknowledge) {
                Text("我知道了")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("忽略")
            }
        },
    )
}

/**
 * 橙色弹窗：全屏问询卡片 + 通知紧急联系人
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrangeAlertContent(
    alert: AlertDto,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("健康关注")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFEF3C7),
                    ),
                )
            },
            containerColor = Color(0xFFFFFBEB),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // 大图标
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFFB45309),
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (alert.triggerType) {
                        "bp_high" -> "血压最近波动有点大 📈"
                        "bg_high" -> "血糖最近有点高 📈"
                        "missed_dose" -> "服药需要坚持哦 💊"
                        else -> "身体状况需要关注"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = alert.triggerValue,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF92400E),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 问询卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "建议联系家庭医生咨询",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. 先休息15分钟后复测一次\n2. 如复测仍偏高，建议电话联系家庭医生\n3. 已通知您的第一紧急联系人关注",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 操作按钮
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("我已了解，会注意", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("稍后再说", color = Color(0xFF92400E))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 红色弹窗：全屏遮罩 + 危险提示 + 一键呼救
 */
@Composable
private fun RedAlertContent(
    alert: AlertDto,
    onAcknowledge: () -> Unit,
    onEmergencyCall: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFEF2F2)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 红色警告图标
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Color(0xFFDC2626),
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (alert.triggerType) {
                        "bp_critical" -> "血压危险值!"
                        "bg_critical" -> "血糖危险值!"
                        "bg_low" -> "低血糖危险!"
                        "symptom_combo" -> "需要紧急关注!"
                        else -> "危险预警!"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 具体数值
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = alert.triggerValue,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (alert.triggerType) {
                                "bp_critical" -> "请立即休息并联系医生"
                                "bg_critical" -> "请立即采取措施"
                                "bg_low" -> "请立即摄入糖分"
                                else -> "请立即关注"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF991B1B),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 核心操作按钮
                Button(
                    onClick = onEmergencyCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "一键拨打120",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 次要操作
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626),
                    ),
                ) {
                    Text(
                        text = "我知道了（仍会记录此条预警）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 急救信息提示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "急救提示",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "• 保持镇静，立即休息\n• 如有急救药物，按医嘱服用\n• 已通知全部紧急联系人",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
