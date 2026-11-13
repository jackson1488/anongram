package com.anongram.web.features.chats.mainmenu

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.anongram.web.core.navigation.PhysicsSprings
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.common.timeLabel
import com.anongram.web.features.chats.mainmenu.components.ArchivePill
import com.anongram.web.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.web.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.web.features.chats.mainmenu.components.ChatsFab
import com.anongram.web.features.chats.mainmenu.components.ChatsTopBar
import com.anongram.web.features.chats.mainmenu.components.EmptyChatsState
import com.anongram.web.features.chats.mainmenu.components.OnionOfflineBanner
import com.anongram.web.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.web.features.chats.state.ChatListViewModel
import com.anongram.web.platform.PlatformBackHandler
import kotlinx.coroutines.launch

@Composable
fun ChatsMainMenuScreen(
    viewModel: ChatListViewModel,
    onChatOpen: (IChatSession) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val pillTravelPx = with(density) { (64 + 10).dp.toPx() }
    val archiveHideProgress = remember { Animatable(if (state.isArchiveCollapsed) 1f else 0f) }

    LaunchedEffect(state.isArchiveCollapsed, state.hasArchived) {
        val target = if (!state.hasArchived || state.isArchiveCollapsed) 1f else 0f
        archiveHideProgress.animateTo(target, PhysicsSprings.screenSpringSpec())
    }

    val archiveScrollConnection = remember(state.hasArchived, pillTravelPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!state.hasArchived || available.y >= 0f) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!atTop || archiveHideProgress.value >= 1f) return Offset.Zero
                val neededPx = (1f - archiveHideProgress.value) * pillTravelPx
                val consumePx = minOf(neededPx, -available.y)
                scope.launch {
                    archiveHideProgress.snapTo(
                        (archiveHideProgress.value + consumePx / pillTravelPx).coerceIn(0f, 1f)
                    )
                }
                return Offset(0f, -consumePx)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!state.hasArchived || available.y <= 0f) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!atTop) return Offset.Zero
                val neededPx = archiveHideProgress.value * pillTravelPx
                val consumePx = minOf(neededPx, available.y)
                scope.launch {
                    archiveHideProgress.snapTo(
                        (archiveHideProgress.value - consumePx / pillTravelPx).coerceIn(0f, 1f)
                    )
                }
                return Offset(0f, consumePx)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (state.hasArchived) {
                    viewModel.setArchiveCollapsed(archiveHideProgress.value > 0.5f)
                }
                return Velocity.Zero
            }
        }
    }

    PlatformBackHandler(enabled = state.isSelection) {
        viewModel.clearSelection()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp)
        ) {
            if (state.isSelection) {
                SelectionTopBar(
                    selectedCount = state.selectedIds.size,
                    allPinned = state.allSelectedPinned,
                    allMuted = state.allSelectedMuted,
                    isArchiveMode = false,
                    onClose = viewModel::clearSelection,
                    onTogglePin = viewModel::onTogglePinAction,
                    onDelete = viewModel::onDeleteAction,
                    onMute = viewModel::onMuteAction,
                    onArchive = { viewModel.archiveSelected(true) },
                    onMarkRead = viewModel::onMarkReadAction,
                    onClearHistory = viewModel::onClearHistoryAction
                )
            } else {
                ChatsTopBar(
                    onOpenSearch = onOpenSearch,
                    onEnterSelection = { viewModel.selectAllSessions(archived = false) }
                )
            }

            if (!state.onionCircuitOnline) OnionOfflineBanner()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(archiveScrollConnection)
            ) {
                when {
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = scheme.accent)
                    }
                    state.activeSessions.isEmpty() && !state.hasArchived -> EmptyChatsState()
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 104.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (state.hasArchived && archiveHideProgress.value < 0.99f) {
                                item(key = "archive_pill") {
                                    ArchivePill(
                                        chatCount = state.archivedSessions.size,
                                        preview = state.archivePreview,
                                        onClick = onOpenArchive,
                                        modifier = Modifier.graphicsLayer {
                                            val p = archiveHideProgress.value
                                            translationY = -p * pillTravelPx
                                            alpha = 1f - p
                                        }
                                    )
                                }
                            }
                            items(state.activeSessions, key = { it.id }) { session ->
                                val isSelected = session.id in state.selectedIds
                                ChatListItemCard(
                                    session = session,
                                    timeLabel = session.timeLabel(),
                                    selectionMode = state.isSelection,
                                    isSelected = isSelected,
                                    draftText = state.drafts[session.id],
                                    onClick = {
                                        if (state.isSelection) viewModel.toggleSelection(session.id)
                                        else onChatOpen(session)
                                    },
                                    onLongClick = { viewModel.enterSelection(session.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        ChatsFab(
            visible = !state.isSelection,
            onClick = onOpenContacts,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 108.dp)
        )
    }

    ChatListDialogs(state = state, viewModel = viewModel)
}
