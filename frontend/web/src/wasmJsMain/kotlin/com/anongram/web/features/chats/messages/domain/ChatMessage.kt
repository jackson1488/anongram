package com.anongram.web.features.chats.messages.domain

import com.anongram.web.platform.currentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class MessageDirection { INCOMING, OUTGOING }

enum class MessageStatus { SENDING, SENT, READ, FAILED }

data class ChatMessage(
    val id: String,
    val direction: MessageDirection,
    val text: String = "",
    val epochMs: Long,
    val status: MessageStatus = MessageStatus.READ,
    val replyToId: String? = null,
    val replyToSubIndex: Int? = null,
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

fun ChatMessage.resolveSubItem(subIndex: Int?): ChatMessage {
    if (subIndex == null) return this
    val singleAttachment: MessageAttachment? = when (val att = attachment) {
        is MessageAttachment.MediaAlbum -> {
            val item = att.items.getOrNull(subIndex) ?: att.items.firstOrNull()
            item?.let {
                MessageAttachment.Photo(
                    label = "",
                    colorSeed = (subIndex % 6) + 1,
                    isVideo = it.isVideo,
                    uri = it.uri,
                    hasSpoiler = it.hasSpoiler
                )
            } ?: att
        }
        is MessageAttachment.FileGroup -> att.items.getOrNull(subIndex) ?: att.items.firstOrNull() ?: att
        is MessageAttachment.MusicGroup -> att.items.getOrNull(subIndex) ?: att.items.firstOrNull() ?: att
        else -> att
    }
    return copy(attachment = singleAttachment, replyToSubIndex = subIndex)
}

fun ChatMessage.timeLabel(): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}

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

private val RU_MONTHS = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)

fun epochDateLabel(epochMs: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(currentTimeMillis()).toLocalDateTime(zone).date
    val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
    return when (date) {
        today -> "Сегодня"
        yesterday -> "Вчера"
        else -> "${date.dayOfMonth} ${RU_MONTHS[(date.monthNumber - 1).coerceIn(0, 11)]}"
    }
}

fun isSameDay(firstMs: Long, secondMs: Long): Boolean {
    val zone = TimeZone.currentSystemDefault()
    return Instant.fromEpochMilliseconds(firstMs).toLocalDateTime(zone).date ==
        Instant.fromEpochMilliseconds(secondMs).toLocalDateTime(zone).date
}
