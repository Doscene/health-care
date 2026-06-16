package com.healthcare.family.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * 录音状态
 */
enum class VoiceRecordState {
    IDLE,       // 空闲
    RECORDING,  // 录音中
    COMPLETED,  // 录音完成
}

/**
 * 语音录音按钮组件
 * 支持长按录音，显示波形动画
 */
@Composable
fun VoiceRecordButton(
    recordState: VoiceRecordState,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onRecordCancel: () -> Unit,
    modifier: Modifier = Modifier,
    maxDurationSeconds: Int = 60,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onRecordStart()
        }
    }

    var recordingTime by remember { mutableFloatStateOf(0f) }

    // 录音计时
    LaunchedEffect(recordState) {
        if (recordState == VoiceRecordState.RECORDING) {
            recordingTime = 0f
            while (recordingTime < maxDurationSeconds) {
                delay(100)
                recordingTime += 0.1f
            }
            onRecordStop()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 波形动画
        if (recordState == VoiceRecordState.RECORDING) {
            WaveformAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTime(recordingTime),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "松开结束录音",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (recordState == VoiceRecordState.COMPLETED) "录音完成" else "按住开始录音",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 录音按钮
        RecordButton(
            recordState = recordState,
            onRecordStart = {
                if (hasPermission) {
                    onRecordStart()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onRecordStop = onRecordStop,
            onRecordCancel = onRecordCancel,
        )
    }
}

/**
 * 录音按钮（长按录音）
 */
@Composable
private fun RecordButton(
    recordState: VoiceRecordState,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onRecordCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColor by animateColorAsState(
        targetValue = when (recordState) {
            VoiceRecordState.RECORDING -> MaterialTheme.colorScheme.error
            VoiceRecordState.COMPLETED -> MaterialTheme.colorScheme.primary
            VoiceRecordState.IDLE -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "buttonColor",
    )

    val iconColor by animateColorAsState(
        targetValue = when (recordState) {
            VoiceRecordState.RECORDING -> Color.White
            VoiceRecordState.COMPLETED -> MaterialTheme.colorScheme.onPrimary
            VoiceRecordState.IDLE -> MaterialTheme.colorScheme.primary
        },
        label = "iconColor",
    )

    val buttonSize by animateFloatAsState(
        targetValue = if (recordState == VoiceRecordState.RECORDING) 72f else 64f,
        label = "buttonSize",
    )

    Box(
        modifier = modifier
            .size(buttonSize.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .pointerInput(recordState) {
                detectTapGestures(
                    onPress = {
                        when (recordState) {
                            VoiceRecordState.IDLE -> {
                                onRecordStart()
                                tryAwaitRelease()
                                onRecordStop()
                            }
                            VoiceRecordState.RECORDING -> {
                                onRecordStop()
                            }
                            VoiceRecordState.COMPLETED -> {
                                onRecordCancel()
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (recordState) {
                VoiceRecordState.RECORDING -> Icons.Default.Stop
                else -> Icons.Default.Mic
            },
            contentDescription = when (recordState) {
                VoiceRecordState.RECORDING -> "停止录音"
                VoiceRecordState.COMPLETED -> "重新录音"
                VoiceRecordState.IDLE -> "开始录音"
            },
            tint = iconColor,
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * 波形动画组件
 */
@Composable
private fun WaveformAnimation(
    modifier: Modifier = Modifier,
    barCount: Int = 40,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // 为每个波形条生成随机相位
    val phases = remember {
        List(barCount) { Random.nextFloat() * 2f * Math.PI.toFloat() }
    }

    // 为每个波形条生成随机振幅
    val amplitudes = remember {
        List(barCount) { 0.3f + Random.nextFloat() * 0.7f }
    }

    // 动画进度
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveformProgress",
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2)
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val x = (i * 2 + 1) * barWidth

            // 计算波形高度（使用正弦函数模拟）
            val phase = phases[i]
            val amplitude = amplitudes[i]
            val waveHeight = (sin(phase + animationProgress * 4 * Math.PI.toFloat()) * amplitude)
                .coerceIn(0.1f, 1f)

            val barHeight = waveHeight * size.height * 0.8f

            // 绘制上半部分
            drawLine(
                color = primaryColor.copy(alpha = 0.6f + waveHeight * 0.4f),
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * 格式化时间（秒 -> mm:ss）
 */
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%02d:%02d".format(minutes, secs)
}
