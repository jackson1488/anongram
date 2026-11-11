package com.anongram.app.features.chats.ui.input.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings
import com.anongram.app.features.chats.ui.input.video.TelegramVideoNoteIcon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Круглая кнопка действия с поддержкой Monet-ауры, свайпов блокировки/отмены и физического подъёма:
 * - С текстом: самолётик / галочка при редактировании
 * - Без текста: микрофон / видео-сообщение Telegram
 * - Быстрый тап (< 180 мс): переключение между микрофоном и камерой
 * - Удержание (hold): запуск реальной записи с мягким ореолом (halo)
 * - Свайп вверх (< -80dp): блокировка записи (Lock)
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
    onDragCancelOffset: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    val currentCanSend by rememberUpdatedState(canSend)
    val currentIsRecording by rememberUpdatedState(isRecording)
    val currentIsLocked by rememberUpdatedState(isLockedRecording)
    val currentIsCameraMode by rememberUpdatedState(isCameraMode)
    val currentOnSend by rememberUpdatedState(onSend)
    val currentOnToggleCameraMode by rememberUpdatedState(onToggleCameraMode)
    val currentOnStartRecording by rememberUpdatedState(onStartRecording)
    val currentOnFinishRecording by rememberUpdatedState(onFinishRecording)
    val currentOnStopLockedRecording by rememberUpdatedState(onStopLockedRecording)
    val currentOnLockRecording by rememberUpdatedState(onLockRecording)
    val currentOnCancelRecording by rememberUpdatedState(onCancelRecording)
    val currentOnDragCancelOffset by rememberUpdatedState(onDragCancelOffset)

    val density = LocalDensity.current
    val lockDistancePx = with(density) { 88.dp.toPx() }
    // Порог отмены — примерно половина ширины поля ввода (~145.dp), чтобы кнопка плавно ехала за пальцем без раннего сброса
    val cancelThresholdPx = with(density) { 145.dp.toPx() }
    val maxCancelDragPx = with(density) { 180.dp.toPx() }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val isElevated = isHolding || isRecording
    val buttonSize by animateDpAsState(
        targetValue = if (isElevated) 54.dp else 42.dp,
        animationSpec = tween(150),
        label = "btnSize"
    )

    // Мягкий ореол (halo/glow) вокруг кнопки в стиле Telegram / Monet
    val haloSize by animateDpAsState(
        targetValue = if (isElevated || isLockedRecording) 78.dp else 0.dp,
        animationSpec = tween(180),
        label = "haloSize"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isElevated || isLockedRecording) 0.34f else 0f,
        animationSpec = tween(180),
        label = "haloAlpha"
    )

    // ВАЖНО: pointerInput висит на НЕПОДВИЖНОМ внешнем слоте 42x42dp!
    // Благодаря этому система координат жеста не смещается вместе с кнопкой и кнопка НИКОГДА не дёргается при свайпе!
    Box(
        modifier = modifier
            .size(42.dp)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            val startPos = down.position

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

                            isHolding = true
                            dragOffsetY = 0f
                            dragOffsetX = 0f
                            currentOnDragCancelOffset(0f)

                            var isRecordingStarted = false
                            var isCancelled = false
                            var isLocked = false

                            val holdJob = this@coroutineScope.launch {
                                delay(160)
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
                                    val totalDx = change.position.x - startPos.x
                                    val totalDy = change.position.y - startPos.y

                                    // Плавное движение влево или вверх без рывков
                                    if (totalDx < -12f && kotlin.math.abs(totalDx) >= kotlin.math.abs(totalDy) * 0.7f) {
                                        dragOffsetX = totalDx.coerceIn(-maxCancelDragPx, 0f)
                                        dragOffsetY = (totalDy * 0.2f).coerceIn(-lockDistancePx, 0f)
                                    } else if (totalDy < -8f) {
                                        dragOffsetY = totalDy.coerceIn(-lockDistancePx, 0f)
                                        dragOffsetX = (totalDx * 0.2f).coerceIn(-maxCancelDragPx, 0f)
                                    } else {
                                        dragOffsetX = totalDx.coerceIn(-maxCancelDragPx, 0f)
                                        dragOffsetY = totalDy.coerceIn(-lockDistancePx, 0f)
                                    }
                                    currentOnDragCancelOffset(dragOffsetX)

                                    if (isRecordingStarted) {
                                        if (!isCancelled && dragOffsetX <= -cancelThresholdPx) {
                                            isCancelled = true
                                            isHolding = false
                                            dragOffsetY = 0f
                                            dragOffsetX = 0f
                                            currentOnDragCancelOffset(0f)
                                            currentOnCancelRecording()
                                        } else if (!isLocked && dragOffsetY <= -lockDistancePx + with(density) { 8.dp.toPx() }) {
                                            isLocked = true
                                            isHolding = false
                                            dragOffsetY = 0f
                                            dragOffsetX = 0f
                                            currentOnDragCancelOffset(0f)
                                            currentOnLockRecording()
                                        }
                                    }
                                } else {
                                    holdJob.cancel()
                                    val duration = System.currentTimeMillis() - downTime
                                    if (!isRecordingStarted && duration < 200) {
                                        if (currentIsRecording) {
                                            currentOnStopLockedRecording()
                                        } else {
                                            currentOnToggleCameraMode()
                                        }
                                    } else if (isRecordingStarted) {
                                        if (!isCancelled && !isLocked) {
                                            currentOnFinishRecording(true)
                                        }
                                    }
                                    isHolding = false
                                    dragOffsetY = 0f
                                    dragOffsetX = 0f
                                    currentOnDragCancelOffset(0f)
                                    break
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Вертикальный трек с замочком над кнопкой при удержании записи (через requiredSize, без влияния на высоту Row)
        AnimatedVisibility(
            visible = (isHolding || isRecording) && !isLockedRecording,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .requiredSize(width = 42.dp, height = 92.dp)
                .offset {
                    IntOffset(
                        0,
                        with(density) { (-76).dp.roundToPx() } + (dragOffsetY * 0.25f).roundToInt()
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .requiredSize(width = 42.dp, height = 92.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color(0xFF222227).copy(alpha = 0.94f))
                    .border(1.dp, CardBorder, RoundedCornerShape(21.dp))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Заблокировать",
                    tint = if (dragOffsetY <= -lockDistancePx + with(density) { 14.dp.toPx() }) scheme.accent else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Физический подъем и сдвиг кнопки и ауры вместе с пальцем пользователя (вверх к замку и влево к отмене)
        Box(
            modifier = Modifier
                .requiredSize(78.dp)
                .offset {
                    IntOffset(
                        dragOffsetX.roundToInt(),
                        dragOffsetY.roundToInt()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Ореол (Monet Halo Glow) — фото 1, 2, 6
            if (haloSize > 0.dp) {
                Box(
                    modifier = Modifier
                        .requiredSize(haloSize)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = haloAlpha))
                )
            }

            // Основная круглая кнопка
            Box(
                modifier = Modifier
                    .requiredSize(buttonSize)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording || isHolding -> scheme.accent
                            canSend -> scheme.accent
                            else -> Color(0xFF26262B)
                        }
                    )
                    .border(
                        1.dp,
                        if (canSend || isRecording || isHolding) scheme.accent else CardBorder,
                        CircleShape
                    ),
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
                    if (isCameraMode) {
                        TelegramVideoNoteIcon(
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = strings.micDesc,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
}
