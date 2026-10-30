package com.anongram.app.features.chats.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import androidx.compose.ui.text.style.TextOverflow
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.messages.state.RecordingState

/**
 * Поле ввода по ТЗ (шаги 2, 3, 16):
 * - Слева: кнопка эмодзи/клавиатура
 * - По центру: «Сообщение»
 * - Справа: скрепка, стрелка вверх, круглая кнопка (отправить / микрофон / камера)
 * - Смена микрофон ↔ видео-кружок по тапу
 * - Удержание: запись, свайп вверх — замок, влево — отмена
 * - Плашка ответа / редактирования
 */
@Composable
internal fun ChatInputBar(
    input: String,
    canSend: Boolean,
    replyTo: ChatMessage?,
    peerName: String = "",
    editing: ChatMessage?,
    recording: RecordingState,
    isCameraMode: Boolean,
    isEmojiOpen: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onReplyClick: (ChatMessage) -> Unit = {},
    onReplyLongClick: (ChatMessage) -> Unit = {},
    onToggleEmoji: () -> Unit,
    onOpenAttach: () -> Unit,
    onToggleCameraMode: () -> Unit,
    onStartRecording: (RecordMode) -> Unit,
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopLockedRecording: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val isRecording = recording.mode != RecordMode.NONE

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (isRecording) {
            // Строка записи голоса / кружка (шаг 3)
            RecordingBar(
                recording = recording,
                onCancel = onCancelRecording,
                onStop = onStopLockedRecording,
                onLock = onLockRecording,
                modifier = Modifier.weight(1f)
            )
        } else {
            var textValue: String by remember(input) { mutableStateOf(input) }

            // Основное поле ввода — единый парящий островок со слитно встроенной плашкой ответа (Фото 1)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(IslandBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            ) {
                // Слитно встроенная плашка ответа (Фото 1) — без разделительной полоски
                if (replyTo != null && editing == null) {
                    FusedReplyBar(
                        replyTo = replyTo,
                        peerName = peerName,
                        onClick = { onReplyClick(replyTo) },
                        onLongClick = { onReplyLongClick(replyTo) },
                        onClose = onCancelReply
                    )
                } else if (editing != null) {
                    FusedEditBar(
                        editing = editing,
                        onClose = onCancelEdit
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    // Кнопка эмодзи/клавиатура
                    IconButton(onClick = onToggleEmoji, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = if (isEmojiOpen) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                            contentDescription = "Эмодзи",
                            tint = if (isEmojiOpen) scheme.accent else TextMuted,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Поле ввода текста (жирнее на 1 см, без лагов)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        if (textValue.isEmpty()) {
                            Text(
                                text = "Сообщение",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = textValue,
                            onValueChange = { newText ->
                                textValue = newText
                                onInputChange(newText)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(scheme.accent),
                            maxLines = 5
                        )
                    }

                    // Скрепка (меню вложений)
                    IconButton(onClick = onOpenAttach, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Вложить",
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

            Spacer(modifier = Modifier.width(8.dp))

            // Круглая кнопка действия справа — отдельный парящий островок
            ActionButton(
                canSend = canSend,
                isEditing = editing != null,
                isRecording = isRecording,
                isLockedRecording = recording.isLocked,
                isCameraMode = isCameraMode,
                onSend = onSend,
                onToggleCameraMode = onToggleCameraMode,
                onStartRecording = onStartRecording,
                onStopLockedRecording = onStopLockedRecording,
                onLockRecording = onLockRecording,
                onCancelRecording = onCancelRecording
            )
        }
    }

/**
 * Строка активной записи: красная пульсирующая точка, таймер, «Влево - отмена».
 */
@Composable
private fun RecordingBar(
    recording: RecordingState,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, FlameOrange.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Красная пульсирующая точка
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Таймер 0:00,7
            Text(
                text = recording.timeLabel,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            // Подсказка «Влево - отмена»
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onCancel)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (recording.isLocked) "Отмена" else "« Влево - отмена",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            if (!recording.isLocked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Заблокировать",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onLock)
                )
            }
        }
    }
}

/**
 * Круглая кнопка справа:
 * - С текстом: самолётик / галочка при редактировании
 * - Без текста: микрофон (тап -> переключает на камеру) / удержание (запись)
 */
@Composable
private fun ActionButton(
    canSend: Boolean,
    isEditing: Boolean,
    isRecording: Boolean,
    isLockedRecording: Boolean,
    isCameraMode: Boolean,
    onSend: () -> Unit,
    onToggleCameraMode: () -> Unit,
    onStartRecording: (RecordMode) -> Unit,
    onStopLockedRecording: () -> Unit,
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (isRecording) FlameOrange
                else if (canSend) scheme.accent
                else IslandBackground
            )
            .border(
                1.dp,
                if (canSend) scheme.accent else CardBorder,
                CircleShape
            )
            .pointerInput(canSend, isRecording, isCameraMode) {
                if (canSend) {
                    return@pointerInput
                }
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        onStartRecording(if (isCameraMode) RecordMode.CIRCLE else RecordMode.VOICE)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        if (totalDragX < -150f) {
                            onCancelRecording()
                        } else if (totalDragY < -120f) {
                            onLockRecording()
                        }
                    },
                    onDragEnd = {
                        if (!isLockedRecording) {
                            onStopLockedRecording()
                        }
                    },
                    onDragCancel = {
                        onCancelRecording()
                    }
                )
            }
            .clickable {
                if (canSend) {
                    onSend()
                } else if (isLockedRecording) {
                    onStopLockedRecording()
                } else {
                    onToggleCameraMode()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FusedReplyBar(
    replyTo: ChatMessage,
    peerName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val author = if (replyTo.direction == MessageDirection.INCOMING) peerName else "себе"
    val snippet = replyPreviewSnippet(replyTo)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = null,
            tint = scheme.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Вертикальная полоска акцента рядом с полем ввода
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "В ответ $author",
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = snippet,
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Отменить ответ",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FusedEditBar(
    editing: ChatMessage,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = scheme.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Вертикальная полоска акцента
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Изменение сообщения",
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = editing.text.ifBlank { "Сообщение" },
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Отменить редактирование",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun replyPreviewSnippet(message: ChatMessage): String = when {
    message.text.isNotBlank() -> message.text
    message.attachment is MessageAttachment.File -> message.attachment.name
    message.attachment is MessageAttachment.FileGroup -> "Файлы (${message.attachment.items.size} шт.)"
    message.attachment is MessageAttachment.Photo -> if (message.attachment.isVideo) "Видео" else "Фото"
    message.attachment is MessageAttachment.Music -> "${message.attachment.artist} — ${message.attachment.title}"
    message.attachment is MessageAttachment.MusicGroup -> "Аудиозаписи (${message.attachment.items.size} шт.)"
    message.attachment is MessageAttachment.Voice -> "Голосовое сообщение"
    message.attachment is MessageAttachment.VideoCircle -> "Видеосообщение"
    message.attachment is MessageAttachment.Contact -> "Контакт: ${message.attachment.name}"
    message.attachment is MessageAttachment.Geo -> "Геолокация: ${message.attachment.address}"
    message.attachment is MessageAttachment.Sticker -> "Стикер ${message.attachment.symbol}"
    message.attachment is MessageAttachment.MediaAlbum -> "Альбом (${message.attachment.items.size} фото)"
    else -> "Сообщение"
}
