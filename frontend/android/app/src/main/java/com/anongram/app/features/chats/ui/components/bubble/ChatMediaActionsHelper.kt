package com.anongram.app.features.chats.ui.components.bubble

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.ui.graphics.vector.ImageVector
import com.anongram.app.features.chats.media.circle.VideoCircleExportHelper
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.ui.components.savePhotoToDeviceGallery
import com.anongram.app.features.chats.ui.input.banner.ChatActionBannerBus

/**
 * Вынесенные из ChatScreen.kt хелперы сохранения в галерею/загрузки и системного шеринга вложений.
 */
suspend fun saveMessageMediaToDevice(
    context: Context,
    msg: ChatMessage,
    subPhotoIdx: Int? = null,
    onBanner: (text: String, icon: ImageVector, isError: Boolean) -> Unit = { text, icon, isError ->
        ChatActionBannerBus.show(text, icon, isError)
    }
) {
    val att = msg.attachment
    var savedOk = false
    val msgText = when (att) {
        is MessageAttachment.VideoCircle -> {
            savedOk = VideoCircleExportHelper.saveVideoCircleToGallery(context, att.uri)
            if (savedOk) "Видео-кружок сохранён в галерею" else "Не удалось сохранить кружок"
        }
        is MessageAttachment.Voice -> {
            savedOk = saveVoiceToDeviceDownloads(context, att)
            if (savedOk) "Голосовое сохранено в Загрузки" else "Не удалось сохранить голосовое"
        }
        is MessageAttachment.Photo -> {
            val uri = att.uri
            savedOk = when {
                uri.isNullOrBlank() -> false
                att.isVideo -> saveVideoToDeviceGallery(context, uri)
                else -> savePhotoToDeviceGallery(context, uri)
            }
            if (savedOk) {
                if (att.isVideo) "Видео сохранено в галерею" else "Фото сохранено в галерею"
            } else "Не удалось сохранить"
        }
        is MessageAttachment.MediaAlbum -> {
            if (subPhotoIdx != null) {
                val item = att.items.getOrNull(subPhotoIdx)
                savedOk = if (item == null) false else if (item.isVideo) {
                    saveVideoToDeviceGallery(context, item.uri)
                } else {
                    savePhotoToDeviceGallery(context, item.uri)
                }
                if (savedOk) "Сохранено в галерею" else "Не удалось сохранить"
            } else {
                val count = saveMediaAlbumToGallery(context, att)
                savedOk = count > 0
                if (savedOk) {
                    if (count == att.items.size) "Скачано $count фото в галерею"
                    else "Скачано $count из ${att.items.size} в галерею"
                } else "Не удалось сохранить альбом"
            }
        }
        is MessageAttachment.File -> {
            savedOk = saveFileToDeviceDownloads(context, att)
            if (savedOk) "Файл сохранён в Загрузки" else "Не удалось сохранить файл"
        }
        is MessageAttachment.FileGroup -> {
            var saved = 0
            for (f in att.items) {
                if (saveFileToDeviceDownloads(context, f)) saved++
            }
            savedOk = saved > 0
            if (savedOk) "Скачано файлов: $saved из ${att.items.size} в Загрузки" else "Не удалось сохранить файлы"
        }
        is MessageAttachment.Music -> {
            savedOk = saveMusicToDeviceMusic(context, att)
            if (savedOk) "Трек сохранён в Музыку" else "Не удалось сохранить трек"
        }
        is MessageAttachment.MusicGroup -> {
            var saved = 0
            for (m in att.items) {
                if (saveMusicToDeviceMusic(context, m)) saved++
            }
            savedOk = saved > 0
            if (savedOk) "Сохранено треков: $saved из ${att.items.size}" else "Не удалось сохранить треки"
        }
        else -> "Нечего сохранять"
    }
    onBanner(
        msgText,
        if (savedOk) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
        !savedOk
    )
}

suspend fun shareMessageMediaSystem(
    context: Context,
    msg: ChatMessage,
    subPhotoIdx: Int? = null,
    onError: (String) -> Unit = { errText ->
        ChatActionBannerBus.show(errText, Icons.Outlined.ErrorOutline, isError = true)
    }
) {
    when (val att = msg.attachment) {
        is MessageAttachment.VideoCircle -> {
            val ok = VideoCircleExportHelper.shareVideoCircleSystemChooser(context, att.uri)
            if (!ok) onError("Не удалось подготовить видео для отправки")
        }
        is MessageAttachment.Voice -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "audio/mp4",
                chooserTitle = "Поделиться голосовым",
                suggestedFileName = "voice_${msg.id}.m4a"
            )
            if (!ok) onError("Файл голосового недоступен")
        }
        is MessageAttachment.Photo -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = if (att.isVideo) "video/mp4" else "image/jpeg",
                chooserTitle = if (att.isVideo) "Поделиться видео" else "Поделиться фото"
            )
            if (!ok) onError("Файл недоступен")
        }
        is MessageAttachment.MediaAlbum -> {
            val item = subPhotoIdx?.let { att.items.getOrNull(it) } ?: att.items.firstOrNull()
            val ok = if (item != null) {
                shareMediaUriSystemChooser(
                    context = context,
                    uriString = item.uri,
                    mimeType = if (item.isVideo) "video/mp4" else "image/jpeg",
                    chooserTitle = "Поделиться"
                )
            } else false
            if (!ok) onError("Файл недоступен")
        }
        is MessageAttachment.File -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "*/*",
                chooserTitle = att.name,
                suggestedFileName = att.name
            )
            if (!ok) onError("Файл недоступен")
        }
        is MessageAttachment.Music -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "audio/*",
                chooserTitle = "${att.artist} - ${att.title}"
            )
            if (!ok) onError("Аудио недоступно")
        }
        else -> {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg.text.ifBlank { "AnonGram" })
            }
            context.startActivity(Intent.createChooser(shareIntent, "Поделиться"))
        }
    }
}
