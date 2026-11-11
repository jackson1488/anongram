package com.anongram.app.features.chats.ui.input.attach

import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.ui.input.editor.TextStyleFormat
import java.util.Locale

/**
 * Единый маппер черновиков вложений (отправка с группировкой по 10 шт.,
 * замена 1-на-1 при редактировании и форматирование текста из полного редактора).
 */
internal fun AttachmentDraft.File.toMessageAttachment(): MessageAttachment.File {
    val ext = name.substringAfterLast('.', "").lowercase()
    val kind = when (ext) {
        "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> FileKind.IMAGE
        "pdf" -> FileKind.PDF
        "apk" -> FileKind.APK
        "doc", "docx", "txt", "rtf" -> FileKind.DOC
        "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
        else -> FileKind.UNKNOWN
    }
    val kb = size / 1024.0
    val sizeLabel = if (kb >= 1024.0) {
        String.format(Locale.forLanguageTag("ru"), "%.1f MB", kb / 1024.0)
    } else {
        String.format(Locale.forLanguageTag("ru"), "%.1f KB", kb.coerceAtLeast(0.1))
    }
    return MessageAttachment.File(
        name = name,
        sizeLabel = sizeLabel,
        kind = kind,
        uri = uri.toString()
    )
}

internal fun AttachmentDraft.Audio.toMessageAttachment(colorSeed: Int = 1): MessageAttachment.Music {
    val min = durationMs / 60000
    val sec = (durationMs % 60000) / 1000
    val durLabel = String.format("%d:%02d", min, sec)
    return MessageAttachment.Music(
        title = title,
        artist = artist,
        durationLabel = durLabel,
        colorSeed = colorSeed,
        uri = uri.toString()
    )
}

