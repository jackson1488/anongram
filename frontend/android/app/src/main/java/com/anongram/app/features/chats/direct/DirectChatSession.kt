package com.anongram.app.features.chats.direct

import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph

/** Стратегия личного P2P-чата 1:1 (Double Ratchet между двумя устройствами). */
data class DirectChatSession(
    override val id: String,
    override val title: String,
    override val fingerprint: String,
    override val lastMessagePreview: String,
    override val lastMessageEpochMs: Long,
    override val unreadCount: Int = 0,
    override val isPinned: Boolean = false,
    override val isOnline: Boolean = false,
    override val isArchived: Boolean = false,
    override val tier: ChatSecurityTier = ChatSecurityTier.STANDARD,
    override val mutedUntil: Long? = null
) : IChatSession {

    override val avatarGlyph: SessionGlyph = SessionGlyph.MONOGRAM

    override val avatarMonogram: String
        get() = title.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    override fun withPinned(pinned: Boolean): IChatSession = copy(isPinned = pinned)

    override fun withArchived(archived: Boolean): IChatSession = copy(isArchived = archived)

    override fun withMutedUntil(untilEpochMs: Long?): IChatSession =
        copy(mutedUntil = untilEpochMs)

    override fun withHistoryCleared(): IChatSession =
        copy(lastMessagePreview = "", unreadCount = 0)
}
