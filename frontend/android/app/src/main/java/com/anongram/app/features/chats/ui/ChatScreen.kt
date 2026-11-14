package com.anongram.app.features.chats.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.anongram.app.features.chats.ui.animations.LocalMessageSnapshotRegistry
import com.anongram.app.features.chats.ui.animations.MessageDeleteOverlay
import com.anongram.app.features.chats.ui.animations.rememberMessageSnapshotRegistry
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.media.circle.VideoCircleRecordOverlay
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.ui.components.ChatMessagesBody
import com.anongram.app.features.chats.ui.components.ChatOverlaysHost
import com.anongram.app.features.chats.input.ChatInputDock
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import com.anongram.app.features.chats.input.banner.ChatActionBannerHost
import com.anongram.app.features.chats.input.banner.SelectionLimitBannerHost
import com.anongram.app.features.chats.input.rememberChatImeMetrics
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Экран активного чата (тонкий оркестратор состояния и композиции):
 * объединяет фон/список сообщений, нижнюю панель ввода (`ChatInputDock`),
 * оверлей записи кружков, плавающую шапку, модальные оверлеи и баннеры уведомлений.
 */
@Composable
fun ChatScreen(
    session: IChatSession,
    viewModel: ChatMessagesViewModel,
    onBack: () -> Unit,
    activeSessions: List<IChatSession> = emptyList(),
    onOpenTargetSession: (IChatSession) -> Unit = {},
    onOpenDirectPeer: (name: String, handleOrPhone: String, onSessionReady: (IChatSession) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }

    var isWallpaperOpen by remember { mutableStateOf(false) }
    var isReplyPreviewOpen by remember { mutableStateOf(false) }
    var isSearchListMode by remember { mutableStateOf(false) }
    var viewingMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingGeoMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingMediaInitialIndex by remember { mutableIntStateOf(0) }
    var viewingOverrideSingleUri by remember { mutableStateOf<String?>(null) }
    var wasKeyboardOpenBeforeViewer by remember { mutableStateOf(false) }
    var forwardingMessageIds by remember { mutableStateOf<List<String>?>(null) }
    var isReplaceAttachSheetOpen by remember { mutableStateOf(false) }
    var replaceInitialTab by remember { mutableStateOf(AttachTab.GALLERY) }
    var isMusicSheetOpen by remember { mutableStateOf(false) }
    var isFullEditorOpen by remember { mutableStateOf(false) }
    var fullTextMessageModal by remember { mutableStateOf<ChatMessage?>(null) }
    var actionMenuAnchorTopY by remember { mutableFloatStateOf(0f) }
    var actionMenuAnchorBottomY by remember { mutableFloatStateOf(0f) }

    val audioController = remember { ChatAudioController.getInstance(context) }
    val isVideoCircleExpanded by remember {
        audioController.state.map { it.currentTrack?.isVideoCircle == true }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val isAudioSheetOpen by remember {
        audioController.state.map { it.isSheetOpen }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val isAudioFullScreenOpen by remember {
        audioController.state.map { it.isFullScreenOpen }.distinctUntilChanged()
    }.collectAsState(initial = false)

    var pendingRecordMode by remember { mutableStateOf<RecordMode?>(null) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true ||
            PermissionUtils.hasRecordAudioPermission(context)
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true ||
            PermissionUtils.hasCameraPermission(context)
        val mode = pendingRecordMode
        pendingRecordMode = null
        when {
            mode == RecordMode.VOICE && audioGranted -> viewModel.startRecording(RecordMode.VOICE, context)
            mode == RecordMode.CIRCLE && audioGranted && cameraGranted -> viewModel.startRecording(RecordMode.CIRCLE, context)
            else -> ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                it.bannerRecordPermissionRequired
            }
        }
    }

    LaunchedEffect(state.isSearchOpen, state.searchQuery) {
        if (!state.isSearchOpen || state.searchQuery.isBlank()) {
            isSearchListMode = false
        }
    }

    LaunchedEffect(state.searchIndex, state.searchMatches) {
        val currentMatchId = state.searchMatches.getOrNull(state.searchIndex)
        if (currentMatchId != null) {
            viewModel.highlightMessage(currentMatchId)
            val matchIndex = state.messages.asReversed().indexOfFirst { it.id == currentMatchId }
            if (matchIndex >= 0) {
                runCatching { listState.animateScrollAndCenterItem(matchIndex) }
            }
        }
    }

    val previousMessagesCount = remember { mutableIntStateOf(state.messages.size) }
    LaunchedEffect(state.messages.size) {
        if (previousMessagesCount.intValue > 0 && state.messages.size > previousMessagesCount.intValue) {
            val lastMessage = state.messages.lastOrNull()
            val isOutgoing = lastMessage?.direction == MessageDirection.OUTGOING
            val isNearBottom = listState.firstVisibleItemIndex <= 1
            if (isOutgoing || isNearBottom) {
                listState.animateScrollToItem(0)
                if (isNearBottom) {
                    viewModel.clearUnreadBadge()
                }
            }
        }
        previousMessagesCount.intValue = state.messages.size
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx <= 1 && state.unreadBadge > 0) {
                    viewModel.clearUnreadBadge()
                }
            }
    }

    val hasInnerBackTarget = fullTextMessageModal != null ||
        isFullEditorOpen ||
        state.videoReviewDraft != null ||
        state.voiceReviewDraft != null ||
        state.expandedVideoCircle != null ||
        isMusicSheetOpen ||
        isReplaceAttachSheetOpen ||
        state.deleteDialogFor != null ||
        forwardingMessageIds != null ||
        viewingMediaMessage != null ||
        isWallpaperOpen ||
        isReplyPreviewOpen ||
        isSearchListMode ||
        state.isSearchOpen ||
        state.isSelectionMode ||
        state.isEmojiOpen ||
        state.isAttachOpen ||
        state.actionMenuFor != null ||
        state.isNotificationsOpen ||
        state.isKeyCheckOpen ||
        state.editing != null

    BackHandler(enabled = hasInnerBackTarget) {
        when {
            fullTextMessageModal != null -> fullTextMessageModal = null
            isFullEditorOpen -> isFullEditorOpen = false
            state.videoReviewDraft != null -> viewModel.discardVideoReview()
            state.voiceReviewDraft != null -> viewModel.discardVoiceReview()
            state.expandedVideoCircle != null -> viewModel.closeExpandedVideoCircle()
            isMusicSheetOpen -> isMusicSheetOpen = false
            isReplaceAttachSheetOpen -> isReplaceAttachSheetOpen = false
            state.deleteDialogFor != null -> viewModel.dismissDeleteDialog()
            forwardingMessageIds != null -> forwardingMessageIds = null
            viewingMediaMessage != null -> {
                viewingMediaMessage = null
                viewingOverrideSingleUri = null
                wasKeyboardOpenBeforeViewer = false
            }
            isWallpaperOpen -> isWallpaperOpen = false
            isReplyPreviewOpen -> isReplyPreviewOpen = false
            isSearchListMode -> isSearchListMode = false
            state.isSearchOpen -> viewModel.closeSearch()
            state.isSelectionMode -> viewModel.clearSelection()
            state.isEmojiOpen -> viewModel.closeEmojiPanel()
            state.isAttachOpen -> viewModel.closeAttach()
            state.actionMenuFor != null -> viewModel.closeActionMenu()
            state.isNotificationsOpen -> viewModel.closeNotifications()
            state.isKeyCheckOpen -> viewModel.closeKeyCheck()
            state.editing != null -> viewModel.cancelEdit()
            else -> onBack()
        }
    }

    val imeMetrics = rememberChatImeMetrics(state, isReplaceAttachOpen = isReplaceAttachSheetOpen)
    val isCircleOverlayActive = state.recording.mode == RecordMode.CIRCLE || state.videoReviewDraft != null
    val scheme = com.anongram.app.core.theme.LocalAnonGramScheme.current

    val videoReviewKey = state.videoReviewDraft?.uri
    var isVideoReviewPlaying by remember(videoReviewKey) { mutableStateOf(false) }
    var videoReviewDurationMs by remember(videoReviewKey) { mutableLongStateOf(1000L) }
    var videoReviewProgress by remember(videoReviewKey) { mutableFloatStateOf(0f) }
    var videoReviewTrimStart by remember(videoReviewKey) { mutableFloatStateOf(0f) }
    var videoReviewTrimEnd by remember(videoReviewKey) { mutableFloatStateOf(1f) }
    var videoReviewSeekCount by remember(videoReviewKey) { mutableIntStateOf(0) }
    var videoReviewSeekTrigger by remember(videoReviewKey) { mutableStateOf<Pair<Int, Float>?>(null) }

    val snapshotRegistry = rememberMessageSnapshotRegistry()
    snapshotRegistry.isCaptureArmed = state.actionMenuFor != null ||
        state.isSelectionMode ||
        !state.deleteDialogFor.isNullOrEmpty() ||
        viewingMediaMessage != null

    CompositionLocalProvider(LocalMessageSnapshotRegistry provides snapshotRegistry) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        WallpaperBackground(
            chatId = session.id,
            modifier = Modifier
                .matchParentSize()
                .then(if (isCircleOverlayActive || isReplyPreviewOpen) Modifier.blur(22.dp) else Modifier)
        )

        ChatMessagesBody(
            session = session,
            state = state,
            viewModel = viewModel,
            listState = listState,
            audioController = audioController,
            inputFocusRequester = inputFocusRequester,
            isCircleOverlayActive = isCircleOverlayActive,
            isVideoCircleExpanded = isVideoCircleExpanded,
            isKeyboardVisible = imeMetrics.isKeyboardVisible,
            isSearchListMode = isSearchListMode,
            bottomDockSpacePx = imeMetrics.bottomDockSpacePx,
            topListPadding = imeMetrics.topListPadding,
            bottomListPadding = imeMetrics.bottomListPadding,
            onCloseSearchListMode = { isSearchListMode = false },
            onOpenActionMenu = { message, topY, bottomY ->
                actionMenuAnchorTopY = topY
                actionMenuAnchorBottomY = bottomY
                viewModel.openActionMenu(message)
            },
            onOpenMediaViewer = { message, initialIndex, overrideUri ->
                wasKeyboardOpenBeforeViewer = imeMetrics.isKeyboardVisible
                if (imeMetrics.isKeyboardVisible) keyboardController?.hide()
                viewingOverrideSingleUri = overrideUri
                viewingMediaInitialIndex = initialIndex
                viewingMediaMessage = message
            },
            onOpenDirectPeer = onOpenDirectPeer,
            onOpenTargetSession = onOpenTargetSession,
            onStartForward = { ids -> forwardingMessageIds = ids },
            onOpenFullTextMessage = { msg -> fullTextMessageModal = msg },
            onOpenGeoViewer = { msg ->
                if (imeMetrics.isKeyboardVisible) keyboardController?.hide()
                viewingGeoMessage = msg
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isCircleOverlayActive) {
            VideoCircleRecordOverlay(
                recordingState = state.recording,
                isKeyboardVisible = imeMetrics.effectiveKeyboardVisible,
                bottomInputTopDp = with(density) { imeMetrics.bottomInputTopPx(density).toDp() },
                reviewVideo = state.videoReviewDraft,
                isReviewPlaying = isVideoReviewPlaying,
                reviewProgress = videoReviewProgress,
                reviewTrimStart = videoReviewTrimStart,
                reviewTrimEnd = videoReviewTrimEnd,
                reviewSeekTrigger = videoReviewSeekTrigger,
                onToggleReviewPlay = { isVideoReviewPlaying = !isVideoReviewPlaying },
                onReviewDurationReady = { dur -> videoReviewDurationMs = dur },
                onReviewProgressChange = { prog -> videoReviewProgress = prog },
                onReviewPlayingChange = { playing -> isVideoReviewPlaying = playing },
                onCancel = {
                    if (state.videoReviewDraft != null) {
                        viewModel.discardVideoReview()
                    } else {
                        viewModel.cancelRecording()
                    }
                },
                onSend = viewModel::stopLockedRecording,
                onToggleLock = viewModel::lockRecording,
                onPauseReview = viewModel::pauseVoiceRecordingToDraft,
                onPauseVideoReview = { uri, dur -> viewModel.pauseVideoRecordingToDraft(uri, dur) },
                onSendVideo = { uri, dur -> viewModel.sendVideoCircle(uri, dur) },
                modifier = Modifier.zIndex(2f)
            )
        }

        ChatInputDock(
            state = state,
            peerName = session.title,
            viewModel = viewModel,
            listState = listState,
            imeMetrics = imeMetrics,
            inputFocusRequester = inputFocusRequester,
            isSearchListMode = isSearchListMode,
            onToggleSearchListMode = { isSearchListMode = !isSearchListMode },
            isVideoReviewPlaying = isVideoReviewPlaying,
            videoReviewProgress = videoReviewProgress,
            videoReviewDurationMs = videoReviewDurationMs,
            videoReviewTrimStart = videoReviewTrimStart,
            videoReviewTrimEnd = videoReviewTrimEnd,
            onToggleVideoReviewPlay = { isVideoReviewPlaying = !isVideoReviewPlaying },
            onSeekVideoReview = { frac ->
                videoReviewProgress = frac
                videoReviewSeekCount += 1
                videoReviewSeekTrigger = videoReviewSeekCount to frac
            },
            onVideoTrimStartChange = { start -> videoReviewTrimStart = start },
            onVideoTrimEndChange = { end -> videoReviewTrimEnd = end },
            onFinishVideoReviewSend = {
                viewModel.sendVideoReview(context, videoReviewTrimStart, videoReviewTrimEnd)
            },
            onOpenFullEditor = { isFullEditorOpen = true },
            onOpenReplyPreview = { isReplyPreviewOpen = true },
            onOpenReplaceAttach = { tab ->
                replaceInitialTab = tab
                isReplaceAttachSheetOpen = true
            },
            onRequestRecordPermissions = { mode, perms ->
                pendingRecordMode = mode
                recordPermissionLauncher.launch(perms)
            },
            modifier = Modifier
                .zIndex(3f)
                .align(Alignment.BottomCenter)
        )

        val scrimHeight = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) {
            144.dp
        } else {
            100.dp
        }
        ChatHeaderScrim(
            listState = listState,
            scrimHeight = scrimHeight,
            isBlurred = isCircleOverlayActive,
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.TopCenter)
        )

        ChatFloatingTopHeader(
            session = session,
            state = state,
            viewModel = viewModel,
            listState = listState,
            audioController = audioController,
            inputFocusRequester = inputFocusRequester,
            isCircleOverlayActive = isCircleOverlayActive,
            onBack = onBack,
            onCycleWallpaper = { isWallpaperOpen = true },
            onStartForward = { ids -> forwardingMessageIds = ids },
            onOpenMusicSheet = {
                isMusicSheetOpen = true
                audioController.openSheet()
            },
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.TopCenter)
        )

        // Модальные оверлеи (скрепка, замена вложения, просмотр медиа, меню и т.д.)
        // ВСЕГДА выше шапки (z=1) и поля ввода (z=3).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            ChatOverlaysHost(
                session = session,
                state = state,
                viewModel = viewModel,
                listState = listState,
                audioController = audioController,
                imeMetrics = imeMetrics,
                inputFocusRequester = inputFocusRequester,
                activeSessions = activeSessions,
                onOpenTargetSession = onOpenTargetSession,
                onOpenDirectPeer = onOpenDirectPeer,
                isFullEditorOpen = isFullEditorOpen,
                onCloseFullEditor = { isFullEditorOpen = false },
                isWallpaperOpen = isWallpaperOpen,
                onCloseWallpaper = { isWallpaperOpen = false },
                isReplyPreviewOpen = isReplyPreviewOpen,
                onCloseReplyPreview = { isReplyPreviewOpen = false },
                isReplaceAttachSheetOpen = isReplaceAttachSheetOpen,
                replaceInitialTab = replaceInitialTab,
                onCloseReplaceAttach = { isReplaceAttachSheetOpen = false },
                forwardingMessageIds = forwardingMessageIds,
                onStartForward = { ids -> forwardingMessageIds = ids },
                onCloseForward = { forwardingMessageIds = null },
                viewingMediaMessage = viewingMediaMessage,
                viewingMediaInitialIndex = viewingMediaInitialIndex,
                viewingOverrideSingleUri = viewingOverrideSingleUri,
                wasKeyboardOpenBeforeViewer = wasKeyboardOpenBeforeViewer,
                onUpdateMediaViewer = { updated -> viewingMediaMessage = updated },
                onCloseMediaViewer = { _ ->
                    viewingMediaMessage = null
                    viewingOverrideSingleUri = null
                    wasKeyboardOpenBeforeViewer = false
                },
                actionMenuAnchorTopY = actionMenuAnchorTopY,
                actionMenuAnchorBottomY = actionMenuAnchorBottomY,
                isMusicSheetOpen = isMusicSheetOpen,
                isAudioSheetOpen = isAudioSheetOpen,
                isAudioFullScreenOpen = isAudioFullScreenOpen,
                onCloseMusicSheet = { isMusicSheetOpen = false },
                fullTextMessageModal = fullTextMessageModal,
                onCloseFullTextModal = { fullTextMessageModal = null },
                viewingGeoMessage = viewingGeoMessage,
                onCloseGeoViewer = { viewingGeoMessage = null }
            )
        }

        MessageDeleteOverlay(
            registry = snapshotRegistry,
            modifier = Modifier.zIndex(2f)
        )

        SelectionLimitBannerHost(
            warningText = state.limitWarning,
            onDismiss = viewModel::dismissLimitWarning,
            bottomOffsetPx = imeMetrics.bottomInputTopPx
        )
        ChatActionBannerHost(bottomOffsetPx = imeMetrics.bottomInputTopPx)
    }
    }
}
