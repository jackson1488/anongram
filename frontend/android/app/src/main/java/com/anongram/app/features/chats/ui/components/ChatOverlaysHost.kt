package com.anongram.app.features.chats.ui.components

import android.content.Intent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import com.anongram.app.features.chats.attach.api.AttachSheet
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.media.MediaCompressor
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatMusicPlayerSheet
import com.anongram.app.features.chats.media.player.MusicFullScreenPlayer
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesUiState
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.tools.DeleteMessageDialog
import com.anongram.app.features.chats.tools.ForwardTargetPickerScreen
import com.anongram.app.features.chats.tools.MessageContextMenu
import com.anongram.app.features.chats.tools.PinnedMessagesScreen
import com.anongram.app.features.chats.ui.animateScrollAndCenterItem
import com.anongram.app.features.chats.ui.animations.LocalMessageSnapshotRegistry
import com.anongram.app.features.chats.ui.components.bubble.MessageFullTextViewerModal
import com.anongram.app.features.chats.ui.components.bubble.saveMessageMediaToDevice
import com.anongram.app.features.chats.ui.components.bubble.shareMessageMediaSystem
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.chats.input.ChatImeMetrics
import com.anongram.app.features.chats.input.attach.formatFullEditorMessageText
import com.anongram.app.features.chats.input.attach.mapSingleReplaceDraft
import com.anongram.app.features.chats.input.attach.sendAttachDraftsToChat
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import com.anongram.app.features.chats.input.editor.ChatFullTextEditorScreen
import com.anongram.app.features.chats.input.reply.ReplyPreviewDialog
import com.anongram.app.features.chats.viewer.ChatGeoViewerScreen
import com.anongram.app.features.chats.viewer.ChatMediaFullscreenViewer
import com.anongram.app.features.wallpapers.api.WallpaperRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Хост полноэкранных оверлеев и модальных диалогов чата,
 * разбитый на компактные функции для низкого числа Dalvik-регистров.
 */
