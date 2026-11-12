package com.anongram.app.features.chats.input.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.ui.AttachSheetContent
import com.anongram.app.features.chats.attach.ui.AttachTab
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.input.i18n.LocalInputStrings

/**
 * Специализированная шторка вложений для полноэкранного редактора сообщений (EditorAttachSheet).
 * Наследует UI и функционал основного AttachSheetContent, оставляя модуль features/attach/ нетронутым ("атаку щит").
 *
 * Перехватывает выбор вложений и перенаправляет их в черновик редактора (attachedMediaList),
 * не закрывая редактор и не отправляя сообщение сразу в базу данных чата.
 */
@Composable
fun EditorAttachSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAttachSelected: (List<MessageAttachment>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val strings = LocalInputStrings.current

    AttachSheetContent(
        chatId = "editor_draft",
        recipientName = strings.draft,
        initialTab = AttachTab.GALLERY,
        isSingleReplaceMode = false,
        onDismiss = onDismiss,
        onSend = { drafts ->
            val attachments = drafts.map { draft ->
                when (draft) {
                    is AttachmentDraft.Media -> {
                        MessageAttachment.Photo(
                            label = draft.uri.lastPathSegment ?: strings.media,
                            colorSeed = 2,
                            isVideo = draft.isVideo,
                            uri = draft.uri.toString(),
                            hasSpoiler = draft.hasSpoiler
                        )
                    }
                    is AttachmentDraft.File -> {
                        MessageAttachment.File(
                            name = draft.name,
                            sizeLabel = formatBytes(draft.size),
                            kind = resolveFileKind(draft.name),
                            uri = draft.uri.toString()
                        )
                    }
                    is AttachmentDraft.Audio -> {
                        MessageAttachment.Music(
                            title = draft.title.ifBlank { strings.audioTrack },
                            artist = draft.artist.ifBlank { strings.unknown },
                            durationLabel = formatDuration(draft.durationMs),
                            colorSeed = 3,
                            uri = draft.uri.toString()
                        )
                    }
                    is AttachmentDraft.Location -> {
                        MessageAttachment.Geo(
                            address = draft.address ?: strings.location(draft.lat, draft.lon),
                            lat = draft.lat,
                            lon = draft.lon
                        )
                    }
                    is AttachmentDraft.Contact -> {
                        MessageAttachment.Contact(
                            name = draft.name,
                            phone = draft.phones.firstOrNull() ?: ""
                        )
                    }
                }
            }
            onAttachSelected(attachments)
            onDismiss()
        },
        modifier = modifier
    )
}

private fun resolveFileKind(name: String): FileKind {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> FileKind.PDF
        "doc", "docx", "txt", "rtf" -> FileKind.DOC
        "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
        "png", "jpg", "jpeg", "webp", "gif" -> FileKind.IMAGE
        "mp4", "mkv", "mov", "webm" -> FileKind.VIDEO
        "apk" -> FileKind.APK
        else -> FileKind.UNKNOWN
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
