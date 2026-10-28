package com.anongram.app.features.player.api

import android.net.Uri

/**
 * Музыкальный трек в очереди плеера (ТЗ, player).
 */
@androidx.compose.runtime.Immutable
data class Track(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUri: Uri? = null,
    val isDownloaded: Boolean = true
) {
    val durationLabel: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
}
