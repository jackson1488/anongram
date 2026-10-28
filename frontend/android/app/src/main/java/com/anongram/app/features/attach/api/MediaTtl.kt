package com.anongram.app.features.attach.api

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
}
