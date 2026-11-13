package com.anongram.web.model

enum class MessageDirection { INCOMING, OUTGOING }

enum class MessageStatus { SENDING, SENT, READ, FAILED }

data class WebChatMessage(
    val id: String,
    val direction: MessageDirection,
    val text: String,
    val time: String,
    val status: MessageStatus = MessageStatus.READ,
    val isEdited: Boolean = false
)

data class WebChatSession(
    val id: String,
    val title: String,
    val fingerprint: String,
    val lastMessagePreview: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isGroup: Boolean = false,
    val memberCount: Int? = null
)
