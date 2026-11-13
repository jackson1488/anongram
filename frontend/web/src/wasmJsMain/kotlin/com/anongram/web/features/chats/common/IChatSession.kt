package com.anongram.web.features.chats.common

import com.anongram.web.features.chats.messages.domain.MessageAttachment

interface IChatSession {
    val id: String
    val title: String
    val fingerprint: String
    val lastMessagePreview: String
    val lastMessageEpochMs: Long
    val unreadCount: Int
    val isPinned: Boolean
    val isOnline: Boolean
    val isArchived: Boolean
    val lastMessageAttachment: MessageAttachment? get() = null
    val tier: ChatSecurityTier
    val securityLabel: String get() = tier.label
    val avatarGlyph: SessionGlyph
    val avatarMonogram: String? get() = null
    val burnsWithEmbers: Boolean get() = false
    val deleteActionLabel: String get() = "Удалить чат"
    val padPagesLeft: Int? get() = null
    val mutedUntil: Long?
    val isMuted: Boolean get() = mutedUntil != null

    fun withPinned(pinned: Boolean): IChatSession
    fun withArchived(archived: Boolean): IChatSession
    fun withMutedUntil(untilEpochMs: Long?): IChatSession
    fun withHistoryCleared(): IChatSession
    fun withLastMessage(
        preview: String,
        epochMs: Long,
        attachment: MessageAttachment? = null
    ): IChatSession
}
