package com.healthcare.family.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 常规字体 */
val RegularTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

/** 老人端大字号 (全局 1.3x 放大) */
val ElderlyTypography = Typography(
    headlineLarge = RegularTypography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = RegularTypography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
    titleLarge = RegularTypography.titleLarge.copy(fontSize = 26.sp, lineHeight = 34.sp),
    titleMedium = RegularTypography.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyLarge = RegularTypography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = RegularTypography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
    bodySmall = RegularTypography.bodySmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
    labelLarge = RegularTypography.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
    labelSmall = RegularTypography.labelSmall.copy(fontSize = 14.sp, lineHeight = 18.sp)
)
