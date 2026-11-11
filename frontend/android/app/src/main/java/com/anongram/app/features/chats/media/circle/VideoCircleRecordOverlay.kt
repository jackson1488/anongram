package com.anongram.app.features.chats.media.circle

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Оверлей записи круглого видео-сообщения (кружочка):
 * - Живой видоискатель Camera2 в круге (диаметр 280.dp)
 * - Круговой индикатор таймера записи по контуру (до 60 секунд)
 * - Кнопки смены камеры (фронтальная/основная) и фонарик
 * - Замочек блокировки сверху
 * - Нижняя плашка с таймером и отменой
 */
@Composable
fun VideoCircleRecordOverlay(
    recordingState: com.anongram.app.features.chats.messages.state.RecordingState? = null,
    isLocked: Boolean = recordingState?.isLocked ?: false,
    onCancel: () -> Unit = {},
    onSend: (() -> Unit)? = null,
    onStopAndSend: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    onLock: () -> Unit = {},
    onSendVideo: ((uri: String, durationLabel: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sendAction = onSend ?: onStopAndSend
    val lockAction = onToggleLock ?: onLock
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current

    val cameraManager = remember { VideoCircleCameraManager(context) }

    var secondsElapsed by remember { mutableIntStateOf(recordingState?.seconds ?: 0) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    fun finishAndSend() {
        val file = cameraManager.stopRecording()
        val timeLabel = "%02d:%02d".format(secondsElapsed / 60, secondsElapsed % 60)
        val tenths = recordingState?.elapsedTenths ?: 0
        if (file != null && file.exists() && file.length() > 0 && (secondsElapsed >= 1 || tenths >= 5) && onSendVideo != null) {
            onSendVideo(file.absolutePath, timeLabel)
        } else {
            file?.delete()
            onCancel()
        }
    }

    fun cancelAndCleanup() {
        cameraManager.cancelRecording()
        onCancel()
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
    val timeLabel = "%02d:%02d".format(secondsElapsed / 60, secondsElapsed % 60)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        // Центральный круглый видоискатель камеры (диаметр 280.dp)
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Живое превью с камеры телефона через Camera2 и TextureView
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    cameraManager.startPreview(st, front = isFrontCamera)
                                    cameraManager.startRecording()
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Круговой индикатор времени записи
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.minDimension - 14.dp.toPx()) / 2f
                val strokeWidth = 4.dp.toPx()
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

        // В заблокированном режиме: панель управления камерой, таймер, отмена и кнопка отправки
        if (isLocked) {
            // Кнопки управления слева (смена камеры, фонарик)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 90.dp)
            ) {
                IconButton(
                    onClick = {
                        isFrontCamera = !isFrontCamera
                        cameraManager.switchCamera()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cameraswitch,
                        contentDescription = "Сменить камеру",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraManager.toggleFlash()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = "Вспышка",
                        tint = if (isFlashOn) FlameOrange else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Нижняя плашка с таймером и кнопкой отмены / отправки
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(IslandBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "ОТМЕНА",
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { cancelAndCleanup() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // Кнопка остановки и отправки
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { finishAndSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = "Отправить кружочек",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
