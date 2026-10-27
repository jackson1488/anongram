package com.anongram.app.features.chats.mainmenu

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.timeLabel
import com.anongram.app.features.chats.mainmenu.components.ArchivePill
import com.anongram.app.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.app.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.app.features.chats.mainmenu.components.ChatsFab
import com.anongram.app.features.chats.mainmenu.components.ChatsTopBar
import com.anongram.app.features.chats.mainmenu.components.EmptyChatsState
import com.anongram.app.features.chats.mainmenu.components.OnionOfflineBanner
import com.anongram.app.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.app.features.chats.state.ChatListViewModel
import kotlinx.coroutines.launch

/**
 * Главное меню «Чаты» (секция 1 из 5), Telegram-style: топбар с поиском,
 * закреплённая пилюля архива с жестом скрытия (свайп вверх — спрятать,
 * потянуть вниз от верха — показать), список карточек-стратегий, режим
 * выбора с панелью управления и FAB «Новое сообщение» → контакты.
 */
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
    val haptic = LocalHapticFeedback.current

    // Пилюля архива уезжает вверх на свою высоту + отступ между элементами.
    val pillTravelPx = with(density) { (64 + 10).dp.toPx() }
    val archiveHideProgress = remember { Animatable(if (state.isArchiveCollapsed) 1f else 0f) }

    // Коммиты из ViewModel дожимают прогресс анимацией (после жеста или архивации).
    LaunchedEffect(state.isArchiveCollapsed, state.hasArchived) {
        val target = if (!state.hasArchived || state.isArchiveCollapsed) 1f else 0f
        archiveHideProgress.animateTo(target, PhysicsSprings.screenSpringSpec())
    }

    fun requestArchiveCollapse(collapsed: Boolean) {
        viewModel.setArchiveCollapsed(collapsed)
    }

    // Жесты архива через nestedScroll (как в Telegram): тяга вверх при верхнем
    // положении списка сворачивает пилюлю, тяга вниз с самого верха — раскрывает.
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
                    requestArchiveCollapse(archiveHideProgress.value > 0.5f)
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = state.isSelection) {
        viewModel.clearSelection()
    }

    Column(
        modifier = modifier
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
                    CircularProgressIndicator(
                        color = scheme.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (state.hasArchived) {
                        item(key = "archive_pill") {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationY = -pillTravelPx * archiveHideProgress.value
                                        alpha = 1f - archiveHideProgress.value
                                    }
                                    .padding(bottom = 10.dp)
                            ) {
                                ArchivePill(
                                    chatCount = state.archivedSessions.size,
                                    preview = state.archivePreview,
                                    onClick = onOpenArchive
                                )
                            }
                        }
                    }

                    if (state.activeSessions.isEmpty() && !state.hasArchived) {
                        item(key = "empty") { EmptyChatsState() }
                    } else {
                        items(state.activeSessions, key = { it.id }) { session ->
                            ChatListItemCard(
                                session = session,
                                timeLabel = remember(session.id, session.lastMessageEpochMs) {
                                    session.timeLabel()
                                },
                                selectionMode = state.isSelection,
                                isSelected = session.id in state.selectedIds,
                                draftText = state.drafts[session.id],
                                onClick = {
                                    if (state.isSelection) {
                                        viewModel.toggleSelection(session.id)
                                    } else {
                                        onChatOpen(session)
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (state.isSelection) {
                                        viewModel.toggleSelection(session.id)
                                    } else {
                                        viewModel.enterSelection(session.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ChatsFab(
                visible = !state.isSelection && !state.isLoading,
                onClick = onOpenContacts,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 116.dp)
            )
        }
    }

    ChatListDialogs(state = state, viewModel = viewModel)
}
