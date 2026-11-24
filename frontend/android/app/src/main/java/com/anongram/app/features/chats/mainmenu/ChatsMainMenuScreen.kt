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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramGlassHeaderColumn
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.chats.archive.i18n.ArchiveI18n
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

    // Поиск как в Telegram: скрыт по умолчанию, раскрывается ТОЛЬКО при осознанном потягивании вниз с верха.
    val searchTravelPx = with(density) { 52.dp.toPx() }
    val searchDeadzonePx = with(density) { 12.dp.toPx() }
    var accumulatedDownPx by remember { mutableStateOf(0f) }
    val searchRevealProgress = remember { Animatable(0f) }

    // Баннер «Отпустите для вывода архива» (как на скриншоте Telegram, только если есть архивные чаты).
    val archivePullTravelPx = with(density) { 84.dp.toPx() }
    val archivePullProgress = remember { Animatable(0f) }
    var archiveHapticFired by remember { mutableStateOf(false) }

    val archiveStrings = ArchiveI18n.current

    // Коммиты из ViewModel дожимают прогресс анимацией (после жеста или архивации).
    LaunchedEffect(state.isArchiveCollapsed, state.hasArchived) {
        val target = if (!state.hasArchived || state.isArchiveCollapsed) 1f else 0f
        archiveHideProgress.animateTo(target, PhysicsSprings.screenSpringSpec())
    }

    fun requestArchiveCollapse(collapsed: Boolean) {
        viewModel.setArchiveCollapsed(collapsed)
    }

    // Жесты архива и поиска через nestedScroll (как в Telegram):
    // - Инерция скролла (SideEffect / Fling) ИГНОРИРУЕТСЯ, чтобы быстрый скролл вверх не открывал поиск.
    // - Потягивание вниз с самого верха: после мёртвой зоны (12.dp) раскрывает Поиск, а при дальнейшей
    //   глубокой тяге вниз (если есть архивные чаты) — выводит баннер «Отпустите для вывода архива».
    // - Любая тяга вверх мгновенно сворачивает баннер, Поиск и затем Архив.
    val archiveScrollConnection = remember(state.hasArchived, pillTravelPx, searchTravelPx, archivePullTravelPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                accumulatedDownPx = 0f

                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!atTop) return Offset.Zero

                var remainingUp = -available.y
                var totalConsumed = 0f

                if (archivePullProgress.value > 0f) {
                    val neededPullPx = archivePullProgress.value * archivePullTravelPx
                    val consumePull = minOf(neededPullPx, remainingUp)
                    if (consumePull > 0f) {
                        scope.launch {
                            archivePullProgress.snapTo(
                                (archivePullProgress.value - consumePull / archivePullTravelPx).coerceIn(0f, 1f)
                            )
                        }
                        if (archivePullProgress.value < 0.75f) archiveHapticFired = false
                        remainingUp -= consumePull
                        totalConsumed += consumePull
                    }
                }

                if (searchRevealProgress.value > 0f && remainingUp > 0f) {
                    val neededSearchPx = searchRevealProgress.value * searchTravelPx
                    val consumeSearch = minOf(neededSearchPx, remainingUp)
                    if (consumeSearch > 0f) {
                        scope.launch {
                            searchRevealProgress.snapTo(
                                (searchRevealProgress.value - consumeSearch / searchTravelPx).coerceIn(0f, 1f)
                            )
                        }
                        remainingUp -= consumeSearch
                        totalConsumed += consumeSearch
                    }
                }

                if (state.hasArchived && archiveHideProgress.value < 1f && remainingUp > 0f) {
                    val neededArchivePx = (1f - archiveHideProgress.value) * pillTravelPx
                    val consumeArchive = minOf(neededArchivePx, remainingUp)
                    if (consumeArchive > 0f) {
                        scope.launch {
                            archiveHideProgress.snapTo(
                                (archiveHideProgress.value + consumeArchive / pillTravelPx).coerceIn(0f, 1f)
                            )
                        }
                        totalConsumed += consumeArchive
                    }
                }

                return if (totalConsumed > 0f) Offset(0f, -totalConsumed) else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Строго только прямой палец пользователя (никакой инерции/флинга!)
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!atTop) return Offset.Zero

                var remainingDown = available.y
                var totalConsumed = 0f

                // Мёртвая зона сопротивления перед раскрытием поиска, чтобы резкий свайп не открывал случайно
                if (searchRevealProgress.value == 0f && accumulatedDownPx < searchDeadzonePx) {
                    val absorb = minOf(searchDeadzonePx - accumulatedDownPx, remainingDown)
                    accumulatedDownPx += absorb
                    remainingDown -= absorb
                    totalConsumed += absorb
                    if (remainingDown <= 0f) return Offset(0f, totalConsumed)
                }

                if (remainingDown > 0f && searchRevealProgress.value < 1f) {
                    val neededSearchPx = (1f - searchRevealProgress.value) * searchTravelPx
                    val consumeSearch = minOf(neededSearchPx, remainingDown)
                    if (consumeSearch > 0f) {
                        scope.launch {
                            searchRevealProgress.snapTo(
                                (searchRevealProgress.value + consumeSearch / searchTravelPx).coerceIn(0f, 1f)
                            )
                        }
                        remainingDown -= consumeSearch
                        totalConsumed += consumeSearch
                    }
                }

                if (state.hasArchived && searchRevealProgress.value >= 0.98f && remainingDown > 0f && archivePullProgress.value < 1f) {
                    val neededPullPx = (1f - archivePullProgress.value) * archivePullTravelPx
                    val consumePull = minOf(neededPullPx, remainingDown)
                    if (consumePull > 0f) {
                        val nextProgress = (archivePullProgress.value + consumePull / archivePullTravelPx).coerceIn(0f, 1f)
                        scope.launch {
                            archivePullProgress.snapTo(nextProgress)
                        }
                        if (nextProgress >= 0.78f && !archiveHapticFired) {
                            archiveHapticFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        totalConsumed += consumePull
                    }
                }

                return if (totalConsumed > 0f) Offset(0f, totalConsumed) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                accumulatedDownPx = 0f
                val shouldOpenArchive = state.hasArchived && archivePullProgress.value >= 0.78f
                archiveHapticFired = false

                scope.launch {
                    archivePullProgress.animateTo(0f, PhysicsSprings.screenSpringSpec())
                }

                if (shouldOpenArchive) {
                    scope.launch { searchRevealProgress.snapTo(0f) }
                    onOpenArchive()
                    return Velocity.Zero
                }

                if (state.hasArchived) {
                    requestArchiveCollapse(archiveHideProgress.value > 0.5f)
                }

                // Резкий быстрый свайп вниз (высокая скорость) не фиксирует поиск, если он не дотянут почти полностью
                val isFastFlickDown = available.y > 1600f && searchRevealProgress.value < 0.85f
                val targetSearch = if (!isFastFlickDown && searchRevealProgress.value > 0.50f) 1f else 0f
                scope.launch {
                    searchRevealProgress.animateTo(targetSearch, PhysicsSprings.screenSpringSpec())
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

    AnonGramScreenSurface(
        modifier = modifier,
        applySystemBarsPadding = false
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
                        start = 0.dp,
                        end = 0.dp,
                        top = statusBarTop + 62.dp,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Поиск в стиле Telegram: скрыт по умолчанию, раскрывается при потягивании вниз
                    if (!state.isSelection) {
                        item(key = "telegram_search_bar") {
                            val reveal = searchRevealProgress.value
                            if (reveal > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp * reveal)
                                        .clipToBounds()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                                            .height(42.dp)
                                            .graphicsLayer {
                                                alpha = reveal
                                                translationY = -12.dp.toPx() * (1f - reveal)
                                            }
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
                        }

                        // Баннер «Потяните вниз для архива» -> «Отпустите для вывода архива» (как на 2-м скриншоте)
                        if (state.hasArchived) {
                            item(key = "archive_pull_banner") {
                                val pull = archivePullProgress.value
                                if (pull > 0.01f) {
                                    val isReady = pull >= 0.78f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp * pull)
                                            .clipToBounds()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                scheme.accent.copy(alpha = if (isReady) 0.85f else 0.32f + 0.30f * pull)
                                            )
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Вертикальная капсула-индикатор слева (как в Telegram)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(24.dp)
                                                .height(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.24f)),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(bottom = 2.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isReady) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = scheme.accent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isReady) archiveStrings.releaseToOpenArchive else archiveStrings.pullDownForArchive,
                                            color = if (isReady) scheme.onAccent else scheme.textPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.hasArchived) {
                        item(key = "archive_pill") {
                            val archiveReveal = 1f - archiveHideProgress.value
                            if (archiveReveal > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(74.dp * archiveReveal)
                                        .clipToBounds()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                                            .graphicsLayer {
                                                alpha = archiveReveal
                                                translationY = -18.dp.toPx() * (1f - archiveReveal)
                                            }
                                    ) {
                                        ArchivePill(
                                            chatCount = state.archivedSessions.size,
                                            preview = state.archivePreview,
                                            onClick = onOpenArchive
                                        )
                                    }
                                }
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
                                isArchiveMode = false,
                                onSwipeArchive = { viewModel.archiveSession(session.id, archived = true) },
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

        // 2. Закреплённая сверху прозрачная шапка с бесшовным матовым стеклом
        AnonGramGlassHeaderColumn(
            hazeState = hazeState,
            glassBlurEnabled = animState.glassBlurEnabled,
            scrollFraction = animatedHeaderAlpha,
            modifier = Modifier.align(Alignment.TopCenter)
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
                    onClearHistory = viewModel::onClearHistoryAction,
                    hazeState = hazeState,
                    glassBlurEnabled = animState.glassBlurEnabled
                )
            } else {
                ChatsTopBar(
                    onOpenSearch = onOpenSearch,
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
            Spacer(modifier = Modifier.height(24.dp))
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
