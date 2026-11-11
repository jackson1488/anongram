package com.anongram.app.features.chats.ui.input.video

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.features.chats.media.circle.VideoCircleCameraManager
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.RecordingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Оверлей записи и предпросмотра круглого видео-сообщения:
 * - Поглощает все нажатия по размытому фону чата (меню и выделение не прокликиваются)
 * - Параллельное открытие камеры (preOpenCamera) для мгновенного старта
 * - При нажатии Стоп/Пауза [ ⏸ ] переходит в режим предпросмотра и обрезки (reviewVideo != null)
 */
@Composable
fun VideoCircleRecordOverlay(
    recordingState: RecordingState? = null,
    isLocked: Boolean = recordingState?.isLocked ?: false,
    isKeyboardVisible: Boolean = false,
    bottomInputTopDp: Dp = 76.dp,
    reviewVideo: MessageAttachment.VideoCircle? = null,
    isReviewPlaying: Boolean = false,
    reviewProgress: Float = 0f,
    reviewTrimStart: Float = 0f,
    reviewTrimEnd: Float = 1f,
    reviewSeekTrigger: Pair<Int, Float>? = null,
    onToggleReviewPlay: () -> Unit = {},
    onReviewDurationReady: (Long) -> Unit = {},
    onReviewProgressChange: (Float) -> Unit = {},
    onReviewPlayingChange: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {},
    onSend: (() -> Unit)? = null,
    onStopAndSend: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    onLock: () -> Unit = {},
    onPauseReview: (() -> Unit)? = null,
    onPauseVideoReview: ((uri: String, durationLabel: String) -> Unit)? = null,
    onSendVideo: ((uri: String, durationLabel: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sendAction = onSend ?: onStopAndSend
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isReviewMode = reviewVideo != null

    // При открытой клавиатуре круг чуть меньше (~62% ширины экрана), при закрытой — почти во всю ширину
    val targetCircleSize = if (isKeyboardVisible) {
        (configuration.screenWidthDp.dp * 0.62f).coerceIn(200.dp, 260.dp)
    } else {
        (configuration.screenWidthDp.dp - 4.dp).coerceIn(310.dp, 440.dp)
    }
    val outerCircleSize by animateDpAsState(targetValue = targetCircleSize, label = "recordCircleSize")
    val innerCircleSize = outerCircleSize - 10.dp

    val cameraManager = remember { VideoCircleCameraManager(context) }

    var secondsElapsed by remember { mutableIntStateOf(recordingState?.seconds ?: 0) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }
    var camVideoW by remember { mutableIntStateOf(cameraManager.videoSize.width) }
    var camVideoH by remember { mutableIntStateOf(cameraManager.videoSize.height) }
    var liveTextureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var focusTapState by remember { mutableStateOf<Triple<Float, Float, Long>?>(null) }
    val focusRingProgress = remember { Animatable(1f) }

    LaunchedEffect(focusTapState?.third) {
        if (focusTapState != null) {
            focusRingProgress.snapTo(0f)
            focusRingProgress.animateTo(1f, animationSpec = tween(durationMillis = 850))
            focusTapState = null
        }
    }

    val isSoftwareWhiteFlash = !isReviewMode && isFrontCamera && isFlashOn

    LaunchedEffect(cameraManager) {
        cameraManager.onVideoSizeReady = { w, h ->
            camVideoW = w
            camVideoH = h
        }
    }

    // Параллельно начинаем открывать камеру сразу, не дожидаясь создания TextureView
    LaunchedEffect(isReviewMode) {
        if (!isReviewMode && !hasFinished) {
            cameraManager.preOpenCamera(front = isFrontCamera, autoStartRecording = true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    fun finishAndSend() {
        if (hasFinished) return
        hasFinished = true
        val effectiveSec = maxOf(secondsElapsed, (recordingState?.elapsedTenths ?: 0) / 10, 1)
        val timeLabel = "%02d:%02d".format(effectiveSec / 60, effectiveSec % 60)
        cameraManager.stopRecordingAsync { file ->
            if (onSendVideo != null) {
                onSendVideo(file?.absolutePath ?: "", timeLabel)
            } else {
                sendAction()
            }
        }
    }

    fun stopToReview() {
        if (hasFinished) return
        hasFinished = true
        val effectiveSec = maxOf(secondsElapsed, (recordingState?.elapsedTenths ?: 0) / 10, 1)
        val timeLabel = "%02d:%02d".format(effectiveSec / 60, effectiveSec % 60)
        cameraManager.pauseRecordingForReviewAsync { file ->
            if (onPauseVideoReview != null) {
                onPauseVideoReview(file?.absolutePath ?: "", timeLabel)
            } else if (onSendVideo != null) {
                onSendVideo(file?.absolutePath ?: "", timeLabel)
            } else {
                sendAction()
            }
        }
    }

    LaunchedEffect(recordingState?.finishRequested, isReviewMode) {
        if (!isReviewMode && recordingState?.finishRequested == true) {
            finishAndSend()
        }
    }

    LaunchedEffect(isReviewMode) {
        if (isReviewMode) return@LaunchedEffect
        while (isActive) {
            delay(1000)
            secondsElapsed++
            if (secondsElapsed >= 60) {
                finishAndSend()
                break
            }
        }
    }

    // Управление плеером в режиме предпросмотра/обрезки
    var reviewPlayer by remember(reviewVideo?.uri) { mutableStateOf<MediaPlayer?>(null) }
    var reviewSurfaceRef by remember(reviewVideo?.uri) { mutableStateOf<Surface?>(null) }
    var hasReviewFrame by remember(reviewVideo?.uri) { mutableStateOf(false) }

    DisposableEffect(reviewVideo?.uri) {
        onDispose {
            val mp = reviewPlayer
            reviewPlayer = null
            val s = reviewSurfaceRef
            reviewSurfaceRef = null
            if (mp != null) {
                Thread {
                    runCatching { mp.stop() }
                    runCatching { mp.release() }
                    runCatching { s?.release() }
                }.start()
            } else {
                runCatching { s?.release() }
            }
        }
    }

    LaunchedEffect(isReviewPlaying, reviewPlayer) {
        val mp = reviewPlayer ?: return@LaunchedEffect
        try {
            if (isReviewPlaying) {
                val dur = mp.duration.coerceAtLeast(1)
                val curFrac = mp.currentPosition.toFloat() / dur.toFloat()
                if (curFrac < reviewTrimStart || curFrac >= reviewTrimEnd - 0.01f) {
                    mp.seekTo((reviewTrimStart * dur).toInt())
                    onReviewProgressChange(reviewTrimStart)
                }
                if (!mp.isPlaying) mp.start()
            } else {
                if (mp.isPlaying) mp.pause()
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(reviewSeekTrigger, reviewPlayer) {
        val mp = reviewPlayer ?: return@LaunchedEffect
        val req = reviewSeekTrigger ?: return@LaunchedEffect
        val dur = mp.duration.coerceAtLeast(1)
        val clamped = req.second.coerceIn(reviewTrimStart, reviewTrimEnd)
        runCatching { mp.seekTo((clamped * dur).toInt()) }
        onReviewProgressChange(clamped)
    }

    LaunchedEffect(isReviewMode, isReviewPlaying, reviewTrimStart, reviewTrimEnd, reviewPlayer) {
        if (!isReviewMode || !isReviewPlaying) return@LaunchedEffect
        val mp = reviewPlayer ?: return@LaunchedEffect
        while (isActive && isReviewPlaying) {
            delay(50)
            try {
                if (mp.isPlaying) {
                    val dur = mp.duration.coerceAtLeast(1)
                    val prog = (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    onReviewProgressChange(prog)
                    if (prog >= reviewTrimEnd) {
                        mp.pause()
                        mp.seekTo((reviewTrimStart * dur).toInt())
                        onReviewProgressChange(reviewTrimStart)
                        onReviewPlayingChange(false)
                    }
                }
            } catch (_: Exception) {
                break
            }
        }
    }

    val progress = if (isReviewMode) {
        reviewProgress.coerceIn(0f, 1f)
    } else {
        (secondsElapsed.toFloat() / 60f).coerceIn(0f, 1f)
    }
    val controlsBottomPadding = bottomInputTopDp + 26.dp

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Фоновая подложка перехватывает касания к чату позади, не мешая кнопкам управления поверх неё
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isSoftwareWhiteFlash) Color.White else Color.Black.copy(alpha = 0.36f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )
        // Область позиционирования кружка (между шапкой сверху и кнопками управления над клавиатурой/полем ввода)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (isKeyboardVisible) 80.dp else 56.dp,
                    bottom = controlsBottomPadding + (if (isReviewMode) 12.dp else 54.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isSoftwareWhiteFlash) {
                Box(
                    modifier = Modifier
                        .size(outerCircleSize + 22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(outerCircleSize + 10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }

            Box(
                modifier = Modifier.size(outerCircleSize),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerCircleSize)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .then(
                            if (isReviewMode) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleReviewPlay
                                )
                            } else {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val nx = (offset.x / w).coerceIn(0f, 1f)
                                        val ny = (offset.y / h).coerceIn(0f, 1f)
                                        focusTapState = Triple(nx, ny, System.nanoTime())
                                        cameraManager.focusAtNormalizedPoint(nx, ny)
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isReviewMode) {
                        val reviewFile = remember(reviewVideo?.uri) {
                            reviewVideo?.uri?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
                        }
                        val reviewThumb by produceState<Bitmap?>(initialValue = null, key1 = reviewFile?.absolutePath) {
                            val file = reviewFile ?: return@produceState
                            value = withContext(Dispatchers.IO) {
                                val mmr = MediaMetadataRetriever()
                                try {
                                    mmr.setDataSource(file.absolutePath)
                                    mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                } catch (_: Exception) {
                                    null
                                } finally {
                                    runCatching { mmr.release() }
                                }
                            }
                        }
                        if (reviewThumb != null) {
                            Image(
                                bitmap = reviewThumb!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (reviewFile != null) {
                            AndroidView(
                                factory = { ctx ->
                                    TextureView(ctx).apply {
                                        isOpaque = false
                                        clipToOutline = true
                                        outlineProvider = object : android.view.ViewOutlineProvider() {
                                            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                                outline.setOval(0, 0, view.width, view.height)
                                            }
                                        }
                                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                                invalidateOutline()
                                                try {
                                                    reviewSurfaceRef?.let { runCatching { it.release() } }
                                                    val surf = Surface(st)
                                                    reviewSurfaceRef = surf
                                                    val mp = MediaPlayer().apply {
                                                        setSurface(surf)
                                                        setDataSource(reviewFile.absolutePath)
                                                        setVolume(1f, 1f)
                                                        setOnInfoListener { _, what, _ ->
                                                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                                                hasReviewFrame = true
                                                            }
                                                            false
                                                        }
                                                        setOnVideoSizeChangedListener { _, vw, vh ->
                                                            if (vw > 0 && vh > 0 && width > 0 && height > 0) {
                                                                val viewW = width.toFloat()
                                                                val viewH = height.toFloat()
                                                                val videoAspect = vw.toFloat() / vh.toFloat()
                                                                val viewAspect = viewW / viewH
                                                                val sx = if (videoAspect > viewAspect) videoAspect / viewAspect else 1f
                                                                val sy = if (videoAspect > viewAspect) 1f else viewAspect / videoAspect
                                                                setTransform(Matrix().apply { setScale(sx, sy, viewW / 2f, viewH / 2f) })
                                                            }
                                                        }
                                                        setOnPreparedListener { prepared ->
                                                            onReviewDurationReady(prepared.duration.toLong().coerceAtLeast(1000L))
                                                            runCatching {
                                                                prepared.seekTo(1)
                                                            }
                                                        }
                                                        prepareAsync()
                                                    }
                                                    reviewPlayer = mp
                                                } catch (_: Exception) {}
                                            }

                                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                                invalidateOutline()
                                            }
                                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                                hasReviewFrame = false
                                                val mp = reviewPlayer
                                                reviewPlayer = null
                                                val surf = reviewSurfaceRef
                                                reviewSurfaceRef = null
                                                if (mp != null) {
                                                    Thread {
                                                        runCatching { mp.stop() }
                                                        runCatching { mp.release() }
                                                        runCatching { surf?.release() }
                                                    }.start()
                                                } else {
                                                    runCatching { surf?.release() }
                                                }
                                                return true
                                            }

                                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                                                if (!hasReviewFrame) hasReviewFrame = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .alpha(if (hasReviewFrame) 1f else 0f)
                            )
                        }

                        if (!isReviewPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Воспроизвести",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    clipToOutline = true
                                    outlineProvider = object : android.view.ViewOutlineProvider() {
                                        override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                            outline.setOval(0, 0, view.width, view.height)
                                        }
                                    }
                                    liveTextureViewRef = this
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                            invalidateOutline()
                                            cameraManager.startPreview(
                                                surfaceTexture = st,
                                                width = w,
                                                height = h,
                                                front = isFrontCamera,
                                                autoStartRecording = true
                                            )
                                        }

                                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                            invalidateOutline()
                                            cameraManager.updatePreviewSize(w, h)
                                        }

                                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                            if (liveTextureViewRef === this@apply) liveTextureViewRef = null
                                            cameraManager.detachPreviewSurface(st)
                                            return true
                                        }
                                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        focusTapState?.let { (nx, ny, _) ->
                            val t = focusRingProgress.value.coerceIn(0f, 1f)
                            val alpha = (1f - t).coerceIn(0f, 1f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = nx * size.width
                                val cy = ny * size.height
                                val radius = (34.dp - (12.dp * t)).toPx()
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.92f),
                                    radius = radius,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Белый круговой индикатор времени записи / воспроизведения
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (size.minDimension - 8.dp.toPx()) / 2f
                    val strokeWidth = 3.5.dp.toPx()
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Панель управления камерой и паузы/отмены показывается только во время живой записи
        if (!isReviewMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = controlsBottomPadding)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, CardBorder.copy(alpha = 0.55f), CircleShape)
                            .clickable {
                                isFrontCamera = !isFrontCamera
                                isFlashOn = false
                                cameraManager.switchCamera()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = "Сменить камеру",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, CardBorder.copy(alpha = 0.55f), CircleShape)
                            .clickable {
                                isFlashOn = !isFlashOn
                                cameraManager.toggleFlash()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = "Вспышка",
                            tint = if (isFlashOn) FlameOrange else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Справа при закрепе: кнопка отмены [ ✕ ] и кнопка стоп/пауза [ ⏸ ] (переход к обрезке и просмотру)
                AnimatedVisibility(
                    visible = isLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(1.dp, CardBorder.copy(alpha = 0.55f), CircleShape)
                                .clickable {
                                    hasFinished = true
                                    cameraManager.cancelRecording()
                                    onCancel()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Отмена",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(1.dp, CardBorder.copy(alpha = 0.55f), CircleShape)
                                .clickable {
                                    stopToReview()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Пауза и обрезка",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
