package com.anongram.app.features.chats.messages.data

import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.LinkPreview
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import kotlin.random.Random

/**
 * FakeRepository переписок: демо-тред со всеми типами сообщений (текст, цитата,
 * «изменено», переслано, голосовое, фото, файл, музыка, контакт, гео, ссылка,
 * стикер, реакции, непрочитанные), постраничная история и пул автоответов.
 */
class InMemoryChatMessagesRepository : ChatMessagesRepository {

    private val threads = mutableMapOf<String, MutableList<ChatMessage>>()
    private val replyIndex = mutableMapOf<String, Int>()
    private val olderPagesLeft = mutableMapOf<String, Int>()
    private var counter = 0

    private val cannedReplies = listOf(
        "Принято. Ratchet-цепочка обновлена.",
        "Понял, сверю SAS-код при встрече.",
        "Хендшейк ML-KEM-1024 прошёл без ошибок.",
        "Ок, посмотрю через пару минут.",
        "Onion-маршрут стабильный, задержка в норме.",
        "Согласен. Включу это в следующий слив."
    )

    private val olderTexts = listOf(
        "Не забудь про ротацию ключей в пятницу.",
        "Новый мост подняли, пинг 40 мс.",
        "Проверь подпись на прошивке узла.",
        "Держи черновик регламента.",
        "Тор-метрики за неделю стабильные.",
        "Созвон по onion-роутингу в 18:00?",
        "Сжал архив с логами, весит 8 МБ.",
        "Kать, а если KEM-ключи менять каждый час?"
    )

    init {
        val now = System.currentTimeMillis()
        val m = 60_000L
        val hello = msg(MessageDirection.INCOMING, "Привет! Я на связи через новый луковый узел.", now - 3 * 3_600_000L)
        val ack = msg(
            MessageDirection.OUTGOING, "Отлично, вижу. Соединение чистое, 3 hops.",
            now - 3 * 3_600_000L + 2 * m, replyTo = hello,
            reactions = mapOf("👍" to 1, "🔥" to 1)
        )
        val edited = msg(
            MessageDirection.OUTGOING, "Пересчитал MAC — подпись сошлась, отчёт целый.",
            now - 2 * 3_600_000L, isEdited = true
        )
        val forwarded = msg(
            MessageDirection.INCOMING, "Планы на субботу в силе?",
            now - 110 * m, forwardedFrom = "Боб"
        )
        val voice = msg(
            MessageDirection.INCOMING, "", now - 90 * m,
            attachment = MessageAttachment.Voice("0:14", listOf(3, 8, 5, 12, 7, 14, 4, 9, 13, 6, 11, 3, 8, 12, 5, 9, 14, 6, 4, 10, 7, 12, 5, 8))
        )
        val photo = msg(
            MessageDirection.INCOMING, "Вечерний Карабах", now - 70 * m,
            attachment = MessageAttachment.Photo(label = "IMG_2043", colorSeed = 3)
        )
        val doc = msg(
            MessageDirection.OUTGOING, "", now - 55 * m,
            attachment = MessageAttachment.File("anongram_whitepaper.pdf", "1,2 МБ", FileKind.PDF)
        )
        val music = msg(
            MessageDirection.INCOMING, "", now - 40 * m,
            attachment = MessageAttachment.Music("Crypto Lullaby", "Onion Ensemble", "3:45", 2)
        )
        val contact = msg(
            MessageDirection.INCOMING, "", now - 30 * m,
            attachment = MessageAttachment.Contact("Вера Криптон", "+996 555 12-34-56")
        )
        val geo = msg(
            MessageDirection.INCOMING, "", now - 22 * m,
            attachment = MessageAttachment.Geo("ул. Исы Ахунбаева 18, Бишкек")
        )
        val link = msg(
            MessageDirection.OUTGOING,
            "Разбор: как работает Double Ratchet https://anongram.example/ratchet",
            now - 15 * m,
            linkPreview = LinkPreview(
                url = "https://anongram.example/ratchet",
                domain = "anongram.example",
                title = "Double Ratchet: математика впереди",
                description = "Квантово-стойкий обмен ключами без доверенных серверов"
            )
        )
        val sticker = msg(
            MessageDirection.INCOMING, "", now - 12 * m,
            attachment = MessageAttachment.Sticker("🐉", 1)
        )
        val unread1 = msg(
            MessageDirection.INCOMING, "Ты уже посмотрел новый маршрут?", now - 6 * m, isUnread = true
        )
        val unread2 = msg(
            MessageDirection.INCOMING, "Напиши, как проверишь подпись.", now - 4 * m, isUnread = true
        )

        put("2", listOf(
            hello, ack, edited, forwarded, voice, photo, doc, music, contact, geo, link,
            sticker, unread1, unread2
        ))
        put("3", listOf(
            msg(MessageDirection.INCOMING, "Квантово-стойкий SAS код подтверждён.", now - 29 * m)
        ))
        put("1", listOf(
            msg(MessageDirection.INCOMING, "Ratchet session verified. Strict pairwise E2EE.", now - 5 * m)
        ))
        put("4", listOf(
            msg(MessageDirection.INCOMING, "Новый луковый узел в сети: 3 hops", now - 112 * m)
        ))
        put("5", listOf(
            msg(MessageDirection.INCOMING, "Одноразовый секретный блокнот активен", now - 5 * 3_600_000L)
        ))
        olderPagesLeft["2"] = 2
    }

