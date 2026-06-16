package com.healthcare.family.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 解析后的健康数据类型
 */
enum class ParsedDataType {
    BLOOD_PRESSURE,
    BLOOD_SUgar,
    UNKNOWN,
}

/**
 * 解析后的健康数据
 */
data class ParsedHealthData(
    val type: ParsedDataType,
    val rawText: String,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val heartRate: Int? = null,
    val bgType: String? = null,
    val bgValue: Double? = null,
    val parsed: Boolean = false,
)

/**
 * 语音识别结果展示组件
 * 支持显示识别文本、解析结果、手动修正
 */
@Composable
fun VoiceRecognitionResult(
    recognizedText: String,
    parsedData: ParsedHealthData?,
    isProcessing: Boolean,
    onConfirm: (ParsedHealthData) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editText by remember(recognizedText) { mutableStateOf(recognizedText) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // 识别结果卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (parsedData?.parsed == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (parsedData?.parsed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (parsedData?.parsed == true) "识别成功" else "识别结果",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("识别文本") },
                        minLines = 2,
                    )
                } else {
                    Text(
                        text = recognizedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 解析结果卡片
        if (parsedData != null && parsedData.parsed) {
            ParsedDataCard(parsedData = parsedData)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    if (isEditing) {
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEditing) "取消编辑" else "手动修正")
            }

            Button(
                onClick = {
                    if (parsedData != null && parsedData.parsed) {
                        onConfirm(parsedData)
                    } else {
                        // 尝试用编辑后的文本重新解析
                        onConfirm(
                            ParsedHealthData(
                                type = ParsedDataType.UNKNOWN,
                                rawText = editText,
                                parsed = false,
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isProcessing,
            ) {
                Text("确认保存")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("重新录音")
        }
    }
}

/**
 * 解析结果详情卡片
 */
@Composable
private fun ParsedDataCard(
    parsedData: ParsedHealthData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "解析结果",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            when (parsedData.type) {
                ParsedDataType.BLOOD_PRESSURE -> {
                    DataRow(label = "类型", value = "血压")
                    DataRow(label = "收缩压", value = "${parsedData.systolic} mmHg")
                    DataRow(label = "舒张压", value = "${parsedData.diastolic} mmHg")
                    if (parsedData.heartRate != null) {
                        DataRow(label = "心率", value = "${parsedData.heartRate} 次/分")
                    }
                }
                ParsedDataType.BLOOD_SUgar -> {
                    DataRow(label = "类型", value = "血糖")
                    DataRow(label = "测量时间", value = getBgTypeLabel(parsedData.bgType ?: ""))
                    DataRow(label = "血糖值", value = "${parsedData.bgValue} mmol/L")
                }
                ParsedDataType.UNKNOWN -> {
                    Text(
                        text = "无法自动解析，请手动修正",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * 数据行
 */
@Composable
private fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 获取血糖类型标签
 */
private fun getBgTypeLabel(type: String): String {
    return when (type) {
        "fasting" -> "空腹"
        "before_meal" -> "餐前"
        "after_meal" -> "餐后"
        "after_meal_2h" -> "餐后两小时"
        "random" -> "随机"
        "bedtime" -> "睡前"
        else -> type
    }
}