@Composable
internal fun ChatOverlaysHost(
    session: IChatSession,
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    audioController: ChatAudioController,
    imeMetrics: ChatImeMetrics,
    inputFocusRequester: FocusRequester,
    activeSessions: List<IChatSession>,
    onOpenTargetSession: (IChatSession) -> Unit,
    onOpenDirectPeer: (name: String, handleOrPhone: String, onSessionReady: (IChatSession) -> Unit) -> Unit,
    isFullEditorOpen: Boolean,
    onCloseFullEditor: () -> Unit,
    isWallpaperOpen: Boolean,
    onCloseWallpaper: () -> Unit,
    isReplyPreviewOpen: Boolean,
    onCloseReplyPreview: () -> Unit,
    isReplaceAttachSheetOpen: Boolean,
    replaceInitialTab: AttachTab,
    onCloseReplaceAttach: () -> Unit,
    forwardingMessageIds: List<String>?,
    onStartForward: (List<String>) -> Unit,
    onCloseForward: () -> Unit,
    viewingMediaMessage: ChatMessage?,
    viewingMediaInitialIndex: Int,
    viewingOverrideSingleUri: String?,
    wasKeyboardOpenBeforeViewer: Boolean,
    onUpdateMediaViewer: (ChatMessage?) -> Unit,
    onCloseMediaViewer: (restoreKeyboard: Boolean) -> Unit,
    actionMenuAnchorTopY: Float,
    actionMenuAnchorBottomY: Float,
    isMusicSheetOpen: Boolean,
    isAudioSheetOpen: Boolean,
    isAudioFullScreenOpen: Boolean,
    onCloseMusicSheet: () -> Unit,
    fullTextMessageModal: ChatMessage?,
    onCloseFullTextModal: () -> Unit,
    viewingGeoMessage: ChatMessage? = null,
    onCloseGeoViewer: () -> Unit = {}
) {
    ChatInputAndMediaOverlays(
        session = session,
        state = state,
        viewModel = viewModel,
        listState = listState,
        imeMetrics = imeMetrics,
        inputFocusRequester = inputFocusRequester,
        isFullEditorOpen = isFullEditorOpen,
        onCloseFullEditor = onCloseFullEditor,
        isReplyPreviewOpen = isReplyPreviewOpen,
        onCloseReplyPreview = onCloseReplyPreview,
        isReplaceAttachSheetOpen = isReplaceAttachSheetOpen,
        replaceInitialTab = replaceInitialTab,
        onCloseReplaceAttach = onCloseReplaceAttach,
        viewingMediaMessage = viewingMediaMessage,
        viewingMediaInitialIndex = viewingMediaInitialIndex,
        viewingOverrideSingleUri = viewingOverrideSingleUri,
        wasKeyboardOpenBeforeViewer = wasKeyboardOpenBeforeViewer,
        onUpdateMediaViewer = onUpdateMediaViewer,
        onCloseMediaViewer = onCloseMediaViewer,
        onStartForward = onStartForward
    )

    ChatMessageActionAndSettingsOverlays(
        session = session,
        state = state,
        viewModel = viewModel,
        listState = listState,
        inputFocusRequester = inputFocusRequester,
        activeSessions = activeSessions,
        onOpenTargetSession = onOpenTargetSession,
        onOpenDirectPeer = onOpenDirectPeer,
        actionMenuAnchorTopY = actionMenuAnchorTopY,
        actionMenuAnchorBottomY = actionMenuAnchorBottomY,
        forwardingMessageIds = forwardingMessageIds,
        onStartForward = onStartForward,
        onCloseForward = onCloseForward,
        isWallpaperOpen = isWallpaperOpen,
        onCloseWallpaper = onCloseWallpaper
    )

    ChatPlayerAndTextOverlays(
        state = state,
        viewModel = viewModel,
        listState = listState,
        audioController = audioController,
        isMusicSheetOpen = isMusicSheetOpen,
        isAudioSheetOpen = isAudioSheetOpen,
        isAudioFullScreenOpen = isAudioFullScreenOpen,
        onCloseMusicSheet = onCloseMusicSheet,
        onStartForward = onStartForward,
        fullTextMessageModal = fullTextMessageModal,
        onCloseFullTextModal = onCloseFullTextModal
    )

    val geoAtt = viewingGeoMessage?.attachment as? MessageAttachment.Geo
    if (viewingGeoMessage != null && geoAtt != null) {
        ChatGeoViewerScreen(
            message = viewingGeoMessage,
            geo = geoAtt,
            peerName = session.title,
            onBack = onCloseGeoViewer
        )
    }
}

