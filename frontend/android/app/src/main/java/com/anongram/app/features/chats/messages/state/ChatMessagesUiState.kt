package com.anongram.app.features.chats.messages.state

import com.anongram.app.features.chats.messages.domain.ChatMessage

/** Неизменяемое состояние открытого чата; UI только отображает его. */
data class ChatMessagesUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val replyTo: ChatMessage? = null,
    val isPeerTyping: Boolean = false
) {
    val canSend: Boolean get() = input.isNotBlank()
}
