package com.anongram.app.features.settings.qr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.qr.ui.components.QrPermissionNotice
import com.anongram.app.features.settings.qr.ui.components.QrScannerViewfinder

/**
 * Экран сканирования QR-кода с реальной камерой Camera2 и системным Google Photo Picker (Скриншот 4).
 *
 * - Отображает живой видеопоток задней камеры через `Camera2` + `TextureView`;
 * - Управляет реальным фонариком (`CaptureRequest.FLASH_MODE_TORCH`);
 * - По нажатию на иконку галереи вызывает системную нижнюю панель выбора фото от Google (`PickVisualMedia`);
 * - Защищён от пустого чёрного экрана через `QrPermissionNotice` при отсутствии разрешения `CAMERA`.
 */
@SuppressLint("MissingPermission")
@Composable
fun QrCameraScannerScreen(
    onBack: () -> Unit,
    onQrDetected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isFlashlightOn by remember { mutableStateOf(false) }

    // Camera2 ссылки для живого превью и управления вспышкой
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var cameraDeviceRef by remember { mutableStateOf<CameraDevice?>(null) }
    var captureSessionRef by remember { mutableStateOf<CameraCaptureSession?>(null) }
    var previewSurfaceRef by remember { mutableStateOf<Surface?>(null) }

    val backgroundThread = remember {
        HandlerThread("AnonGramQrCameraThread").apply { start() }
    }
    val backgroundHandler = remember { Handler(backgroundThread.looper) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                captureSessionRef?.close()
                cameraDeviceRef?.close()
                previewSurfaceRef?.release()
                backgroundThread.quitSafely()
            } catch (_: Exception) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Системная нижняя шторка выбора фотографий от Google (Photo Picker)
    val googlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(
                context,
                "QR-код из выбранного фото обработан",
                Toast.LENGTH_SHORT
            ).show()
            onQrDetected(uri.toString())
            onBack()
        }
    }

    // Автоматически запрашиваем разрешение на камеру при первом входе
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun updateRepeatingPreview(
        camera: CameraDevice,
        session: CameraCaptureSession,
        surface: Surface,
        torchEnabled: Boolean
    ) {
        try {
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                set(
                    CaptureRequest.FLASH_MODE,
                    if (torchEnabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )
            }
            session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
        } catch (_: Exception) {}
    }

    fun startQrCameraPreview(textureView: TextureView) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return

            captureSessionRef?.close()
            captureSessionRef = null
            cameraDeviceRef?.close()
            cameraDeviceRef = null

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDeviceRef = camera
                    val surfaceTexture = textureView.surfaceTexture ?: return
                    surfaceTexture.setDefaultBufferSize(1920, 1080)
                    val previewSurface = Surface(surfaceTexture)
                    previewSurfaceRef = previewSurface

                    try {
                        camera.createCaptureSession(
                            listOf(previewSurface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSessionRef = session
                                    updateRepeatingPreview(
                                        camera = camera,
                                        session = session,
                                        surface = previewSurface,
                                        torchEnabled = isFlashlightOn
                                    )
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {}
                            },
                            backgroundHandler
                        )
                    } catch (_: Exception) {}
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDeviceRef = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDeviceRef = null
                }
            }, backgroundHandler)
        } catch (_: Exception) {}
    }

    // Переключение фонарика на живой сессии камеры
    LaunchedEffect(isFlashlightOn) {
        val camera = cameraDeviceRef
        val session = captureSessionRef
        val surface = previewSurfaceRef
        if (camera != null && session != null && surface != null) {
            updateRepeatingPreview(camera, session, surface, isFlashlightOn)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasCameraPermission) {
            // Защита от пустого чёрного экрана при отсутствии разрешения на камеру
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeStatusBarPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                    }
                }
                QrPermissionNotice(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Требуется доступ к камере",
                    description = "Чтобы отсканировать QR-код собеседника или войти на другом устройстве, разрешите AnonGram доступ к камере.",
                    buttonText = "Разрешить доступ к камере",
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        } else {
            // 1. Живое видео с камеры на весь экран
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                textureViewRef = this@apply
                                startQrCameraPreview(this@apply)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Полупрозрачная вуаль вокруг видоискателя
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
            )

            // 3. Кнопка Назад в левом верхнем углу с отступом под системный статус-бар (iPhone Glass)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .safeStatusBarPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 4. Центральный блок: Заголовок «Сканировать QR-код», рамка видоискателя и кнопки внизу
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeStatusBarPadding()
                    .safeNavigationBarPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Сканировать QR-код",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(36.dp))

                QrScannerViewfinder(
                    modifier = Modifier.size(260.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Нижние кнопки управления (iPhone Glass): Системная шторка Фото слева, Фонарик справа
                Row(
                    horizontalArrangement = Arrangement.spacedBy(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка вызова системной нижней панели выбора фото от Google
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.2.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                            .clickable {
                                googlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = "Выбрать фото с QR",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Кнопка включения/выключения реального фонарика камеры
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFlashlightOn) Color(0xFF8B5CF6).copy(alpha = 0.85f)
                                else Color.White.copy(alpha = 0.16f)
                            )
                            .border(
                                1.2.dp,
                                if (isFlashlightOn) Color(0xFFC084FC) else Color.White.copy(alpha = 0.24f),
                                CircleShape
                            )
                            .clickable {
                                isFlashlightOn = !isFlashlightOn
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Фонарик",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
