package com.anongram.web.data

import com.anongram.web.model.MessageDirection
import com.anongram.web.model.MessageStatus
import com.anongram.web.model.WebChatMessage
import com.anongram.web.model.WebChatSession

object WebChatSampleData {
    val sampleSessions = listOf(
        WebChatSession(
            id = "0",
            title = "Избранное",
            fingerprint = "0x0000…SAVED",
            lastMessagePreview = "Ключи и секретные заметки",
            timeLabel = "16:40",
            unreadCount = 0,
            isPinned = true
        ),
        WebChatSession(
            id = "1",
            title = "Cypherpunk Alpha",
            fingerprint = "0x7F2A…9C",
            lastMessagePreview = "Ratchet session verified. Strict pairwise E2EE",
            timeLabel = "16:32",
            unreadCount = 3,
            isPinned = true,
            isGroup = true,
            memberCount = 6
        ),
        WebChatSession(
            id = "2",
            title = "Алиса",
            fingerprint = "0x4D9F…88E1",
            lastMessagePreview = "Напиши, как проверишь подпись.",
            timeLabel = "15:20",
            unreadCount = 1
        ),
        WebChatSession(
            id = "3",
            title = "Боб (Burn OTP)",
            fingerprint = "0x99AA…3B02",
            lastMessagePreview = "Одноразовый квантово-стойкий SAS код подтверждён.",
            timeLabel = "14:05",
            unreadCount = 0
        ),
        WebChatSession(
            id = "4",
            title = "Onion Relay Ops",
            fingerprint = "0x33C1…00F7",
            lastMessagePreview = "Новый луковый узел в сети: 3 hops",
            timeLabel = "Вчера",
            unreadCount = 0,
            isGroup = true,
            memberCount = 14
        )
    )

    fun getInitialMessages(sessionId: String): List<WebChatMessage> {
        return when (sessionId) {
            "1" -> listOf(
                WebChatMessage("m1", MessageDirection.INCOMING, "Приветствую! Узел развёрнут в изолированной сети.", "16:15"),
                WebChatMessage("m2", MessageDirection.INCOMING, "Ratchet session verified. Strict pairwise E2EE", "16:20"),
                WebChatMessage("m3", MessageDirection.OUTGOING, "Отлично, проверяю отпечаток ключа ML-KEM-1024.", "16:25", MessageStatus.READ),
                WebChatMessage("m4", MessageDirection.OUTGOING, "Канал чист, утечек трафика нет.", "16:28", MessageStatus.READ),
                WebChatMessage("m5", MessageDirection.INCOMING, "Готовы к тестовой передаче payload.", "16:32")
            )
            "2" -> listOf(
                WebChatMessage("m20", MessageDirection.INCOMING, "Привет! Ты обновил ключи авторизации?", "15:02"),
                WebChatMessage("m21", MessageDirection.OUTGOING, "Да, сгенерировал новый BIP-39 мастер-сид.", "15:10", MessageStatus.READ),
                WebChatMessage("m22", MessageDirection.INCOMING, "Напиши, как проверишь подпись.", "15:20")
            )
            "0" -> listOf(
                WebChatMessage("m01", MessageDirection.OUTGOING, "Личные заметки: seed хранится в защищенном анклаве.", "12:00", MessageStatus.READ),
                WebChatMessage("m02", MessageDirection.OUTGOING, "Тестовый запуск AnonGram Web через Compose Wasm.", "16:40", MessageStatus.READ)
            )
            else -> listOf(
                WebChatMessage("m_def1", MessageDirection.INCOMING, "Сессия открыта. End-to-end шифрование активно.", "14:00"),
                WebChatMessage("m_def2", MessageDirection.OUTGOING, "Проверка связи.", "14:05", MessageStatus.READ)
            )
        }
    }
}
