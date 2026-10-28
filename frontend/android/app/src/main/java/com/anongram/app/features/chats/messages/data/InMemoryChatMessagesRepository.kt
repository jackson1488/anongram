package com.anongram.app.features.chats.messages.data

import com.anongram.app.features.chats.data.DEV_TEST_DATA_ENABLED
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

        if (DEV_TEST_DATA_ENABLED) {
            put("2", build100DevMessages(now))
            put("1", listOf(
                msg(MessageDirection.INCOMING, "Ratchet session verified. Strict pairwise E2EE.", now - 4 * m)
            ))
            put("3", listOf(
                msg(MessageDirection.INCOMING, "Квантово-стойкий SAS код подтверждён.", now - 28 * m)
            ))
            put("4", listOf(
                msg(MessageDirection.INCOMING, "Новый луковый узел в сети: 3 hops", now - 112 * m)
            ))
            put("5", listOf(
                msg(MessageDirection.INCOMING, "Одноразовый секретный блокнот активен", now - 5 * 3_600_000L)
            ))
            put("6", listOf(
                msg(MessageDirection.INCOMING, "Передала открытый ключ по защищённому каналу", now - 6 * 3_600_000L, isUnread = true)
            ))
            put("7", listOf(
                msg(MessageDirection.INCOMING, "Все резервные маршруты проверены и зафиксированы", now - 9 * 3_600_000L)
            ))
            put("8", listOf(
                msg(MessageDirection.INCOMING, "Ноутбук настроен, жду команды на выезд", now - 14 * 3_600_000L)
            ))
            put("9", listOf(
                msg(MessageDirection.INCOMING, "Релиз v1.0.48 готов к интеграции ML-KEM", now - 18 * 3_600_000L)
            ))
            put("10", listOf(
                msg(MessageDirection.INCOMING, "Пакеты фрагментированы, следы в сети отсутствуют", now - 22 * 3_600_000L)
            ))
            put("11", listOf(
                msg(MessageDirection.INCOMING, "Документ с отчётом отправлен, сверь хеш SHA-256", now - 26 * 3_600_000L, isUnread = true)
            ))
            put("12", listOf(
                msg(MessageDirection.INCOMING, "Ключевая пара Kyber-1024 обновлена успешно", now - 29 * 3_600_000L)
            ))
            put("13", listOf(
                msg(MessageDirection.INCOMING, "Привет! Завтра созвон в защищённой комнате?", now - 32 * 3_600_000L)
            ))
            put("14", listOf(
                msg(MessageDirection.INCOMING, "zk-SNARK пруф валидирован на стороне клиента", now - 36 * 3_600_000L)
            ))
            put("15", listOf(
                msg(MessageDirection.INCOMING, "Локальная база синхронизирована с аппаратным ключом", now - 48 * 3_600_000L)
            ))
            put("16", listOf(
                msg(MessageDirection.INCOMING, "Обнаружено 4 новых obfs4 моста, скорость в норме", now - 52 * 3_600_000L)
            ))
            put("17", listOf(
                msg(MessageDirection.INCOMING, "Встреча перенесена на 16:00, геолокацию скину", now - 56 * 3_600_000L)
            ))
            put("18", listOf(
                msg(MessageDirection.INCOMING, "Спецификация X3DH дополнена квантовыми KEM", now - 72 * 3_600_000L)
            ))
            put("19", listOf(
                msg(MessageDirection.INCOMING, "Архив с исходниками распакован, тесты пройдены", now - 78 * 3_600_000L)
            ))
            put("20", listOf(
                msg(MessageDirection.INCOMING, "Блокнот на 3 страницы. Уничтожение через 24 часа", now - 96 * 3_600_000L)
            ))
            olderPagesLeft["2"] = 5
        } else {
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
            val unread1 = msg(
                MessageDirection.INCOMING, "Ты уже посмотрел новый маршрут?", now - 6 * m, isUnread = true
            )
            val unread2 = msg(
                MessageDirection.INCOMING, "Напиши, как проверишь подпись.", now - 4 * m, isUnread = true
            )
            put("2", listOf(hello, ack, edited, unread1, unread2))
            olderPagesLeft["2"] = 2
        }
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

    private fun build100DevMessages(now: Long): List<ChatMessage> {
        val m = 60_000L
        val h = 3_600_000L
        val d = 86_400_000L
        val list = mutableListOf<ChatMessage>()

        // --- День 4 назад (сообщения 1..20) ---
        val t4 = now - 4 * d
        val m1 = msg(MessageDirection.INCOMING, "Привет! Инициализирую тестовый канал связи.", t4 + 10 * m)
        list.add(m1)
        val m2 = msg(MessageDirection.OUTGOING, "Привет, вижу тебя. Тестируем шифрование.", t4 + 12 * m, replyTo = m1)
        list.add(m2)
        list.add(msg(MessageDirection.INCOMING, "Отлично. Проверь открытый ключ в отчёте.", t4 + 15 * m))
        list.add(msg(MessageDirection.OUTGOING, "Сверил fingerprint: 0x4D9F…88E1. Всё сошлось.", t4 + 17 * m, reactions = mapOf("👍" to 1)))
        list.add(msg(MessageDirection.INCOMING, "Супер. Начинаем пересылку тестовых пакетов.", t4 + 20 * m))
        list.add(msg(MessageDirection.OUTGOING, "", t4 + 25 * m, attachment = MessageAttachment.File("spec_v1.pdf", "850 КБ", FileKind.PDF)))
        list.add(msg(MessageDirection.INCOMING, "Документ получен, распаковка прошла успешно.", t4 + 27 * m))
        list.add(msg(MessageDirection.OUTGOING, "Какой там пинг через луковый маршрут?", t4 + 30 * m))
        list.add(msg(MessageDirection.INCOMING, "Маршрут показывает задержку около 45 мс.", t4 + 32 * m))
        list.add(msg(MessageDirection.OUTGOING, "Это отличный результат для 3 узлов.", t4 + 35 * m))
        list.add(msg(MessageDirection.INCOMING, "", t4 + 40 * m, attachment = MessageAttachment.Sticker("⚡", 1)))
        list.add(msg(MessageDirection.INCOMING, "Сервер авторизации подняли в дата-центре.", t4 + 1 * h))
        list.add(msg(MessageDirection.OUTGOING, "Логи сервера пишутся в зашифрованный том?", t4 + 1 * h + 3 * m))
        list.add(msg(MessageDirection.INCOMING, "Да, AES-256-XTS с аппаратным токеном.", t4 + 1 * h + 5 * m))
        list.add(msg(MessageDirection.OUTGOING, "Тогда база данных в полной безопасности.", t4 + 1 * h + 8 * m))
        list.add(msg(MessageDirection.INCOMING, "Скину геопозицию тестовой лаборатории.", t4 + 2 * h))
        list.add(msg(MessageDirection.INCOMING, "", t4 + 2 * h + 1 * m, attachment = MessageAttachment.Geo("пр. Чуй 265, Бишкек")))
        list.add(msg(MessageDirection.OUTGOING, "Принял геолокацию, завтра буду там.", t4 + 2 * h + 5 * m))
        list.add(msg(MessageDirection.INCOMING, "Не забудь взять с собой ключи от сейфа.", t4 + 2 * h + 7 * m))
        list.add(msg(MessageDirection.OUTGOING, "Ключи уже у меня в кармане, не переживай.", t4 + 2 * h + 10 * m))

        // --- День 3 назад (сообщения 21..40) ---
        val t3 = now - 3 * d
        val m21 = msg(MessageDirection.INCOMING, "Привет! Новый день, новые тесты.", t3 + 9 * h)
        list.add(m21)
        list.add(msg(MessageDirection.OUTGOING, "Привет! Какие задачи на сегодня в плане?", t3 + 9 * h + 2 * m))
        list.add(msg(MessageDirection.INCOMING, "Нужно проверить код протокола двойного храповика.", t3 + 9 * h + 5 * m))
        list.add(msg(MessageDirection.OUTGOING, "Исходный код я обновил вчера вечером.", t3 + 9 * h + 7 * m))
        list.add(msg(MessageDirection.INCOMING, "", t3 + 10 * h, attachment = MessageAttachment.Photo("Стенд тестирования", 2)))
        list.add(msg(MessageDirection.INCOMING, "Смотри, собрали стенд для замера пропускной способности.", t3 + 10 * h + 1 * m))
        list.add(msg(MessageDirection.OUTGOING, "Выглядит солидно. Сколько потоков держит?", t3 + 10 * h + 4 * m))
        list.add(msg(MessageDirection.INCOMING, "До 10 000 сообщений в секунду без задержек.", t3 + 10 * h + 6 * m, reactions = mapOf("🔥" to 2)))
        list.add(msg(MessageDirection.OUTGOING, "Отличная скорость! Завтра подключим мобильных клиентов.", t3 + 10 * h + 10 * m))
        list.add(msg(MessageDirection.INCOMING, "Кстати, Боб спрашивал про архив документации.", t3 + 12 * h))
        list.add(msg(MessageDirection.OUTGOING, "Архив выгружен на защищённый сервер.", t3 + 12 * h + 3 * m))
        list.add(msg(MessageDirection.INCOMING, "", t3 + 13 * h, attachment = MessageAttachment.Voice("0:28", listOf(4, 9, 7, 14, 6, 12, 5, 8, 11, 4, 10, 6, 13, 8, 5, 12, 7, 9, 14, 5, 8, 11, 6, 9))))
        list.add(msg(MessageDirection.OUTGOING, "Послушал голосовое. По созвону согласен, давай в 15:00.", t3 + 13 * h + 5 * m))
        list.add(msg(MessageDirection.INCOMING, "Договорились, создам защищённую комнату.", t3 + 13 * h + 7 * m))
        list.add(msg(MessageDirection.OUTGOING, "Скинь контакт дежурного инженера.", t3 + 15 * h))
        list.add(msg(MessageDirection.INCOMING, "", t3 + 15 * h + 1 * m, attachment = MessageAttachment.Contact("Артур Безопасность", "+996 700 88-99-00")))
        list.add(msg(MessageDirection.OUTGOING, "Записал контакт Артура.", t3 + 15 * h + 4 * m))
        list.add(msg(MessageDirection.INCOMING, "Проверь статус сервера в мониторинге.", t3 + 16 * h))
        list.add(msg(MessageDirection.OUTGOING, "Сервер работает штатно, нагрузка 12%.", t3 + 16 * h + 3 * m, isEdited = true))
        list.add(msg(MessageDirection.INCOMING, "Хорошо, тогда на сегодня закругляемся.", t3 + 18 * h))

        // --- День 2 назад (сообщения 41..60) ---
        val t2 = now - 2 * d
        list.add(msg(MessageDirection.INCOMING, "Привет! Срочная новость по протоколу.", t2 + 10 * h))
        list.add(msg(MessageDirection.OUTGOING, "Привет, что случилось?", t2 + 10 * h + 2 * m))
        list.add(msg(MessageDirection.INCOMING, "Вышла новая статья про квантовые атаки на RSA.", t2 + 10 * h + 4 * m))
        list.add(msg(MessageDirection.OUTGOING, "Вот почему мы сразу перешли на Kyber и Dilithium!", t2 + 10 * h + 6 * m))
        list.add(msg(MessageDirection.INCOMING, "", t2 + 10 * h + 8 * m, linkPreview = LinkPreview("https://anongram.example/pqc", "anongram.example", "Постквантовая защита в AnonGram", "Архитектура гибридного шифрования Kyber-1024 и X25519")))
        list.add(msg(MessageDirection.OUTGOING, "Отличный материал, прочитал с интересом.", t2 + 11 * h))
        list.add(msg(MessageDirection.INCOMING, "Наш отчёт по безопасности тоже готов.", t2 + 12 * h))
        list.add(msg(MessageDirection.OUTGOING, "Отчёт уже проверен аудиторами?", t2 + 12 * h + 3 * m))
        list.add(msg(MessageDirection.INCOMING, "Да, все 4 аудитора подтвердили отсутствие уязвимостей.", t2 + 12 * h + 5 * m, reactions = mapOf("👍" to 2)))
        list.add(msg(MessageDirection.OUTGOING, "", t2 + 13 * h, attachment = MessageAttachment.Music("Cyberpunk Echoes", "Krypton Labs", "4:12", 3)))
        list.add(msg(MessageDirection.INCOMING, "Классный трек для кодинга!", t2 + 13 * h + 5 * m))
        list.add(msg(MessageDirection.OUTGOING, "Всегда включаю его, когда пишу сетевой код.", t2 + 13 * h + 7 * m))
        list.add(msg(MessageDirection.INCOMING, "Как продвигается оптимизация базы данных?", t2 + 14 * h))
        list.add(msg(MessageDirection.OUTGOING, "База теперь отвечает за 2 миллисекунды.", t2 + 14 * h + 4 * m))
        list.add(msg(MessageDirection.INCOMING, "Впечатляет! Быстрее чем у конкурентов.", t2 + 14 * h + 6 * m))
        list.add(msg(MessageDirection.OUTGOING, "Потому что база локальная и без облачных посредников.", t2 + 14 * h + 8 * m))
        list.add(msg(MessageDirection.INCOMING, "Боб прислал фото нового роутера.", t2 + 16 * h, forwardedFrom = "Боб"))
        list.add(msg(MessageDirection.INCOMING, "", t2 + 16 * h + 1 * m, attachment = MessageAttachment.Photo("Аппаратный роутер", 1)))
        list.add(msg(MessageDirection.OUTGOING, "Этот роутер поддерживает луковый маршрут из коробки?", t2 + 16 * h + 4 * m))
        list.add(msg(MessageDirection.INCOMING, "Да, прошивка OpenWrt с нашим демоном.", t2 + 16 * h + 6 * m))

        // --- Вчера (сообщения 61..80) ---
        val t1 = now - 1 * d
        list.add(msg(MessageDirection.INCOMING, "Привет! Сегодня важный день для сборки.", t1 + 10 * h))
        list.add(msg(MessageDirection.OUTGOING, "Привет! Готовим релиз v1.0.48?", t1 + 10 * h + 2 * m))
        list.add(msg(MessageDirection.INCOMING, "Именно. Нужно проверить все экраны чата.", t1 + 10 * h + 4 * m))
        list.add(msg(MessageDirection.OUTGOING, "Поиск в чате уже доработан: подсветка кусочка слова и навигация.", t1 + 10 * h + 7 * m))
        list.add(msg(MessageDirection.INCOMING, "О, это круто! Сейчас протестирую поиск.", t1 + 10 * h + 9 * m))
        list.add(msg(MessageDirection.INCOMING, "Скинь архив с тестовыми данными.", t1 + 11 * h))
        list.add(msg(MessageDirection.OUTGOING, "", t1 + 11 * h + 2 * m, attachment = MessageAttachment.File("Depseek.zip", "3,4 МБ", FileKind.ARCHIVE)))
        list.add(msg(MessageDirection.INCOMING, "Depseek.zip получен, внутри всё аккуратно.", t1 + 11 * h + 5 * m))
        list.add(msg(MessageDirection.OUTGOING, "Завтра посмотрю я ноутбук выключил", t1 + 14 * h))
        list.add(msg(MessageDirection.INCOMING, "Хорошо, отдыхай, завтра продолжим.", t1 + 14 * h + 3 * m))
        list.add(msg(MessageDirection.INCOMING, "", t1 + 14 * h + 5 * m, attachment = MessageAttachment.Sticker("😴", 2)))
        list.add(msg(MessageDirection.OUTGOING, "Кстати, проверь подпись на прошивке узла.", t1 + 16 * h))
        list.add(msg(MessageDirection.INCOMING, "Подпись проверена, хэш SHA-512 совпадает.", t1 + 16 * h + 4 * m))
        list.add(msg(MessageDirection.OUTGOING, "Отлично. Значит сервер обновлён без сюрпризов.", t1 + 16 * h + 6 * m))
        list.add(msg(MessageDirection.INCOMING, "Держи черновик регламента безопасности.", t1 + 17 * h))
        list.add(msg(MessageDirection.INCOMING, "", t1 + 17 * h + 1 * m, attachment = MessageAttachment.File("security_policy.pdf", "620 КБ", FileKind.PDF)))
        list.add(msg(MessageDirection.OUTGOING, "Документ скачал, изучу пункты по шифрованию.", t1 + 17 * h + 5 * m))
        list.add(msg(MessageDirection.INCOMING, "Созвон по onion-роутингу в силе?", t1 + 18 * h))
        list.add(msg(MessageDirection.OUTGOING, "Да, созвон в силе, подключаюсь через 5 минут.", t1 + 18 * h + 2 * m))
        list.add(msg(MessageDirection.INCOMING, "Жду в защищённом канале связи.", t1 + 18 * h + 4 * m))

        // --- Сегодня (сообщения 81..100) ---
        val m81 = msg(MessageDirection.INCOMING, "Привет! Я на связи через новый луковый узел.", now - 5 * h)
        list.add(m81)
        val m82 = msg(MessageDirection.OUTGOING, "Отлично, вижу. Соединение чистое, 3 hops.", now - 5 * h + 2 * m, replyTo = m81, reactions = mapOf("👍" to 1, "🔥" to 1))
        list.add(m82)
        list.add(msg(MessageDirection.OUTGOING, "Пересчитал MAC — подпись сошлась, отчёт целый.", now - 4 * h, isEdited = true))
        list.add(msg(MessageDirection.INCOMING, "Планы на субботу в силе?", now - 3 * h - 40 * m, forwardedFrom = "Боб"))
        list.add(msg(MessageDirection.INCOMING, "", now - 3 * h - 20 * m, attachment = MessageAttachment.Voice("0:14", listOf(3, 8, 5, 12, 7, 14, 4, 9, 13, 6, 11, 3, 8, 12, 5, 9, 14, 6, 4, 10, 7, 12, 5, 8))))
        list.add(msg(MessageDirection.INCOMING, "Вечерний Карабах", now - 3 * h, attachment = MessageAttachment.Photo(label = "IMG_2043", colorSeed = 3)))
        list.add(msg(MessageDirection.OUTGOING, "", now - 2 * h - 45 * m, attachment = MessageAttachment.File("anongram_whitepaper.pdf", "1,2 МБ", FileKind.PDF)))
        list.add(msg(MessageDirection.INCOMING, "", now - 2 * h - 30 * m, attachment = MessageAttachment.Music("Crypto Lullaby", "Onion Ensemble", "3:45", 2)))
        list.add(msg(MessageDirection.INCOMING, "", now - 2 * h - 15 * m, attachment = MessageAttachment.Contact("Вера Криптон", "+996 555 12-34-56")))
        list.add(msg(MessageDirection.INCOMING, "", now - 2 * h, attachment = MessageAttachment.Geo("ул. Исы Ахунбаева 18, Бишкек")))
        list.add(msg(MessageDirection.OUTGOING, "Разбор: как работает Double Ratchet https://anongram.example/ratchet", now - 90 * m, linkPreview = LinkPreview("https://anongram.example/ratchet", "anongram.example", "Double Ratchet: математика впереди", "Квантово-стойкий обмен ключами без доверенных серверов")))
        list.add(msg(MessageDirection.INCOMING, "", now - 75 * m, attachment = MessageAttachment.Sticker("🐉", 1)))
        list.add(msg(MessageDirection.OUTGOING, "Привет, обновил ключи сессии, проверь handshake.", now - 50 * m))
        list.add(msg(MessageDirection.INCOMING, "Handshake успешен, секретный ключ согласован.", now - 45 * m))
        list.add(msg(MessageDirection.OUTGOING, "Документ с описанием API готов к ревью.", now - 30 * m))
        list.add(msg(MessageDirection.INCOMING, "Отлично, смотрю документ прямо сейчас.", now - 25 * m))
        list.add(msg(MessageDirection.OUTGOING, "Маршрут переключился на резервный мост.", now - 15 * m))
        list.add(msg(MessageDirection.INCOMING, "Вижу, пинг остался стабильным, потерь пакетов 0%.", now - 10 * m))
        list.add(msg(MessageDirection.INCOMING, "Ты уже посмотрел новый маршрут?", now - 6 * m, isUnread = true))
        list.add(msg(MessageDirection.INCOMING, "Напиши, как проверишь подпись.", now - 3 * m, isUnread = true))

        return list
    }
}
