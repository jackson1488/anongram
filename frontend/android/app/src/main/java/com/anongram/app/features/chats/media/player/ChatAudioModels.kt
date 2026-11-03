package com.anongram.app.features.chats.media.player

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class ChatRepeatMode {
    OFF,
    ALL,
    ONE
}

@Immutable
data class ChatTrack(
    val id: String,
    val messageId: String = "",
    val chatId: String = "",
    val uri: Uri = Uri.EMPTY,
    val title: String,
    val artist: String = "",
    val durationLabel: String = "0:00",
    val durationMs: Long = 0L,
    val performer: String = artist
)

@Immutable
data class ChatPlayerState(
    val currentTrack: ChatTrack? = null,
    val queue: List<ChatTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: ChatRepeatMode = ChatRepeatMode.OFF,
    val isVisible: Boolean = false,
    val isSheetOpen: Boolean = false
) {
    val progressMs: Long get() = positionMs

    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionLabel: String
        get() {
            val totalSec = positionMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
}
