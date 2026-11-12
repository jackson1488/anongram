package com.anongram.app.features.chats.input.bar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.messages.state.RecordingState
import com.anongram.app.features.chats.input.button.ActionButton
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import com.anongram.app.features.chats.input.reply.FusedEditBar
import com.anongram.app.features.chats.input.reply.FusedReplyBar
import com.anongram.app.features.chats.input.voice.VoiceRecordingBar

/**
 * Единый монолитный островок ввода:
 * - BasicTextField всегда остаётся смонтированным в дереве (скрывается через alpha=0f при записи),
 *   благодаря чему клавиатура НЕ закрывается при старте и блокировке записи ГС / видео-кружочка.
 */
@Composable
fun ChatInputBar(
    input: String,
    canSend: Boolean,
    replyTo: ChatMessage?,
    peerName: String = "",
    editing: ChatMessage?,
    recording: RecordingState,
    isCameraMode: Boolean,
    isEmojiOpen: Boolean,
    isHiddenByTrim: Boolean = false,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onReplaceAttachment: () -> Unit = {},
    onEditAttachment: () -> Unit = {},
    onReplyClick: (ChatMessage) -> Unit = {},
    onReplyLongClick: (ChatMessage) -> Unit = {},
    onToggleEmoji: () -> Unit,
    onOpenAttach: () -> Unit,
    onToggleCameraMode: () -> Unit,
    onStartRecording: (RecordMode) -> Unit,
    onFinishRecording: (Boolean) -> Unit = {},
    onLockRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopLockedRecording: () -> Unit,
    onPauseVoiceRecording: () -> Unit = {},
    onOpenFullEditor: () -> Unit = {},
    onFocusInput: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val isRecording = recording.mode != RecordMode.NONE
    val isLockedRecording = recording.isLocked && isRecording

    val scrollState = rememberScrollState()
    val isMultiline = !isRecording && !isHiddenByTrim && (input.lines().size >= 3 || input.length >= 60)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Основной островок ввода всегда в дереве (при isLockedRecording / isHiddenByTrim прозрачен, чтобы не терять фокус/клавиатуру)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isLockedRecording || isHiddenByTrim) 0f else 1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.inputBg, RoundedCornerShape(24.dp))
                    .border(1.dp, scheme.inputBorder, RoundedCornerShape(24.dp))
            ) {
                if (!isRecording && !isHiddenByTrim && replyTo != null && editing == null) {
                    FusedReplyBar(
                        replyTo = replyTo,
                        peerName = peerName,
                        onClick = { onReplyClick(replyTo) },
                        onLongClick = { onReplyLongClick(replyTo) },
                        onClose = onCancelReply
                    )
                } else if (!isRecording && !isHiddenByTrim && editing != null) {
                    FusedEditBar(
                        editing = editing,
                        onClose = onCancelEdit,
                        onReplaceAttachment = onReplaceAttachment,
                        onEditAttachment = onEditAttachment
                    )
                }

                // Единая нижняя строка: строго 48.dp при записи/обрезке, без малейшего раздувания
                Row(
                    verticalAlignment = if (isRecording || isHiddenByTrim) Alignment.CenterVertically else Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isRecording || isHiddenByTrim) Modifier.height(48.dp)
                            else Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                        .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Обычный ввод (всегда в дереве, во время записи невидим)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isRecording) 0f else 1f)
                        ) {
                            IconButton(
                                onClick = { if (!isRecording) onToggleEmoji() },
                                enabled = !isRecording,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEmojiOpen) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                                    contentDescription = strings.emoji,
                                    tint = if (isEmojiOpen) scheme.accent else scheme.textMuted,
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            val placeholderText = if (editing?.attachment != null) strings.captionPlaceholder else strings.messagePlaceholder
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (isRecording) Modifier.height(34.dp) else Modifier.heightIn(min = 34.dp, max = 160.dp))
                                        .verticalScroll(scrollState)
                                        .padding(end = if (isMultiline) 28.dp else 0.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (input.isEmpty()) {
                                        Text(
                                            text = placeholderText,
                                            color = scheme.textMuted,
                                            fontSize = 15.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = input,
                                        onValueChange = { newText ->
                                            if (!isRecording) {
                                                onInputChange(newText)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused && !isRecording) onFocusInput() }
                                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                                        textStyle = TextStyle(
                                            color = scheme.textPrimary,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp
                                        ),
                                        cursorBrush = SolidColor(scheme.accent)
                                    )
                                }
                            }

                            if (editing == null && !canSend) {
                                IconButton(
                                    onClick = { if (!isRecording) onOpenAttach() },
                                    enabled = !isRecording,
                                    modifier = Modifier
                                        .padding(bottom = 2.dp)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AttachFile,
                                        contentDescription = strings.attach,
                                        tint = scheme.textMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Строка удержания записи поверх скрытого ввода
                        if (isRecording && !isLockedRecording) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                                label = "pulseAlpha"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 8.dp),
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
                                    text = recording.timeLabel,
                                    color = scheme.textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = strings.slideLeftToCancel,
                                    color = scheme.textMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    ActionButton(
                        canSend = canSend,
                        isEditing = editing != null,
                        isRecording = isRecording,
                        isLockedRecording = recording.isLocked,
                        isCameraMode = isCameraMode,
                        onSend = onSend,
                        onToggleCameraMode = onToggleCameraMode,
                        onStartRecording = onStartRecording,
                        onFinishRecording = onFinishRecording,
                        onStopLockedRecording = onStopLockedRecording,
                        onLockRecording = onLockRecording,
                        onCancelRecording = onCancelRecording,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (isMultiline) {
                val hasTopBar = (replyTo != null || editing != null)
                IconButton(
                    onClick = onOpenFullEditor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = if (hasTopBar) 44.dp else 6.dp,
                            end = 8.dp
                        )
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInFull,
                        contentDescription = strings.openFullEditor,
                        tint = scheme.accent.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Панель заблокированной записи (и для ГС, и для видео-кружочка): таймер, «ОТМЕНА», кнопка [ ➤ ]
        if (isLockedRecording) {
            VoiceRecordingBar(
                recording = recording,
                onCancel = onCancelRecording,
                onSend = onStopLockedRecording,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
