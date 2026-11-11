package com.anongram.app.features.chats.ui.input.video

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.media.circle.VideoCircleCameraManager
import com.anongram.app.features.chats.messages.state.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Оверлей записи круглого видео-сообщения (Фото 1 и Фото 3):
 * - Компактный круглый видоискатель Camera2 (210dp) с белым ободком таймера
 * - Слева внизу над строкой ввода: капсула смены камеры [ 🔄 ] и вспышки [ ⚡ ]
 * - Справа внизу над строкой ввода: круглая кнопка паузы [ ❙❙ ] и всплывающий тултип «Нажмите для обрезки или прослушивания»
 */
@Composable
fun VideoCircleRecordOverlay(
    recordingState: RecordingState? = null,
    isLocked: Boolean = recordingState?.isLocked ?: false,
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
    val lockAction = onToggleLock ?: onLock
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current

    val cameraManager = remember { VideoCircleCameraManager(context) }

    var secondsElapsed by remember { mutableIntStateOf(recordingState?.seconds ?: 0) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(true) }

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

    fun pauseForReview() {
        val file = cameraManager.stopRecording()
        val timeLabel = "%02d:%02d".format(secondsElapsed / 60, secondsElapsed % 60)
        if (file != null && file.exists() && file.length() > 0 && onPauseReview != null) {
            onPauseReview()
        } else {
            finishAndSend()
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        // Компактный круглый видоискатель камеры (210dp)
        Box(
            modifier = Modifier
                .offset(y = (-40).dp)
                .size(224.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
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

            // Белый круговой индикатор времени записи
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.minDimension - 10.dp.toPx()) / 2f
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

        // Панель управления над строкой ввода (Фото 1 и Фото 3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                .fillMaxSize()
        ) {
            // Слева: капсула переключения камеры и вспышки ( 🔄  ⚡ )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
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

            // Справа: кнопка паузы ( ❙❙ ) и всплывающий тултип «Нажмите для обрезки или прослушивания»
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Всплывающий тултип
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, CardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showTooltip = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Нажмите для обрезки или прослушивания",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Круглая кнопка Паузы
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, CardBorder.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            showTooltip = false
                            if (onPauseReview != null) {
                                pauseForReview()
                            } else {
                                finishAndSend()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Пауза",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
