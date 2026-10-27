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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.SelfDestruct
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

    // Затемнение шапки при скролле ленты сообщений вверх
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10
        }
    }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.72f else 0.32f,
        animationSpec = tween(durationMillis = 220),
        label = "headerScrimAlpha"
    )

    // Обработка системной кнопки назад: закрывать открытые панели и модалки по очереди
    BackHandler {
        when {
            isWallpaperOpen -> isWallpaperOpen = false
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

    // Автопрокрутка к результатам поиска
    LaunchedEffect(state.searchIndex, state.searchMatches) {
        val currentMatchId = state.searchMatches.getOrNull(state.searchIndex)
        if (currentMatchId != null) {
            val matchIndex = state.messages.asReversed().indexOfFirst { it.id == currentMatchId }
            if (matchIndex >= 0) {
                listState.animateScrollToItem(matchIndex)
            }
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

        // 1. Содержимое чата: сообщения во всю высоту + поле ввода
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .safeNavigationBarPadding()
        ) {
            // Список сообщений занимает всё пространство от верха до поля ввода (сообщения скроллятся под плавающей шапкой)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val topListPadding = if (state.pinnedMessage != null) 124.dp else 80.dp
                MessagesList(
                    messages = state.messages,
                    peerName = session.title,
                    listState = listState,
                    selection = state.selection,
                    firstUnreadId = state.firstUnreadId,
                    topPadding = topListPadding,
                    onClick = { message ->
                        if (state.isSelectionMode) {
                            viewModel.toggleSelection(message.id)
                        } else {
                            viewModel.openActionMenu(message)
                        }
                    },
                    onLongClick = { message ->
                        viewModel.enterSelection(message.id)
                    },
                    onSwipeReply = viewModel::startReply,
                    onToggleReaction = viewModel::toggleReaction,
                    onSelectMessage = viewModel::toggleSelection,
                    onRetryMessage = viewModel::retry
                )

                ScrollDownButton(
                    visible = !state.isSearchOpen && listState.firstVisibleItemIndex > 2,
                    unreadBadge = state.unreadBadge,
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                        viewModel.clearUnreadBadge()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                )

                // Плавающие круглые кнопки поиска (вверх / вниз по найденным сообщениям) (Фото 6)
                if (state.isSearchOpen) {
                    ChatSearchNavigationButtons(
                        canNavigate = state.searchMatches.isNotEmpty(),
                        onPrevious = viewModel::previousSearchMatch,
                        onNext = viewModel::nextSearchMatch,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    )
                }
            }

            // В режиме поиска показываем нижний островок поиска (Фото 6), иначе — поле ввода (шаги 2, 3, 16)
            if (state.isSearchOpen) {
                ChatSearchBottomBar(
                    counter = state.searchCounter,
                    onCalendarClick = { },
                    onListClick = { }
                )
            } else {
                ChatInputBar(
                    input = state.input,
                    canSend = state.canSend,
                    replyTo = state.replyTo,
                    editing = state.editing,
                    recording = state.recording,
                    isCameraMode = state.isCameraMode,
                    isEmojiOpen = state.isEmojiOpen,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::send,
                    onCancelReply = viewModel::cancelReply,
                    onCancelEdit = viewModel::cancelEdit,
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

        // 2. Мягкая тёмная вуаль-градиент за плавающей шапкой (усиливается при скролле)
        val scrimHeight = if (state.pinnedMessage != null && !state.isSearchOpen) 144.dp else 100.dp
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
                        onOpenNotifications = viewModel::openNotificationsMuteDialog
                    )
                }
            }

            // Закреплённое сообщение (шаг 12, Фото 4) — не показывается в режиме поиска
            if (!state.isSearchOpen) {
                state.pinnedMessage?.let { pinned ->
                    PinnedBar(
                        text = pinned.text.ifBlank { "Вложение" },
                        onClick = viewModel::openPinnedMessages
                    )
                }
            }
        }

        // Модальное меню вложений (скрепка, шаги 4 и 5)
        if (state.isAttachOpen) {
            AttachBottomSheet(
                onDismiss = viewModel::closeAttach,
                onSendAttachment = { attachment, caption ->
                    viewModel.sendAttachment(attachment, caption)
                },
                onOpenGeoPicker = viewModel::openGeoPicker
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

        // Меню действий по тапу на сообщение (шаг 9, Фото 2)
        state.actionMenuFor?.let { message ->
            MessageContextMenu(
                message = message,
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

        // Мини-диалог управления уведомлениями (открывается из меню чата)
        if (state.isNotificationsMuteDialogOpen) {
            NotificationsMuteDialog(
                peerName = session.title,
                onMuteFor = { _ -> },
                onOpenCustomSettings = viewModel::openNotifications,
                onDismiss = viewModel::closeNotificationsMuteDialog
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
                onUnpinAll = viewModel::unpinAllMessages
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
    }
}
