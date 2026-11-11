package com.anongram.app.features.chats.ui.input.video

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.features.chats.media.circle.VideoCircleCameraManager
import com.anongram.app.features.chats.messages.state.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Оверлей записи круглого видео-сообщения:
 * - Поверх размытого фона чата (или белой программной вспышки при селфи)
 * - При закрытой клавиатуре: крупный круг по центру; при открытой клавиатуре: чуть меньше и ниже шапки
 * - Слева над полем ввода (приподнято): капсула смены камеры [ 🔄 ] и вспышки [ ⚡ ]
 * - Справа при закрепе (приподнято): кнопка отмены [ ✕ ] и кнопка стоп/пауза [ ⏸ ]
 */
@Composable
fun VideoCircleRecordOverlay(
    recordingState: RecordingState? = null,
    isLocked: Boolean = recordingState?.isLocked ?: false,
    isKeyboardVisible: Boolean = false,
    bottomInputTopDp: Dp = 76.dp,
    onCancel: () -> Unit = {},
    onSend: (() -> Unit)? = null,
    onStopAndSend: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    onLock: () -> Unit = {},
    onPauseReview: (() -> Unit)? = null,
    onSendVideo: ((uri: String, durationLabel: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sendAction = onSend ?: onStopAndSend
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

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

    val isSoftwareWhiteFlash = isFrontCamera && isFlashOn

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    fun finishAndSend() {
        if (hasFinished) return
        hasFinished = true
        val file = cameraManager.stopRecording()
        val effectiveSec = maxOf(secondsElapsed, (recordingState?.elapsedTenths ?: 0) / 10, 1)
        val timeLabel = "%02d:%02d".format(effectiveSec / 60, effectiveSec % 60)
        if (onSendVideo != null) {
            onSendVideo(file?.absolutePath ?: "", timeLabel)
        } else {
            sendAction()
        }
    }

    LaunchedEffect(recordingState?.finishRequested) {
        if (recordingState?.finishRequested == true) {
            finishAndSend()
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            secondsElapsed++
            if (secondsElapsed >= 60) {
                finishAndSend()
                break
            }
        }
    }

    val progress = (secondsElapsed.toFloat() / 60f).coerceIn(0f, 1f)
    val controlsBottomPadding = bottomInputTopDp + 26.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSoftwareWhiteFlash) Color.White else Color.Black.copy(alpha = 0.36f))
    ) {
        // Область позиционирования кружка (между шапкой сверху и кнопками управления над клавиатурой/полем ввода)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (isKeyboardVisible) 80.dp else 56.dp,
                    bottom = controlsBottomPadding + 54.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            // Мягкий ореол вокруг видоискателя в обычном режиме
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
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                fun applyCenterCropTransform(viewWidth: Int, viewHeight: Int) {
                                    if (viewWidth <= 0 || viewHeight <= 0) return
                                    val videoW = cameraManager.videoSize.height.toFloat().coerceAtLeast(1f)
                                    val videoH = cameraManager.videoSize.width.toFloat().coerceAtLeast(1f)
                                    val viewW = viewWidth.toFloat()
                                    val viewH = viewHeight.toFloat()

                                    val scaleX: Float
                                    val scaleY: Float
                                    if (videoW / videoH > viewW / viewH) {
                                        scaleX = (videoW / videoH) / (viewW / viewH)
                                        scaleY = 1f
                                    } else {
                                        scaleX = 1f
                                        scaleY = (viewW / viewH) / (videoW / videoH)
                                    }
                                    val matrix = Matrix()
                                    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
                                    setTransform(matrix)
                                }

                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                        cameraManager.startPreview(st, front = isFrontCamera, autoStartRecording = true)
                                        applyCenterCropTransform(w, h)
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                        applyCenterCropTransform(w, h)
                                    }

                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Белый круговой индикатор времени записи
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

        // Панель управления над полем ввода (поднята выше над ореолом и клавиатурой)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = controlsBottomPadding)
        ) {
            // Слева: капсула переключения камеры и вспышки ( 🔄  ⚡ )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, CardBorder.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isFrontCamera = !isFrontCamera
                        cameraManager.switchCamera()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cameraswitch,
                        contentDescription = "Сменить камеру",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraManager.toggleFlash()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = "Вспышка",
                        tint = if (isFlashOn) FlameOrange else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Справа при закрепе: кнопка отмены [ ✕ ] и кнопка стоп/пауза [ ⏸ ]
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
                                finishAndSend()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Стоп",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
