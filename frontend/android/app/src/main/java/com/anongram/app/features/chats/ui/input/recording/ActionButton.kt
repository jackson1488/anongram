package com.anongram.app.features.chats.ui.input.recording

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Фирменная иконка видео-сообщения (кружочка) в стиле Telegram:
 * Закруглённый корпус камеры с центральным круглым объективом и индикатором видоискателя.
 */
@Composable
fun TelegramVideoNoteIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // Закруглённый прямоугольник (корпус камеры)
        drawRoundRect(
            color = tint,
            topLeft = Offset(stroke / 2f, stroke / 2f + h * 0.06f),
            size = Size(w - stroke, h * 0.88f - stroke),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = stroke)
        )
        // Центральный объектив камеры
        drawCircle(
            color = tint,
            radius = w * 0.22f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = stroke)
        )
        // Точка-индикатор видоискателя в правом верхнем углу
        drawCircle(
            color = tint,
            radius = 1.4.dp.toPx(),
            center = Offset(w * 0.76f, h * 0.28f)
        )
    }
}

/**
 * Круглая кнопка действия:
 * - С текстом: самолётик / галочка при редактировании
 * - Без текста: микрофон / видео-сообщение Telegram
 * - Быстрый тап (< 180 мс): переключение между микрофоном и камерой
 * - Удержание (hold): запуск реальной записи
 * - Свайп вверх (< -70dp): блокировка записи (Lock)
 * - Свайп влево (< -80dp): отмена записи (Cancel)
 * - Отпускание пальца: автоматическая отправка в чат (Release to Send)
 */
@Composable
fun ActionButton(
    canSend: Boolean,
    isEditing: Boolean,
    isRecording: Boolean,
    isLockedRecording: Boolean,
    isCameraMode: Boolean,
    onSend: () -> Unit,
    onToggleCameraMode: () -> Unit,
    onStartRecording: (RecordMode) -> Unit,
    onFinishRecording: (Boolean) -> Unit,
    onStopLockedRecording: () -> Unit,
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    // Обновляемые ссылки для предотвращения сброса корутины жестов
    val currentCanSend by rememberUpdatedState(canSend)
    val currentIsLocked by rememberUpdatedState(isLockedRecording)
    val currentIsCameraMode by rememberUpdatedState(isCameraMode)
    val currentOnSend by rememberUpdatedState(onSend)
    val currentOnToggleCameraMode by rememberUpdatedState(onToggleCameraMode)
    val currentOnStartRecording by rememberUpdatedState(onStartRecording)
    val currentOnFinishRecording by rememberUpdatedState(onFinishRecording)
    val currentOnStopLockedRecording by rememberUpdatedState(onStopLockedRecording)
    val currentOnLockRecording by rememberUpdatedState(onLockRecording)
    val currentOnCancelRecording by rememberUpdatedState(onCancelRecording)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // Плавающий замочек блокировки над кнопкой при удержании записи
        AnimatedVisibility(
            visible = isRecording && !isLockedRecording,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.offset(y = (-52).dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF222227).copy(alpha = 0.92f))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Заблокировать",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Основная круглая кнопка
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) FlameOrange
                    else if (canSend) scheme.accent
                    else Color(0xFF26262B)
                )
                .border(
                    1.dp,
                    if (canSend) scheme.accent else CardBorder,
                    CircleShape
                )
                .pointerInput(Unit) {
                    coroutineScope {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downTime = System.currentTimeMillis()

                                if (currentCanSend) {
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        currentOnSend()
                                    }
                                    continue
                                }

                                if (currentIsLocked) {
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        currentOnStopLockedRecording()
                                    }
                                    continue
                                }

                                var isRecordingStarted = false
                                var totalDragX = 0f
                                var totalDragY = 0f
                                var isCancelled = false
                                var isLocked = false

                                val holdJob = this@coroutineScope.launch {
                                    delay(180)
                                    if (!isCancelled && !isLocked) {
                                        isRecordingStarted = true
                                        currentOnStartRecording(
                                            if (currentIsCameraMode) RecordMode.CIRCLE else RecordMode.VOICE
                                        )
                                    }
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (change.pressed) {
                                        val delta = change.position - change.previousPosition
                                        totalDragX += delta.x
                                        totalDragY += delta.y

                                        if (isRecordingStarted) {
                                            if (!isCancelled && totalDragX < -80f) {
                                                isCancelled = true
                                                currentOnCancelRecording()
                                            } else if (!isLocked && totalDragY < -70f) {
                                                isLocked = true
                                                currentOnLockRecording()
                                            }
                                        }
                                    } else {
                                        holdJob.cancel()
                                        val duration = System.currentTimeMillis() - downTime
                                        if (!isRecordingStarted && duration < 200) {
                                            // Быстрый одиночный тап — переключение между голосом и видео-кружочком
                                            currentOnToggleCameraMode()
                                        } else if (isRecordingStarted) {
                                            // Отпустил палец после записи: автоотправка (если не заблокировано и не отменено)
                                            if (!isCancelled && !isLocked) {
                                                currentOnFinishRecording(true)
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (canSend) {
                Icon(
                    imageVector = if (isEditing) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.Send,
                    contentDescription = if (isEditing) strings.save else strings.send,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isRecording) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = strings.stop,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isCameraMode) {
                TelegramVideoNoteIcon(
                    tint = scheme.accent,
                    modifier = Modifier.size(21.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = strings.micDesc,
                    tint = scheme.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