    private fun msg(
        direction: MessageDirection,
        text: String,
        epochMs: Long,
        replyTo: ChatMessage? = null,
        isEdited: Boolean = false,
        isUnread: Boolean = false,
        forwardedFrom: String? = null,
        attachment: MessageAttachment? = null,
        linkPreview: LinkPreview? = null,
        reactions: Map<String, Int> = emptyMap()
    ): ChatMessage {
        counter += 1
        return ChatMessage(
            id = "seed_$counter",
            direction = direction,
            text = text,
            epochMs = epochMs,
            replyToId = replyTo?.id,
            isEdited = isEdited,
            isUnread = isUnread,
            forwardedFrom = forwardedFrom,
            attachment = attachment,
            linkPreview = linkPreview,
            reactions = reactions,
            myReaction = null
        )
    }

    private fun put(sessionId: String, messages: List<ChatMessage>) {
        threads[sessionId] = messages.toMutableList()
    }

    private fun thread(sessionId: String): MutableList<ChatMessage> =
        threads.getOrPut(sessionId) { mutableListOf() }

    override suspend fun loadMessages(sessionId: String): List<ChatMessage> =
        thread(sessionId).toList()

    override suspend fun loadOlder(sessionId: String, beforeEpochMs: Long): List<ChatMessage> {
        val left = olderPagesLeft[sessionId] ?: 0
        if (left <= 0) return emptyList()
        olderPagesLeft[sessionId] = left - 1
        var time = beforeEpochMs - 3_600_000L
        return List(8) { i ->
            counter += 1
            time -= 5 * 60_000L * (i + 1)
            ChatMessage(
                id = "old_${sessionId}_$counter",
                direction = if (i % 2 == 0) MessageDirection.INCOMING else MessageDirection.OUTGOING,
                text = olderTexts[Random.nextInt(olderTexts.size)],
                epochMs = time
            )
        }
    }

    override suspend fun hasMoreOlder(sessionId: String): Boolean =
        (olderPagesLeft[sessionId] ?: 0) > 0

    override suspend fun addMessage(sessionId: String, message: ChatMessage) {
        thread(sessionId).add(message)
    }

    override suspend fun updateStatus(sessionId: String, messageId: String, status: MessageStatus) {
        val list = thread(sessionId)
        val index = list.indexOfFirst { it.id == messageId }
        if (index >= 0) list[index] = list[index].copy(status = status)
    }

    override suspend fun updateMessage(sessionId: String, message: ChatMessage) {
        val list = thread(sessionId)
        val index = list.indexOfFirst { it.id == message.id }
        if (index >= 0) list[index] = message
    }

    override suspend fun deleteMessage(sessionId: String, messageId: String) {
        thread(sessionId).removeAll { it.id == messageId }
    }

    override suspend fun markAllRead(sessionId: String) {
        val list = thread(sessionId)
        for (i in list.indices) {
            if (list[i].isUnread) list[i] = list[i].copy(isUnread = false)
        }
    }

    override suspend fun clearHistory(sessionId: String) {
        thread(sessionId).clear()
    }

    override suspend fun generateAutoReply(sessionId: String): ChatMessage? {
        val index = (replyIndex[sessionId] ?: 0) % cannedReplies.size
        replyIndex[sessionId] = index + 1
        counter += 1
        val asVoice = Random.nextInt(100) < 25
        return if (asVoice) {
            ChatMessage(
                id = "reply_$counter",
                direction = MessageDirection.INCOMING,
                text = "",
                epochMs = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachment = MessageAttachment.Voice(
                    durationLabel = "0:0${Random.nextInt(3, 9)}",
                    waveform = List(24) { Random.nextInt(3, 15) }
                )
            )
        } else {
            ChatMessage(
                id = "reply_$counter",
                direction = MessageDirection.INCOMING,
                text = cannedReplies[index],
                epochMs = System.currentTimeMillis()
            )
        }
    }
}
