package com.anongram.web.features.chats.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.common.timeLabel
import com.anongram.web.features.chats.mainmenu.components.ChatListDialogs
import com.anongram.web.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.web.features.chats.mainmenu.components.EmptyChatsState
import com.anongram.web.features.chats.mainmenu.components.SearchRow
import com.anongram.web.features.chats.mainmenu.components.SelectionTopBar
import com.anongram.web.features.chats.state.ChatListViewModel
import com.anongram.web.platform.PlatformBackHandler

@Composable
fun ArchiveScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onChatOpen: (IChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val archived = if (query.isBlank()) {
        state.archivedSessions
    } else {
        state.archivedSessions.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.fingerprint.contains(query, ignoreCase = true)
        }
    }

    PlatformBackHandler(enabled = state.isSelection) {
        viewModel.clearSelection()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
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
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = scheme.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Архив",
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Меню", tint = scheme.textPrimary)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = scheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Выбрать чаты", color = scheme.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.SelectAll, contentDescription = null, tint = scheme.textPrimary) },
                            onClick = { menuOpen = false; viewModel.selectAllSessions(archived = true) }
                        )
                    }
                }
            }
        }

        SearchRow(query = query, onQueryChange = { query = it }, placeholder = "Поиск в архиве")

        if (archived.isEmpty()) {
            EmptyChatsState(
                title = "Архив пуст",
                subtitle = "Скрытые чаты появятся здесь"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(archived, key = { it.id }) { session ->
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

    ChatListDialogs(state = state, viewModel = viewModel)
}
