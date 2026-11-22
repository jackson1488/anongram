package com.anongram.app.features.chats.archive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.archive.i18n.ArchiveI18n
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.timeLabel
import com.anongram.app.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.app.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.app.features.chats.mainmenu.components.SearchRow
import com.anongram.app.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.app.features.chats.state.ChatListViewModel
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.common.ui.SettingsGlassBackButton
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Экран «Архив чатов» (Telegram-style):
 * - Бесшовный прогрессивный блюр шапки (`HazeProgressive.verticalGradient`) и стеклянные кнопки;
 * - Меню `⋮`: «Настройки архива», «Как это работает?», «Выбрать чаты»;
 * - Полная поддержка тем (`LocalAnonGramScheme`) и локализации (`ArchiveI18n`).
 */
@Composable
fun ArchiveScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onChatOpen: (IChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ArchiveI18n.current
    val state by viewModel.uiState.collectAsState()
    val animState by AnimationsSettingsStore.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var query by remember { mutableStateOf("") }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isInfoSheetOpen by remember { mutableStateOf(false) }

    if (isSettingsOpen) {
        ArchiveSettingsScreen(
            onBack = { isSettingsOpen = false },
            modifier = modifier
        )
        return
    }

    val archived = if (query.isBlank()) {
        state.archivedSessions
    } else {
        state.archivedSessions.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.fingerprint.contains(query, ignoreCase = true)
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12
        }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        label = "archiveHeaderAlpha"
    )
    val themedHeaderTint = androidx.compose.ui.graphics.lerp(
        scheme.card,
        scheme.accent,
        if (scheme.isDark) 0.22f else 0.14f
    )

    val headerBackgroundModifier = when {
        animatedHeaderAlpha <= 0.05f -> Modifier
        animState.glassBlurEnabled -> Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = scheme.background.copy(alpha = 0.35f * animatedHeaderAlpha),
                tint = HazeTint(color = themedHeaderTint.copy(alpha = 0.52f * animatedHeaderAlpha)),
                blurRadius = 22.dp,
                noiseFactor = 0.02f
            )
        ) {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f
            )
        }
        else -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    themedHeaderTint.copy(alpha = 0.88f * animatedHeaderAlpha),
                    scheme.background.copy(alpha = 0.45f * animatedHeaderAlpha),
                    Color.Transparent
                )
            )
        )
    }

    BackHandler(enabled = isInfoSheetOpen || state.isSelection) {
        when {
            isInfoSheetOpen -> isInfoSheetOpen = false
            state.isSelection -> viewModel.clearSelection()
        }
    }

    AnonGramScreenSurface(
        modifier = modifier,
        applySystemBarsPadding = false
    ) {
        // 1. Основной скроллируемый контент (проезжает под прозрачной блюр-шапкой)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(scheme.background)
        ) {
            if (archived.isEmpty() && query.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = scheme.accent.copy(alpha = 0.75f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.archiveEmptyTitle,
                            color = scheme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
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
                    if (!state.isSelection) {
                        item(key = "archive_search") {
                            SearchRow(
                                query = query,
                                onQueryChange = { query = it },
                                placeholder = strings.searchChatsPlaceholder,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                            )
                        }
                    }

                    items(archived, key = { it.id }) { session ->
                        ChatListItemCard(
                            session = session,
                            timeLabel = remember(session.id, session.lastMessageEpochMs) {
                                session.timeLabel()
                            },
                            selectionMode = state.isSelection,
                            isSelected = session.id in state.selectedIds,
                            isArchiveMode = true,
                            onSwipeArchive = { viewModel.archiveSession(session.id, archived = false) },
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

        // 2. Парящая прозрачная шапка с бесшовным блюром
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
                    isArchiveMode = true,
                    onClose = viewModel::clearSelection,
                    onTogglePin = viewModel::onTogglePinAction,
                    onDelete = viewModel::onDeleteAction,
                    onMute = viewModel::onMuteAction,
                    onArchive = { viewModel.archiveSelected(false) },
                    onMarkRead = viewModel::onMarkReadAction,
                    onClearHistory = viewModel::onClearHistoryAction,
                    hazeState = hazeState,
                    glassBlurEnabled = animState.glassBlurEnabled
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeStatusBarPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsGlassBackButton(
                        hazeState = hazeState,
                        onClick = onBack,
                        contentDescription = strings.back,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.archiveTitle,
                        fontWeight = FontWeight.Bold,
                        color = scheme.textPrimary,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ArchiveMenu(
                        hazeState = hazeState,
                        glassBlurEnabled = animState.glassBlurEnabled,
                        strings = strings,
                        onOpenSettings = { isSettingsOpen = true },
                        onOpenInfo = { isInfoSheetOpen = true },
                        onSelectAll = { viewModel.selectAllSessions(archived = true) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Шторка «Это Ваш архив»
        AnimatedVisibility(
            visible = isInfoSheetOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ArchiveInfoBottomSheet(
                onDismiss = { isInfoSheetOpen = false },
                onOpenSettings = { isSettingsOpen = true }
            )
        }

        ChatListDialogs(state = state, viewModel = viewModel)
    }
}

/**
 * Меню ⋮ архива (по 4-му скриншоту Telegram):
 * - Настройки архива
 * - Как это работает?
 * - Выбрать чаты
 */
@Composable
private fun ArchiveMenu(
    hazeState: HazeState,
    glassBlurEnabled: Boolean,
    strings: com.anongram.app.features.chats.archive.i18n.ArchiveI18nStrings,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit,
    onSelectAll: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .settingsGlassPill(
                    hazeState = hazeState,
                    shape = CircleShape,
                    glassBlurEnabled = glassBlurEnabled
                )
                .clickable { menuOpen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = scheme.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = scheme.card,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Text(text = strings.archiveSettingsMenu, color = scheme.textPrimary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = scheme.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onOpenSettings()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(text = strings.howItWorksMenu, color = scheme.textPrimary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = scheme.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onOpenInfo()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(text = strings.selectChats, color = scheme.textPrimary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.SelectAll,
                        contentDescription = null,
                        tint = scheme.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                },
                onClick = {
                    menuOpen = false
                    onSelectAll()
                }
            )
        }
    }
}
