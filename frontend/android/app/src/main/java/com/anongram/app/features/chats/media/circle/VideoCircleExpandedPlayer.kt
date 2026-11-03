package com.anongram.app.features.chats.media.circle

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
import com.anongram.app.core.theme.FlameOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Развёрнутый плеер видео-кружочка с круговым SeekBar-ом (Фото 1 и 2):
 * - Крупный круг по центру экрана (диаметр ~300.dp)
 * - По внешнему ободу белая дуга прогресса воспроизведения
 * - По тапу во время воспроизведения: ставится на паузу и появляется белая точка-ручка (●)
 * - Белую точку можно пальцем водить по кругу для перемотки видеосообщения в любое место!
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

    // Автоматический прогресс воспроизведения
    LaunchedEffect(isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            delay(100)
            currentPosMs += 100
            if (currentPosMs >= totalMs) {
                currentPosMs = totalMs
                isPlaying = false
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
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Тело кругового видео
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B221B)),
                contentAlignment = Alignment.Center
            ) {
                // Иконка / превью камеры
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = FlameOrange.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )

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
                        text = "$timeLabel ✔✔",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Круговой SeekBar (обод с прогрессом и перетаскиваемой белой точкой)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalMs) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val touchAngle = atan2(offset.y - centerY, offset.x - centerX)
                                // Преобразуем угол: 0 на 12 часах (-PI/2)
                                var norm = touchAngle + (PI / 2.0).toFloat()
                                if (norm < 0f) norm += (2f * PI.toFloat())
                                val fraction = (norm / (2f * PI.toFloat())).coerceIn(0f, 1f)
                                currentPosMs = (fraction * totalMs).toLong()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val touchAngle = atan2(change.position.y - centerY, change.position.x - centerX)
                                var norm = touchAngle + (PI / 2.0).toFloat()
                                if (norm < 0f) norm += (2f * PI.toFloat())
                                val fraction = (norm / (2f * PI.toFloat())).coerceIn(0f, 1f)
                                currentPosMs = (fraction * totalMs).toLong()
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            ) {
                val radius = (size.minDimension - 24.dp.toPx()) / 2f
                val strokeWidth = 3.5.dp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Фоновое кольцо трека (полупрозрачное)
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Дуга воспроизведения (активная белая дуга)
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Белая круглая точка-манипулятор (●) на конце дуги (видна при паузе или перетаскивании)
                if (!isPlaying || isDragging) {
                    val angleRad = (-90f + animatedProgress * 360f) * (PI / 180.0)
                    val thumbX = centerOffset.x + radius * cos(angleRad).toFloat()
                    val thumbY = centerOffset.y + radius * sin(angleRad).toFloat()

                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }
            }
        }
    }
}
