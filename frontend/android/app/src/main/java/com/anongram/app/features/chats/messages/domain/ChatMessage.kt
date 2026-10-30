package com.anongram.app.features.chats.messages.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MessageDirection { INCOMING, OUTGOING }

/**
 * Жизненный цикл исходящего сообщения: часы (SENDING) → галочка (SENT) →
 * двойная (READ). FAILED — красный «!», тап = повторить.
 */
enum class MessageStatus { SENDING, SENT, READ, FAILED }

/**
 * Сообщение переписки: текст, вложение, цитата-ответ (replyToId), пометка
 * «изменено», пересылка (forwardedFrom), реакции (эмодзи → счётчик),
 * непрочитанность для разделителя «Непрочитанные сообщения».
 */
data class ChatMessage(
    val id: String,
    val direction: MessageDirection,
    val text: String = "",
    val epochMs: Long,
    val status: MessageStatus = MessageStatus.READ,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val isUnread: Boolean = false,
    val forwardedFrom: String? = null,
    val attachment: MessageAttachment? = null,
    val linkPreview: LinkPreview? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val myReaction: String? = null
) {
    val isStickerOnly: Boolean
        get() = attachment is MessageAttachment.Sticker && text.isBlank()
}

fun ChatMessage.timeLabel(): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

/**
 * Проверка совпадения поискового запроса: ищет подстроку без учёта регистра
 * в тексте, авторе пересылки, превью ссылок и метаданных вложений (файл, музыка, контакт, гео).
 */
fun ChatMessage.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return false
    val trimmed = query.trim()
    if (text.contains(trimmed, ignoreCase = true)) return true
    if (forwardedFrom?.contains(trimmed, ignoreCase = true) == true) return true
    if (linkPreview != null) {
        if (linkPreview.title.contains(trimmed, ignoreCase = true) ||
            linkPreview.description.contains(trimmed, ignoreCase = true) ||
            linkPreview.domain.contains(trimmed, ignoreCase = true) ||
            linkPreview.url.contains(trimmed, ignoreCase = true)
        ) return true
    }
    when (val att = attachment) {
        is MessageAttachment.File -> if (att.name.contains(trimmed, ignoreCase = true)) return true
        is MessageAttachment.FileGroup -> if (att.items.any { it.name.contains(trimmed, ignoreCase = true) }) return true
        is MessageAttachment.Music -> if (att.title.contains(trimmed, ignoreCase = true) || att.artist.contains(trimmed, ignoreCase = true)) return true
        is MessageAttachment.MusicGroup -> if (att.items.any { it.title.contains(trimmed, ignoreCase = true) || it.artist.contains(trimmed, ignoreCase = true) }) return true
        is MessageAttachment.Contact -> if (att.name.contains(trimmed, ignoreCase = true) || att.phone.contains(trimmed, ignoreCase = true)) return true
        is MessageAttachment.Geo -> if (att.address.contains(trimmed, ignoreCase = true)) return true
        is MessageAttachment.Photo -> if (att.label.contains(trimmed, ignoreCase = true)) return true
        is MessageAttachment.Sticker -> if (att.symbol.contains(trimmed, ignoreCase = true)) return true
        else -> Unit
    }
    return false
}

/** Заголовок дат-чипа над группой: «Сегодня», «Вчера» или «27 октября». */
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
