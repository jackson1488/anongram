package com.anongram.app.features.chats.data

import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.direct.DirectChatSession
import com.anongram.app.features.chats.group.GroupChatSession
import com.anongram.app.features.chats.otp.OtpBurnChatSession
import java.util.concurrent.atomic.AtomicLong

/**
 * Флаг режима разработки/тестирования: легко отключить или убрать при релизе.
 */
const val DEV_TEST_DATA_ENABLED = true

/**
 * Локальное хранилище сессий pre-bridge этапа: демонстрационный набор стратегий
 * Direct / Group / OTP-Burn. Архив при первом заходе пуст — пилюля архива не
 * существует, пока пользователь сам не заархивирует чат.
 */
class InMemoryChatListRepository : ChatListRepository {

    private val nextId = AtomicLong(100)

    private val sessions = if (DEV_TEST_DATA_ENABLED) {
        val now = System.currentTimeMillis()
        val m = 60_000L
        val h = 3_600_000L
        val d = 86_400_000L

        mutableListOf<IChatSession>(
            GroupChatSession(
                id = "1",
                title = "Cypherpunk Alpha",
                fingerprint = "0x7F2A…9C",
                lastMessagePreview = "Ratchet session verified. Strict pairwise E2EE",
                lastMessageEpochMs = now - 4 * m,
                isPinned = true,
                memberCount = 6
            ),
            DirectChatSession(
                id = "2",
                title = "Алиса",
                fingerprint = "0x4D9F…88E1",
                lastMessagePreview = "Файл отчёта зашифрован ChaCha20-Poly1305.",
                lastMessageEpochMs = now - 10 * m,
                isPinned = true,
                isOnline = true,
                unreadCount = 2
            ),
            DirectChatSession(
                id = "3",
                title = "Боб",
                fingerprint = "0x1B77…D3A0",
                lastMessagePreview = "Квантово-стойкий SAS код подтверждён.",
                lastMessageEpochMs = now - 28 * m,
                tier = ChatSecurityTier.POST_QUANTUM
            ),
            GroupChatSession(
                id = "4",
                title = "Onion Relay Ops",
                fingerprint = "0x99C2…04FF",
                lastMessagePreview = "Новый луковый узел в сети: 3 hops",
                lastMessageEpochMs = now - 112 * m,
                memberCount = 12
            ),
            OtpBurnChatSession(
                id = "5",
                title = "OTP Vault",
                fingerprint = "0xE210…7B44",
                lastMessagePreview = "Одноразовый секретный блокнот активен",
                lastMessageEpochMs = now - 5 * h,
                pagesLeft = 7
            ),
            DirectChatSession(
                id = "6",
                title = "Вера Криптон",
                fingerprint = "0x33A1…8B12",
                lastMessagePreview = "Передала открытый ключ по защищённому каналу",
                lastMessageEpochMs = now - 6 * h,
                isOnline = true,
                unreadCount = 1
            ),
            GroupChatSession(
                id = "7",
                title = "Кибер-штаб",
                fingerprint = "0x8821…9F00",
                lastMessagePreview = "Все резервные маршруты проверены и зафиксированы",
                lastMessageEpochMs = now - 9 * h,
                memberCount = 8
            ),
            DirectChatSession(
                id = "8",
                title = "Артур",
                fingerprint = "0x5C90…41D3",
                lastMessagePreview = "Ноутбук настроен, жду команды на выезд",
                lastMessageEpochMs = now - 14 * h,
                unreadCount = 0
            ),
            GroupChatSession(
                id = "9",
                title = "Dev Core Team",
                fingerprint = "0x44FE…119A",
                lastMessagePreview = "Релиз v1.0.48 готов к интеграции ML-KEM",
                lastMessageEpochMs = now - 18 * h,
                memberCount = 15
            ),
            GroupChatSession(
                id = "10",
                title = "Секретный канал",
                fingerprint = "0x90B3…471C",
                lastMessagePreview = "Пакеты фрагментированы, следы в сети отсутствуют",
                lastMessageEpochMs = now - 22 * h,
                memberCount = 4
            ),
            DirectChatSession(
                id = "11",
                title = "Даниил",
                fingerprint = "0x6A19…328B",
                lastMessagePreview = "Документ с отчётом отправлен, сверь хеш SHA-256",
                lastMessageEpochMs = now - 1 * d - 2 * h,
                unreadCount = 3
            ),
            GroupChatSession(
                id = "12",
                title = "Квантовый анклав",
                fingerprint = "0x00F1…88D9",
                lastMessagePreview = "Ключевая пара Kyber-1024 обновлена успешно",
                lastMessageEpochMs = now - 1 * d - 5 * h,
                tier = ChatSecurityTier.POST_QUANTUM,
                memberCount = 9
            ),
            DirectChatSession(
                id = "13",
                title = "Елена",
                fingerprint = "0x77E4…5532",
                lastMessagePreview = "Привет! Завтра созвон в защищённой комнате?",
                lastMessageEpochMs = now - 1 * d - 8 * h,
                isOnline = false
            ),
            GroupChatSession(
                id = "14",
                title = "Zero-Knowledge Labs",
                fingerprint = "0x3344…1289",
                lastMessagePreview = "zk-SNARK пруф валидирован на стороне клиента",
                lastMessageEpochMs = now - 1 * d - 12 * h,
                memberCount = 7
            ),
            DirectChatSession(
                id = "15",
                title = "Максим",
                fingerprint = "0x1109…87BA",
                lastMessagePreview = "Локальная база синхронизирована с аппаратным ключом",
                lastMessageEpochMs = now - 2 * d,
                unreadCount = 0
            ),
            GroupChatSession(
                id = "16",
                title = "Tor Bridge Monitor",
                fingerprint = "0x55AA…9941",
                lastMessagePreview = "Обнаружено 4 новых obfs4 моста, скорость в норме",
                lastMessageEpochMs = now - 2 * d - 4 * h,
                memberCount = 21
            ),
            DirectChatSession(
                id = "17",
                title = "София",
                fingerprint = "0x89C1…664B",
                lastMessagePreview = "Встреча перенесена на 16:00, геолокацию скину",
                lastMessageEpochMs = now - 2 * d - 8 * h,
                isOnline = true
            ),
            GroupChatSession(
                id = "18",
                title = "Signal Protocol WG",
                fingerprint = "0x22DC…7711",
                lastMessagePreview = "Спецификация X3DH дополнена квантовыми KEM",
                lastMessageEpochMs = now - 3 * d,
                memberCount = 34
            ),
            DirectChatSession(
                id = "19",
                title = "Виктор",
                fingerprint = "0x4477…3322",
                lastMessagePreview = "Архив с исходниками распакован, тесты пройдены",
                lastMessageEpochMs = now - 3 * d - 6 * h,
                unreadCount = 0
            ),
            OtpBurnChatSession(
                id = "20",
                title = "Ephemeral Drop #2",
                fingerprint = "0xFFA0…0129",
                lastMessagePreview = "Блокнот на 3 страницы. Уничтожение через 24 часа",
                lastMessageEpochMs = now - 4 * d,
                pagesLeft = 3
            )
        )
    } else {
        mutableListOf<IChatSession>(
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
            )
        )
    }

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

    private val drafts = mutableMapOf<String, String>()

    override suspend fun saveDraft(sessionId: String, text: String) {
        if (text.isBlank()) drafts.remove(sessionId) else drafts[sessionId] = text
    }

    override suspend fun loadDrafts(): Map<String, String> = drafts.toMap()
}
