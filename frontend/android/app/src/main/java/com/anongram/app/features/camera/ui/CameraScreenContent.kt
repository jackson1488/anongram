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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.camera.api.CapturedMedia
import com.anongram.app.features.chats.attach.components.PermissionDeniedView
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class FlashMode { OFF, ON, AUTO }

@SuppressLint("MissingPermission")
@Composable
fun CameraScreenContent(
    onResult: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

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
            message = if (isRu) {
                "Для работы камеры требуется разрешение на съёмку фото и видео."
            } else {
                "Camera permission is required to capture photos and videos."
            },
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
    var isRecordingLocked by remember { mutableStateOf(false) }
    var dragUpProgress by remember { mutableFloatStateOf(0f) }
    var recordingProgress by remember { mutableFloatStateOf(0f) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isCapturingPhoto by remember { mutableStateOf(false) }

    var sensorOrientation by remember { mutableIntStateOf(90) }

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
            runCatching { mediaRecorderRef?.stop() }
            runCatching { mediaRecorderRef?.release() }
            runCatching { captureSessionRef?.close() }
            runCatching { cameraDeviceRef?.close() }
            runCatching { imageReaderRef?.close() }
            backgroundThread.quitSafely()
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

            val chars = cameraManager.getCameraCharacteristics(cameraId)
            sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

            captureSessionRef?.close()
            captureSessionRef = null
            cameraDeviceRef?.close()
            cameraDeviceRef = null
            imageReaderRef?.close()

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
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
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

    LaunchedEffect(isFrontCamera) {
        textureViewRef?.let { tv ->
            if (tv.isAvailable) startCamera(tv)
        }
    }

    fun takePhoto() {
        if (isCapturingPhoto || isRecording) return
        val camera = cameraDeviceRef
        val reader = imageReaderRef
        val session = captureSessionRef
        val tv = textureViewRef

        // Fast fallback via TextureView bitmap if session isn't ready yet, or hardware JPEG capture
        if (camera == null || reader == null || session == null) {
            val fallbackBmp = tv?.bitmap
            if (fallbackBmp != null) {
                isCapturingPhoto = true
                val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { fos ->
                    fallbackBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, fos)
                }
                isCapturingPhoto = false
                onResult(
                    CapturedMedia(
                        uri = Uri.fromFile(cacheFile),
                        isVideo = false,
                        width = fallbackBmp.width,
                        height = fallbackBmp.height
                    )
                )
            }
            return
        }

        isCapturingPhoto = true
        try {
            reader.setOnImageAvailableListener({ ir ->
                val image = ir.acquireLatestImage() ?: run {
                    isCapturingPhoto = false
                    return@setOnImageAvailableListener
                }
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                val cacheFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { fos ->
                    fos.write(bytes)
                }

                scope.launch {
                    isCapturingPhoto = false
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
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                val jpegRotation = if (isFrontCamera) (360 - sensorOrientation) % 360 else sensorOrientation
                set(CaptureRequest.JPEG_ORIENTATION, jpegRotation)
                when (flashMode) {
                    FlashMode.ON -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                    FlashMode.AUTO -> set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    FlashMode.OFF -> set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }
            session.capture(captureBuilder.build(), null, backgroundHandler)
        } catch (_: Exception) {
            isCapturingPhoto = false
        }
    }

    fun stopVideoRecording() {
        if (!isRecording) return
        isRecording = false
        isRecordingLocked = false
        dragUpProgress = 0f
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val rec = mediaRecorderRef
        mediaRecorderRef = null
        try {
            rec?.stop()
        } catch (_: Exception) {}
        try {
            rec?.reset()
            rec?.release()
        } catch (_: Exception) {}

        val file = currentVideoFile
        if (file != null && file.exists() && file.length() > 0L) {
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
        } else {
            textureViewRef?.let { if (it.isAvailable) startCamera(it) }
        }
    }

    fun startVideoRecording() {
        if (isRecording) return
        val camera = cameraDeviceRef ?: return
        val tv = textureViewRef ?: return
        val st = tv.surfaceTexture ?: return

        isRecording = true
        isRecordingLocked = false
        dragUpProgress = 0f
        recordingSeconds = 0
        recordingProgress = 0f

        val videoFile = File(context.cacheDir, "camera_video_${System.currentTimeMillis()}.mp4")
        currentVideoFile = videoFile

        try {
            captureSessionRef?.close()
            captureSessionRef = null

            @Suppress("DEPRECATION")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            recorder.apply {
                if (hasAudioPermission) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(videoFile.absolutePath)
                setVideoEncodingBitRate(6_000_000)
                setVideoFrameRate(30)
                setVideoSize(1280, 720)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (hasAudioPermission) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                setOrientationHint(sensorOrientation)
                prepare()
            }
            mediaRecorderRef = recorder

            st.setDefaultBufferSize(1280, 720)
            val previewSurface = Surface(st)
            val recorderSurface = recorder.surface

            val recordBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }

            camera.createCaptureSession(
                listOf(previewSurface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSessionRef = session
                        try {
                            session.setRepeatingRequest(recordBuilder.build(), null, backgroundHandler)
                            recorder.start()
                        } catch (_: Exception) {
                            scope.launch { stopVideoRecording() }
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        scope.launch { stopVideoRecording() }
                    }
                },
                backgroundHandler
            )

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
        } catch (_: Exception) {
            isRecording = false
            isRecordingLocked = false
            runCatching { mediaRecorderRef?.release() }
            mediaRecorderRef = null
            startCamera(tv)
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
                        color = scheme.accent,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        val cameraTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
        val cameraBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp

        // Top Floating Controls
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

            if (isRecording) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCCEF4444))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRecordingLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = cameraBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swipe-up-to-lock track indicator when holding video recording
            AnimatedVisibility(
                visible = isRecording && !isRecordingLocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val lockSlideOffsetPx = with(density) { (-dragUpProgress * 44.dp.toPx()).roundToInt() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .offset { IntOffset(0, lockSlideOffsetPx) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (dragUpProgress > 0.85f) scheme.accent else Color(0x99000000))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (dragUpProgress > 0.85f) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = when {
                    isRecording && isRecordingLocked -> if (isRu) "Запись зафиксирована • нажмите ■ для остановки" else "Recording locked • tap ■ to stop"
                    isRecording -> if (isRu) "Вверх — зафиксировать запись" else "Slide up to lock recording"
                    else -> if (isRu) "Нажмите для фото, удерживайте для видео" else "Tap for photo, hold for video"
                },
                color = if (isRecording) Color(0xFFEF4444) else Color(0xCCFFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash mode button
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
                        tint = if (flashMode == FlashMode.OFF) Color.White else scheme.accent
                    )
                }

                // Big Shutter Button:
                // - Quick tap (< 230ms) -> Photo
                // - Hold (>= 230ms) -> Video recording
                // - Drag up >= 64.dp while holding -> Lock hands-free recording
                // - Tap while locked -> Stop recording
                val lockThresholdPx = with(density) { 64.dp.toPx() }
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .pointerInput(isRecording, isRecordingLocked) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (isRecording && isRecordingLocked) {
                                    // Wait for finger release to stop locked recording
                                    do {
                                        val ev = awaitPointerEvent()
                                    } while (ev.changes.any { it.pressed })
                                    stopVideoRecording()
                                    return@awaitEachGesture
                                }

                                val downTime = System.currentTimeMillis()
                                val startY = down.position.y
                                var holdJobStarted = false
                                val holdTriggerJob = scope.launch {
                                    delay(230L)
                                    holdJobStarted = true
                                    startVideoRecording()
                                }

                                var lockedDuringHold = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activeChange = event.changes.firstOrNull { it.pressed } ?: break
                                    if (holdJobStarted && !lockedDuringHold) {
                                        val dyUp = (startY - activeChange.position.y).coerceAtLeast(0f)
                                        dragUpProgress = (dyUp / lockThresholdPx).coerceIn(0f, 1f)
                                        if (dyUp >= lockThresholdPx) {
                                            lockedDuringHold = true
                                            isRecordingLocked = true
                                            dragUpProgress = 0f
                                        }
                                    }
                                }

                                holdTriggerJob.cancel()
                                val elapsed = System.currentTimeMillis() - downTime
                                if (!holdJobStarted && elapsed < 230L) {
                                    takePhoto()
                                } else if (holdJobStarted && !lockedDuringHold) {
                                    stopVideoRecording()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer progress circle
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0x66FFFFFF),
                            radius = size.minDimension / 2 - 4.dp.toPx(),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                        if (isRecording) {
                            drawArc(
                                color = Color(0xFFEF4444),
                                startAngle = -90f,
                                sweepAngle = recordingProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner shutter button (white circle for photo/idle, red rounded square while recording)
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 34.dp else 62.dp)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
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
