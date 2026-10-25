package com.anongram.app.features.chats.data

import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.direct.DirectChatSession
import com.anongram.app.features.chats.group.GroupChatSession
import com.anongram.app.features.chats.otp.OtpBurnChatSession
import java.util.concurrent.atomic.AtomicLong

/**
 * Локальное хранилище сессий pre-bridge этапа: демонстрационный набор стратегий
 * Direct / Group / OTP-Burn. Архив при первом заходе пуст — пилюля архива не
 * существует, пока пользователь сам не заархивирует чат.
 */
class InMemoryChatListRepository : ChatListRepository {

    private val nextId = AtomicLong(100)

    private val sessions = mutableListOf<IChatSession>(
        GroupChatSession(
            id = "1",
            title = "Cypherpunk Alpha",
            fingerprint = "0x7F2A…9C",
            lastMessagePreview = "Ratchet session verified. Strict pairwise E2EE",
            lastMessageEpochMs = System.currentTimeMillis() - 4 * 60_000L,
            isPinned = true,
            memberCount = 6
        ),
        DirectChatSession(
            id = "2",
            title = "Алиса",
            fingerprint = "0x4D9F…88E1",
            lastMessagePreview = "Файл отчёта зашифрован ChaCha20-Poly1305.",
            lastMessageEpochMs = System.currentTimeMillis() - 10 * 60_000L,
            isPinned = true,
            isOnline = true,
            unreadCount = 2
        ),
        DirectChatSession(
            id = "3",
            title = "Боб",
            fingerprint = "0x1B77…D3A0",
            lastMessagePreview = "Квантово-стойкий SAS код подтверждён.",
            lastMessageEpochMs = System.currentTimeMillis() - 28 * 60_000L,
            tier = ChatSecurityTier.POST_QUANTUM
        ),
        GroupChatSession(
            id = "4",
            title = "Onion Relay Ops",
            fingerprint = "0x99C2…04FF",
            lastMessagePreview = "Новый луковый узел в сети: 3 hops",
            lastMessageEpochMs = System.currentTimeMillis() - 112 * 60_000L,
            memberCount = 12
        ),
        OtpBurnChatSession(
            id = "5",
            title = "OTP Vault",
            fingerprint = "0xE210…7B44",
            lastMessagePreview = "Одноразовый секретный блокнот активен",
            lastMessageEpochMs = System.currentTimeMillis() - 5 * 60 * 60_000L,
            pagesLeft = 7
        )
    )

    override suspend fun loadSessions(): List<IChatSession> = sessions.toList()

    override suspend fun setPinned(
        sessionIds: Collection<String>,
        pinned: Boolean
    ): List<IChatSession> {
        mutateAll(sessionIds) { it.withPinned(pinned) }
        return sessions.toList()
    }

    override suspend fun setMuted(
        sessionIds: Collection<String>,
        untilEpochMs: Long?
    ): List<IChatSession> {
        mutateAll(sessionIds) { it.withMutedUntil(untilEpochMs) }
        return sessions.toList()
    }

    override suspend fun setArchived(
        sessionIds: Collection<String>,
        archived: Boolean
    ): List<IChatSession> {
        mutateAll(sessionIds) { it.withArchived(archived) }
        return sessions.toList()
    }

    override suspend fun markAsRead(sessionIds: Collection<String>): List<IChatSession> {
        mutateAll(sessionIds) {
            when (it) {
                is DirectChatSession -> it.copy(unreadCount = 0)
                is GroupChatSession -> it.copy(unreadCount = 0)
                is OtpBurnChatSession -> it.copy(unreadCount = 0)
                else -> it
            }
        }
        return sessions.toList()
    }

    override suspend fun clearHistory(
        sessionIds: Collection<String>,
        forEveryone: Boolean
    ): List<IChatSession> {
        mutateAll(sessionIds) { it.withHistoryCleared() }
        return sessions.toList()
    }

    override suspend fun deleteSessions(
        sessionIds: Collection<String>,
        alsoForPeer: Boolean
    ): List<IChatSession> {
        sessions.removeAll { it.id in sessionIds }
        return sessions.toList()
    }

    override suspend fun findOrCreateDirectChat(name: String, fingerprint: String): IChatSession {
        sessions.firstOrNull { it is DirectChatSession && it.fingerprint == fingerprint }
            ?.let { return it }

        val created = DirectChatSession(
            id = "d${nextId.incrementAndGet()}",
            title = name,
            fingerprint = fingerprint,
            lastMessagePreview = "",
            lastMessageEpochMs = System.currentTimeMillis()
        )
        sessions.add(created)
        return created
    }

    private fun mutateAll(
        sessionIds: Collection<String>,
        transform: (IChatSession) -> IChatSession
    ) {
        for (index in sessions.indices) {
            val session = sessions[index]
            if (session.id in sessionIds) {
                sessions[index] = transform(session)
            }
        }
    }
}
