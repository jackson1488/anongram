package com.anongram.app.features.chats.attach.api

import com.anongram.app.features.chats.attach.i18n.AttachI18nStrings

/**
 * Таймер исчезновения медиа (шаг 11 ТЗ):
 * Выберите, через какое время после открытия медиафайл исчезнет.
 */
enum class MediaTtl(val title: String, val seconds: Int) {
    Never("Никогда", 0),
    OneView("Один просмотр", 1),
    Sec3("3 сек.", 3),
    Sec10("10 сек.", 10),
    Sec30("30 сек.", 30);

    val isEphemeral: Boolean get() = this != Never

    fun label(strings: AttachI18nStrings): String = when (this) {
        Never -> strings.ttlNever
        OneView -> strings.ttlOneView
        Sec3 -> strings.ttl3s
        Sec10 -> strings.ttl10s
        Sec30 -> strings.ttl30s
    }
}
