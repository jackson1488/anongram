package com.anongram.app.features.chats.common

/**
 * Длительность отключения уведомлений для bottom-листа «Без звука».
 * durationMs = 0 означает «навсегда».
 */
enum class MuteDuration(val label: String, val durationMs: Long) {
    HOUR_1("1 час", 3_600_000L),
    HOURS_8("8 часов", 28_800_000L),
    DAY_1("1 сутки", 86_400_000L),
    DAYS_2("2 дня", 172_800_000L),
    FOREVER("Навсегда", 0L);

    /** epoch millis окончания мьюта; 0L — навсегда. */
    fun untilEpochMs(now: Long = System.currentTimeMillis()): Long =
        if (this == FOREVER) 0L else now + durationMs
}
