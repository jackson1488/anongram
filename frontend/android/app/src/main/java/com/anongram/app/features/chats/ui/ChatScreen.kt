package com.anongram.app.features.chats.ui

import androidx.activity.compose.BackHandler
import com.anongram.app.features.chats.media.player.AudioMetadataHelper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.attach.api.AttachSheet
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.AttachTab
import com.anongram.app.features.chats.media.circle.VideoCircleRecordOverlay
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatMiniPlayer
import com.anongram.app.features.chats.media.player.ChatMusicPlayerSheet
import com.anongram.app.features.chats.media.player.ChatTrack
import com.anongram.app.features.chats.media.player.MusicFullScreenPlayer
import com.anongram.app.features.chats.ui.input.video.VideoCircleTrimBar
import com.anongram.app.features.chats.ui.input.voice.VoiceLockedTopControls
import com.anongram.app.features.chats.ui.input.voice.VoiceTrimBar
import com.anongram.app.features.chats.messages.state.RecordMode
import com.anongram.app.features.chats.ui.components.AttachBottomSheet
import com.anongram.app.features.chats.ui.components.ChatInputBar
import com.anongram.app.features.chats.ui.components.bubble.MessageFullTextViewerModal
import com.anongram.app.features.chats.ui.input.editor.ChatFullTextEditorScreen
import com.anongram.app.features.chats.ui.input.editor.TextStyleFormat
import com.anongram.app.features.chats.ui.components.ChatMediaFullscreenViewer
import com.anongram.app.features.chats.ui.components.DeleteMessageDialog
import com.anongram.app.features.chats.ui.components.EmojiStickerPanel
import com.anongram.app.features.chats.ui.components.ForwardTargetPickerScreen
import com.anongram.app.features.chats.ui.components.GeoPickerScreen
import com.anongram.app.features.chats.ui.components.KeyVerificationDialog
import com.anongram.app.features.chats.ui.components.MessageContextMenu
import com.anongram.app.features.chats.ui.components.MessagesList
import com.anongram.app.features.chats.ui.components.NotificationsMuteDialog
import com.anongram.app.features.chats.ui.components.NotificationsSettingsDialog
import com.anongram.app.features.chats.ui.components.PinnedMessagesScreen
import com.anongram.app.features.chats.ui.components.reply.ReplyPreviewDialog
import com.anongram.app.features.chats.ui.components.ScrollDownButton
import com.anongram.app.features.wallpapers.api.WallpaperRoute
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.chats.ui.components.bubble.saveFileToDeviceDownloads
import com.anongram.app.features.chats.ui.components.bubble.saveMusicToDeviceMusic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

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
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var isWallpaperOpen by remember { mutableStateOf(false) }
    var isReplyPreviewOpen by remember { mutableStateOf(false) }
    var isSearchListMode by remember { mutableStateOf(false) }
    var viewingMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingMediaInitialIndex by remember { mutableIntStateOf(0) }
    var viewingOverrideSingleUri by remember { mutableStateOf<String?>(null) }
    var wasKeyboardOpenBeforeViewer by remember { mutableStateOf(false) }
    var forwardingMessageIds by remember { mutableStateOf<List<String>?>(null) }
    var isReplaceAttachSheetOpen by remember { mutableStateOf(false) }
    var replaceInitialTab by remember { mutableStateOf(AttachTab.GALLERY) }
    val audioController = remember { ChatAudioController.getInstance(context) }
    val isVideoCircleExpanded by remember {
        audioController.state
            .map { it.currentTrack?.isVideoCircle == true }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    val isAudioSheetOpen by remember {
        audioController.state
            .map { it.isSheetOpen }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    val isAudioFullScreenOpen by remember {
        audioController.state
            .map { it.isFullScreenOpen }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    var isMusicSheetOpen by remember { mutableStateOf(false) }
    var isFullEditorOpen by remember { mutableStateOf(false) }
    var fullTextMessageModal by remember { mutableStateOf<ChatMessage?>(null) }
    var inputBarHeightPx by remember { mutableStateOf(0) }
    var pendingRecordMode by remember { mutableStateOf<RecordMode?>(null) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true || PermissionUtils.hasRecordAudioPermission(context)
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true || PermissionUtils.hasCameraPermission(context)
        val mode = pendingRecordMode
        pendingRecordMode = null
        if (mode == RecordMode.VOICE && audioGranted) {
            viewModel.startRecording(RecordMode.VOICE, context)
        } else if (mode == RecordMode.CIRCLE && audioGranted && cameraGranted) {
            viewModel.startRecording(RecordMode.CIRCLE, context)
        } else {
            Toast.makeText(context, "Требуется разрешение для записи", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.isSearchOpen) {
        if (!state.isSearchOpen) {
            isSearchListMode = false
        }
    }

    var actionMenuAnchorY by remember { mutableFloatStateOf(0f) }

    BackHandler {
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
            }
            isWallpaperOpen -> isWallpaperOpen = false
            isReplyPreviewOpen -> isReplyPreviewOpen = false
            isSearchListMode -> isSearchListMode = false
            state.isSearchOpen -> viewModel.closeSearch()
            state.isSelectionMode -> viewModel.clearSelection()
            state.isEmojiOpen -> viewModel.closeEmojiPanel()
            state.isAttachOpen -> viewModel.closeAttach()
            state.isGeoOpen -> viewModel.closeGeoPicker()
            state.actionMenuFor != null -> viewModel.closeActionMenu()
            state.isNotificationsOpen -> viewModel.closeNotifications()
            state.isKeyCheckOpen -> viewModel.closeKeyCheck()
            state.editing != null -> viewModel.cancelEdit()
            else -> onBack()
        }
    }

    LaunchedEffect(state.searchIndex, state.searchMatches) {
        val currentMatchId = state.searchMatches.getOrNull(state.searchIndex)
        if (currentMatchId != null) {
            viewModel.highlightMessage(currentMatchId)
            val matchIndex = state.messages.asReversed().indexOfFirst { it.id == currentMatchId }
            if (matchIndex >= 0) {
                runCatching {
                    listState.animateScrollAndCenterItem(matchIndex)
                }
            }
        }
    }

    LaunchedEffect(state.isSearchOpen, state.searchQuery) {
        if (!state.isSearchOpen || state.searchQuery.isBlank()) {
            isSearchListMode = false
        }
    }

    val previousMessagesCount = remember { mutableIntStateOf(state.messages.size) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.size > previousMessagesCount.intValue) {
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

    val isCircleOverlayActive = state.recording.mode == RecordMode.CIRCLE || state.videoReviewDraft != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        WallpaperBackground(
            chatId = session.id,
            modifier = Modifier
                .matchParentSize()
                .then(if (isCircleOverlayActive || isReplyPreviewOpen) Modifier.blur(22.dp) else Modifier)
        )

        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val inputFocusRequester = remember { FocusRequester() }
        val currentImeBottom = WindowInsets.ime.getBottom(density)
        var lastKeyboardHeightPx by remember { mutableIntStateOf(0) }
        if (currentImeBottom > 0) {
            lastKeyboardHeightPx = currentImeBottom
        }
        val isKeyboardVisible = currentImeBottom > 0

        var wasKeyboardOpenWhenAttached by remember { mutableStateOf(false) }
        var frozenKeyboardHeightPx by remember { mutableIntStateOf(0) }

        if (state.isAttachOpen && wasKeyboardOpenWhenAttached && currentImeBottom > 0) {
            frozenKeyboardHeightPx = currentImeBottom
        }

        var wasKeyboardOpenWhenRecording by remember { mutableStateOf(false) }
        var frozenRecordingImePx by remember { mutableIntStateOf(0) }
        val isRecordingActive = state.recording.mode != RecordMode.NONE

        LaunchedEffect(isRecordingActive) {
            if (isRecordingActive) {
                if (currentImeBottom > 0) {
                    wasKeyboardOpenWhenRecording = true
                    frozenRecordingImePx = currentImeBottom
                }
            } else {
                wasKeyboardOpenWhenRecording = false
                frozenRecordingImePx = 0
            }
        }

        val isRecordingImeFrozen = isRecordingActive && wasKeyboardOpenWhenRecording
        val isChatImeFrozen = (state.isAttachOpen && wasKeyboardOpenWhenAttached) || isRecordingImeFrozen
        val chatBottomImePx = when {
            isRecordingImeFrozen -> frozenRecordingImePx
            state.isAttachOpen && wasKeyboardOpenWhenAttached -> frozenKeyboardHeightPx
            else -> currentImeBottom
        }
        val effectiveKeyboardVisible = if (isChatImeFrozen) true else isKeyboardVisible

        val topListPadding = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 130.dp else 86.dp
        val inputBarHeightDp = with(density) { inputBarHeightPx.toDp() }
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomListPadding = if (effectiveKeyboardVisible) {
            inputBarHeightDp + 4.dp + 16.dp
        } else {
            inputBarHeightDp + 24.dp + navBottom + 20.dp
        }
        val bottomInputTopDp = with(density) { chatBottomImePx.toDp() } +
            navBottom.coerceAtMost(48.dp) +
            (if (effectiveKeyboardVisible) 4.dp else 24.dp) +
            inputBarHeightDp.coerceAtLeast(56.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isReplyPreviewOpen) Modifier.blur(20.dp) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isCircleOverlayActive) Modifier.blur(22.dp) else Modifier)
                    .padding(bottom = with(density) { chatBottomImePx.toDp() })
            ) {
            if (state.isSearchOpen && isSearchListMode) {
                val searchMatchingMessages = remember(state.messages, state.searchMatches) {
                    val messageMap = state.messages.associateBy { it.id }
                    state.searchMatches.mapNotNull { messageMap[it] }
                }
                ChatSearchResultsList(
                    query = state.searchQuery,
                    matches = searchMatchingMessages,
                    peerName = session.title,
                    onSelectMatch = { message ->
                        isSearchListMode = false
                        viewModel.highlightMessage(message.id)
                        val matchIdx = state.searchMatches.indexOf(message.id)
                        if (matchIdx >= 0) {
                            viewModel.jumpToSearchMatch(matchIdx)
                        }
                        val idx = state.messages.asReversed().indexOfFirst { it.id == message.id }
                        if (idx >= 0) {
                            scope.launch {
                                runCatching {
                                    listState.animateScrollAndCenterItem(idx)
                                }
                            }
                        }
                    },
                    topPadding = topListPadding,
                    bottomPadding = bottomListPadding
                )
            } else {
                MessagesList(
                    messages = state.messages,
                    peerName = session.title,
                    listState = listState,
                    searchQuery = if (state.isSearchOpen) state.searchQuery else "",
                    selection = state.selection,
                    firstUnreadId = state.firstUnreadId,
                    highlightedMessageId = state.highlightedMessageId,
                    pinnedIds = state.pinnedIds.toSet(),
                    isVideoCircleExpanded = isVideoCircleExpanded,
                    topPadding = topListPadding,
                    bottomPadding = bottomListPadding,
                    onClick = { message, topY ->
                        if (state.isSelectionMode) {
                            viewModel.toggleSelection(message.id)
                        } else {
                            actionMenuAnchorY = topY
                            viewModel.openActionMenu(message)
                        }
                    },
                    onLongClick = { message ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (state.isSelectionMode) {
                            viewModel.toggleSelection(message.id)
                        } else {
                            viewModel.enterSelection(message.id)
                        }
                    },
                    onSwipeReply = { msg ->
                        viewModel.startReply(msg)
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    onReplyMessage = { msg, subIdx ->
                        viewModel.startReply(msg, subIdx)
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    onToggleReaction = viewModel::toggleReaction,
                    onSelectMessage = viewModel::toggleSelection,
                    onToggleAlbumPhoto = viewModel::toggleAlbumPhotoSelection,
                    onToggleFileGroupItem = viewModel::toggleFileGroupItemSelection,
                    onToggleMusicGroupItem = viewModel::toggleMusicGroupItemSelection,
                    onEnterAlbumPhotoSelection = { key ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.enterSelection(key)
                    },
                    onPinMessage = viewModel::togglePinMessage,
                    onEditMessage = { msg, subIdx ->
                        viewModel.startEdit(msg, subIdx)
                        val att = msg.attachment
                        if (att !is MessageAttachment.Voice && att !is MessageAttachment.VideoCircle) {
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    onRetryMessage = viewModel::retry,
                    onHighlightMessage = viewModel::highlightMessage,
                    onOpenMediaViewer = { message, photoIdx ->
                        wasKeyboardOpenBeforeViewer = isKeyboardVisible
                        if (isKeyboardVisible) {
                            keyboardController?.hide()
                        }
                        viewingOverrideSingleUri = null
                        viewingMediaMessage = message
                        viewingMediaInitialIndex = photoIdx
                    },
                    onOpenUncompressedPhoto = { message, uri ->
                        wasKeyboardOpenBeforeViewer = isKeyboardVisible
                        if (isKeyboardVisible) {
                            keyboardController?.hide()
                        }
                        viewingOverrideSingleUri = uri
                        viewingMediaMessage = message
                        viewingMediaInitialIndex = 0
                    },
                    onOpenContactChat = { contactName, contactPhone ->
                        onOpenDirectPeer(contactName, contactPhone.ifBlank { contactName }) { targetSession ->
                            onOpenTargetSession(targetSession)
                        }
                    },
                    onForwardSubItem = { subKey ->
                        forwardingMessageIds = listOf(subKey)
                    },
                    onDeleteSubItem = { subKey ->
                        viewModel.requestDelete(listOf(subKey))
                    },
                    onPlayVideoCircle = { targetMsg ->
                        val vc = targetMsg.attachment as? MessageAttachment.VideoCircle
                        val path = vc?.uri
                        if (vc != null && !path.isNullOrBlank()) {
                            val f = java.io.File(path)
                            val isOut = targetMsg.direction == MessageDirection.OUTGOING
                            val name = if (isOut) "Вы" else session.title
                            val track = ChatTrack(
                                id = "circle_${targetMsg.id}",
                                messageId = targetMsg.id,
                                chatId = session.id,
                                uri = if (f.exists()) android.net.Uri.fromFile(f) else android.net.Uri.parse(path),
                                title = name,
                                artist = name,
                                durationLabel = vc.durationLabel,
                                durationMs = 0L, // реальная длительность берётся из файла, чтобы не обрезать конец
                                isVoice = true,
                                isOutgoing = isOut,
                                chatTitle = session.title,
                                chatAvatarSeed = session.title.hashCode(),
                                isVideoCircle = true
                            )
                            audioController.play(listOf(track), track, autoStart = true)
                        }
                    },
                    onEmptyAreaTap = { audioController.stopVideoCircle() },
                    onPlayVoice = { targetMsg ->
                        val voiceTracks: List<ChatTrack> = state.messages
                            .filter { it.epochMs >= targetMsg.epochMs && it.attachment is MessageAttachment.Voice }
                            .map { msg ->
                                val v = msg.attachment as MessageAttachment.Voice
                                val sec = runCatching {
                                    val parts = v.durationLabel.split(":")
                                    parts[0].toLong() * 60_000L + parts[1].toLong() * 1000L
                                }.getOrDefault(15_000L)
                                val isOut = msg.direction == MessageDirection.OUTGOING
                                val name = if (isOut) "Вы" else session.title
                                val voiceUri = if (!v.uri.isNullOrBlank()) {
                                    val f = java.io.File(v.uri)
                                    if (f.exists()) {
                                        android.net.Uri.fromFile(f)
                                    } else {
                                        android.net.Uri.parse(v.uri)
                                    }
                                } else android.net.Uri.EMPTY
                                ChatTrack(
                                    id = "voice_${msg.id}",
                                    messageId = msg.id,
                                    chatId = session.id,
                                    uri = voiceUri,
                                    title = name,
                                    artist = name,
                                    durationLabel = v.durationLabel,
                                    durationMs = sec,
                                    isVoice = true,
                                    isOutgoing = isOut,
                                    chatTitle = session.title,
                                    chatAvatarSeed = session.title.hashCode()
                                )
                            }
                        val current = voiceTracks.firstOrNull { it.id == "voice_${targetMsg.id}" } ?: voiceTracks.firstOrNull()
                        if (current != null) {
                            audioController.playVoiceSequence(voiceTracks, current)
                        }
                    },
                    onPlayMusic = { targetTrack ->
                        val chatMusicQueue: List<ChatTrack> = state.messages.asReversed().flatMap { msg ->
                            when (val att = msg.attachment) {
                                is MessageAttachment.Music -> listOf(
                                    ChatTrack(
                                        id = "${msg.id}_${att.title}",
                                        messageId = msg.id,
                                        chatId = session.id,
                                        title = att.title,
                                        artist = att.artist,
                                        performer = if (att.artist.isNotBlank() && att.artist != "Неизвестен") att.artist else "AnonGram",
                                        durationLabel = att.durationLabel,
                                        durationMs = AudioMetadataHelper.parseDurationLabelToMs(att.durationLabel),
                                        sizeLabel = att.sizeLabel,
                                        chatTitle = session.title,
                                        chatAvatarSeed = session.title.hashCode(),
                                        uri = att.uri?.let { android.net.Uri.parse(it) } ?: android.net.Uri.EMPTY
                                    )
                                )
                                is MessageAttachment.MusicGroup -> att.items.mapIndexed { idx, item ->
                                    ChatTrack(
                                        id = "${msg.id}_${item.title}_$idx",
                                        messageId = msg.id,
                                        chatId = session.id,
                                        title = item.title,
                                        artist = item.artist,
                                        performer = if (item.artist.isNotBlank() && item.artist != "Неизвестен") item.artist else "AnonGram",
                                        durationLabel = item.durationLabel,
                                        durationMs = AudioMetadataHelper.parseDurationLabelToMs(item.durationLabel),
                                        sizeLabel = item.sizeLabel,
                                        chatTitle = session.title,
                                        chatAvatarSeed = session.title.hashCode(),
                                        uri = item.uri?.let { android.net.Uri.parse(it) } ?: android.net.Uri.EMPTY
                                    )
                                }
                                else -> emptyList()
                            }
                        }.take(50)
                        val toPlay = chatMusicQueue.firstOrNull { it.id == targetTrack.id || (it.messageId == targetTrack.messageId && it.title == targetTrack.title) } ?: targetTrack
                        audioController.play(if (chatMusicQueue.isNotEmpty()) chatMusicQueue else listOf(toPlay), toPlay)
                    },
                    onOpenFullTextMessage = { fullTextMessageModal = it }
                )
            }

            ChatScrollDownOverlay(
                listState = listState,
                isSearchOpen = state.isSearchOpen,
                unreadBadge = state.unreadBadge,
                onClick = {
                    scope.launch {
                        if (listState.firstVisibleItemIndex > 6) {
                            listState.scrollToItem(2)
                        }
                        listState.animateScrollToItem(0)
                    }
                    viewModel.clearUnreadBadge()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (isKeyboardVisible) 126.dp else 146.dp)
            )

            // Плавающие круглые кнопки поиска (вверх / вниз по найденным сообщениям) (Фото 6)
            if (state.isSearchOpen && !isSearchListMode) {
                ChatSearchNavigationButtons(
                    canNavigate = state.searchMatches.isNotEmpty(),
                    onPrevious = viewModel::searchMoveUp,
                    onNext = viewModel::searchMoveDown,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (isKeyboardVisible) 126.dp else 146.dp)
                )
            }
        }

        val videoReviewKey = state.videoReviewDraft?.uri
        var isVideoReviewPlaying by remember(videoReviewKey) { mutableStateOf(false) }
        var videoReviewDurationMs by remember(videoReviewKey) { mutableLongStateOf(1000L) }
        var videoReviewProgress by remember(videoReviewKey) { mutableFloatStateOf(0f) }
        var videoReviewTrimStart by remember(videoReviewKey) { mutableFloatStateOf(0f) }
        var videoReviewTrimEnd by remember(videoReviewKey) { mutableFloatStateOf(1f) }
        var videoReviewSeekCount by remember(videoReviewKey) { mutableIntStateOf(0) }
        var videoReviewSeekTrigger by remember(videoReviewKey) { mutableStateOf<Pair<Int, Float>?>(null) }

        // Оверлей записи и обрезки видео-кружочка (поверх размытого чата и шапки, но под строкой ввода)
        if (isCircleOverlayActive) {
            VideoCircleRecordOverlay(
                recordingState = state.recording,
                isKeyboardVisible = effectiveKeyboardVisible,
                bottomInputTopDp = bottomInputTopDp,
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

        // 1b. Парящее поле ввода поверх списка сообщений и оверлея записи
        Box(
            modifier = Modifier
                .zIndex(3f)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = with(density) { chatBottomImePx.toDp() })
                .safeNavigationBarPadding()
                .padding(bottom = if (effectiveKeyboardVisible) 4.dp else 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Плавающая кнопка паузы и тултип СВЕРХУ над полем ввода при заблокированной записи ГС
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
                            inputBarHeightPx = coordinates.size.height
                        }
                ) {
                    if (state.isSearchOpen) {
                        val counterText = if (isSearchListMode) {
                            formatResultsCount(state.searchMatches.size)
                        } else if (state.searchQuery.isNotBlank() && state.searchMatches.isEmpty()) {
                            "Ничего не найдено"
                        } else if (state.searchCounter.isNotBlank()) {
                            state.searchCounter
                        } else {
                            "0 из 0"
                        }
                        val canOpenList = state.searchMatches.isNotEmpty() && state.searchQuery.isNotBlank()
                        ChatSearchBottomBar(
                            counter = counterText,
                            isListMode = isSearchListMode,
                            enabled = canOpenList,
                            onListClick = {
                                if (canOpenList) {
                                    isSearchListMode = !isSearchListMode
                                }
                            }
                        )
                    } else {
                        val hasTrimOverlay = state.voiceReviewDraft != null || state.videoReviewDraft != null
                        val isEditingVoice = state.editing?.attachment is MessageAttachment.Voice
                        val isEditingVideo = state.editing?.attachment is MessageAttachment.VideoCircle
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // ChatInputBar остаётся смонтированным с isHiddenByTrim, чтобы клавиатура НЕ закрывалась при паузе/обрезке ГС
                            ChatInputBar(
                                input = state.input,
                                canSend = state.canSend,
                                replyTo = state.replyTo,
                                peerName = session.title,
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
                                            replaceInitialTab = when (att) {
                                                is MessageAttachment.File, is MessageAttachment.FileGroup -> AttachTab.FILE
                                                is MessageAttachment.Music, is MessageAttachment.MusicGroup -> AttachTab.MUSIC
                                                else -> AttachTab.GALLERY
                                            }
                                            isReplaceAttachSheetOpen = true
                                        }
                                    }
                                },
                                onEditAttachment = {
                                    when (state.editing?.attachment) {
                                        is MessageAttachment.Voice -> viewModel.openVoiceEditTrim()
                                        is MessageAttachment.VideoCircle -> viewModel.openVideoEditTrim()
                                        else -> Toast.makeText(context, "Редактирование фото", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReplyClick = { replyMsg ->
                                    viewModel.highlightMessage(replyMsg.id)
                                    val index = state.messages.asReversed().indexOfFirst { it.id == replyMsg.id }
                                    if (index >= 0) {
                                        scope.launch {
                                            runCatching {
                                                listState.animateScrollAndCenterItem(index)
                                            }
                                        }
                                    }
                                },
                                onReplyLongClick = {
                                    isReplyPreviewOpen = true
                                },
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
                                    if (isKeyboardVisible) {
                                        wasKeyboardOpenWhenAttached = true
                                        frozenKeyboardHeightPx = lastKeyboardHeightPx
                                    } else {
                                        wasKeyboardOpenWhenAttached = false
                                        frozenKeyboardHeightPx = 0
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
                                                pendingRecordMode = mode
                                                recordPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
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
                                                pendingRecordMode = mode
                                                recordPermissionLauncher.launch(perms.toTypedArray())
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
                                onOpenFullEditor = { isFullEditorOpen = true },
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
                            } else {
                                val videoDraft = state.videoReviewDraft
                                if (videoDraft != null) {
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
                                        video = videoDraft,
                                        durationMs = videoReviewDurationMs,
                                        isPlaying = isVideoReviewPlaying,
                                        currentProgress = videoReviewProgress,
                                        trimStartFraction = videoReviewTrimStart,
                                        trimEndFraction = videoReviewTrimEnd,
                                        isEditingMode = isEditingVideo,
                                        onTogglePlay = { isVideoReviewPlaying = !isVideoReviewPlaying },
                                        onSeekFraction = { frac ->
                                            val next = videoReviewSeekCount + 1
                                            videoReviewSeekCount = next
                                            videoReviewSeekTrigger = next to frac
                                        },
                                        onTrimStartChange = { s ->
                                            videoReviewTrimStart = s
                                            val next = videoReviewSeekCount + 1
                                            videoReviewSeekCount = next
                                            videoReviewSeekTrigger = next to s
                                        },
                                        onTrimEndChange = { e ->
                                            videoReviewTrimEnd = e
                                            val next = videoReviewSeekCount + 1
                                            videoReviewSeekCount = next
                                            videoReviewSeekTrigger = next to e
                                        },
                                        onDiscard = viewModel::discardVideoReview,
                                        onSend = {
                                            isVideoReviewPlaying = false
                                            viewModel.sendVideoReview(context, videoReviewTrimStart, videoReviewTrimEnd)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                }
                            }
                        }

                        // Панель эмодзи/стикеров под полем ввода (высота синхронизирована с клавиатурой)
                        if (state.isEmojiOpen && !hasTrimOverlay) {
                            val stickerHeight = if (lastKeyboardHeightPx > 0) with(density) { lastKeyboardHeightPx.toDp() } else 290.dp
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

        // 2. Мягкая тёмная вуаль-градиент за плавающей шапкой (усиливается при скролле)
        val scrimHeight = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 144.dp else 100.dp
        ChatHeaderScrim(
            listState = listState,
            scrimHeight = scrimHeight,
            isBlurred = isCircleOverlayActive,
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.TopCenter)
        )

        // 3. Плавающая шапка чата поверх сообщений (парящие островки)
        Column(
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (isCircleOverlayActive) Modifier.blur(22.dp) else Modifier)
                .safeStatusBarPadding()
        ) {
            when {
                state.isSelectionMode -> {
                    SelectionPanel(
                        selectedCount = state.selection.size,
                        isPhoto = state.isSingleSelectedPhoto,
                        isFileOrMusic = state.isSingleSelectedFileOrMusic,
                        isVideoCircle = state.isSingleSelectedVideoCircle,
                        isVoice = state.isSingleSelectedVoice,
                        onClose = viewModel::clearSelection,
                        onCopy = {
                            val combined = state.selectedMessages.joinToString("\n") { it.text }
                            clipboard.setText(AnnotatedString(combined))
                            viewModel.clearSelection()
                        },
                        onBrushEdit = {
                            val openKeyboard = !state.isSingleSelectedVideoCircle && !state.isSingleSelectedVoice
                            viewModel.startEditFromSelection()
                            if (openKeyboard) {
                                inputFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                        onDownload = {
                            val file = state.getSingleSelectedFile()
                            val music = state.getSingleSelectedMusic()
                            val videoCircle = state.getSingleSelectedVideoCircle()
                            viewModel.clearSelection()
                            scope.launch {
                                val ok = when {
                                    file != null -> saveFileToDeviceDownloads(context, file)
                                    music != null -> saveMusicToDeviceMusic(context, music)
                                    videoCircle != null -> true
                                    else -> false
                                }
                                Toast.makeText(
                                    context,
                                    if (ok) "Сохранено на устройство" else "Не удалось сохранить",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShare = {
                            val selectedId = state.selection.firstOrNull()
                            val msg = state.messages.firstOrNull { it.id == selectedId }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, msg?.text?.ifBlank { "AnonGram Media" } ?: "AnonGram Media")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться"))
                            viewModel.clearSelection()
                        },
                        onForward = {
                            forwardingMessageIds = state.selection.toList()
                        },
                        onDelete = {
                            viewModel.requestDelete(state.selection.toList())
                        }
                    )
                }

                state.isSearchOpen -> {
                    ChatSearchTopBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClose = viewModel::closeSearch
                    )
                }

                else -> {
                    ChatTopBar(
                        session = session,
                        isPeerTyping = state.isPeerTyping,
                        peerActivity = state.peerActivity,
                        selfDestruct = state.selfDestruct,
                        onBack = onBack,
                        onOpenSearch = viewModel::openSearch,
                        onCycleWallpaper = { isWallpaperOpen = true },
                        onOpenKeyCheck = viewModel::openKeyCheck,
                        onSelfDestruct = {
                            val next = when (state.selfDestruct) {
                                SelfDestruct.OFF -> SelfDestruct.S5
                                SelfDestruct.S5 -> SelfDestruct.M1
                                SelfDestruct.M1 -> SelfDestruct.H1
                                SelfDestruct.H1 -> SelfDestruct.D1
                                SelfDestruct.D1 -> SelfDestruct.W1
                                SelfDestruct.W1 -> SelfDestruct.OFF
                            }
                            viewModel.setSelfDestruct(next)
                        },
                        onClearHistory = viewModel::clearHistory,
                        onDeleteChat = {
                            viewModel.clearHistory()
                            onBack()
                        },
                        onOpenNotifications = viewModel::openNotifications
                    )
                }
            }

            if (!state.isSearchOpen && !state.isSelectionMode) {
                state.pinnedMessage?.let { pinned ->
                    PinnedBar(
                        pinned = pinned,
                        currentIndex = state.pinnedIndex,
                        totalCount = state.pinnedIds.size.coerceAtLeast(1),
                        onNavigateToPinned = {
                            val targetId = if (state.pinnedIds.size > 1) {
                                viewModel.cycleNextPinnedMessage()
                            } else {
                                pinned.id
                            }
                            if (targetId != null) {
                                viewModel.highlightMessage(targetId)
                                val index = state.messages.asReversed().indexOfFirst { it.id == targetId }
                                if (index >= 0) {
                                    scope.launch {
                                        runCatching {
                                            listState.animateScrollAndCenterItem(index)
                                        }
                                    }
                                }
                            }
                        },
                        onOpenAllPinned = viewModel::openPinnedMessages
                    )
                }
            }

            if (!state.isSearchOpen && !state.isSelectionMode) {
                ChatMiniPlayer(
                    controller = audioController,
                    onOpenSheet = {
                        isMusicSheetOpen = true
                        audioController.openSheet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }

        // Полноэкранный редактор текста (расширенный ввод)
        if (isFullEditorOpen) {
            ChatFullTextEditorScreen(
                initialText = state.input,
                onClose = { textAfterEdit ->
                    viewModel.onInputChange(textAfterEdit)
                    isFullEditorOpen = false
                },
                onOpenNativeAttach = {
                    if (isKeyboardVisible) {
                        wasKeyboardOpenWhenAttached = true
                        frozenKeyboardHeightPx = lastKeyboardHeightPx
                    } else {
                        wasKeyboardOpenWhenAttached = false
                        frozenKeyboardHeightPx = 0
                    }
                    viewModel.openAttach()
                },
                onSend = { textToSend, format, language, attachment ->
                    val formattedToSend = when (format) {
                        TextStyleFormat.CODE -> {
                            if (textToSend.startsWith("```")) textToSend
                            else "```$language\n$textToSend\n```"
                        }
                        TextStyleFormat.SPOILER -> {
                            if (textToSend.contains("||")) textToSend
                            else "||$textToSend||"
                        }
                        TextStyleFormat.HEADING -> {
                            if (textToSend.startsWith("#")) textToSend
                            else "# $textToSend"
                        }
                        TextStyleFormat.QUOTE -> {
                            if (textToSend.startsWith(">")) textToSend
                            else "> $textToSend"
                        }
                        TextStyleFormat.CALLOUT -> {
                            if (textToSend.startsWith(">>>")) textToSend
                            else ">>> $textToSend"
                        }
                        else -> textToSend
                    }
                    if (attachment != null) {
                        viewModel.sendAttachment(attachment, caption = formattedToSend)
                    } else {
                        viewModel.onInputChange(formattedToSend)
                        viewModel.send()
                    }
                    isFullEditorOpen = false
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
        }

        // Модальное меню вложений Telegram-style (скрепка, ТЗ)
        if (state.isAttachOpen) {
            AttachSheet(
                visible = true,
                chatId = session.id,
                recipientName = session.title,
                keyboardHeightPx = frozenKeyboardHeightPx,
                wasKeyboardOpen = wasKeyboardOpenWhenAttached,
                onRestoreKeyboard = {
                    try {
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: Exception) {}
                },
                onDismiss = {
                    wasKeyboardOpenWhenAttached = false
                    viewModel.closeAttach()
                },
                onSend = { drafts ->
                    // 1. Сжатые медиа с группировкой (до 10 шт. в альбом)
                    val groupedMedia = drafts.filterIsInstance<AttachmentDraft.Media>().filter { !it.ungrouped }
                    if (groupedMedia.isNotEmpty()) {
                        val anyCaption = groupedMedia.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
                        val mediaChunks = groupedMedia.chunked(10)
                        mediaChunks.forEachIndexed { chunkIdx, chunk ->
                            val isLastChunk = (chunkIdx == mediaChunks.lastIndex)
                            val chunkCaption = if (isLastChunk) anyCaption else ""
                            if (chunk.size == 1) {
                                val m = chunk[0]
                                viewModel.sendAttachment(
                                    MessageAttachment.Photo(
                                        label = "",
                                        colorSeed = 1,
                                        isVideo = m.isVideo,
                                        uri = m.uri.toString(),
                                        hasSpoiler = m.hasSpoiler
                                    ),
                                    caption = chunkCaption
                                )
                            } else {
                                viewModel.sendAttachment(
                                    MessageAttachment.MediaAlbum(
                                        items = chunk.map { m ->
                                            MessageAttachment.AlbumMediaItem(
                                                uri = m.uri.toString(),
                                                isVideo = m.isVideo,
                                                hasSpoiler = m.hasSpoiler
                                            )
                                        },
                                        hasSpoiler = chunk.any { it.hasSpoiler }
                                    ),
                                    caption = chunkCaption
                                )
                            }
                        }
                    }

                    // 2. Сжатые медиа «Отправить без группировки» — каждое отдельным пузырём
                    val ungroupedMedia = drafts.filterIsInstance<AttachmentDraft.Media>().filter { it.ungrouped }
                    ungroupedMedia.forEach { draft ->
                        viewModel.sendAttachment(
                            MessageAttachment.Photo(
                                label = "",
                                colorSeed = 1,
                                isVideo = draft.isVideo,
                                uri = draft.uri.toString(),
                                hasSpoiler = draft.hasSpoiler
                            ),
                            caption = draft.caption
                        )
                    }

                    // 3. Файлы и фото без сжатия — слитно группами по 10 штук (свыше 10 — отдельная группа)
                    val fileDrafts = drafts.filterIsInstance<AttachmentDraft.File>()
                    if (fileDrafts.isNotEmpty()) {
                        val anyFileCaption = fileDrafts.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
                        val fileChunks = fileDrafts.chunked(10)
                        fileChunks.forEachIndexed { chunkIdx, chunk ->
                            val isLastChunk = (chunkIdx == fileChunks.lastIndex)
                            val chunkCaption = if (isLastChunk) anyFileCaption else ""
                            val mappedFiles = chunk.map { draft ->
                                val ext = draft.name.substringAfterLast('.', "").lowercase()
                                val kind = when (ext) {
                                    "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> FileKind.IMAGE
                                    "pdf" -> FileKind.PDF
                                    "apk" -> FileKind.APK
                                    "doc", "docx", "txt", "rtf" -> FileKind.DOC
                                    "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
                                    else -> FileKind.UNKNOWN
                                }
                                val kb = draft.size / 1024.0
                                val sizeLabel = if (kb >= 1024.0) {
                                    String.format(Locale.forLanguageTag("ru"), "%.1f MB", kb / 1024.0)
                                } else {
                                    String.format(Locale.forLanguageTag("ru"), "%.1f KB", kb.coerceAtLeast(0.1))
                                }
                                MessageAttachment.File(
                                    name = draft.name,
                                    sizeLabel = sizeLabel,
                                    kind = kind,
                                    uri = draft.uri.toString()
                                )
                            }
                            if (mappedFiles.size == 1) {
                                viewModel.sendAttachment(mappedFiles[0], caption = chunkCaption)
                            } else {
                                viewModel.sendAttachment(
                                    MessageAttachment.FileGroup(items = mappedFiles),
                                    caption = chunkCaption
                                )
                            }
                        }
                    }

                    // 4. Музыка — слитно группами по 10 треков
                    val audioDrafts = drafts.filterIsInstance<AttachmentDraft.Audio>()
                    if (audioDrafts.isNotEmpty()) {
                        val anyAudioCaption = audioDrafts.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
                        val audioChunks = audioDrafts.chunked(10)
                        audioChunks.forEachIndexed { chunkIdx, chunk ->
                            val isLastChunk = (chunkIdx == audioChunks.lastIndex)
                            val chunkCaption = if (isLastChunk) anyAudioCaption else ""
                            val mappedTracks = chunk.mapIndexed { idx, draft ->
                                val min = draft.durationMs / 60000
                                val sec = (draft.durationMs % 60000) / 1000
                                val durLabel = String.format("%d:%02d", min, sec)
                                MessageAttachment.Music(
                                    title = draft.title,
                                    artist = draft.artist,
                                    durationLabel = durLabel,
                                    colorSeed = (idx % 6) + 1,
                                    uri = draft.uri.toString()
                                )
                            }
                            if (mappedTracks.size == 1) {
                                viewModel.sendAttachment(mappedTracks[0], caption = chunkCaption)
                            } else {
                                viewModel.sendAttachment(
                                    MessageAttachment.MusicGroup(items = mappedTracks),
                                    caption = chunkCaption
                                )
                            }
                        }
                    }

                    // 5. Контакты и геопозиции — каждый контакт отправляется ОТДЕЛЬНО (не слитно!)
                    drafts.forEach { draft ->
                        when (draft) {
                            is AttachmentDraft.Location -> {
                                viewModel.sendAttachment(
                                    MessageAttachment.Geo(address = draft.address ?: "${draft.lat}, ${draft.lon}")
                                )
                            }
                            is AttachmentDraft.LiveLocation -> {
                                viewModel.sendAttachment(
                                    MessageAttachment.Geo(address = "Трансляция геопозиции (${draft.durationMin} мин.)")
                                )
                            }
                            is AttachmentDraft.Contact -> {
                                viewModel.sendAttachment(
                                    MessageAttachment.Contact(
                                        name = draft.name,
                                        phone = draft.phones.firstOrNull() ?: ""
                                    )
                                )
                            }
                            else -> Unit
                        }
                    }
                    viewModel.closeAttach()
                }
            )
        }

        // Экран выбора геопозиции (шаг 5)
        if (state.isGeoOpen) {
            GeoPickerScreen(
                onBack = viewModel::closeGeoPicker,
                onSendGeo = { geoAttachment ->
                    viewModel.sendAttachment(geoAttachment)
                    viewModel.closeGeoPicker()
                }
            )
        }

        // Меню действий по тапу на сообщение прямо рядом с пузырём (Фото 2)
        state.actionMenuFor?.let { message ->
            MessageContextMenu(
                message = message,
                anchorYPx = actionMenuAnchorY,
                isPinned = state.pinnedIds.contains(message.id),
                onReaction = { emoji ->
                    viewModel.toggleReaction(message.id, emoji)
                    viewModel.closeActionMenu()
                },
                onReply = {
                    viewModel.startReply(message)
                    inputFocusRequester.requestFocus()
                    keyboardController?.show()
                },
                onCopy = {
                    clipboard.setText(AnnotatedString(message.text))
                    viewModel.closeActionMenu()
                },
                onEdit = {
                    viewModel.startEdit(message)
                    val att = message.attachment
                    if (att !is MessageAttachment.Voice && att !is MessageAttachment.VideoCircle) {
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                onForward = {
                    viewModel.closeActionMenu()
                    forwardingMessageIds = listOf(message.id)
                },
                onPin = { viewModel.pinMessage(message.id) },
                onDelete = { viewModel.requestDelete(listOf(message.id)) },
                onDismiss = viewModel::closeActionMenu
            )
        }

        // Полноэкранный экран «Переслать...» (актуальные чаты + «Избранное» + глобальный поиск по серверу)
        forwardingMessageIds?.let { idsToForward ->
            ForwardTargetPickerScreen(
                activeSessions = activeSessions,
                onClose = { forwardingMessageIds = null },
                onSelectSavedMessages = {
                    forwardingMessageIds = null
                    val savedExisting = activeSessions.firstOrNull {
                        it.id == "0" || it.title.equals("Избранное", ignoreCase = true)
                    }
                    if (savedExisting != null) {
                        viewModel.forwardMessagesToTargetChat(
                            targetSessionId = savedExisting.id,
                            messageIds = idsToForward,
                            authorName = session.title
                        ) {
                            onOpenTargetSession(savedExisting)
                        }
                    } else {
                        onOpenDirectPeer("Избранное", "saved_messages") { createdSaved ->
                            viewModel.forwardMessagesToTargetChat(
                                targetSessionId = createdSaved.id,
                                messageIds = idsToForward,
                                authorName = session.title
                            ) {
                                onOpenTargetSession(createdSaved)
                            }
                        }
                    }
                },
                onSelectSession = { targetSession ->
                    forwardingMessageIds = null
                    viewModel.forwardMessagesToTargetChat(
                        targetSessionId = targetSession.id,
                        messageIds = idsToForward,
                        authorName = session.title
                    ) {
                        onOpenTargetSession(targetSession)
                    }
                },
                onSelectGlobalItem = { name, handle ->
                    forwardingMessageIds = null
                    onOpenDirectPeer(name, handle) { createdSession ->
                        viewModel.forwardMessagesToTargetChat(
                            targetSessionId = createdSession.id,
                            messageIds = idsToForward,
                            authorName = session.title
                        ) {
                            onOpenTargetSession(createdSession)
                        }
                    }
                }
            )
        }

        // Полноэкранный просмотр фото и альбомов из чата + автовозврат клавиатуры
        fun closeMediaViewerAndRestoreKeyboard() {
            viewingMediaMessage = null
            viewingOverrideSingleUri = null
            if (wasKeyboardOpenBeforeViewer) {
                wasKeyboardOpenBeforeViewer = false
                scope.launch {
                    delay(80)
                    try {
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: Exception) {}
                }
            }
        }

        LaunchedEffect(state.messages, viewingMediaMessage) {
            val current = viewingMediaMessage
            if (current != null) {
                val updated = state.messages.firstOrNull { it.id == current.id }
                if (updated == null) {
                    closeMediaViewerAndRestoreKeyboard()
                } else if (updated != current) {
                    viewingMediaMessage = updated
                }
            }
        }

        viewingMediaMessage?.let { mediaMessage ->
            ChatMediaFullscreenViewer(
                message = mediaMessage,
                initialPhotoIndex = viewingMediaInitialIndex,
                peerName = session.title,
                allChatMessages = state.messages,
                overrideSingleUri = viewingOverrideSingleUri,
                onClose = {
                    closeMediaViewerAndRestoreKeyboard()
                },
                onShowInChat = { msg ->
                    closeMediaViewerAndRestoreKeyboard()
                    viewModel.highlightMessage(msg.id)
                    val index = state.messages.asReversed().indexOfFirst { it.id == msg.id }
                    if (index >= 0) {
                        scope.launch {
                            runCatching {
                                listState.animateScrollAndCenterItem(index)
                            }
                        }
                    }
                },
                onReply = { msg, subIdx ->
                    viewingMediaMessage = null
                    viewingOverrideSingleUri = null
                    wasKeyboardOpenBeforeViewer = false
                    viewModel.startReply(msg, subIdx)
                    scope.launch {
                        delay(80)
                        try {
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        } catch (_: Exception) {}
                    }
                },
                onRequestDelete = { msg, photoIdx ->
                    when (msg.attachment) {
                        is MessageAttachment.MediaAlbum -> {
                            viewModel.requestDelete(listOf("${msg.id}#photo_$photoIdx"))
                        }
                        is MessageAttachment.FileGroup -> {
                            viewModel.requestDelete(listOf("${msg.id}#file_$photoIdx"))
                        }
                        else -> {
                            viewModel.requestDelete(listOf(msg.id))
                        }
                    }
                }
            )
        }

        // Диалог удаления сообщений (шаг 9)
        state.deleteDialogFor?.let { targets ->
            DeleteMessageDialog(
                count = targets.size,
                peerName = session.title,
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::dismissDeleteDialog
            )
        }

        // Полноэкранный раздел «Особые уведомления» (Фото 1)
        if (state.isNotificationsOpen) {
            NotificationsSettingsDialog(
                peerName = session.title,
                onDismiss = viewModel::closeNotifications
            )
        }

        // Экран закреплённых сообщений (Фото 5)
        if (state.isPinnedMessagesScreenOpen) {
            PinnedMessagesScreen(
                chatId = session.id,
                pinnedMessages = state.pinnedMessages,
                peerName = session.title,
                selection = state.selection,
                limitWarning = state.limitWarning,
                onBack = viewModel::closePinnedMessages,
                onMessageClick = { message ->
                    if (state.isSelectionMode) {
                        viewModel.toggleSelection(message.id)
                    } else {
                        viewModel.closePinnedMessages()
                        viewModel.highlightMessage(message.id)
                        val index = state.messages.asReversed().indexOfFirst { it.id == message.id }
                        if (index >= 0) {
                            scope.launch {
                                runCatching {
                                    listState.animateScrollAndCenterItem(index)
                                }
                            }
                        }
                    }
                },
                onMessageLongClick = { message ->
                    viewModel.enterSelection(message.id)
                },
                onSelectMessage = viewModel::toggleSelection,
                onSwipeReply = viewModel::startReply,
                onToggleReaction = viewModel::toggleReaction,
                onUnpinAll = viewModel::unpinAllMessages,
                onTriggerLimitDemo = viewModel::triggerDemoPinnedLimitWarning,
                onDismissLimitWarning = viewModel::dismissLimitWarning
            )
        }

        // Диалог верификации ключей шифрования (шаг 18)
        if (state.isKeyCheckOpen) {
            KeyVerificationDialog(
                peerName = session.title,
                onDismiss = viewModel::closeKeyCheck
            )
        }

        // Экран/окно выбора обоев (Telegram-style, модуль wallpapers)
        if (isWallpaperOpen) {
            WallpaperRoute(
                chatId = session.id,
                onDismiss = { isWallpaperOpen = false }
            )
        }

        // Превью ответа на сообщение при долгом зажатии (Фото 2)
        if (isReplyPreviewOpen && state.replyTo != null) {
            ReplyPreviewDialog(
                chatId = session.id,
                replyMessage = state.replyTo!!,
                replyToSubIndex = state.replyToSubIndex,
                peerName = session.title,
                onRemoveReply = {
                    viewModel.cancelReply()
                    isReplyPreviewOpen = false
                },
                onJumpToMessage = {
                    val targetId = state.replyTo?.id
                    if (targetId != null) {
                        viewModel.highlightMessage(targetId)
                        val index = state.messages.asReversed().indexOfFirst { it.id == targetId }
                        if (index >= 0) {
                            scope.launch {
                                runCatching {
                                    listState.animateScrollAndCenterItem(index)
                                }
                            }
                        }
                    }
                    isReplyPreviewOpen = false
                },
                onDismiss = { isReplyPreviewOpen = false }
            )
        }

        // Модальное окно замены вложения «1 на 1» (строгий равноценный обмен без таббара)
        if (isReplaceAttachSheetOpen) {
            AttachSheet(
                visible = true,
                chatId = session.id,
                recipientName = session.title,
                initialTab = replaceInitialTab,
                isSingleReplaceMode = true,
                onSingleReplace = { draft ->
                    isReplaceAttachSheetOpen = false
                    val (newAttachment, sheetCaption) = when (draft) {
                        is AttachmentDraft.Media -> MessageAttachment.Photo(
                            label = "",
                            colorSeed = 1,
                            isVideo = draft.isVideo,
                            uri = draft.uri.toString(),
                            hasSpoiler = draft.hasSpoiler
                        ) to draft.caption
                        is AttachmentDraft.File -> {
                            val ext = draft.name.substringAfterLast('.', "").lowercase()
                            val kind = when (ext) {
                                "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> FileKind.IMAGE
                                "pdf" -> FileKind.PDF
                                "apk" -> FileKind.APK
                                "doc", "docx", "txt", "rtf" -> FileKind.DOC
                                "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
                                else -> FileKind.UNKNOWN
                            }
                            val kb = draft.size / 1024.0
                            val sizeLabel = if (kb >= 1024.0) {
                                String.format(Locale.forLanguageTag("ru"), "%.1f MB", kb / 1024.0)
                            } else {
                                String.format(Locale.forLanguageTag("ru"), "%.1f KB", kb.coerceAtLeast(0.1))
                            }
                            MessageAttachment.File(
                                name = draft.name,
                                sizeLabel = sizeLabel,
                                kind = kind,
                                uri = draft.uri.toString()
                            ) to draft.caption
                        }
                        is AttachmentDraft.Audio -> {
                            val min = draft.durationMs / 60000
                            val sec = (draft.durationMs % 60000) / 1000
                            val durLabel = String.format("%d:%02d", min, sec)
                            MessageAttachment.Music(
                                title = draft.title,
                                artist = draft.artist,
                                durationLabel = durLabel,
                                colorSeed = 1,
                                uri = draft.uri.toString()
                            ) to draft.caption
                        }
                        else -> null to null
                    }
                    if (newAttachment != null) {
                        viewModel.replaceEditingAttachment(newAttachment, sheetCaption?.ifBlank { null })
                    }
                },
                onDismiss = { isReplaceAttachSheetOpen = false },
                onSend = { /* handled in onSingleReplace */ }
            )
        }

        // Предупреждение о превышении лимита 99 сообщений (шаг 12)
        AnimatedVisibility(
            visible = state.limitWarning != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeNavigationBarPadding()
                .padding(bottom = if (isKeyboardVisible) 80.dp else 100.dp, start = 20.dp, end = 20.dp)
        ) {
            state.limitWarning?.let { warningText ->
                Box(
                    modifier = Modifier
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp), spotColor = CrimsonKillSwitch)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardDark)
                        .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .clickable { viewModel.dismissLimitWarning() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = CrimsonKillSwitch,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = warningText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 6. Шторка музыкального плеера чата
        if (isMusicSheetOpen || isAudioSheetOpen) {
            ChatMusicPlayerSheet(
                controller = audioController,
                onDismiss = {
                    isMusicSheetOpen = false
                    audioController.closeSheet()
                },
                onForwardTrack = { track ->
                    isMusicSheetOpen = false
                    audioController.closeSheet()
                    forwardingMessageIds = listOf(track.id.substringBefore("_"))
                },
                onShareTrack = { track ->
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "${track.performer} - ${track.title}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться треком"))
                },
                onShowInChat = { track ->
                    isMusicSheetOpen = false
                    audioController.closeSheet()
                    val msgId = track.id.substringBefore("_")
                    viewModel.highlightMessage(msgId)
                    val index = state.messages.asReversed().indexOfFirst { it.id == msgId }
                    if (index >= 0) {
                        scope.launch {
                            runCatching { listState.animateScrollAndCenterItem(index) }
                        }
                    }
                }
            )
        }

        // 7. Полноэкранный плеер для музыки в стиле iOS
        if (isAudioFullScreenOpen) {
            MusicFullScreenPlayer(
                controller = audioController,
                onDismiss = { audioController.closeFullScreen() },
                onForwardTrack = { track ->
                    audioController.closeFullScreen()
                    forwardingMessageIds = listOf(track.id.substringBefore("_"))
                },
                onJumpToMessage = { msgId ->
                    audioController.closeFullScreen()
                    viewModel.highlightMessage(msgId)
                    val index = state.messages.asReversed().indexOfFirst { it.id == msgId }
                    if (index >= 0) {
                        scope.launch {
                            runCatching { listState.animateScrollAndCenterItem(index) }
                        }
                    }
                }
            )
        }

        // 9. Модальное окно просмотра длинного текста сообщения (>4000 симв) / блока кода
        fullTextMessageModal?.let { msg ->
            MessageFullTextViewerModal(
                message = msg,
                onClose = { fullTextMessageModal = null }
            )
        }
    }
}

@Composable
private fun ChatHeaderScrim(
    listState: LazyListState,
    scrimHeight: androidx.compose.ui.unit.Dp,
    isBlurred: Boolean,
    modifier: Modifier = Modifier
) {
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10
        }
    }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.88f else 0.58f,
        animationSpec = tween(durationMillis = 220),
        label = "headerScrimAlpha"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scrimHeight)
            .then(if (isBlurred) Modifier.blur(22.dp) else Modifier)
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = headerScrimAlpha),
                    0.65f to Color.Black.copy(alpha = headerScrimAlpha * 0.65f),
                    1.0f to Color.Transparent
                )
            )
    )
}

@Composable
private fun ChatScrollDownOverlay(
    listState: LazyListState,
    isSearchOpen: Boolean,
    unreadBadge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showScrollDown by remember(listState, isSearchOpen) {
        derivedStateOf {
            !isSearchOpen && listState.firstVisibleItemIndex > 2
        }
    }
    ScrollDownButton(
        visible = showScrollDown,
        unreadBadge = unreadBadge,
        onClick = onClick,
        modifier = modifier
    )
}
