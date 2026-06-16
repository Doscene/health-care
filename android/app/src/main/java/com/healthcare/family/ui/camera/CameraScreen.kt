package com.healthcare.family.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 相机模式
 */
enum class CameraMode {
    MEDICINE_BOX,    // 药盒拍照
    BLOOD_PRESSURE,  // 血压计屏幕
    GENERAL,         // 通用拍照
}

/**
 * 相机页面
 * 支持拍照识别药盒和血压计屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    cameraMode: CameraMode = CameraMode.GENERAL,
    onBack: () -> Unit,
    onImageCaptured: (String) -> Unit, // 返回图片文件路径
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        NoPermissionView(onBack = onBack)
        return
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (cameraMode) {
                            CameraMode.MEDICINE_BOX -> "拍摄药盒"
                            CameraMode.BLOOD_PRESSURE -> "拍摄血压计"
                            CameraMode.GENERAL -> "拍照"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                        Icon(
                            imageVector = if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "闪光灯",
                        )
                    }
                    IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                        Icon(Icons.Default.SwitchCamera, contentDescription = "切换摄像头")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 相机预览
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()

                        imageCapture = capture

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture,
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )

            // 取景框指引
            when (cameraMode) {
                CameraMode.MEDICINE_BOX -> {
                    GuideOverlay(
                        text = "将药盒放入框内",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                CameraMode.BLOOD_PRESSURE -> {
                    GuideOverlay(
                        text = "对准血压计屏幕",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                CameraMode.GENERAL -> { /* 无指引 */ }
            }

            // 拍照按钮
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 提示文字
                Text(
                    text = when (cameraMode) {
                        CameraMode.MEDICINE_BOX -> "确保药品名称和规格清晰可见"
                        CameraMode.BLOOD_PRESSURE -> "确保数字清晰可见"
                        CameraMode.GENERAL -> "点击拍照"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 拍照按钮
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton

                        val photoFile = File(
                            context.cacheDir,
                            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.jpg"
                        )

                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onImageCaptured(photoFile.absolutePath)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Photo capture failed", exception)
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color.Gray, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "拍照",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/**
 * 取景框指引覆盖层
 */
@Composable
private fun GuideOverlay(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(280.dp, 200.dp)
                .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * 无权限视图
 */
@Composable
private fun NoPermissionView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "需要相机权限才能使用此功能",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("返回")
        }
    }
}
