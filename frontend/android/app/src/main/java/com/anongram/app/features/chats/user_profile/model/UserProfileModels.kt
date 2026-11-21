package com.anongram.app.features.chats.user_profile.model

import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.SelfDestruct

enum class UserProfileTab {
    MEDIA,
    FILES,
    LINKS,
    GIF,
    AUDIO
}

data class UserProfileMediaItem(
    val message: ChatMessage,
    val uri: String,
    val isVideo: Boolean,
    val durationSec: Int = 0
)

data class UserProfileFileItem(
    val message: ChatMessage,
    val file: MessageAttachment.File
)

data class UserProfileLinkItem(
    val message: ChatMessage,
    val url: String
)

data class UserProfileAudioItem(
    val message: ChatMessage,
    val title: String,
    val subtitle: String,
    val uri: String?,
    val isVoice: Boolean
)

data class UserProfileBuckets(
    val media: List<UserProfileMediaItem> = emptyList(),
    val files: List<UserProfileFileItem> = emptyList(),
    val links: List<UserProfileLinkItem> = emptyList(),
    val gifs: List<UserProfileMediaItem> = emptyList(),
    val audio: List<UserProfileAudioItem> = emptyList()
) {
    fun availableTabs(): List<UserProfileTab> = buildList {
        if (media.isNotEmpty()) add(UserProfileTab.MEDIA)
        if (files.isNotEmpty()) add(UserProfileTab.FILES)
        if (links.isNotEmpty()) add(UserProfileTab.LINKS)
        if (gifs.isNotEmpty()) add(UserProfileTab.GIF)
        if (audio.isNotEmpty()) add(UserProfileTab.AUDIO)
    }
}

enum class AutoDeleteWheelOption(
    val mapped: SelfDestruct,
    val ruLabel: String,
    val enLabel: String
) {
    OFF(SelfDestruct.OFF, "Нет", "Off"),
    S5(SelfDestruct.S5, "5 секунд", "5 seconds"),
    M1(SelfDestruct.M1, "1 минута", "1 minute"),
    H1(SelfDestruct.H1, "1 час", "1 hour"),
    D1(SelfDestruct.D1, "1 день", "1 day"),
    W1(SelfDestruct.W1, "1 неделя", "1 week")
}
