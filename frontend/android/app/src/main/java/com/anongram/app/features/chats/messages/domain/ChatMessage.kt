package com.anongram.app.features.chats.messages.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MessageDirection { INCOMING, OUTGOING }

/**
 * Жизненный цикл исходящего сообщения (как в Telegram):
 * часы (SENDING) → одинарная галочка (SENT) → двойная (READ).
 */
enum class MessageStatus { SENDING, SENT, READ }

/** Сообщение переписки. replyToSnippet — цитата ответа, показывается в пузыре. */
data class ChatMessage(
    val id: String,
    val direction: MessageDirection,
    val text: String,
    val epochMs: Long,
    val status: MessageStatus = MessageStatus.READ,
    val replyToSnippet: String? = null
)

fun ChatMessage.timeLabel(): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

/** Заголовок дат-чипа над группой сообщений: «Сегодня», «Вчера» или «12 марта». */
fun epochDateLabel(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("d MMMM").withLocale(Locale.forLanguageTag("ru"))
    return when (date) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> date.format(formatter)
    }
}

fun isSameDay(firstMs: Long, secondMs: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(firstMs).atZone(zone).toLocalDate() ==
        Instant.ofEpochMilli(secondMs).atZone(zone).toLocalDate()
}
