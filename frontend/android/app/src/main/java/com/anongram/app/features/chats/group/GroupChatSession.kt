package com.anongram.app.features.chats.group

import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph

/** Стратегия группового чата: pairwise E2EE ко всем участникам поверх onion-маршрута. */
data class GroupChatSession(
    override val id: String,
    override val title: String,
    override val fingerprint: String,
    override val lastMessagePreview: String,
    override val lastMessageEpochMs: Long,
    override val unreadCount: Int = 0,
    override val isPinned: Boolean = false,
    override val isArchived: Boolean = false,
    override val tier: ChatSecurityTier = ChatSecurityTier.POST_QUANTUM,
    override val mutedUntil: Long? = null,
    val memberCount: Int = 2
) : IChatSession {

    override val isOnline: Boolean = false

    override val avatarGlyph: SessionGlyph = SessionGlyph.GROUP
    override val deleteActionLabel: String = "Покинуть чат"

    override fun withPinned(pinned: Boolean): IChatSession = copy(isPinned = pinned)

    override fun withArchived(archived: Boolean): IChatSession = copy(isArchived = archived)

    override fun withMutedUntil(untilEpochMs: Long?): IChatSession =
        copy(mutedUntil = untilEpochMs)

    override fun withHistoryCleared(): IChatSession =
        copy(lastMessagePreview = "", unreadCount = 0)
}
