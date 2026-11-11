package com.anongram.app.features.chats.media.circle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.messages.state.RecordingState

/**
 * Прокси для обратной совместимости: делегирует в модульный [com.anongram.app.features.chats.ui.input.video.VideoCircleRecordOverlay].
 */
@Composable
fun VideoCircleRecordOverlay(
    recordingState: RecordingState? = null,
    isLocked: Boolean = recordingState?.isLocked ?: false,
    isKeyboardVisible: Boolean = false,
    bottomInputTopDp: Dp = 76.dp,
    onCancel: () -> Unit = {},
    onSend: (() -> Unit)? = null,
    onStopAndSend: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    onLock: () -> Unit = {},
    onPauseReview: (() -> Unit)? = null,
    onSendVideo: ((uri: String, durationLabel: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    com.anongram.app.features.chats.ui.input.video.VideoCircleRecordOverlay(
        recordingState = recordingState,
        isLocked = isLocked,
        isKeyboardVisible = isKeyboardVisible,
        bottomInputTopDp = bottomInputTopDp,
        onCancel = onCancel,
        onSend = onSend,
        onStopAndSend = onStopAndSend,
        onToggleLock = onToggleLock,
        onLock = onLock,
        onPauseReview = onPauseReview,
        onSendVideo = onSendVideo,
        modifier = modifier
    )
}
