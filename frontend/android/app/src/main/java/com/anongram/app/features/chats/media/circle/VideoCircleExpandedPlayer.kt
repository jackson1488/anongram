package com.anongram.app.features.chats.media.circle

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.FlameOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Развёрнутый плеер видео-кружочка с круговым SeekBar-ом:
 * - Крупный круг по центру экрана (диаметр ~300.dp)
 * - Воспроизведение реального видео через MediaPlayer в TextureView
 * - По внешнему ободу белая дуга прогресса воспроизведения
 * - По тапу во время воспроизведения: ставится на паузу и появляется белая точка-ручка (●)
 * - Белую точку можно пальцем водить по кругу для перемотки видеосообщения в любое место
 */
@Composable
fun VideoCircleExpandedPlayer(
    attachment: com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle? = null,
    durationSeconds: Int = 14,
    timeLabel: String = "19:07",
    onDismiss: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val closeHandler = onClose ?: onDismiss
    val durSec = remember(attachment, durationSeconds) {
        if (attachment != null) {
            val parts = attachment.durationLabel.split(":")
            if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 14)
            } else 14
        } else {
            durationSeconds
        }
    }
    val totalMs = (durSec * 1000L).coerceAtLeast(1000L)
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val videoFile = remember(attachment?.uri) {
        attachment?.uri?.let { File(it) }?.takeIf { it.exists() }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.run {
                runCatching { stop() }
                release()
            }
            mediaPlayer = null
        }
    }

    // Синхронизация прогресса с реальным MediaPlayer или таймером
    LaunchedEffect(isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            delay(50)
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                currentPosMs = mp.currentPosition.toLong()
            } else if (mp == null) {
                currentPosMs += 50
                if (currentPosMs >= totalMs) {
                    currentPosMs = totalMs
                    isPlaying = false
                }
            }
        }
    }

    val progress = (currentPosMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "circleProgress")

    val elapsedSec = (currentPosMs / 1000).toInt()
    val elapsedLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)

    val circleSize = 310.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = closeHandler
            ),
        contentAlignment = Alignment.Center
    ) {
        // Контейнер кругового видео и кольца SeekBar
        Box(
            modifier = Modifier
                .size(circleSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isPlaying = !isPlaying
                        val mp = mediaPlayer
                        if (mp != null) {
                            if (isPlaying) mp.start() else mp.pause()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Тело кругового видео
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (videoFile != null) {
                    // Реальное воспроизведение видеофайла через TextureView
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                        try {
                                            val mp = MediaPlayer().apply {
                                                setSurface(Surface(st))
                                                setDataSource(videoFile.absolutePath)
                                                isLooping = true
                                                prepare()
                                                if (isPlaying) start()
                                            }
                                            mediaPlayer = mp
                                        } catch (_: Exception) {}
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        mediaPlayer?.run {
                                            runCatching { stop() }
                                            release()
                                        }
                                        mediaPlayer = null
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback иконка если файл не задан
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = FlameOrange.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Иконка паузы / воспроизведения по центру при паузе
                if (!isPlaying) {
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

                // Внизу слева счётчик времени воспроизведения
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 28.dp, bottom = 18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$elapsedLabel ${if (isPlaying) "🔊" else "⏸"}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Внизу справа время сообщения
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 28.dp, bottom = 18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = timeLabel,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            // Круговой SeekBar (дуга прогресса + точка ручки)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.minDimension - 14.dp.toPx()) / 2f
                val strokeWidth = 4.dp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Фоновое кольцо (полупрозрачное)
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Белая дуга прогресса
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Белая точка-ручка (●) на ободе при паузе или перемотке
            if (!isPlaying || isDragging) {
                val angleDeg = -90f + animatedProgress * 360f
                val angleRad = angleDeg * (PI.toFloat() / 180f)
                val radiusPx = (circleSize.value * 2.8f)

                val handleSize = 16.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset {
                            val r = 148.dp.toPx()
                            val x = (r * cos(angleRad)).roundToInt()
                            val y = (r * sin(angleRad)).roundToInt()
                            IntOffset(x, y)
                        }
                        .size(handleSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .pointerInput(totalMs) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    mediaPlayer?.seekTo(currentPosMs.toInt())
                                },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val touchPos = change.position
                                    val angle = atan2(touchPos.y, touchPos.x)
                                    var deg = (angle * 180f / PI.toFloat()) + 90f
                                    if (deg < 0) deg += 360f
                                    val fraction = (deg / 360f).coerceIn(0f, 1f)
                                    currentPosMs = (fraction * totalMs).toLong()
                                }
                            )
                        }
                )
            }
        }
    }
}