@Composable
private fun ChatInputAndMediaOverlays(
    session: IChatSession,
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    imeMetrics: ChatImeMetrics,
    inputFocusRequester: FocusRequester,
    isFullEditorOpen: Boolean,
    onCloseFullEditor: () -> Unit,
    isReplyPreviewOpen: Boolean,
    onCloseReplyPreview: () -> Unit,
    isReplaceAttachSheetOpen: Boolean,
    replaceInitialTab: AttachTab,
    onCloseReplaceAttach: () -> Unit,
    viewingMediaMessage: ChatMessage?,
    viewingMediaInitialIndex: Int,
    viewingOverrideSingleUri: String?,
    wasKeyboardOpenBeforeViewer: Boolean,
    onUpdateMediaViewer: (ChatMessage?) -> Unit,
    onCloseMediaViewer: (restoreKeyboard: Boolean) -> Unit,
    onStartForward: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isFullEditorOpen) {
        ChatFullTextEditorScreen(
            initialText = state.input,
            onClose = { textAfterEdit ->
                viewModel.onInputChange(textAfterEdit)
                onCloseFullEditor()
            },
            onOpenNativeAttach = {
                if (imeMetrics.isKeyboardVisible) {
                    imeMetrics.imeState.wasKeyboardOpenWhenAttached = true
                    imeMetrics.imeState.frozenKeyboardHeightPx = imeMetrics.imeState.lastKeyboardHeightPx
                } else {
                    imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                    imeMetrics.imeState.frozenKeyboardHeightPx = 0
                }
                viewModel.openAttach()
            },
            onSend = { textToSend, format, language, attachment ->
                val formattedToSend = formatFullEditorMessageText(textToSend, format, language)
                if (attachment != null) {
                    viewModel.sendAttachment(attachment, caption = formattedToSend)
                } else {
                    viewModel.onInputChange(formattedToSend)
                    viewModel.send()
                }
                onCloseFullEditor()
                scope.launch { listState.animateScrollToItem(0) }
            }
        )
    }

    if (state.isAttachOpen) {
        AttachSheet(
            visible = true,
            chatId = session.id,
            recipientName = session.title,
            keyboardHeightPx = imeMetrics.imeState.frozenKeyboardHeightPx,
            wasKeyboardOpen = imeMetrics.imeState.wasKeyboardOpenWhenAttached,
            onRestoreKeyboard = {
                runCatching {
                    inputFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            onDismiss = {
                imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                imeMetrics.imeState.frozenKeyboardHeightPx = 0
                viewModel.closeAttach()
            },
            onSend = { drafts ->
                imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                imeMetrics.imeState.frozenKeyboardHeightPx = 0
                viewModel.closeAttach()
                com.anongram.app.features.chats.input.attach.sendOptimisticAttachDraftsToChat(
                    context = context,
                    scope = scope,
                    drafts = drafts,
                    viewModel = viewModel
                )
            }
        )
    }

    if (isReplaceAttachSheetOpen) {
        AttachSheet(
            visible = true,
            chatId = session.id,
            recipientName = session.title,
            initialTab = replaceInitialTab,
            isSingleReplaceMode = true,
            keyboardHeightPx = imeMetrics.imeState.frozenKeyboardHeightPx,
            wasKeyboardOpen = imeMetrics.imeState.wasKeyboardOpenWhenAttached,
            onRestoreKeyboard = {
                runCatching {
                    inputFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            onSingleReplace = { draft ->
                imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                imeMetrics.imeState.frozenKeyboardHeightPx = 0
                onCloseReplaceAttach()
                val (newAttachment, sheetCaption) = mapSingleReplaceDraft(draft)
                if (newAttachment != null) {
                    viewModel.replaceEditingAttachment(newAttachment, sheetCaption?.ifBlank { null })
                }
            },
            onDismiss = {
                imeMetrics.imeState.wasKeyboardOpenWhenAttached = false
                imeMetrics.imeState.frozenKeyboardHeightPx = 0
                onCloseReplaceAttach()
            },
            onSend = {}
        )
    }

    if (isReplyPreviewOpen && state.replyTo != null) {
        ReplyPreviewDialog(
            chatId = session.id,
            replyMessage = state.replyTo,
            replyToSubIndex = state.replyToSubIndex,
            peerName = session.title,
            onRemoveReply = {
                viewModel.cancelReply()
                onCloseReplyPreview()
            },
            onJumpToMessage = {
                val targetId = state.replyTo.id
                viewModel.highlightMessage(targetId)
                val index = state.messages.asReversed().indexOfFirst { it.id == targetId }
                if (index >= 0) {
                    scope.launch {
                        runCatching { listState.animateScrollAndCenterItem(index) }
                    }
                }
                onCloseReplyPreview()
            },
            onDismiss = onCloseReplyPreview
        )
    }

    fun closeViewerAndRestoreIme() {
        val shouldRestore = wasKeyboardOpenBeforeViewer
        onCloseMediaViewer(false)
        if (shouldRestore) {
            scope.launch {
                delay(80)
                runCatching {
                    inputFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        }
    }

    LaunchedEffect(state.messages, viewingMediaMessage) {
        val current = viewingMediaMessage ?: return@LaunchedEffect
        val updated = state.messages.firstOrNull { it.id == current.id }
        if (updated == null) {
            closeViewerAndRestoreIme()
        } else if (updated != current) {
            onUpdateMediaViewer(updated)
        }
    }

    viewingMediaMessage?.let { mediaMessage ->
        ChatMediaFullscreenViewer(
            message = mediaMessage,
            initialPhotoIndex = viewingMediaInitialIndex,
            peerName = session.title,
            allChatMessages = state.messages,
            overrideSingleUri = viewingOverrideSingleUri,
            onClose = { closeViewerAndRestoreIme() },
            onShowInChat = { msg ->
                closeViewerAndRestoreIme()
                viewModel.highlightMessage(msg.id)
                val index = state.messages.asReversed().indexOfFirst { it.id == msg.id }
                if (index >= 0) {
                    scope.launch {
                        runCatching { listState.animateScrollAndCenterItem(index) }
                    }
                }
            },
            onReply = { msg, subIdx ->
                onCloseMediaViewer(false)
                viewModel.startReply(msg, subIdx)
                scope.launch {
                    delay(80)
                    runCatching {
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            },
            onForward = { ids ->
                onCloseMediaViewer(false)
                onStartForward(ids)
            },
            onRequestDelete = { msg, photoIdx ->
                when (msg.attachment) {
                    is MessageAttachment.MediaAlbum -> viewModel.requestDelete(listOf("${msg.id}#photo_$photoIdx"))
                    is MessageAttachment.FileGroup -> viewModel.requestDelete(listOf("${msg.id}#file_$photoIdx"))
                    else -> viewModel.requestDelete(listOf(msg.id))
                }
            }
        )
    }
}

@Composable
private fun ChatMessageActionAndSettingsOverlays(
    session: IChatSession,
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    inputFocusRequester: FocusRequester,
    activeSessions: List<IChatSession>,
    onOpenTargetSession: (IChatSession) -> Unit,
    onOpenDirectPeer: (name: String, handleOrPhone: String, onSessionReady: (IChatSession) -> Unit) -> Unit,
    actionMenuAnchorTopY: Float,
    actionMenuAnchorBottomY: Float,
    forwardingMessageIds: List<String>?,
    onStartForward: (List<String>) -> Unit,
    onCloseForward: () -> Unit,
    isWallpaperOpen: Boolean,
    onCloseWallpaper: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    state.actionMenuFor?.let { message ->
        MessageContextMenu(
            message = message,
            anchorTopYPx = actionMenuAnchorTopY,
            anchorBottomYPx = actionMenuAnchorBottomY,
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
                val copyText = when (val att = message.attachment) {
                    is MessageAttachment.Contact -> message.text.ifBlank { "${att.name}: ${att.phone}" }
                    is MessageAttachment.Geo -> message.text.ifBlank { att.address }
                    else -> message.text
                }
                clipboard.setText(AnnotatedString(copyText))
                viewModel.closeActionMenu()
                ChatActionBannerBus.show(Icons.Outlined.ContentCopy) { it.bannerCopiedToClipboard }
            },
            onSave = {
                viewModel.closeActionMenu()
                scope.launch { saveMessageMediaToDevice(context, message) }
            },
            onShare = {
                viewModel.closeActionMenu()
                scope.launch { shareMessageMediaSystem(context, message) }
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
                onStartForward(listOf(message.id))
            },
            onPin = { viewModel.pinMessage(message.id) },
            onDelete = { viewModel.requestDelete(listOf(message.id)) },
            onDismiss = viewModel::closeActionMenu
        )
    }

    forwardingMessageIds?.let { idsToForward ->
        ForwardTargetPickerScreen(
            activeSessions = activeSessions,
            onClose = onCloseForward,
            onSelectSavedMessages = {
                onCloseForward()
                val savedExisting = activeSessions.firstOrNull {
                    it.id == "0" || it.title.equals("Избранное", ignoreCase = true)
                }
                if (savedExisting != null) {
                    viewModel.forwardMessagesToTargetChat(savedExisting.id, idsToForward, session.title) {
                        onOpenTargetSession(savedExisting)
                    }
                } else {
                    onOpenDirectPeer("Избранное", "saved_messages") { createdSaved ->
                        viewModel.forwardMessagesToTargetChat(createdSaved.id, idsToForward, session.title) {
                            onOpenTargetSession(createdSaved)
                        }
                    }
                }
            },
            onSelectSession = { targetSession ->
                onCloseForward()
                viewModel.forwardMessagesToTargetChat(targetSession.id, idsToForward, session.title) {
                    onOpenTargetSession(targetSession)
                }
            },
            onSelectGlobalItem = { name, handle ->
                onCloseForward()
                onOpenDirectPeer(name, handle) { createdSession ->
                    viewModel.forwardMessagesToTargetChat(createdSession.id, idsToForward, session.title) {
                        onOpenTargetSession(createdSession)
                    }
                }
            }
        )
    }

    val deleteSnapshotRegistry = LocalMessageSnapshotRegistry.current
    val deleteCoroutineScope = rememberCoroutineScope()
    state.deleteDialogFor?.let { targets ->
        DeleteMessageDialog(
            count = targets.size,
            peerName = session.title,
            onConfirm = { forEveryone ->
                val animState = AnimationsSettingsStore.state.value
                deleteCoroutineScope.launch {
                    deleteSnapshotRegistry?.triggerDeleteAnimations(
                        targetKeys = targets,
                        style = animState.messageDeleteStyle,
                        speedMultiplier = animState.speedMultiplier,
                        reduceMotion = animState.reduceMotionEnabled
                    )
                    viewModel.confirmDelete(forEveryone)
                }
            },
            onDismiss = viewModel::dismissDeleteDialog
        )
    }

    if (state.isNotificationsOpen) {
        NotificationsSettingsDialog(
            peerName = session.title,
            onDismiss = viewModel::closeNotifications
        )
    }

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
                            runCatching { listState.animateScrollAndCenterItem(index) }
                        }
                    }
                }
            },
            onMessageLongClick = { viewModel.enterSelection(it.id) },
            onSelectMessage = viewModel::toggleSelection,
            onSwipeReply = viewModel::startReply,
            onToggleReaction = viewModel::toggleReaction,
            onUnpinAll = viewModel::unpinAllMessages,
            onTriggerLimitDemo = viewModel::triggerDemoPinnedLimitWarning,
            onDismissLimitWarning = viewModel::dismissLimitWarning
        )
    }

    if (state.isKeyCheckOpen) {
        KeyVerificationDialog(
            peerName = session.title,
            onDismiss = viewModel::closeKeyCheck
        )
    }

    if (isWallpaperOpen) {
        WallpaperRoute(
            chatId = session.id,
            onDismiss = onCloseWallpaper
        )
    }
}

