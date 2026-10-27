package com.anongram.app.features.chats.messages.domain

import androidx.compose.ui.graphics.Color

/**
 * Вложения сообщения (шаги 4–6, 15). Отправитель формирует контент локально;
 * реальные байты поедут через ICryptoBridge — UI работает только с моделью.
 */
sealed interface MessageAttachment {

    /** Фото/видео из галереи (fake-превью по colorSeed). */
    data class Photo(val label: String, val colorSeed: Int, val isVideo: Boolean = false) : MessageAttachment

    data class File(val name: String, val sizeLabel: String, val kind: FileKind) : MessageAttachment

    data class Music(val title: String, val artist: String, val durationLabel: String, val colorSeed: Int) : MessageAttachment

    data class Voice(val durationLabel: String, val waveform: List<Int>) : MessageAttachment

    /** Видео-кружок: круглое превью, лимит 60 сек. */
    data class VideoCircle(val durationLabel: String, val colorSeed: Int) : MessageAttachment

    data class Contact(val name: String, val phone: String) : MessageAttachment

    data class Geo(val address: String) : MessageAttachment

    /** Стикер/крупный эмодзи — рисуется без пузыря. */
    data class Sticker(val symbol: String, val colorSeed: Int) : MessageAttachment
}

enum class FileKind(val label: String) {
    PDF("PDF"), DOC("DOC"), ARCHIVE("ZIP"), IMAGE("IMG"), VIDEO("VID"), UNKNOWN("FILE")
}

/** Превью ссылки: заголовок, описание, домен — считается на стороне отправителя. */
data class LinkPreview(
    val url: String,
    val domain: String,
    val title: String,
    val description: String
)

/** Цвет-заглушка для fake-превью по сиду (реальные битмапы придут с медиа-слоем). */
fun attachmentColor(seed: Int): Color {
    val palette = listOf(0xFF0C243B, 0xFF123328, 0xFF2B1A3D, 0xFF3A2413, 0xFF10303A)
    return Color(palette[((seed % palette.size) + palette.size) % palette.size])
}
