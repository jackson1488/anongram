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
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.attach.api.AttachSheet
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.player.api.MiniPlayer
import com.anongram.app.features.player.api.FullPlayerSheet
import com.anongram.app.features.chats.ui.components.AttachBottomSheet
import com.anongram.app.features.chats.ui.components.ChatInputBar
import com.anongram.app.features.chats.ui.components.DeleteMessageDialog
import com.anongram.app.features.chats.ui.components.EmojiStickerPanel
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
import kotlinx.coroutines.launch

/**
 * Экран чата Telegram-style (все 18 шагов):
 * - Обои с узором и стилями (ChatWallpaper)
 * - Плавающие таблетки тулбара (ChatTopBar), поиск (ChatSearchPanel), выборка (SelectionPanel)
 * - Закреплённое сообщение (PinnedBar)
 * - Список сообщений с группировкой, дат-чипами, непрочитанными, реакциями, свайпом ответа (MessagesList)
 * - Кнопка прокрутки вниз со счётчиком непрочитанных (ScrollDownButton)
 * - Поле ввода со скрепкой, переключением микрофон/кружок, свайп-замком (ChatInputBar)
 * - Панель эмодзи, GIF и стикеров (EmojiStickerPanel)
 * - Модальное меню вложений (AttachBottomSheet)
 * - Экран выбора геопозиции (GeoPickerScreen)
 * - Контекстное меню сообщений по долгому тапу (MessageContextMenu)
 * - Диалог подтверждения удаления (DeleteMessageDialog)
 * - Настройки уведомлений чата (NotificationsSettingsDialog)
 * - Проверка сквозного шифрования (KeyVerificationDialog)
 */
