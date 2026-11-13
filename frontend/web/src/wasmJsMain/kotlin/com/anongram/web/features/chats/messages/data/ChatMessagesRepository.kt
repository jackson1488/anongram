package com.anongram.web.features.chats.messages.data

import com.anongram.web.features.chats.messages.domain.ChatMessage
import com.anongram.web.features.chats.messages.domain.MessageStatus

interface ChatMessagesRepository {
    suspend fun loadMessages(sessionId: String): List<ChatMessage>
    suspend fun loadOlder(sessionId: String, beforeEpochMs: Long): List<ChatMessage>
    suspend fun hasMoreOlder(sessionId: String): Boolean
    suspend fun addMessage(sessionId: String, message: ChatMessage)
    suspend fun updateStatus(sessionId: String, messageId: String, status: MessageStatus)
    suspend fun updateMessage(sessionId: String, message: ChatMessage)
    suspend fun deleteMessage(sessionId: String, messageId: String)
    suspend fun markAllRead(sessionId: String)
    suspend fun clearHistory(sessionId: String)
    suspend fun generateAutoReply(sessionId: String): ChatMessage?
}
