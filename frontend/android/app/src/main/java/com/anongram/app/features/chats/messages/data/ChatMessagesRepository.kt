package com.anongram.app.features.chats.messages.data

import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageStatus

/**
 * Контракт хранилища переписки (FakeRepository до Rust-моста): треды,
 * постраничная подгрузка истории, черновики не здесь (они в ChatListRepository).
 */
interface ChatMessagesRepository {
    suspend fun loadMessages(sessionId: String): List<ChatMessage>

    /** Порция истории старше указанного сообщения (paging без прыжков позиции). */
    suspend fun loadOlder(sessionId: String, beforeEpochMs: Long): List<ChatMessage>

    suspend fun hasMoreOlder(sessionId: String): Boolean

    suspend fun addMessage(sessionId: String, message: ChatMessage)
    suspend fun updateStatus(sessionId: String, messageId: String, status: MessageStatus)
    suspend fun updateMessage(sessionId: String, message: ChatMessage)
    suspend fun deleteMessage(sessionId: String, messageId: String)
    suspend fun markAllRead(sessionId: String)
    suspend fun clearHistory(sessionId: String)

    /** Демо-ответ собеседника (может быть голосовым); null — ответа нет. */
    suspend fun generateAutoReply(sessionId: String): ChatMessage?
}
