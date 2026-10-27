package com.anongram.app.features.chats.state

import com.anongram.app.features.chats.common.IChatSession

/**
 * Неизменяемое состояние списка чатов: единственный источник истины для UI.
 * Диалоги представлены не-null множествами целевых id (открыт = не null).
 */
data class ChatListUiState(
    val sessions: List<IChatSession> = emptyList(),
    val drafts: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val onionCircuitOnline: Boolean = true,
    val selectedIds: Set<String> = emptySet(),
    val isArchiveCollapsed: Boolean = true,
    val muteSheetFor: Set<String>? = null,
    val deleteDialogFor: Set<String>? = null,
    val clearDialogFor: Set<String>? = null
) {
    val activeSessions: List<IChatSession>
        get() = sessions
            .filter { !it.isArchived }
            .sortedWith(
                compareByDescending<IChatSession> { it.isPinned }
                    .thenByDescending { it.lastMessageEpochMs }
            )

    val archivedSessions: List<IChatSession>
        get() = sessions
            .filter { it.isArchived }
            .sortedByDescending { it.lastMessageEpochMs }

    /** Пилюля архива существует только при непустом архиве. */
    val hasArchived: Boolean get() = archivedSessions.isNotEmpty()

    /** Превью строки архива: до двух названий через запятую + «и ещё N». */
    val archivePreview: String
        get() {
            val titles = archivedSessions.map { it.title }
            return when {
                titles.size <= 2 -> titles.joinToString(", ")
                else -> "${titles[0]}, ${titles[1]} и ещё ${titles.size - 2}"
            }
        }

    val isSelection: Boolean get() = selectedIds.isNotEmpty()

    val selectedSessions: List<IChatSession>
        get() = sessions.filter { it.id in selectedIds }

    val allSelectedMuted: Boolean
        get() = selectedSessions.isNotEmpty() && selectedSessions.all { it.isMuted }

    val allSelectedPinned: Boolean
        get() = selectedSessions.isNotEmpty() && selectedSessions.all { it.isPinned }
}
