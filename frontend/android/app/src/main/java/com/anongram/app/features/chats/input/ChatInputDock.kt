package com.anongram.app.features.chats.input

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesUiState
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.tools.ChatSearchBottomBar
import com.anongram.app.features.chats.tools.formatResultsCount
import com.anongram.app.features.chats.ui.animateScrollAndCenterItem
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import com.anongram.app.features.chats.input.bar.ChatInputBar
import com.anongram.app.features.chats.input.emoji.EmojiStickerPanel
import com.anongram.app.features.chats.input.video.VideoCircleTrimBar
import com.anongram.app.features.chats.input.voice.VoiceLockedTopControls
import com.anongram.app.features.chats.input.voice.VoiceTrimBar
import kotlinx.coroutines.launch

@Stable
internal class ChatImeLayoutState {
    var lastKeyboardHeightPx by mutableIntStateOf(0)
    var wasKeyboardOpenWhenAttached by mutableStateOf(false)
    var frozenKeyboardHeightPx by mutableIntStateOf(0)
    var wasKeyboardOpenWhenRecording by mutableStateOf(false)
    var frozenRecordingImePx by mutableIntStateOf(0)
    var inputBarHeightPx by mutableIntStateOf(0)
}

@Stable
internal data class ChatImeMetrics(
    val imeState: ChatImeLayoutState,
    val isKeyboardVisible: Boolean,
    val effectiveKeyboardVisible: Boolean,
    val topListPadding: Dp,
    val bottomListPadding: Dp,
    val bottomDockSpacePx: Density.() -> Int,
    val bottomInputTopPx: Density.() -> Int
)

/**
 * Управляет высотой системной клавиатуры (IME), «заморозкой» при открытии вложений/записи
 * и согласованными отступами для списка сообщений, кружков и баннеров в layout-фазе без рекомпозиций на каждый кадр.
 */
@Composable
internal fun rememberChatImeMetrics(
    state: ChatMessagesUiState,
    isReplaceAttachOpen: Boolean = false
): ChatImeMetrics {
    val density = LocalDensity.current
    val imeState = remember { ChatImeLayoutState() }
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars

    val isKeyboardVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val isAnyAttachOpen = state.isAttachOpen || isReplaceAttachOpen

    LaunchedEffect(imeInsets, density) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { bottom ->
                if (bottom > 0) {
                    imeState.lastKeyboardHeightPx = bottom
                    if (imeState.wasKeyboardOpenWhenAttached) {
                        imeState.frozenKeyboardHeightPx = bottom
                    }
                }
            }
    }

    LaunchedEffect(isAnyAttachOpen) {
        if (!isAnyAttachOpen) {
            imeState.wasKeyboardOpenWhenAttached = false
            imeState.frozenKeyboardHeightPx = 0
        }
    }

    val isRecordingActive = state.recording.mode != RecordMode.NONE
    LaunchedEffect(isRecordingActive) {
        if (isRecordingActive) {
            val currentBottom = imeInsets.getBottom(density)
            if (currentBottom > 0) {
                imeState.wasKeyboardOpenWhenRecording = true
                imeState.frozenRecordingImePx = currentBottom
            }
        } else {
            imeState.wasKeyboardOpenWhenRecording = false
            imeState.frozenRecordingImePx = 0
        }
    }

    val isRecordingImeFrozen = isRecordingActive && imeState.wasKeyboardOpenWhenRecording
    val isChatImeFrozen = (isAnyAttachOpen && imeState.wasKeyboardOpenWhenAttached) || isRecordingImeFrozen
    val effectiveKeyboardVisible = if (isChatImeFrozen) true else isKeyboardVisible

    val topListPadding = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 130.dp else 86.dp
    val inputBarHeightDp = with(density) { imeState.inputBarHeightPx.toDp() }
    val bottomListPadding = inputBarHeightDp + 16.dp

    val bottomDockSpacePx: Density.() -> Int = remember(
        imeInsets,
        navInsets,
        imeState,
        isRecordingImeFrozen,
        isAnyAttachOpen
    ) {
        {
            val imeBottom = when {
                isRecordingImeFrozen -> imeState.frozenRecordingImePx
                isAnyAttachOpen && imeState.wasKeyboardOpenWhenAttached -> imeState.frozenKeyboardHeightPx
                else -> imeInsets.getBottom(this)
            }
            val navBottom = navInsets.getBottom(this).coerceAtMost(28.dp.roundToPx())
            val closedBottom = navBottom + 24.dp.roundToPx()
            val openBottom = imeBottom + 4.dp.roundToPx()
            maxOf(closedBottom, openBottom)
        }
    }

    val bottomInputTopPx: Density.() -> Int = remember(bottomDockSpacePx, imeState) {
        {
            bottomDockSpacePx(this) + imeState.inputBarHeightPx.coerceAtLeast(56.dp.roundToPx())
        }
    }

    return ChatImeMetrics(
        imeState = imeState,
        isKeyboardVisible = isKeyboardVisible,
        effectiveKeyboardVisible = effectiveKeyboardVisible,
        topListPadding = topListPadding,
        bottomListPadding = bottomListPadding,
        bottomDockSpacePx = bottomDockSpacePx,
        bottomInputTopPx = bottomInputTopPx
    )
}

