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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.timeLabel
import com.anongram.app.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.app.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.app.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.app.features.chats.state.ChatListViewModel

/**
 * Экран архива: те же карточки-стратегии, панель выбора с «убрать из архива».
 * Делит ChatListViewModel с главным меню — состояние едино.
 */
@Composable
fun ArchiveScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onChatOpen: (IChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                onMute = viewModel::onMuteAction,
                onDelete = viewModel::onDeleteAction,
                onArchive = { viewModel.archiveSelected(false) },
                onMarkRead = viewModel::onMarkReadAction,
                onClearHistory = viewModel::onClearHistoryAction
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Архив",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }
        }

        if (state.archivedSessions.isEmpty()) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Архив пуст",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Заархивируйте чат долгим нажатием в списке",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.archivedSessions, key = { it.id }) { session ->
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
