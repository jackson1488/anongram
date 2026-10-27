package com.anongram.app.features.chats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.anongram.app.features.chats.ui.components.NotificationsSettingsDialog
import com.anongram.app.features.chats.ui.components.ScrollDownButton
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

    // Обработка системной кнопки назад: закрывать открытые панели и модалки по очереди
    BackHandler {
        when {
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
        ChatWallpaper(
            style = state.wallpaperStyle,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding()
        ) {
            // Верхняя панель: Режим выбора / Поиск / Плавающие таблетки чата
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
                    ChatSearchPanel(
                        query = state.searchQuery,
                        counter = state.searchCounter,
                        canNavigate = state.searchMatches.isNotEmpty(),
                        onQueryChange = viewModel::onSearchQueryChange,
                        onPrevious = viewModel::previousSearchMatch,
                        onNext = viewModel::nextSearchMatch,
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
                        onCycleWallpaper = viewModel::cycleWallpaper,
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

            // Закреплённое сообщение (шаг 12)
            state.pinnedMessage?.let { pinned ->
                PinnedBar(
                    text = pinned.text.ifBlank { "Вложение" },
                    onClick = {
                        val index = state.messages.asReversed().indexOfFirst { it.id == pinned.id }
                        if (index >= 0) {
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    },
                    onUnpin = viewModel::unpinMessage
                )
            }

            // Список сообщений + кнопка «вниз»
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MessagesList(
                    messages = state.messages,
                    peerName = session.title,
                    listState = listState,
                    selection = state.selection,
                    firstUnreadId = state.firstUnreadId,
                    onLongClick = viewModel::openActionMenu,
                    onSwipeReply = viewModel::startReply,
                    onToggleReaction = viewModel::toggleReaction,
                    onSelectMessage = viewModel::toggleSelection,
                    onRetryMessage = viewModel::retry
                )

                ScrollDownButton(
                    visible = listState.firstVisibleItemIndex > 2,
                    unreadBadge = state.unreadBadge,
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                        viewModel.clearUnreadBadge()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                )
            }

            // Поле ввода сообщений (шаги 2, 3, 16)
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

        // Меню действий по долгому тапу (шаг 9)
        state.actionMenuFor?.let { message ->
            MessageContextMenu(
                message = message,
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
                onSelect = { viewModel.enterSelection(message.id) },
                onDelete = { viewModel.requestDelete(listOf(message.id)) },
                onDismiss = viewModel::closeActionMenu
            )
        }

        // Диалог удаления сообщений (шаг 9)
        state.deleteDialogFor?.let { targets ->
            DeleteMessageDialog(
                count = targets.size,
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::dismissDeleteDialog
            )
        }

        // Диалог настроек уведомлений чата (шаг 8)
        if (state.isNotificationsOpen) {
            NotificationsSettingsDialog(
                onDismiss = viewModel::closeNotifications
            )
        }

        // Диалог верификации ключей шифрования (шаг 18)
        if (state.isKeyCheckOpen) {
            KeyVerificationDialog(
                peerName = session.title,
                onDismiss = viewModel::closeKeyCheck
            )
        }
    }
}
