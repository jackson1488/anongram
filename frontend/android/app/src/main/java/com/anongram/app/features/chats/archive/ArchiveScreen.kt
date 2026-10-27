package com.anongram.app.features.chats.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.timeLabel
import com.anongram.app.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.app.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.app.features.chats.mainmenu.components.SearchRow
import com.anongram.app.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.app.features.chats.state.ChatListViewModel

/**
 * Экран архива (как в Telegram): назад, заголовок, три точки с «Выбрать чаты»,
 * строка поиска, плоский список архивных чатов и режим выбора
 * с «Из архива» в меню точек. Делит ChatListViewModel с главным меню.
 */
@Composable
fun ArchiveScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onChatOpen: (IChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }

    val archived = if (query.isBlank()) {
        state.archivedSessions
    } else {
        state.archivedSessions.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.fingerprint.contains(query, ignoreCase = true)
        }
    }

    BackHandler(enabled = state.isSelection) {
        viewModel.clearSelection()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
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
                onClearHistory = viewModel::onClearHistoryAction
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Архив",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                ArchiveMenu(onSelectAll = { viewModel.selectAllSessions(archived = true) })
            }

            SearchRow(
                query = query,
                onQueryChange = { query = it }
            )
        }

        if (archived.isEmpty()) {
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
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = if (query.isBlank()) "Архив пуст" else "Ничего не найдено",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = if (query.isBlank()) {
                            "Заархивируйте чат через режим выбора"
                        } else {
                            "Попробуйте другое имя или отпечаток"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(archived, key = { it.id }) { session ->
                    ChatListItemCard(
                        session = session,
                        timeLabel = remember(session.id, session.lastMessageEpochMs) {
                            session.timeLabel()
                        },
                        selectionMode = state.isSelection,
                        isSelected = session.id in state.selectedIds,
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

    ChatListDialogs(state = state, viewModel = viewModel)
}

/** Три точки экрана архива: выбор всех архивных чатов. */
@Composable
private fun ArchiveMenu(onSelectAll: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Меню архива",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = CardDark
        ) {
            DropdownMenuItem(
                text = {
                    Text(text = "Выбрать чаты", color = TextPrimary, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.SelectAll,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
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
