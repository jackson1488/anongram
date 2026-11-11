package com.anongram.app.features.chats.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatMiniPlayer
import com.anongram.app.features.chats.messages.state.ChatMessagesUiState
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.chats.ui.components.ScrollDownButton
import com.anongram.app.features.chats.ui.components.bubble.saveFileToDeviceDownloads
import com.anongram.app.features.chats.ui.components.bubble.saveMessageMediaToDevice
import com.anongram.app.features.chats.ui.components.bubble.saveMusicToDeviceMusic
import com.anongram.app.features.chats.ui.components.bubble.shareMessageMediaSystem
import com.anongram.app.features.chats.ui.input.banner.ChatActionBannerBus
import kotlinx.coroutines.launch

/**
 * Изолированная плавающая шапка чата (топбар / поиск / выделение / закреп / мини-плеер),
 * градиентная подложка под шапку и кнопка прокрутки вниз.
 */
@Composable
internal fun ChatFloatingTopHeader(
    session: IChatSession,
    state: ChatMessagesUiState,
    viewModel: ChatMessagesViewModel,
    listState: LazyListState,
    audioController: ChatAudioController,
    inputFocusRequester: FocusRequester,
    isCircleOverlayActive: Boolean,
    onBack: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onStartForward: (List<String>) -> Unit,
    onOpenMusicSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isCircleOverlayActive) Modifier.blur(22.dp) else Modifier)
            .safeStatusBarPadding()
    ) {
        when {
            state.isSelectionMode -> {
                SelectionPanel(
                    selectedCount = state.selection.size,
                    isPhoto = state.isSingleSelectedPhoto,
                    isMediaAlbum = state.isSingleSelectedMediaAlbum,
                    isFileOrMusic = state.isSingleSelectedFileOrMusic,
                    isVideoCircle = state.isSingleSelectedVideoCircle,
                    isVoice = state.isSingleSelectedVoice,
                    onClose = viewModel::clearSelection,
                    onCopy = {
                        val combined = state.selectedMessages.joinToString("\n") { it.text }
                        clipboard.setText(AnnotatedString(combined))
                        viewModel.clearSelection()
                        ChatActionBannerBus.show(Icons.Outlined.ContentCopy) { it.bannerCopiedToClipboard }
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
                        val selectedKey = state.selection.firstOrNull()
                        val file = state.getSingleSelectedFile()
                        val music = state.getSingleSelectedMusic()
                        val targetMsg = selectedKey?.let { key ->
                            val msgId = key.substringBefore("#")
                            state.messages.firstOrNull { it.id == msgId }
                        }
                        val subPhotoIdx = selectedKey?.takeIf { "#photo_" in it }?.substringAfter("#photo_")?.toIntOrNull()
                        viewModel.clearSelection()
                        when {
                            file != null -> scope.launch {
                                val ok = saveFileToDeviceDownloads(context, file)
                                ChatActionBannerBus.show(
                                    icon = if (ok) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
                                    isError = !ok
                                ) { s ->
                                    if (ok) s.bannerFileSavedToDownloads else s.bannerSaveFailed
                                }
                            }
                            music != null -> scope.launch {
                                val ok = saveMusicToDeviceMusic(context, music)
                                ChatActionBannerBus.show(
                                    icon = if (ok) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
                                    isError = !ok
                                ) { s ->
                                    if (ok) s.bannerTrackSavedToMusic else s.bannerSaveFailed
                                }
                            }
                            targetMsg != null -> scope.launch {
                                saveMessageMediaToDevice(context, targetMsg, subPhotoIdx)
                            }
                        }
                    },
                    onShare = {
                        val selectedKey = state.selection.firstOrNull()
                        val targetMsg = selectedKey?.let { key ->
                            val msgId = key.substringBefore("#")
                            state.messages.firstOrNull { it.id == msgId }
                        }
                        val subPhotoIdx = selectedKey?.takeIf { "#photo_" in it }?.substringAfter("#photo_")?.toIntOrNull()
                        viewModel.clearSelection()
                        if (targetMsg != null) {
                            scope.launch {
                                shareMessageMediaSystem(context, targetMsg, subPhotoIdx)
                            }
                        }
                    },
                    onForward = {
                        onStartForward(state.selection.toList())
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
                    onCycleWallpaper = onCycleWallpaper,
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
                onOpenSheet = onOpenMusicSheet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun ChatHeaderScrim(
    listState: LazyListState,
    scrimHeight: Dp,
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
internal fun ChatScrollDownOverlay(
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
