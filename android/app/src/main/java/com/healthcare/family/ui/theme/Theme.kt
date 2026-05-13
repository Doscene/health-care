package com.healthcare.family.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green100,
    secondary = Blue900,
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Gray50,
    onBackground = Gray900,
    onSurface = Gray900,
    outline = Gray200,
    error = RiskRed
)

/**
 * 根据老人模式开关自动切换 Typography。
 * 后续 Phase 1 接入 DataStore 偏好。
 */
@Composable
fun HealthCareTheme(
    isElderlyMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val typography = if (isElderlyMode) ElderlyTypography else RegularTypography

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = typography,
        content = content
    )
}
