package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.runtime.Composable
import com.anongram.app.features.chats.state.ChatListUiState
import com.anongram.app.features.chats.state.ChatListViewModel

/**
 * Единая точка показа диалогов/листов списка чатов — используется и главным
 * меню, и экраном архива, чтобы режим выбора вёл себя одинаково везде.
 */
@Composable
internal fun ChatListDialogs(
    state: ChatListUiState,
    viewModel: ChatListViewModel
) {
    state.muteSheetFor?.let {
        MuteDurationSheet(
            onSelect = viewModel::muteFor,
            onDismiss = viewModel::dismissMuteSheet
        )
    }

    state.deleteDialogFor?.let {
        DeleteSessionsDialog(
            sessions = state.selectedSessions,
            onConfirm = viewModel::confirmDeleteSessions,
            onDismiss = viewModel::dismissDeleteDialog
        )
    }

    state.clearDialogFor?.let {
        ClearHistoryDialog(
            sessions = state.selectedSessions,
            onConfirm = viewModel::confirmClearHistory,
            onDismiss = viewModel::dismissClearDialog
        )
    }
}
