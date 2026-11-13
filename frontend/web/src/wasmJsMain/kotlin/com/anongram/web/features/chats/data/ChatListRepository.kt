package com.anongram.web.features.chats.data

import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.messages.domain.MessageAttachment
import kotlinx.coroutines.flow.StateFlow

interface ChatListRepository {
    val sessionsFlow: StateFlow<List<IChatSession>>

    suspend fun loadSessions(): List<IChatSession>
    suspend fun setPinned(sessionIds: Collection<String>, pinned: Boolean): List<IChatSession>
    suspend fun setMuted(sessionIds: Collection<String>, untilEpochMs: Long?): List<IChatSession>
    suspend fun setArchived(sessionIds: Collection<String>, archived: Boolean): List<IChatSession>
    suspend fun markAsRead(sessionIds: Collection<String>): List<IChatSession>
    suspend fun clearHistory(sessionIds: Collection<String>, forEveryone: Boolean): List<IChatSession>
    suspend fun deleteSessions(sessionIds: Collection<String>, alsoForPeer: Boolean): List<IChatSession>
    suspend fun findOrCreateDirectChat(name: String, fingerprint: String): IChatSession
    suspend fun saveDraft(sessionId: String, text: String)
    suspend fun loadDrafts(): Map<String, String>
    suspend fun updateLastMessage(
        sessionId: String,
        preview: String,
        epochMs: Long,
        attachment: MessageAttachment? = null
    ): List<IChatSession>
}
