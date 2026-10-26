package com.anongram.app.features.chats.messages.data

import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus

/**
 * Локальное хранилище переписок pre-bridge этапа: засеянные демо-треды
 * для каждого чата и циклический пул автоответов собеседника.
 */
class InMemoryChatMessagesRepository : ChatMessagesRepository {

    private val threads = mutableMapOf<String, MutableList<ChatMessage>>()
    private val replyIndex = mutableMapOf<String, Int>()
    private var counter = 0

    private val cannedReplies = listOf(
        "Принято. Ratchet-цепочка обновлена.",
        "Понял, сверю SAS-код при встрече.",
        "Хендшейк ML-KEM-1024 прошёл без ошибок.",
        "Ок, посмотрю через пару минут.",
        "Onion-маршрут стабильный, задержка в норме.",
        "Согласен. Включу это в следующий слив."
    )

    init {
        val now = System.currentTimeMillis()
        put("2", listOf(
            msg(MessageDirection.INCOMING, "Привет! Я на связи через новый луковый узел.", now - 26 * 60_000L),
            msg(MessageDirection.OUTGOING, "Отлично, вижу. Соединение чистое, 3 hops.", now - 24 * 60_000L),
            msg(MessageDirection.INCOMING, "Отчёт готов. Забирай, я его уже подписала.", now - 11 * 60_000L),
            msg(MessageDirection.OUTGOING, "Секунду, расшифровываю…", now - 10 * 60_000L)
        ))
        put("3", listOf(
            msg(MessageDirection.INCOMING, "Квантово-стойкий SAS код подтверждён.", now - 29 * 60_000L),
            msg(MessageDirection.OUTGOING, "У меня тоже 60 цифр сошлись. MITM исключён.", now - 28 * 60_000L)
        ))
        put("1", listOf(
            msg(MessageDirection.INCOMING, "Ratchet session verified. Strict pairwise E2EE.", now - 5 * 60_000L)
        ))
        put("4", listOf(
            msg(MessageDirection.INCOMING, "Новый луковый узел в сети: 3 hops", now - 112 * 60_000L)
        ))
        put("5", listOf(
            msg(MessageDirection.INCOMING, "Одноразовый секретный блокнот активен", now - 5 * 3_600_000L)
        ))
    }

    private fun msg(direction: MessageDirection, text: String, epochMs: Long): ChatMessage {
        counter += 1
        return ChatMessage(id = "seed_$counter", direction = direction, text = text, epochMs = epochMs)
    }

    private fun put(sessionId: String, messages: List<ChatMessage>) {
        threads[sessionId] = messages.toMutableList()
    }

    private fun thread(sessionId: String): MutableList<ChatMessage> =
        threads.getOrPut(sessionId) { mutableListOf() }

    override suspend fun loadMessages(sessionId: String): List<ChatMessage> =
        thread(sessionId).toList()

    override suspend fun addMessage(sessionId: String, message: ChatMessage) {
        thread(sessionId).add(message)
    }

    override suspend fun updateStatus(sessionId: String, messageId: String, status: MessageStatus) {
        val list = thread(sessionId)
        val index = list.indexOfFirst { it.id == messageId }
        if (index >= 0) list[index] = list[index].copy(status = status)
    }

    override suspend fun deleteMessage(sessionId: String, messageId: String) {
        thread(sessionId).removeAll { it.id == messageId }
    }

    override suspend fun clearHistory(sessionId: String) {
        thread(sessionId).clear()
    }

    override suspend fun generateAutoReply(sessionId: String): ChatMessage? {
        val index = (replyIndex[sessionId] ?: 0) % cannedReplies.size
        replyIndex[sessionId] = index + 1
        counter += 1
        return ChatMessage(
            id = "reply_$counter",
            direction = MessageDirection.INCOMING,
            text = cannedReplies[index],
            epochMs = System.currentTimeMillis()
        )
    }
}
