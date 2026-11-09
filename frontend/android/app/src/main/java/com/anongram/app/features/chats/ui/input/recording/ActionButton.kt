package com.anongram.app.features.chats.ui.input.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.state.RecordMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Круглая кнопка действия:
 * - С текстом: самолётик / галочка при редактировании
 * - Без текста: микрофон (тап -> переключает на камеру) / удержание (запись)
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

    Box(
        modifier = modifier
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
            .pointerInput(canSend, isRecording, isLockedRecording, isCameraMode) {
                if (canSend) {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onSend()
                        }
                    }
                    return@pointerInput
                }

                if (isLockedRecording) {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onStopLockedRecording()
                        }
                    }
                    return@pointerInput
                }

                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            var isRecordingActive = false
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var isCancelled = false
                            var isLocked = false

                            val holdJob = this@coroutineScope.launch {
                                delay(160)
                                if (!isRecordingActive && !isCancelled && !isLocked) {
                                    isRecordingActive = true
                                    onStartRecording(if (isCameraMode) RecordMode.CIRCLE else RecordMode.VOICE)
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (change.pressed) {
                                    val dragDelta = change.position - change.previousPosition
                                    totalDragX += dragDelta.x
                                    totalDragY += dragDelta.y

                                    if (isRecordingActive) {
                                        if (!isCancelled && totalDragX < -100f) {
                                            isCancelled = true
                                            onCancelRecording()
                                        } else if (!isLocked && totalDragY < -80f) {
                                            isLocked = true
                                            onLockRecording()
                                        }
                                    }
                                } else {
                                    holdJob.cancel()
                                    val duration = System.currentTimeMillis() - downTime
                                    if (!isRecordingActive && duration < 200) {
                                        onToggleCameraMode()
                                    } else if (isRecordingActive) {
                                        if (!isCancelled && !isLocked) {
                                            onFinishRecording(true)
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
                contentDescription = if (isEditing) "Сохранить" else "Отправить",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else if (isRecording) {
            Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = "Остановить",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        } else if (isCameraMode) {
            Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = "Видео-кружок (тап = микрофон)",
                tint = scheme.accent,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Микрофон (тап = камера)",
                tint = scheme.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
