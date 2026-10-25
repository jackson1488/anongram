package com.anongram.app.features.chats.data

import com.anongram.app.features.chats.common.IChatSession

/**
 * Контракт локального хранилища чат-сессий.
 *
 * InMemory-реализация обслуживает этап до подключения Rust-моста; с появлением
 * ICryptoBridge заменяется на FfiChatListRepository за тем же интерфейсом —
 * без единой правки в ViewModel и UI. Флаги alsoForPeer / forEveryone в сетевой
 * версии управляют удалением у собеседника; локально обе семантики эквивалентны.
 */
interface ChatListRepository {
    suspend fun loadSessions(): List<IChatSession>

    suspend fun setPinned(sessionIds: Collection<String>, pinned: Boolean): List<IChatSession>

    /** untilEpochMs: null — включить звук, 0L — навсегда, иначе epoch millis. */
    suspend fun setMuted(sessionIds: Collection<String>, untilEpochMs: Long?): List<IChatSession>

    suspend fun setArchived(sessionIds: Collection<String>, archived: Boolean): List<IChatSession>

    suspend fun markAsRead(sessionIds: Collection<String>): List<IChatSession>

    suspend fun clearHistory(
        sessionIds: Collection<String>,
        forEveryone: Boolean
    ): List<IChatSession>

    suspend fun deleteSessions(
        sessionIds: Collection<String>,
        alsoForPeer: Boolean
    ): List<IChatSession>

    /** Возвращает существующий личный чат по отпечатку либо создаёт новый. */
    suspend fun findOrCreateDirectChat(name: String, fingerprint: String): IChatSession
}
