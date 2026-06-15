package com.healthcare.family.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 登录注册页面：手机号 + 验证码 + 微信登录 + 隐私协议。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 标题
        Text(
            text = "家庭健康管理",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "守护家人的每一份健康",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 手机号输入
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChanged,
            label = { Text("手机号") },
            placeholder = { Text("请输入手机号") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 验证码输入 + 发送按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChanged,
                label = { Text("验证码") },
                placeholder = { Text("6位验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = viewModel::sendCode,
                enabled = !state.isLoading && state.countdown == 0,
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    text = if (state.countdown > 0) "${state.countdown}s" else "获取验证码",
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 错误提示
        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 登录按钮
        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading && state.phone.isNotEmpty() && state.code.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp).width(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("登录 / 注册", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 分割线
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  或  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 微信登录
        OutlinedButton(
            onClick = { /* TODO: 微信登录 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("微信一键登录")
        }

        Spacer(modifier = Modifier.weight(1f))

        // 隐私协议
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = state.agreedToPrivacy,
                onCheckedChange = viewModel::onPrivacyToggled,
            )
            Text(
                text = "我已阅读并同意《用户协议》和《隐私政策》",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
