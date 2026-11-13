package com.anongram.web.features.chats.messages.domain

import androidx.compose.ui.graphics.Color

sealed interface MessageAttachment {

    data class Photo(
        val label: String,
        val colorSeed: Int,
        val isVideo: Boolean = false,
        val uri: String? = null,
        val hasSpoiler: Boolean = false
    ) : MessageAttachment

    data class AlbumMediaItem(
        val uri: String,
        val isVideo: Boolean = false,
        val hasSpoiler: Boolean = false
    )

    data class MediaAlbum(
        val items: List<AlbumMediaItem>,
        val hasSpoiler: Boolean = false
    ) : MessageAttachment

    data class File(
        val name: String,
        val sizeLabel: String,
        val kind: FileKind,
        val uri: String? = null
    ) : MessageAttachment

    data class FileGroup(
        val items: List<File>
    ) : MessageAttachment

    data class Music(
        val title: String,
        val artist: String,
        val durationLabel: String,
        val colorSeed: Int,
        val uri: String? = null,
        val sizeLabel: String = "3.7 MB"
    ) : MessageAttachment

    data class MusicGroup(
        val items: List<Music>
    ) : MessageAttachment

    data class Voice(
        val durationLabel: String,
        val waveform: List<Int>,
        val uri: String? = null
    ) : MessageAttachment

    data class VideoCircle(
        val durationLabel: String,
        val colorSeed: Int = 0,
        val uri: String? = null
    ) : MessageAttachment

    data class Contact(
        val name: String,
        val phone: String = "",
        val fingerprint: String = "",
        val isSasVerified: Boolean = false
    ) : MessageAttachment

    data class Geo(
        val address: String,
        val lat: Double = 0.0,
        val lon: Double = 0.0
    ) : MessageAttachment

    data class Sticker(val symbol: String, val colorSeed: Int) : MessageAttachment
}

enum class FileKind(val label: String) {
    PDF("PDF"), DOC("DOC"), ARCHIVE("ZIP"), IMAGE("JPG"), VIDEO("VID"), APK("APK"), UNKNOWN("FILE")
}

data class LinkPreview(
    val url: String,
    val domain: String,
    val title: String,
    val description: String
)

fun attachmentColor(seed: Int): Color {
    val palette = listOf(0xFF0C243B, 0xFF123328, 0xFF2B1A3D, 0xFF3A2413, 0xFF10303A)
    return Color(palette[((seed % palette.size) + palette.size) % palette.size])
}
