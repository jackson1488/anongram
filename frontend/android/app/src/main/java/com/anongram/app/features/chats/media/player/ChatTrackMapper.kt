package com.anongram.app.features.chats.media.player

import android.net.Uri
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import java.io.File

/**
 * Сборка треков и очередей воспроизведения для видео-кружков, голосовых и музыки из сообщений чата.
 */
internal fun buildVideoCircleTrack(
    message: ChatMessage,
    peerTitle: String,
    chatId: String
): ChatTrack? {
    val vc = message.attachment as? MessageAttachment.VideoCircle ?: return null
    val path = vc.uri?.takeIf { it.isNotBlank() } ?: return null
    val f = File(path)
    val isOut = message.direction == MessageDirection.OUTGOING
    val name = if (isOut) "Вы" else peerTitle
    return ChatTrack(
        id = "circle_${message.id}",
        messageId = message.id,
        chatId = chatId,
        uri = if (f.exists()) Uri.fromFile(f) else Uri.parse(path),
        title = name,
        artist = name,
        durationLabel = vc.durationLabel,
        durationMs = 0L,
        isVoice = true,
        isOutgoing = isOut,
        chatTitle = peerTitle,
        chatAvatarSeed = peerTitle.hashCode(),
        isVideoCircle = true
    )
}

internal fun buildVoiceQueue(
    messages: List<ChatMessage>,
    targetMessage: ChatMessage,
    peerTitle: String,
    chatId: String
): Pair<List<ChatTrack>, ChatTrack?> {
    val tracks = messages
        .filter { it.epochMs >= targetMessage.epochMs && it.attachment is MessageAttachment.Voice }
        .map { msg ->
            val v = msg.attachment as MessageAttachment.Voice
            val sec = runCatching {
                val parts = v.durationLabel.split(":")
                when (parts.size) {
                    3 -> parts[0].toLong() * 3_600_000L + parts[1].toLong() * 60_000L + parts[2].toLong() * 1000L
                    2 -> parts[0].toLong() * 60_000L + parts[1].toLong() * 1000L
                    else -> 15_000L
                }
            }.getOrDefault(15_000L)
            val isOut = msg.direction == MessageDirection.OUTGOING
            val name = if (isOut) "Вы" else peerTitle
            val voiceUri = if (!v.uri.isNullOrBlank()) {
                val f = File(v.uri)
                if (f.exists()) Uri.fromFile(f) else Uri.parse(v.uri)
            } else Uri.EMPTY
            ChatTrack(
                id = "voice_${msg.id}",
                messageId = msg.id,
                chatId = chatId,
                uri = voiceUri,
                title = name,
                artist = name,
                durationLabel = v.durationLabel,
                durationMs = sec,
                isVoice = true,
                isOutgoing = isOut,
                chatTitle = peerTitle,
                chatAvatarSeed = peerTitle.hashCode()
            )
        }
    val current = tracks.firstOrNull { it.id == "voice_${targetMessage.id}" } ?: tracks.firstOrNull()
    return tracks to current
}

internal fun buildMusicQueue(
    messages: List<ChatMessage>,
    peerTitle: String,
    chatId: String
): List<ChatTrack> = messages.asReversed().flatMap { msg ->
    when (val att = msg.attachment) {
        is MessageAttachment.Music -> listOf(
            ChatTrack(
                id = "${msg.id}_${att.title}",
                messageId = msg.id,
                chatId = chatId,
                title = att.title,
                artist = att.artist,
                performer = if (att.artist.isNotBlank() && att.artist != "Неизвестен") att.artist else "AnonGram",
                durationLabel = att.durationLabel,
                durationMs = AudioMetadataHelper.parseDurationLabelToMs(att.durationLabel),
                sizeLabel = att.sizeLabel,
                chatTitle = peerTitle,
                chatAvatarSeed = peerTitle.hashCode(),
                uri = att.uri?.let { Uri.parse(it) } ?: Uri.EMPTY
            )
        )
        is MessageAttachment.MusicGroup -> att.items.mapIndexed { idx, item ->
            ChatTrack(
                id = "${msg.id}_${item.title}_$idx",
                messageId = msg.id,
                chatId = chatId,
                title = item.title,
                artist = item.artist,
                performer = if (item.artist.isNotBlank() && item.artist != "Неизвестен") item.artist else "AnonGram",
                durationLabel = item.durationLabel,
                durationMs = AudioMetadataHelper.parseDurationLabelToMs(item.durationLabel),
                sizeLabel = item.sizeLabel,
                chatTitle = peerTitle,
                chatAvatarSeed = peerTitle.hashCode(),
                uri = item.uri?.let { Uri.parse(it) } ?: Uri.EMPTY
            )
        }
        else -> emptyList()
    }
}.take(50)
