package com.anongram.app.features.chats.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.MuteDuration
import com.anongram.app.features.chats.data.ChatListRepository
import com.anongram.app.features.chats.data.InMemoryChatListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State-холдер главного меню чатов: режим выбора, мьют с длительностью,
 * архив, диалоги подтверждения. Compose-слой только читает [uiState]
 * и зовёт интенты-методы.
 */
class ChatListViewModel(private val repository: ChatListRepository) : ViewModel() {

    /** Пустой конструктор для viewModel() до внедрения DI-контейнера. */
    constructor() : this(InMemoryChatListRepository())

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = repository.loadSessions()
            _uiState.update { it.copy(sessions = loaded, isLoading = false) }
        }
    }

    // --- Режим выбора ---

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

    // --- Действия панели выбора ---

    fun onTogglePinAction() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        viewModelScope.launch {
            publish(repository.setPinned(state.selectedIds, !state.allSelectedPinned))
        }
    }

    /**
     * «Без звука»: все выбранные уже замьючены → мгновенно включить звук;
     * иначе открыть bottom-лист выбора длительности.
     */
    fun onMuteAction() {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        if (state.allSelectedMuted) {
            viewModelScope.launch {
                publish(repository.setMuted(state.selectedIds, null))
            }
        } else {
            _uiState.update { it.copy(muteSheetFor = it.selectedIds) }
        }
    }

    fun muteFor(duration: MuteDuration) {
        val targets = _uiState.value.muteSheetFor ?: return
        viewModelScope.launch {
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
        viewModelScope.launch {
            publish(repository.deleteSessions(targets, alsoForPeer))
            _uiState.update { it.copy(deleteDialogFor = null, selectedIds = emptySet()) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogFor = null) }
    }

    /** Архивация всегда раскрывает пилюлю архива, чтобы результат был виден. */
    fun archiveSelected(archived: Boolean) {
        val targets = _uiState.value.selectedIds
        if (targets.isEmpty()) return
        viewModelScope.launch {
            publish(repository.setArchived(targets, archived))
            _uiState.update {
                it.copy(selectedIds = emptySet(), isArchiveCollapsed = if (archived) false else it.isArchiveCollapsed)
            }
        }
    }

    fun onMarkReadAction() {
        val targets = _uiState.value.selectedIds
        if (targets.isEmpty()) return
        viewModelScope.launch {
            publish(repository.markAsRead(targets))
            _uiState.update { it.copy(selectedIds = emptySet()) }
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
        viewModelScope.launch {
            publish(repository.clearHistory(targets, forEveryone))
            _uiState.update { it.copy(clearDialogFor = null, selectedIds = emptySet()) }
        }
    }

    fun dismissClearDialog() {
        _uiState.update { it.copy(clearDialogFor = null) }
    }

    // --- Архив ---

    /** Коммит жеста скрытия/показа пилюли архива. */
    fun setArchiveCollapsed(collapsed: Boolean) {
        _uiState.update { it.copy(isArchiveCollapsed = collapsed) }
    }

    // --- Контакты ---

    /** Найти личный чат по отпечатку или создать новый (вход из контактов/поиска). */
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
