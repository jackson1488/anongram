package com.anongram.app.features.chats.media.circle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.messages.domain.MessageAttachment
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
    reviewVideo: MessageAttachment.VideoCircle? = null,
    isReviewPlaying: Boolean = false,
    reviewProgress: Float = 0f,
    reviewTrimStart: Float = 0f,
    reviewTrimEnd: Float = 1f,
    reviewSeekTrigger: Pair<Int, Float>? = null,
    onToggleReviewPlay: () -> Unit = {},
    onReviewDurationReady: (Long) -> Unit = {},
    onReviewProgressChange: (Float) -> Unit = {},
    onReviewPlayingChange: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {},
    onSend: (() -> Unit)? = null,
    onStopAndSend: () -> Unit = {},
    onToggleLock: (() -> Unit)? = null,
    onLock: () -> Unit = {},
    onPauseReview: (() -> Unit)? = null,
    onPauseVideoReview: ((uri: String, durationLabel: String) -> Unit)? = null,
    onSendVideo: ((uri: String, durationLabel: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    com.anongram.app.features.chats.ui.input.video.VideoCircleRecordOverlay(
        recordingState = recordingState,
        isLocked = isLocked,
        isKeyboardVisible = isKeyboardVisible,
        bottomInputTopDp = bottomInputTopDp,
        reviewVideo = reviewVideo,
        isReviewPlaying = isReviewPlaying,
        reviewProgress = reviewProgress,
        reviewTrimStart = reviewTrimStart,
        reviewTrimEnd = reviewTrimEnd,
        reviewSeekTrigger = reviewSeekTrigger,
        onToggleReviewPlay = onToggleReviewPlay,
        onReviewDurationReady = onReviewDurationReady,
        onReviewProgressChange = onReviewProgressChange,
        onReviewPlayingChange = onReviewPlayingChange,
        onCancel = onCancel,
        onSend = onSend,
        onStopAndSend = onStopAndSend,
        onToggleLock = onToggleLock,
        onLock = onLock,
        onPauseReview = onPauseReview,
        onPauseVideoReview = onPauseVideoReview,
        onSendVideo = onSendVideo,
        modifier = modifier
    )
}
