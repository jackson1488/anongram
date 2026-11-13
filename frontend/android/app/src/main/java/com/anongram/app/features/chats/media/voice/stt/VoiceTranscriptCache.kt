package com.anongram.app.features.chats.media.voice.stt

import java.util.concurrent.ConcurrentHashMap

/**
 * Локальный in-memory кэш расшифрованных голосовых сообщений,
 * чтобы повторное открытие или прокрутка чата не запускали распознавание заново.
 */
object VoiceTranscriptCache {
    private val cache = ConcurrentHashMap<String, String>()

    fun get(key: String): String? = cache[key]

    fun put(key: String, text: String) {
        if (text.isNotBlank()) {
            cache[key] = text
        }
    }

    fun remove(key: String) {
        cache.remove(key)
    }
}