internal fun sendAttachDraftsToChat(
    drafts: List<AttachmentDraft>,
    viewModel: ChatMessagesViewModel
) {
    // 1. Сжатые медиа с группировкой (до 10 шт. в альбом)
    val groupedMedia = drafts.filterIsInstance<AttachmentDraft.Media>().filter { !it.ungrouped }
    if (groupedMedia.isNotEmpty()) {
        val anyCaption = groupedMedia.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
        val mediaChunks = groupedMedia.chunked(10)
        mediaChunks.forEachIndexed { chunkIdx, chunk ->
            val chunkCaption = if (chunkIdx == mediaChunks.lastIndex) anyCaption else ""
            if (chunk.size == 1) {
                val m = chunk[0]
                viewModel.sendAttachment(
                    MessageAttachment.Photo(
                        label = "",
                        colorSeed = 1,
                        isVideo = m.isVideo,
                        uri = m.uri.toString(),
                        hasSpoiler = m.hasSpoiler
                    ),
                    caption = chunkCaption
                )
            } else {
                viewModel.sendAttachment(
                    MessageAttachment.MediaAlbum(
                        items = chunk.map { m ->
                            MessageAttachment.AlbumMediaItem(
                                uri = m.uri.toString(),
                                isVideo = m.isVideo,
                                hasSpoiler = m.hasSpoiler
                            )
                        },
                        hasSpoiler = chunk.any { it.hasSpoiler }
                    ),
                    caption = chunkCaption
                )
            }
        }
    }

    // 2. Сжатые медиа «Отправить без группировки» — каждое отдельным пузырём
    drafts.filterIsInstance<AttachmentDraft.Media>().filter { it.ungrouped }.forEach { draft ->
        viewModel.sendAttachment(
            MessageAttachment.Photo(
                label = "",
                colorSeed = 1,
                isVideo = draft.isVideo,
                uri = draft.uri.toString(),
                hasSpoiler = draft.hasSpoiler
            ),
            caption = draft.caption
        )
    }

    // 3. Файлы и фото без сжатия — слитно группами по 10 штук
    val fileDrafts = drafts.filterIsInstance<AttachmentDraft.File>()
    if (fileDrafts.isNotEmpty()) {
        val anyFileCaption = fileDrafts.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
        val fileChunks = fileDrafts.chunked(10)
        fileChunks.forEachIndexed { chunkIdx, chunk ->
            val chunkCaption = if (chunkIdx == fileChunks.lastIndex) anyFileCaption else ""
            val mappedFiles = chunk.map { it.toMessageAttachment() }
            if (mappedFiles.size == 1) {
                viewModel.sendAttachment(mappedFiles[0], caption = chunkCaption)
            } else {
                viewModel.sendAttachment(MessageAttachment.FileGroup(items = mappedFiles), caption = chunkCaption)
            }
        }
    }

    // 4. Музыка — слитно группами по 10 треков
    val audioDrafts = drafts.filterIsInstance<AttachmentDraft.Audio>()
    if (audioDrafts.isNotEmpty()) {
        val anyAudioCaption = audioDrafts.firstOrNull { it.caption.isNotBlank() }?.caption ?: ""
        val audioChunks = audioDrafts.chunked(10)
        audioChunks.forEachIndexed { chunkIdx, chunk ->
            val chunkCaption = if (chunkIdx == audioChunks.lastIndex) anyAudioCaption else ""
            val mappedTracks = chunk.mapIndexed { idx, draft ->
                draft.toMessageAttachment(colorSeed = (idx % 6) + 1)
            }
            if (mappedTracks.size == 1) {
                viewModel.sendAttachment(mappedTracks[0], caption = chunkCaption)
            } else {
                viewModel.sendAttachment(MessageAttachment.MusicGroup(items = mappedTracks), caption = chunkCaption)
            }
        }
    }

    // 5. Контакты и геопозиции — отправляются поштучно
    drafts.forEach { draft ->
        when (draft) {
            is AttachmentDraft.Location -> {
                viewModel.sendAttachment(
                    MessageAttachment.Geo(address = draft.address ?: "${draft.lat}, ${draft.lon}")
                )
            }
            is AttachmentDraft.LiveLocation -> {
                val strings = com.anongram.app.features.chats.ui.input.i18n.InputI18n.forLanguage(
                    com.anongram.app.core.i18n.LanguageSettingsStore.language.value
                )
                viewModel.sendAttachment(
                    MessageAttachment.Geo(address = strings.liveLocation(draft.durationMin))
                )
            }
            is AttachmentDraft.Contact -> {
                viewModel.sendAttachment(
                    MessageAttachment.Contact(
                        name = draft.name,
                        phone = draft.phones.firstOrNull() ?: ""
                    )
                )
            }
            else -> Unit
        }
    }
}

internal fun mapSingleReplaceDraft(draft: AttachmentDraft): Pair<MessageAttachment?, String?> = when (draft) {
    is AttachmentDraft.Media -> MessageAttachment.Photo(
        label = "",
        colorSeed = 1,
        isVideo = draft.isVideo,
        uri = draft.uri.toString(),
        hasSpoiler = draft.hasSpoiler
    ) to draft.caption
    is AttachmentDraft.File -> draft.toMessageAttachment() to draft.caption
    is AttachmentDraft.Audio -> draft.toMessageAttachment(colorSeed = 1) to draft.caption
    else -> null to null
}

internal fun formatFullEditorMessageText(
    text: String,
    format: TextStyleFormat,
    language: String
): String = when (format) {
    TextStyleFormat.CODE -> if (text.startsWith("```")) text else "```$language\n$text\n```"
    TextStyleFormat.SPOILER -> if (text.contains("||")) text else "||$text||"
    TextStyleFormat.HEADING -> if (text.startsWith("#")) text else "# $text"
    TextStyleFormat.QUOTE -> if (text.startsWith(">")) text else "> $text"
    TextStyleFormat.CALLOUT -> if (text.startsWith(">>>")) text else ">>> $text"
    else -> text
}
