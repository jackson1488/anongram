package com.anongram.app.features.player.api

/**
 * Неизменяемое состояние воспроизведения плеера (ТЗ, player: state: StateFlow<PlayerState>).
 */
data class PlayerState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffle: Boolean = false,
    val isVisible: Boolean = false,
    val isFullPlayerOpen: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val positionLabel: String
        get() {
            val totalSec = positionMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

    val durationLabel: String
        get() = currentTrack?.durationLabel ?: "0:00"
}
