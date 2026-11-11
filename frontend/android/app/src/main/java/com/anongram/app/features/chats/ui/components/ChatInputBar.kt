package com.anongram.app.features.chats.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.messages.state.RecordingState

/**
 * Обратная совместимость для компонентов UI чата.
 * Делегирует в модульный [com.anongram.app.features.chats.ui.input.bar.ChatInputBar].
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
    com.anongram.app.features.chats.ui.input.bar.ChatInputBar(
        input = input,
        canSend = canSend,
        replyTo = replyTo,
        peerName = peerName,
        editing = editing,
        recording = recording,
        isCameraMode = isCameraMode,
        isEmojiOpen = isEmojiOpen,
        onInputChange = onInputChange,
        onSend = onSend,
        onCancelReply = onCancelReply,
        onCancelEdit = onCancelEdit,
        onReplaceAttachment = onReplaceAttachment,
        onEditAttachment = onEditAttachment,
        onReplyClick = onReplyClick,
        onReplyLongClick = onReplyLongClick,
        onToggleEmoji = onToggleEmoji,
        onOpenAttach = onOpenAttach,
        onToggleCameraMode = onToggleCameraMode,
        onStartRecording = onStartRecording,
        onFinishRecording = onFinishRecording,
        onLockRecording = onLockRecording,
        onCancelRecording = onCancelRecording,
        onStopLockedRecording = onStopLockedRecording,
        onPauseVoiceRecording = onPauseVoiceRecording,
        onOpenFullEditor = onOpenFullEditor,
        onFocusInput = onFocusInput,
        focusRequester = focusRequester,
        modifier = modifier
    )
}
