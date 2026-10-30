package com.anongram.app.features.chats.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
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
import com.anongram.app.features.chats.ui.components.AttachBottomSheet
import com.anongram.app.features.chats.ui.components.ChatInputBar
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
import com.anongram.app.features.chats.ui.components.ReplyPreviewDialog
import com.anongram.app.features.chats.ui.components.ScrollDownButton
import com.anongram.app.features.wallpapers.api.WallpaperRoute
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.delay
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
    var isWallpaperOpen by remember { mutableStateOf(false) }
    var isReplyPreviewOpen by remember { mutableStateOf(false) }
    var isSearchListMode by remember { mutableStateOf(false) }
    var viewingMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingMediaInitialIndex by remember { mutableIntStateOf(0) }
    var viewingOverrideSingleUri by remember { mutableStateOf<String?>(null) }
    var wasKeyboardOpenBeforeViewer by remember { mutableStateOf(false) }
    var forwardingMessageIds by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(state.isSearchOpen) {
        if (!state.isSearchOpen) {
            isSearchListMode = false
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10
        }
    }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.88f else 0.58f,
        animationSpec = tween(durationMillis = 220),
        label = "headerScrimAlpha"
    )
    var actionMenuAnchorY by remember { mutableFloatStateOf(0f) }

    BackHandler {
        when {
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
            state.replyTo != null -> viewModel.cancelReply()
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

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex <= 1 && state.unreadBadge > 0) {
            viewModel.clearUnreadBadge()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        WallpaperBackground(
            chatId = session.id,
            modifier = Modifier.matchParentSize()
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

        val isChatImeFrozen = state.isAttachOpen && wasKeyboardOpenWhenAttached
        val chatBottomImePx = if (isChatImeFrozen) frozenKeyboardHeightPx else currentImeBottom
        val effectiveKeyboardVisible = if (isChatImeFrozen) true else isKeyboardVisible

        val topListPadding = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 130.dp else 86.dp
        val bottomListPadding = if (effectiveKeyboardVisible) 80.dp else 104.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isReplyPreviewOpen) Modifier.blur(20.dp) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                    onSwipeReply = viewModel::startReply,
                    onToggleReaction = viewModel::toggleReaction,
                    onSelectMessage = viewModel::toggleSelection,
                    onToggleAlbumPhoto = viewModel::toggleAlbumPhotoSelection,
                    onToggleFileGroupItem = viewModel::toggleFileGroupItemSelection,
                    onToggleMusicGroupItem = viewModel::toggleMusicGroupItemSelection,
                    onEnterAlbumPhotoSelection = { key ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.enterSelection(key)
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
                    }
                )
            }

            ScrollDownButton(
                visible = !state.isSearchOpen && listState.firstVisibleItemIndex > 2,
                unreadBadge = state.unreadBadge,
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
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

        // 1b. Парящее поле ввода поверх списка сообщений
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = with(density) { chatBottomImePx.toDp() })
                .safeNavigationBarPadding()
                .padding(bottom = if (effectiveKeyboardVisible) 4.dp else 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                    ChatInputBar(
                        input = state.input,
                        canSend = state.canSend,
                        replyTo = state.replyTo,
                        peerName = session.title,
                        editing = state.editing,
                        recording = state.recording,
                        isCameraMode = state.isCameraMode,
                        isEmojiOpen = state.isEmojiOpen,
                        onInputChange = viewModel::onInputChange,
                        onSend = {
                            viewModel.send()
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        onCancelReply = viewModel::cancelReply,
                        onCancelEdit = viewModel::cancelEdit,
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
                        onToggleEmoji = viewModel::toggleEmojiPanel,
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
                        onToggleCameraMode = viewModel::toggleCameraMode,
                        onStartRecording = viewModel::startRecording,
                        onLockRecording = viewModel::lockRecording,
                        onCancelRecording = viewModel::cancelRecording,
                        onStopLockedRecording = viewModel::stopLockedRecording,
                        focusRequester = inputFocusRequester
                    )

                    // Панель эмодзи/стикеров под полем ввода (шаг 6)
                    if (state.isEmojiOpen) {
                        EmojiStickerPanel(
                            onEmojiClick = viewModel::appendEmoji,
                            onStickerClick = { symbol, colorSeed ->
                                viewModel.sendSticker(symbol, colorSeed)
                            }
                        )
                    }
                }
            }
        }

        // 2. Мягкая тёмная вуаль-градиент за плавающей шапкой (усиливается при скролле)
        val scrimHeight = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 144.dp else 100.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(scrimHeight)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = headerScrimAlpha),
                        0.65f to Color.Black.copy(alpha = headerScrimAlpha * 0.65f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // 3. Плавающая шапка чата поверх сообщений (парящие островки)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .safeStatusBarPadding()
        ) {
            when {
                state.isSelectionMode -> {
                    SelectionPanel(
                        selectedCount = state.selection.size,
                        onClose = viewModel::clearSelection,
                        onCopy = {
                            val combined = state.selectedMessages.joinToString("\n") { it.text }
                            clipboard.setText(AnnotatedString(combined))
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
        }
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
                onReply = { viewModel.startReply(message) },
                onCopy = {
                    clipboard.setText(AnnotatedString(message.text))
                    viewModel.closeActionMenu()
                },
                onEdit = { viewModel.startEdit(message) },
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
                onReply = { msg ->
                    viewingMediaMessage = null
                    viewingOverrideSingleUri = null
                    wasKeyboardOpenBeforeViewer = false
                    viewModel.startReply(msg)
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
    }
}