@Composable
private fun ChatPlayerAndTextOverlays(
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    audioController: ChatAudioController,
    isMusicSheetOpen: Boolean,
    isAudioSheetOpen: Boolean,
    isAudioFullScreenOpen: Boolean,
    onCloseMusicSheet: () -> Unit,
    onStartForward: (List<String>) -> Unit,
    fullTextMessageModal: ChatMessage?,
    onCloseFullTextModal: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (isMusicSheetOpen || isAudioSheetOpen) {
        ChatMusicPlayerSheet(
            controller = audioController,
            onDismiss = {
                onCloseMusicSheet()
                audioController.closeSheet()
            },
            onForwardTrack = { track ->
                onCloseMusicSheet()
                audioController.closeSheet()
                onStartForward(listOf(track.id.substringBefore("_")))
            },
            onShareTrack = { track ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${track.performer} - ${track.title}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Поделиться треком"))
            },
            onShowInChat = { track ->
                onCloseMusicSheet()
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

    if (isAudioFullScreenOpen) {
        MusicFullScreenPlayer(
            controller = audioController,
            onDismiss = { audioController.closeFullScreen() },
            onForwardTrack = { track ->
                audioController.closeFullScreen()
                onStartForward(listOf(track.id.substringBefore("_")))
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

    fullTextMessageModal?.let { msg ->
        MessageFullTextViewerModal(
            message = msg,
            onClose = onCloseFullTextModal
        )
    }
}
