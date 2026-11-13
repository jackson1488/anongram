package com.anongram.web.features.chats.state

import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.common.MuteDuration
import com.anongram.web.features.chats.data.ChatListRepository
import com.anongram.web.features.chats.data.InMemoryChatListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatListRepository = InMemoryChatListRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        refresh()
        scope.launch {
            repository.sessionsFlow.collect { updatedSessions ->
                _uiState.update { state ->
                    state.copy(sessions = updatedSessions, isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        scope.launch {
            val loaded = repository.loadSessions()
            val drafts = repository.loadDrafts()
            _uiState.update { it.copy(sessions = loaded, drafts = drafts, isLoading = false) }
        }
    }

    fun selectAllSessions(archived: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                selectedIds = state.sessions
                    .filter { it.isArchived == archived }
                    .map { it.id }
                    .toSet()
            )
        }
    }

    fun enterSelection(sessionId: String) {
        _uiState.update { it.copy(selectedIds = setOf(sessionId)) }
    }

    fun toggleSelection(sessionId: String) {
        _uiState.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (!next.add(sessionId)) next.remove(sessionId)
            state.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedIds = emptySet(),
                muteSheetFor = null,
                deleteDialogFor = null,
                clearDialogFor = null
            )
        }
    }

    fun onTogglePinAction() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        scope.launch {
            publish(repository.setPinned(state.selectedIds, !state.allSelectedPinned))
            _uiState.update { it.copy(selectedIds = emptySet()) }
        }
    }

    fun onMuteAction() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        if (state.allSelectedMuted) {
            scope.launch {
                publish(repository.setMuted(state.selectedIds, null))
            }
        } else {
            _uiState.update { it.copy(muteSheetFor = it.selectedIds) }
        }
    }

    fun muteFor(duration: MuteDuration) {
        val targets = _uiState.value.muteSheetFor ?: return
        scope.launch {
            publish(repository.setMuted(targets, duration.untilEpochMs()))
            _uiState.update { it.copy(muteSheetFor = null, selectedIds = emptySet()) }
        }
    }

    fun dismissMuteSheet() {
        _uiState.update { it.copy(muteSheetFor = null) }
    }

    fun onDeleteAction() {
        _uiState.update { state ->
            if (state.selectedIds.isEmpty()) state
            else state.copy(deleteDialogFor = state.selectedIds)
        }
    }

    fun confirmDeleteSessions(alsoForPeer: Boolean) {
        val targets = _uiState.value.deleteDialogFor ?: return
        scope.launch {
            publish(repository.deleteSessions(targets, alsoForPeer))
            _uiState.update { it.copy(deleteDialogFor = null, selectedIds = emptySet()) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogFor = null) }
    }

    fun archiveSelected(archived: Boolean) {
        val targets = _uiState.value.selectedIds
        if (targets.isEmpty()) return
        scope.launch {
            publish(repository.setArchived(targets, archived))
            _uiState.update {
                it.copy(selectedIds = emptySet(), isArchiveCollapsed = if (archived) false else it.isArchiveCollapsed)
            }
        }
    }

    fun onMarkReadAction() {
        val targets = _uiState.value.selectedIds
        if (targets.isEmpty()) return
        scope.launch {
            publish(repository.markAsRead(targets))
            _uiState.update { it.copy(selectedIds = emptySet()) }
        }
    }

    fun markRead(sessionId: String) {
        scope.launch {
            publish(repository.markAsRead(listOf(sessionId)))
        }
    }

    fun onClearHistoryAction() {
        _uiState.update { state ->
            if (state.selectedIds.isEmpty()) state
            else state.copy(clearDialogFor = state.selectedIds)
        }
    }

    fun confirmClearHistory(forEveryone: Boolean) {
        val targets = _uiState.value.clearDialogFor ?: return
        scope.launch {
            publish(repository.clearHistory(targets, forEveryone))
            _uiState.update { it.copy(clearDialogFor = null, selectedIds = emptySet()) }
        }
    }

    fun dismissClearDialog() {
        _uiState.update { it.copy(clearDialogFor = null) }
    }

    fun setArchiveCollapsed(collapsed: Boolean) {
        _uiState.update { it.copy(isArchiveCollapsed = collapsed) }
    }

    suspend fun findOrCreateDirectChat(name: String, fingerprint: String): IChatSession {
        val session = repository.findOrCreateDirectChat(name, fingerprint)
        _uiState.update { state ->
            val sessions = if (state.sessions.any { it.id == session.id }) {
                state.sessions.map { if (it.id == session.id) session else it }
            } else {
                state.sessions + session
            }
            state.copy(sessions = sessions)
        }
        return session
    }

    private fun publish(sessions: List<IChatSession>) {
        _uiState.update { state ->
            state.copy(
                sessions = sessions,
                selectedIds = state.selectedIds
                    .mapNotNull { id -> sessions.firstOrNull { it.id == id }?.id }
                    .toSet()
            )
        }
    }
}
