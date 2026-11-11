package com.anongram.app.features.chats.ui.input.bar

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.messages.state.RecordingState
import com.anongram.app.features.chats.ui.input.button.ActionButton
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings
import com.anongram.app.features.chats.ui.input.reply.FusedEditBar
import com.anongram.app.features.chats.ui.input.reply.FusedReplyBar
import com.anongram.app.features.chats.ui.input.voice.VoiceRecordingBar

/**
 * Единый монолитный островок ввода (макет media_1794245430730.png):
 * - Клавиатура, поле ввода, скрепка и кнопка действия объединены в одном скруглённом контейнере.
 * - Поле ввода раздувается ещё на ~2 см (до 160 dp), далее включается вертикальный скролл.
 * - Кнопки эмодзи и отправки зафиксированы внизу (Alignment.Bottom).
 * - При раздувании текста в правом верхнем углу появляется кнопка раскрытия полноценного редактора ⤢.
 * - Плашка ответа слитно встроена сверху на всю ширину поля ввода.
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (recording.isLocked && recording.mode == RecordMode.VOICE) {
            VoiceRecordingBar(
                recording = recording,
                onCancel = onCancelRecording,
                onSend = onStopLockedRecording,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            var textValue: String by remember(input) { mutableStateOf(input) }
            val scrollState = rememberScrollState()

            // Проверяем, нужно ли показывать кнопку раскрытия в углу:
            // показывается при многострочном тексте (3+ строк или длина от 60 символов)
            val isMultiline = textValue.lines().size >= 3 || textValue.length >= 60

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IslandBackground, RoundedCornerShape(24.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                ) {
                    // Слитно встроенная плашка ответа или правки на всю ширину
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
                            onClose = onCancelEdit,
                            onReplaceAttachment = onReplaceAttachment,
                            onEditAttachment = onEditAttachment
                        )
                    }

                    // Единая нижняя строка: эмодзи + раздувающееся поле с кнопкой ⤢ + скрепка + ActionButton
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        if (isRecording) {
                            // Режим удержания записи: скрываем поле ввода, показываем таймер и подсказку свайпа
                            val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                                label = "pulseAlpha"
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
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
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = strings.slideLeftToCancel,
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            // Кнопка эмодзи / клавиатура
                            IconButton(
                                onClick = onToggleEmoji,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEmojiOpen) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                                    contentDescription = strings.emoji,
                                    tint = if (isEmojiOpen) scheme.accent else TextMuted,
                                    modifier = Modifier.size(23.dp)
                                )
                            }

                            // Центральная область ввода: раздувается до 160 dp (~на 2 см выше)
                            val placeholderText = if (editing?.attachment != null) strings.captionPlaceholder else strings.messagePlaceholder
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 34.dp, max = 160.dp)
                                        .verticalScroll(scrollState)
                                        .padding(end = if (isMultiline) 28.dp else 0.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (textValue.isEmpty()) {
                                        Text(
                                            text = placeholderText,
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
                                            .onFocusChanged { if (it.isFocused) onFocusInput() }
                                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                                        textStyle = TextStyle(
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp
                                        ),
                                        cursorBrush = SolidColor(scheme.accent)
                                    )
                                }
                            }

                            // Скрепка (вложения)
                            if (editing?.attachment == null && !canSend) {
                                IconButton(
                                    onClick = onOpenAttach,
                                    modifier = Modifier
                                        .padding(bottom = 2.dp)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AttachFile,
                                        contentDescription = strings.attach,
                                        tint = TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Круглая кнопка действия (отправить / микрофон / видео) прямо внутри островка!
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

            // Иконка ⤢ строго в правом верхнем углу всей капсулы
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
    }
}
}
