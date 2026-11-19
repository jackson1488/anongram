package com.anongram.app.features.chats.mainmenu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatMiniPlayer
import com.anongram.app.features.chats.media.player.ChatMusicPlayerSheet
import com.anongram.app.features.chats.media.player.MusicFullScreenPlayer
import com.anongram.app.features.chats.state.ChatListViewModel
import com.anongram.app.features.drafts.ChatDraftsRepository
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

/**
 * Главное меню «Чаты» (секция 1 из 5), Telegram-style: топбар с поиском,
 * закреплённая пилюля архива с жестом скрытия (свайп вверх — спрятать,
 * потянуть вниз от верха — показать), список карточек-стратегий, режим
 * выбора с панелью управления и FAB «Новое сообщение» → контакты.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val context = LocalContext.current
    val audioController = remember { ChatAudioController.getInstance(context) }
    val audioPlayerState by audioController.state.collectAsState()
    var isMusicSheetOpen by remember { mutableStateOf(false) }
    val richDrafts by ChatDraftsRepository.instance.drafts.collectAsState()

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

    val animState by AnimationsSettingsStore.state.collectAsState()
    val hazeState = remember { HazeState() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12
        }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        label = "chatsHeaderAlpha"
    )
    val glassTintAlpha = if (scheme.isDark) {
        0.30f + 0.14f * animatedHeaderAlpha
    } else {
        0.40f + 0.16f * animatedHeaderAlpha
    }

    val headerBackgroundModifier = if (animState.glassBlurEnabled) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = scheme.background,
                tint = HazeTint(color = scheme.surface.copy(alpha = glassTintAlpha)),
                blurRadius = 22.dp,
                noiseFactor = 0.02f
            )
        ) {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f
            )
        }
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    scheme.surface.copy(alpha = 0.90f + 0.08f * animatedHeaderAlpha),
                    scheme.background.copy(alpha = 0.60f + 0.25f * animatedHeaderAlpha),
                    Color.Transparent
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // 1. Основной скроллируемый список на весь экран (проходит под парящей шапкой)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(scheme.background)
                .nestedScroll(archiveScrollConnection)
        ) {
            val displaySessions = remember(state.activeSessions, richDrafts) {
                com.anongram.app.features.drafts.sortSessionsWithDrafts(state.activeSessions, richDrafts)
            }

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
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarTop + 62.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Поиск в стиле Telegram прямо под шапкой (уезжает под блюр при скролле)
                    if (!state.isSelection) {
                        item(key = "telegram_search_bar") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(scheme.inputBg)
                                    .border(0.8.dp, scheme.inputBorder, RoundedCornerShape(21.dp))
                                    .clickable(onClick = onOpenSearch)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = scheme.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Поиск",
                                        color = scheme.textMuted,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

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

                    if (displaySessions.isEmpty() && !state.hasArchived) {
                        item(key = "empty") { EmptyChatsState() }
                    } else {
                        items(displaySessions, key = { it.id }) { session ->
                            val richDraft = richDrafts[session.id]
                            val draftDisplay = when {
                                richDraft != null && !richDraft.isEmpty -> richDraft.previewText
                                else -> state.drafts[session.id]
                            }
                            ChatListItemCard(
                                modifier = Modifier.animateItemPlacement(),
                                session = session,
                                timeLabel = remember(session.id, session.lastMessageEpochMs) {
                                    session.timeLabel()
                                },
                                selectionMode = state.isSelection,
                                isSelected = session.id in state.selectedIds,
                                draftText = draftDisplay,
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
        }

        // 2. Закреплённая сверху прозрачная шапка с бесшовным прогрессивным блюром
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .then(headerBackgroundModifier)
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
                    onEnterSelection = { viewModel.selectAllSessions(archived = false) },
                    hazeState = hazeState,
                    glassBlurEnabled = animState.glassBlurEnabled
                )
            }

            if (!state.isSelection) {
                ChatMiniPlayer(
                    controller = audioController,
                    onOpenSheet = { audioController.openSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (!state.onionCircuitOnline) OnionOfflineBanner()

            // Плавный хвост прогрессивного блюра — растворяется без границы
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Плавающая кнопка «Новое сообщение»
        ChatsFab(
            visible = !state.isSelection && !state.isLoading,
            onClick = onOpenContacts,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 116.dp)
        )

        ChatListDialogs(state = state, viewModel = viewModel)
    }
}
