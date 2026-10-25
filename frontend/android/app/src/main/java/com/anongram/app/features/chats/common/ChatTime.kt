package com.anongram.app.features.chats.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Метка времени последнего сообщения: «23:14» сегодня, «Вчера», иначе «dd.MM.yy». */
fun IChatSession.timeLabel(now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val messageDate = Instant.ofEpochMilli(lastMessageEpochMs).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = Instant.ofEpochMilli(lastMessageEpochMs).atZone(zone)
    return when (messageDate) {
        today -> time.format(DateTimeFormatter.ofPattern("HH:mm"))
        today.minusDays(1) -> "Вчера"
        else -> time.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
    }
}
