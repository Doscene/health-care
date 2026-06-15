package com.healthcare.family.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthcare.family.data.local.TokenManager
import kotlinx.coroutines.launch

/**
 * 隐私设置页面：老人模式、数据共享等开关。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    tokenManager: TokenManager,
    onBack: () -> Unit,
) {
    val isElderlyMode by tokenManager.isElderlyMode.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var shareDataWithFamily by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("隐私设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // 老人模式
            PrivacySwitchItem(
                title = "老人模式",
                subtitle = "字体放大1.3倍，界面更简洁",
                checked = isElderlyMode,
                onCheckedChange = { enabled ->
                    scope.launch { tokenManager.setElderlyMode(enabled) }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 数据共享
            PrivacySwitchItem(
                title = "与家人共享健康数据",
                subtitle = "允许家庭成员查看您的健康指标",
                checked = shareDataWithFamily,
                onCheckedChange = { shareDataWithFamily = it },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            PrivacySwitchItem(
                title = "接收用药提醒推送",
                subtitle = "通过极光推送接收服药提醒",
                checked = true,
                onCheckedChange = { /* TODO */ },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            PrivacySwitchItem(
                title = "接收风险告警推送",
                subtitle = "当健康指标异常时通知家人",
                checked = true,
                onCheckedChange = { /* TODO */ },
            )
        }
    }
}

@Composable
private fun PrivacySwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