@Composable
fun ChatScreen(
    session: IChatSession,
    viewModel: ChatMessagesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var isWallpaperOpen by remember { mutableStateOf(false) }
    var isReplyPreviewOpen by remember { mutableStateOf(false) }
    var isSearchListMode by remember { mutableStateOf(false) }
    var isFullPlayerFromChatOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSearchOpen) {
        if (!state.isSearchOpen) {
            isSearchListMode = false
        }
    }

    // Затемнение за шапкой: 0.58f по дефолту (при простое), усиливается до 0.88f при скролле
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

    // Обработка системной кнопки назад: закрывать открытые панели и модалки по очереди
    BackHandler {
        when {
            isFullPlayerFromChatOpen -> isFullPlayerFromChatOpen = false
            isWallpaperOpen -> isWallpaperOpen = false
            isReplyPreviewOpen -> isReplyPreviewOpen = false
            isSearchListMode -> isSearchListMode = false
            state.isSearchOpen -> viewModel.closeSearch()
            state.isSelectionMode -> viewModel.clearSelection()
            state.isEmojiOpen -> viewModel.closeEmojiPanel()
            state.isAttachOpen -> viewModel.closeAttach()
            state.isGeoOpen -> viewModel.closeGeoPicker()
            state.actionMenuFor != null -> viewModel.closeActionMenu()
            state.deleteDialogFor != null -> viewModel.dismissDeleteDialog()
            state.isNotificationsOpen -> viewModel.closeNotifications()
            state.isKeyCheckOpen -> viewModel.closeKeyCheck()
            state.editing != null -> viewModel.cancelEdit()
            state.replyTo != null -> viewModel.cancelReply()
            else -> onBack()
        }
    }

    // Автопрокрутка к результатам поиска с анимацией двойного сердцебиения
    LaunchedEffect(state.searchIndex, state.searchMatches) {
        val currentMatchId = state.searchMatches.getOrNull(state.searchIndex)
        if (currentMatchId != null) {
            val matchIndex = state.messages.asReversed().indexOfFirst { it.id == currentMatchId }
            if (matchIndex >= 0) {
                listState.animateScrollAndCenterItem(matchIndex)
                viewModel.highlightMessage(currentMatchId)
            }
        }
    }

    LaunchedEffect(state.isSearchOpen, state.searchQuery) {
        if (!state.isSearchOpen || state.searchQuery.isBlank()) {
            isSearchListMode = false
        }
    }

    // Автопрокрутка вниз при отправке сообщения или в режиме реального общения
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

    // Автоочистка горящего бейджа новых сообщений при скролле до самого низа
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
        val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

        // 1. Содержимое чата: сообщения во всю высоту, скроллятся ПОД плавающей шапкой и ПОД полем ввода
        val topListPadding = if (state.pinnedMessage != null && !state.isSearchOpen && !state.isSelectionMode) 130.dp else 86.dp
        val bottomListPadding = if (isKeyboardVisible) 80.dp else 104.dp

        // Контейнер чата (сообщения, плавающая шапка и поле ввода) — блюрится при открытии превью ответа
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isReplyPreviewOpen) Modifier.blur(20.dp) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
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
                        val matchIdx = state.searchMatches.indexOf(message.id)
                        if (matchIdx >= 0) {
                            viewModel.jumpToSearchMatch(matchIdx)
                        } else {
                            viewModel.highlightMessage(message.id)
                        }
                        val idx = state.messages.asReversed().indexOfFirst { it.id == message.id }
                        if (idx >= 0) {
                            scope.launch { listState.animateScrollAndCenterItem(idx) }
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
                    onRetryMessage = viewModel::retry,
                    onHighlightMessage = viewModel::highlightMessage
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

        // 1b. Парящее поле ввода поверх списка сообщений (область вокруг прозрачная, сообщения проплывают сквозь неё)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .safeNavigationBarPadding()
                .padding(bottom = if (isKeyboardVisible) 4.dp else 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isSearchOpen) {
                    val counterText = if (isSearchListMode) {
                        formatResultsCount(state.searchMatches.size)
                    } else {
                        state.searchCounter
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
                            val index = state.messages.asReversed().indexOfFirst { it.id == replyMsg.id }
                            if (index >= 0) {
                                scope.launch { listState.animateScrollAndCenterItem(index) }
                            }
                            viewModel.highlightMessage(replyMsg.id)
                        },
                        onReplyLongClick = {
                            isReplyPreviewOpen = true
                        },
                        onToggleEmoji = viewModel::toggleEmojiPanel,
                        onOpenAttach = viewModel::openAttach,
                        onToggleCameraMode = viewModel::toggleCameraMode,
                        onStartRecording = viewModel::startRecording,
                        onLockRecording = viewModel::lockRecording,
                        onCancelRecording = viewModel::cancelRecording,
                        onStopLockedRecording = viewModel::stopLockedRecording
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
                            viewModel.forwardMessages(state.selection, "Вы")
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

            // Плавающий мини-плеер под шапкой (ТЗ: чат кладёт MiniPlayer)
            MiniPlayer(
                onExpand = { isFullPlayerFromChatOpen = true },
                modifier = Modifier.fillMaxWidth()
            )

            // Закреплённое сообщение (шаг 12, Фото 4) — не показывается в режиме поиска и режиме выбора
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
                                val index = state.messages.asReversed().indexOfFirst { it.id == targetId }
                                if (index >= 0) {
                                    scope.launch { listState.animateScrollAndCenterItem(index) }
                                }
                                viewModel.highlightMessage(targetId)
                            }
                        },
                        onOpenAllPinned = viewModel::openPinnedMessages
                    )
                }
            }
        }
    }

        // Нижнее окно полного плеера при открытии из мини-плеера в чате
        if (isFullPlayerFromChatOpen) {
            FullPlayerSheet(onDismiss = { isFullPlayerFromChatOpen = false })
        }

        // Модальное меню вложений Telegram-style (скрепка, ТЗ)
        if (state.isAttachOpen) {
            AttachSheet(
                visible = true,
                recipientName = session.title,
                onDismiss = viewModel::closeAttach,
                onSend = { drafts ->
                    drafts.forEach { draft ->
                        when (draft) {
                            is AttachmentDraft.Media -> {
                                val label = if (draft.caption.isNotBlank()) draft.caption else (if (draft.isVideo) "Видео" else "Фото")
                                viewModel.sendAttachment(
                                    MessageAttachment.Photo(label = label, colorSeed = 1, isVideo = draft.isVideo),
                                    caption = draft.caption
                                )
                            }
                            is AttachmentDraft.File -> {
                                val kind = when (draft.name.substringAfterLast('.', "").lowercase()) {
                                    "pdf" -> FileKind.PDF
                                    "doc", "docx" -> FileKind.DOC
                                    "zip", "rar", "7z" -> FileKind.ARCHIVE
                                    else -> FileKind.UNKNOWN
                                }
                                val sizeLabel = "${draft.size / 1024} КБ"
                                viewModel.sendAttachment(
                                    MessageAttachment.File(name = draft.name, sizeLabel = sizeLabel, kind = kind)
                                )
                            }
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
                                    MessageAttachment.Contact(name = draft.name, phone = draft.phones.firstOrNull() ?: "")
                                )
                            }
                            is AttachmentDraft.Audio -> {
                                val min = draft.durationMs / 60000
                                val sec = (draft.durationMs % 60000) / 1000
                                val durLabel = String.format("%d:%02d", min, sec)
                                viewModel.sendAttachment(
                                    MessageAttachment.Music(title = draft.title, artist = draft.artist, durationLabel = durLabel, colorSeed = 2)
                                )
                            }
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
                    viewModel.forwardMessages(listOf(message.id), session.title)
                },
                onPin = { viewModel.pinMessage(message.id) },
                onDelete = { viewModel.requestDelete(listOf(message.id)) },
                onDismiss = viewModel::closeActionMenu
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
                        viewModel.openActionMenu(message)
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
                        val index = state.messages.asReversed().indexOfFirst { it.id == targetId }
                        if (index >= 0) {
                            scope.launch { listState.animateScrollAndCenterItem(index) }
                        }
                        viewModel.highlightMessage(targetId)
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
