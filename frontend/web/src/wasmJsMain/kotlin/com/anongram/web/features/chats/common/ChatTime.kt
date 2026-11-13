package com.anongram.web.features.chats.common

import com.anongram.web.platform.currentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun IChatSession.timeLabel(now: Long = currentTimeMillis()): String {
    val zone = TimeZone.currentSystemDefault()
    val msgDt = Instant.fromEpochMilliseconds(lastMessageEpochMs).toLocalDateTime(zone)
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
    val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
    return when (msgDt.date) {
        today -> "${msgDt.hour.toString().padStart(2, '0')}:${msgDt.minute.toString().padStart(2, '0')}"
        yesterday -> "Вчера"
        else -> "${msgDt.dayOfMonth.toString().padStart(2, '0')}.${msgDt.monthNumber.toString().padStart(2, '0')}.${(msgDt.year % 100).toString().padStart(2, '0')}"
    }
}
