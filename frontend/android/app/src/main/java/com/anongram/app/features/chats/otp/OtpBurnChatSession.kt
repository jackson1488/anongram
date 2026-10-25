package com.anongram.app.features.chats.otp

import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph

/** Стратегия OTP-чата на физическом одноразовом блокноте: страница сгорает после прочтения. */
data class OtpBurnChatSession(
    override val id: String,
    override val title: String,
    override val fingerprint: String,
    override val lastMessagePreview: String,
    override val lastMessageEpochMs: Long,
    override val unreadCount: Int = 0,
    override val isPinned: Boolean = false,
    override val isArchived: Boolean = false,
    override val mutedUntil: Long? = null,
    val pagesLeft: Int
) : IChatSession {

    override val isOnline: Boolean = false
    override val tier: ChatSecurityTier = ChatSecurityTier.OTP_PAD

    override val avatarGlyph: SessionGlyph = SessionGlyph.FLAME
    override val burnsWithEmbers: Boolean = true
    override val deleteActionLabel: String = "Сжечь пад (необратимо)"
    override val padPagesLeft: Int? get() = pagesLeft

    override fun withPinned(pinned: Boolean): IChatSession = copy(isPinned = pinned)

    override fun withArchived(archived: Boolean): IChatSession = copy(isArchived = archived)

    override fun withMutedUntil(untilEpochMs: Long?): IChatSession =
        copy(mutedUntil = untilEpochMs)

    override fun withHistoryCleared(): IChatSession =
        copy(lastMessagePreview = "", unreadCount = 0)
}
