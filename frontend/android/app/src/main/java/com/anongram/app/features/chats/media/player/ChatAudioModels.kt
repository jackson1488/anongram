package com.anongram.app.features.chats.media.player

import android.graphics.Bitmap
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
    val performer: String = artist,
    val isVoice: Boolean = false,
    val isOutgoing: Boolean = false,
    val chatTitle: String = "",
    val chatAvatarSeed: Int = 0,
    val sizeLabel: String = "3.7 MB",
    val albumArtBitmap: Bitmap? = null,
    /** Видеосообщение (кружок): звук и картинка идут через общий плеер, видео рисуется в TextureView кружка */
    val isVideoCircle: Boolean = false
)

@Immutable
data class ChatPlayerState(
    val currentTrack: ChatTrack? = null,
    val queue: List<ChatTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: ChatRepeatMode = ChatRepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val isVisible: Boolean = false,
    val isSheetOpen: Boolean = false,
    val isFullScreenOpen: Boolean = false
) {
    val progressMs: Long get() = positionMs

    val progress: Float
        get() = if (durationMs > 0 && positionMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionLabel: String
        get() {
            val totalSec = positionMs / 1000
            return if (totalSec >= 3600) {
                "%d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
            } else {
                "%d:%02d".format(totalSec / 60, totalSec % 60)
            }
        }

    val remainingLabel: String
        get() {
            val remMs = (durationMs - positionMs).coerceAtLeast(0L)
            val totalSec = remMs / 1000
            return if (totalSec >= 3600) {
                "-%d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
            } else {
                "-%d:%02d".format(totalSec / 60, totalSec % 60)
            }
        }
}
