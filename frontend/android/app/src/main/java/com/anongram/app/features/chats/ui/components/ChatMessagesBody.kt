package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.buildMusicQueue
import com.anongram.app.features.chats.media.player.buildVideoCircleTrack
import com.anongram.app.features.chats.media.player.buildVoiceQueue
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesUiState
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.ui.ChatScrollDownOverlay
import com.anongram.app.features.chats.ui.ChatSearchNavigationButtons
import com.anongram.app.features.chats.ui.ChatSearchResultsList
import com.anongram.app.features.chats.ui.animateScrollAndCenterItem
import kotlinx.coroutines.launch

/**
 * Основное тело чата: список сообщений (или список результатов поиска),
 * кнопка «вниз» и кнопки перехода по результатам поиска.
 */
@Composable
internal fun ChatMessagesBody(
    session: IChatSession,
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    audioController: ChatAudioController,
    inputFocusRequester: FocusRequester,
    isCircleOverlayActive: Boolean,
    isVideoCircleExpanded: Boolean,
    isKeyboardVisible: Boolean,
    isSearchListMode: Boolean,
    chatBottomImeDp: Dp,
    topListPadding: Dp,
    bottomListPadding: Dp,
    onCloseSearchListMode: () -> Unit,
    onOpenActionMenu: (message: ChatMessage, topY: Float, bottomY: Float) -> Unit,
    onOpenMediaViewer: (message: ChatMessage, initialIndex: Int, overrideUri: String?) -> Unit,
    onOpenDirectPeer: (name: String, handleOrPhone: String, onSessionReady: (IChatSession) -> Unit) -> Unit,
    onOpenTargetSession: (IChatSession) -> Unit,
    onStartForward: (List<String>) -> Unit,
    onOpenFullTextMessage: (ChatMessage) -> Unit,
    onOpenGeoViewer: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (isCircleOverlayActive) Modifier.blur(22.dp) else Modifier)
            .padding(bottom = chatBottomImeDp)
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
                    onCloseSearchListMode()
                    viewModel.highlightMessage(message.id)
                    val matchIdx = state.searchMatches.indexOf(message.id)
                    if (matchIdx >= 0) {
                        viewModel.jumpToSearchMatch(matchIdx)
                    }
                    val idx = state.messages.asReversed().indexOfFirst { it.id == message.id }
                    if (idx >= 0) {
                        scope.launch {
                            runCatching { listState.animateScrollAndCenterItem(idx) }
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
                onClick = { message, topY, bottomY ->
                    if (state.isSelectionMode) {
                        viewModel.toggleSelection(message.id)
                    } else {
                        onOpenActionMenu(message, topY, bottomY)
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
                    onOpenMediaViewer(message, photoIdx, null)
                },
                onOpenUncompressedPhoto = { message, uri ->
                    onOpenMediaViewer(message, 0, uri)
                },
                onOpenContactChat = { contactName, contactPhone ->
                    onOpenDirectPeer(contactName, contactPhone.ifBlank { contactName }) { targetSession ->
                        onOpenTargetSession(targetSession)
                    }
                },
                onForwardSubItem = { subKey ->
                    onStartForward(listOf(subKey))
                },
                onDeleteSubItem = { subKey ->
                    viewModel.requestDelete(listOf(subKey))
                },
                onPlayVideoCircle = { targetMsg ->
                    buildVideoCircleTrack(targetMsg, session.title, session.id)?.let { track ->
                        audioController.play(listOf(track), track, autoStart = true)
                    }
                },
                onEmptyAreaTap = { audioController.stopVideoCircle() },
                onPlayVoice = { targetMsg ->
                    val (voiceTracks, current) = buildVoiceQueue(state.messages, targetMsg, session.title, session.id)
                    if (current != null) {
                        audioController.playVoiceSequence(voiceTracks, current)
                    }
                },
                onPlayMusic = { targetTrack ->
                    val queue = buildMusicQueue(state.messages, session.title, session.id)
                    val toPlay = queue.firstOrNull {
                        it.id == targetTrack.id || (it.messageId == targetTrack.messageId && it.title == targetTrack.title)
                    } ?: targetTrack
                    audioController.play(if (queue.isNotEmpty()) queue else listOf(toPlay), toPlay)
                },
                onOpenFullTextMessage = onOpenFullTextMessage,
                onOpenGeoViewer = onOpenGeoViewer
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
}
