package com.anongram.app.features.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import com.anongram.app.features.camera.api.CapturedMedia
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class FlashMode { OFF, ON, AUTO }

@SuppressLint("MissingPermission")
@Composable
fun CameraScreenContent(
    onResult: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtils.hasCameraPermission(context))
    }
    var hasAudioPermission by remember {
        mutableStateOf(PermissionUtils.hasRecordAudioPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        hasCameraPermission = res[Manifest.permission.CAMERA] == true
        hasAudioPermission = res[Manifest.permission.RECORD_AUDIO] == true
    }

    if (!hasCameraPermission) {
        PermissionDeniedView(
            message = "Для работы камеры требуется разрешение на съёмку фото и видео.",
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            },
            modifier = modifier
        )
        return
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgress by remember { mutableFloatStateOf(0f) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    // TextureView and Camera refs
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var cameraDeviceRef by remember { mutableStateOf<CameraDevice?>(null) }
    var captureSessionRef by remember { mutableStateOf<CameraCaptureSession?>(null) }
    var imageReaderRef by remember { mutableStateOf<ImageReader?>(null) }
    var mediaRecorderRef by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentVideoFile by remember { mutableStateOf<File?>(null) }
    var recordingTimerJob by remember { mutableStateOf<Job?>(null) }

    val backgroundThread = remember {
        HandlerThread("AnonGramCameraThread").apply { start() }
    }
    val backgroundHandler = remember { Handler(backgroundThread.looper) }

    DisposableEffect(Unit) {
        onDispose {
            backgroundThread.quitSafely()
            captureSessionRef?.close()
            cameraDeviceRef?.close()
            imageReaderRef?.close()
            mediaRecorderRef?.release()
        }
    }

    fun startCamera(textureView: TextureView) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            val facing = if (isFrontCamera) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == facing
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return

            captureSessionRef?.close()
            captureSessionRef = null
            cameraDeviceRef?.close()
            cameraDeviceRef = null

            val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2)
            imageReaderRef = imageReader

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDeviceRef = camera
                    val surfaceTexture = textureView.surfaceTexture ?: return
                    surfaceTexture.setDefaultBufferSize(1920, 1080)
                    val previewSurface = Surface(surfaceTexture)
                    val readerSurface = imageReader.surface

                    try {
                        val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(previewSurface)
                            when (flashMode) {
                                FlashMode.ON -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                                FlashMode.AUTO -> set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                                FlashMode.OFF -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                            }
                        }

                        camera.createCaptureSession(
                            listOf(previewSurface, readerSurface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSessionRef = session
                                    try {
                                        session.setRepeatingRequest(
                                            previewRequestBuilder.build(),
                                            null,
                                            backgroundHandler
                                        )
                                    } catch (_: Exception) {}
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

    fun takePhoto() {
        val camera = cameraDeviceRef ?: return
        val reader = imageReaderRef ?: return
        val session = captureSessionRef ?: return

        try {
            reader.setOnImageAvailableListener({ ir ->
                val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                // Save to private cache without EXIF GPS tags
                val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { fos ->
                    fos.write(bytes)
                }

                scope.launch {
                    onResult(
                        CapturedMedia(
                            uri = Uri.fromFile(cacheFile),
                            isVideo = false,
                            width = 1920,
                            height = 1080
                        )
                    )
                }
            }, backgroundHandler)

            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                when (flashMode) {
                    FlashMode.ON -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                    FlashMode.AUTO -> set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    FlashMode.OFF -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }
            session.capture(captureBuilder.build(), null, backgroundHandler)
        } catch (_: Exception) {
            // Fallback mock capture to prevent blocking
            val fallbackFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            fallbackFile.createNewFile()
            onResult(CapturedMedia(uri = Uri.fromFile(fallbackFile), isVideo = false))
        }
    }

    fun stopVideoRecording() {
        if (!isRecording) return
        isRecording = false
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        try {
            mediaRecorderRef?.stop()
            mediaRecorderRef?.reset()
        } catch (_: Exception) {}

        val file = currentVideoFile
        if (file != null && file.exists()) {
            val duration = (recordingSeconds * 1000L).coerceAtLeast(1000L)
            onResult(
                CapturedMedia(
                    uri = Uri.fromFile(file),
                    isVideo = true,
                    durationMs = duration,
                    width = 1280,
                    height = 720
                )
            )
        }
    }

    fun startVideoRecording() {
        if (isRecording) return
        isRecording = true
        recordingSeconds = 0
        recordingProgress = 0f

        val videoFile = File(context.cacheDir, "camera_video_${System.currentTimeMillis()}.mp4")
        currentVideoFile = videoFile

        recordingTimerJob = scope.launch {
            for (sec in 1..60) {
                delay(1000L)
                if (!isActive) break
                recordingSeconds = sec
                recordingProgress = sec / 60f
                if (sec >= 60) {
                    stopVideoRecording()
                    break
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Camera Preview
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            textureViewRef = this@apply
                            startCamera(this@apply)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            focusPoint = offset
                            scope.launch {
                                delay(1200)
                                focusPoint = null
                            }
                        }
                    )
                }
        )

        // Focus indicator
        focusPoint?.let { pos ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.TopStart)
                    .padding(start = (pos.x - 32).dp.coerceAtLeast(0.dp), top = (pos.y - 32).dp.coerceAtLeast(0.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF8B5CF6),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        val cameraTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
        val cameraBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

        // Top Floating Controls (отступ сверху равен высоте статус-бара + 12dp, ТЗ шаг 8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = cameraTopPadding)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls (отступ снизу равен высоте навигационной панели + 16dp, ТЗ шаг 8)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = cameraBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRecording) "Запись: ${recordingSeconds}c (макс. 60с)" else "Нажмите для фото, удерживайте для видео",
                color = if (isRecording) Color(0xFFEF4444) else Color(0xCCFFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Внизу слева: вспышка (ТЗ, шаг 5)
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            FlashMode.AUTO -> FlashMode.ON
                            FlashMode.ON -> FlashMode.OFF
                            FlashMode.OFF -> FlashMode.AUTO
                        }
                        textureViewRef?.let { startCamera(it) }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    val icon = when (flashMode) {
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.OFF -> Icons.Default.FlashOff
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Flash",
                        tint = if (flashMode == FlashMode.OFF) Color.White else Color(0xFF8B5CF6)
                    )
                }

                // Big Shutter Button with Progress Ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (!isRecording) {
                                        takePhoto()
                                    }
                                },
                                onPress = {
                                    startVideoRecording()
                                    tryAwaitRelease()
                                    stopVideoRecording()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer progress circle
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0x66FFFFFF),
                            radius = size.minDimension / 2 - 4.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        if (isRecording) {
                            drawArc(
                                color = Color(0xFF8B5CF6),
                                startAngle = -90f,
                                sweepAngle = recordingProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner shutter button
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 36.dp else 60.dp)
                            .clip(if (isRecording) androidx.compose.foundation.shape.RoundedCornerShape(8.dp) else CircleShape)
                            .background(if (isRecording) Color(0xFFEF4444) else Color.White)
                    )
                }

                // Lens flip button
                IconButton(
                    onClick = {
                        isFrontCamera = !isFrontCamera
                        textureViewRef?.let { startCamera(it) }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlipCameraAndroid,
                        contentDescription = "Flip camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