/**
 * Единый нижний док ввода чата: строка ввода, редактирование/замена 1-на-1,
 * плашки ответа, панель эмодзи/стикеров и бары обрезки ГС/видео-кружков.
 */
@Composable
internal fun BoxScope.ChatInputDock(
    state: ChatMessagesUiState,
    peerName: String,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    imeMetrics: ChatImeMetrics,
    inputFocusRequester: FocusRequester,
    isSearchListMode: Boolean,
    onToggleSearchListMode: () -> Unit,
    isVideoReviewPlaying: Boolean,
    videoReviewProgress: Float,
    videoReviewDurationMs: Long,
    videoReviewTrimStart: Float,
    videoReviewTrimEnd: Float,
    onToggleVideoReviewPlay: () -> Unit,
    onSeekVideoReview: (Float) -> Unit,
    onVideoTrimStartChange: (Float) -> Unit,
    onVideoTrimEndChange: (Float) -> Unit,
    onFinishVideoReviewSend: () -> Unit,
    onOpenFullEditor: () -> Unit,
    onOpenReplyPreview: () -> Unit,
    onOpenReplaceAttach: (AttachTab) -> Unit,
    onRequestRecordPermissions: (RecordMode, Array<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .zIndex(3f)
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val bottomSpace = imeMetrics.bottomDockSpacePx(this)
                val childConstraints = if (constraints.hasBoundedHeight) {
                    constraints.copy(
                        maxHeight = (constraints.maxHeight - bottomSpace).coerceAtLeast(0)
                    )
                } else {
                    constraints
                }
                val placeable = measurable.measure(childConstraints)
                val totalHeight = if (constraints.hasBoundedHeight) {
                    (placeable.height + bottomSpace).coerceAtMost(constraints.maxHeight)
                } else {
                    placeable.height + bottomSpace
                }
                layout(placeable.width, totalHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.recording.isLocked && state.recording.mode == RecordMode.VOICE) {
                VoiceLockedTopControls(
                    onPauseReview = viewModel::pauseVoiceRecordingToDraft,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        imeMetrics.imeState.inputBarHeightPx = coordinates.size.height
                    }
            ) {
                if (state.isSearchOpen) {
                    val strings = com.anongram.app.features.chats.input.i18n.LocalInputStrings.current
                    val counterText = if (isSearchListMode) {
                        formatResultsCount(state.searchMatches.size)
                    } else if (state.searchQuery.isNotBlank() && state.searchMatches.isEmpty()) {
                        strings.searchNothingFound
                    } else if (state.searchCounter.isNotBlank()) {
                        state.searchCounter
                    } else {
                        strings.searchZeroOfZero
                    }
                    val canOpenList = state.searchMatches.isNotEmpty() && state.searchQuery.isNotBlank()
                    ChatSearchBottomBar(
                        counter = counterText,
                        isListMode = isSearchListMode,
                        enabled = canOpenList,
                        onListClick = {
                            if (canOpenList) onToggleSearchListMode()
                        }
                    )
                } else {
                    val hasTrimOverlay = state.voiceReviewDraft != null || state.videoReviewDraft != null
                    val isEditingVoice = state.editing?.attachment is MessageAttachment.Voice
                    val isEditingVideo = state.editing?.attachment is MessageAttachment.VideoCircle

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ChatInputBar(
                            input = state.input,
                            canSend = state.canSend,
                            replyTo = state.replyTo,
                            peerName = peerName,
                            editing = state.editing,
                            recording = state.recording,
                            isCameraMode = when {
                                isEditingVoice -> false
                                isEditingVideo -> true
                                else -> state.isCameraMode
                            },
                            isEmojiOpen = state.isEmojiOpen,
                            isHiddenByTrim = hasTrimOverlay,
                            onInputChange = viewModel::onInputChange,
                            onSend = {
                                viewModel.send()
                                scope.launch { listState.animateScrollToItem(0) }
                            },
                            onCancelReply = viewModel::cancelReply,
                            onCancelEdit = viewModel::cancelEdit,
                            onReplaceAttachment = {
                                val att = state.editing?.attachment
                                when (att) {
                                    is MessageAttachment.Voice -> viewModel.openVoiceEditTrim()
                                    is MessageAttachment.VideoCircle -> viewModel.openVideoEditTrim()
                                    else -> {
                                        if (imeMetrics.isKeyboardVisible) {
                                            imeMetrics.imeState.wasKeyboardOpenWhenAttached = true
                                            imeMetrics.imeState.frozenKeyboardHeightPx = imeMetrics.imeState.lastKeyboardHeightPx
                                        } else {
                                            imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                                            imeMetrics.imeState.frozenKeyboardHeightPx = 0
                                        }
                                        val tab = when (att) {
                                            is MessageAttachment.File, is MessageAttachment.FileGroup -> AttachTab.FILE
                                            is MessageAttachment.Music, is MessageAttachment.MusicGroup -> AttachTab.MUSIC
                                            else -> AttachTab.GALLERY
                                        }
                                        onOpenReplaceAttach(tab)
                                    }
                                }
                            },
                            onEditAttachment = {
                                when (state.editing?.attachment) {
                                    is MessageAttachment.Voice -> viewModel.openVoiceEditTrim()
                                    is MessageAttachment.VideoCircle -> viewModel.openVideoEditTrim()
                                    else -> ChatActionBannerBus.show { it.editPhoto }
                                }
                            },
                            onReplyClick = { replyMsg ->
                                viewModel.highlightMessage(replyMsg.id)
                                val index = state.messages.asReversed().indexOfFirst { it.id == replyMsg.id }
                                if (index >= 0) {
                                    scope.launch {
                                        runCatching { listState.animateScrollAndCenterItem(index) }
                                    }
                                }
                            },
                            onReplyLongClick = { onOpenReplyPreview() },
                            onToggleEmoji = {
                                if (state.isEmojiOpen) {
                                    viewModel.closeEmojiPanel()
                                    inputFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.toggleEmojiPanel()
                                }
                            },
                            onFocusInput = {
                                if (state.isEmojiOpen) {
                                    viewModel.closeEmojiPanel()
                                }
                            },
                            onOpenAttach = {
                                if (imeMetrics.isKeyboardVisible) {
                                    imeMetrics.imeState.wasKeyboardOpenWhenAttached = true
                                    imeMetrics.imeState.frozenKeyboardHeightPx = imeMetrics.imeState.lastKeyboardHeightPx
                                } else {
                                    imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                                    imeMetrics.imeState.frozenKeyboardHeightPx = 0
                                }
                                viewModel.openAttach()
                            },
                            onToggleCameraMode = {
                                if (!isEditingVoice && !isEditingVideo) {
                                    viewModel.toggleCameraMode()
                                }
                            },
                            onStartRecording = { mode ->
                                when (mode) {
                                    RecordMode.VOICE -> {
                                        if (PermissionUtils.hasRecordAudioPermission(context)) {
                                            viewModel.startRecording(mode, context)
                                        } else {
                                            onRequestRecordPermissions(mode, arrayOf(Manifest.permission.RECORD_AUDIO))
                                        }
                                    }
                                    RecordMode.CIRCLE -> {
                                        val hasAudio = PermissionUtils.hasRecordAudioPermission(context)
                                        val hasCam = PermissionUtils.hasCameraPermission(context)
                                        if (hasAudio && hasCam) {
                                            viewModel.startRecording(mode, context)
                                        } else {
                                            val perms = mutableListOf<String>()
                                            if (!hasAudio) perms.add(Manifest.permission.RECORD_AUDIO)
                                            if (!hasCam) perms.add(Manifest.permission.CAMERA)
                                            onRequestRecordPermissions(mode, perms.toTypedArray())
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onFinishRecording = { viewModel.endPress() },
                            onLockRecording = viewModel::lockRecording,
                            onCancelRecording = viewModel::cancelRecording,
                            onStopLockedRecording = viewModel::stopLockedRecording,
                            onPauseVoiceRecording = viewModel::pauseVoiceRecordingToDraft,
                            onOpenFullEditor = onOpenFullEditor,
                            focusRequester = inputFocusRequester
                        )

                        if (state.voiceReviewDraft != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    )
                            ) {
                                VoiceTrimBar(
                                    voice = state.voiceReviewDraft,
                                    isEditingMode = isEditingVoice,
                                    onDiscard = viewModel::discardVoiceReview,
                                    onSend = { startFrac, endFrac ->
                                        viewModel.sendVoiceReview(startFrac, endFrac, context)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else if (state.videoReviewDraft != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    )
                            ) {
                                VideoCircleTrimBar(
                                    video = state.videoReviewDraft,
                                    durationMs = videoReviewDurationMs,
                                    isPlaying = isVideoReviewPlaying,
                                    currentProgress = videoReviewProgress,
                                    trimStartFraction = videoReviewTrimStart,
                                    trimEndFraction = videoReviewTrimEnd,
                                    isEditingMode = isEditingVideo,
                                    onTogglePlay = onToggleVideoReviewPlay,
                                    onSeekFraction = onSeekVideoReview,
                                    onTrimStartChange = onVideoTrimStartChange,
                                    onTrimEndChange = onVideoTrimEndChange,
                                    onDiscard = viewModel::discardVideoReview,
                                    onSend = onFinishVideoReviewSend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (state.isEmojiOpen && !hasTrimOverlay) {
                        val stickerHeight = if (imeMetrics.imeState.lastKeyboardHeightPx > 0) {
                            with(density) { imeMetrics.imeState.lastKeyboardHeightPx.toDp() }
                        } else 290.dp
                        EmojiStickerPanel(
                            onEmojiClick = viewModel::appendEmoji,
                            onStickerClick = { symbol, colorSeed ->
                                viewModel.sendSticker(symbol, colorSeed)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(stickerHeight)
                        )
                    }
                }
            }
        }
    }
}
