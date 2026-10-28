package com.anongram.app.features.attach.api

import android.net.Uri

/**
 * Контракт вложений (ТЗ):
 * Модуль ничего не отправляет сам, только отдаёт драфты.
 */
sealed interface AttachmentDraft {
    /** Фото или видео */
    data class Media(
        val uri: Uri,
        val isVideo: Boolean,
        val caption: String = "",
        val hd: Boolean = false,
        val ttl: MediaTtl = MediaTtl.Never
    ) : AttachmentDraft

    /** Файл любого типа без сжатия */
    data class File(
        val uri: Uri,
        val name: String,
        val size: Long,
        val caption: String = ""
    ) : AttachmentDraft

    /** Фиксированная геопозиция */
    data class Location(
        val lat: Double,
        val lon: Double,
        val address: String? = null
    ) : AttachmentDraft

    /** Трансляция геопозиции в реальном времени (15 мин, 1 час, 8 часов) */
    data class LiveLocation(
        val durationMin: Int
    ) : AttachmentDraft

    /** Контакт из телефонной книги */
    data class Contact(
        val name: String,
        val phones: List<String>
    ) : AttachmentDraft

    /** Аудиотрек */
    data class Audio(
        val uri: Uri,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val caption: String = ""
    ) : AttachmentDraft
}
