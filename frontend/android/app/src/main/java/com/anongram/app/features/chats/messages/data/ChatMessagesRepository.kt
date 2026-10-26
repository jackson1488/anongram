package com.anongram.app.features.chats.messages.data

import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageStatus

/**
 * Контракт хранилища переписки. InMemory-реализация обслуживает этап до
 * Rust-моста; FfiMessagesRepository заменит её за тем же интерфейсом.
 */
interface ChatMessagesRepository {
    suspend fun loadMessages(sessionId: String): List<ChatMessage>
    suspend fun addMessage(sessionId: String, message: ChatMessage)
    suspend fun updateStatus(sessionId: String, messageId: String, status: MessageStatus)
    suspend fun deleteMessage(sessionId: String, messageId: String)
    suspend fun clearHistory(sessionId: String)

    /** Демо-ответ собеседника (пока нет сети); null — ответа нет. */
    suspend fun generateAutoReply(sessionId: String): ChatMessage?
}
