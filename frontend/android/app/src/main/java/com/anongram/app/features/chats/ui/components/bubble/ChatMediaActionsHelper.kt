package com.anongram.app.features.chats.ui.components.bubble

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import com.anongram.app.features.chats.media.circle.VideoCircleExportHelper
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.ui.components.savePhotoToDeviceGallery
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import com.anongram.app.features.chats.input.i18n.InputI18nStrings

/**
 * Хелперы сохранения в галерею/загрузки и системного шеринга вложений с локализованными баннерами (i18n).
 */
suspend fun saveMessageMediaToDevice(
    context: Context,
    msg: ChatMessage,
    subPhotoIdx: Int? = null
) {
    val att = msg.attachment
    var savedOk = false
    val resolveText: (InputI18nStrings) -> String = when (att) {
        is MessageAttachment.VideoCircle -> {
            savedOk = VideoCircleExportHelper.saveVideoCircleToGallery(context, att.uri)
            ({ s -> if (savedOk) s.bannerVideoCircleSavedToGallery else s.bannerVideoCircleSaveFailed })
        }
        is MessageAttachment.Voice -> {
            savedOk = saveVoiceToDeviceDownloads(context, att)
            ({ s -> if (savedOk) s.bannerVoiceSavedToDownloads else s.bannerVoiceSaveFailed })
        }
        is MessageAttachment.Photo -> {
            val uri = att.uri
            savedOk = when {
                uri.isNullOrBlank() -> false
                att.isVideo -> saveVideoToDeviceGallery(context, uri)
                else -> savePhotoToDeviceGallery(context, uri)
            }
            ({ s ->
                if (savedOk) {
                    if (att.isVideo) s.bannerVideoSavedToGallery else s.bannerPhotoSavedToGallery
                } else s.bannerSaveFailed
            })
        }
        is MessageAttachment.MediaAlbum -> {
            if (subPhotoIdx != null) {
                val item = att.items.getOrNull(subPhotoIdx)
                savedOk = if (item == null) false else if (item.isVideo) {
                    saveVideoToDeviceGallery(context, item.uri)
                } else {
                    savePhotoToDeviceGallery(context, item.uri)
                }
                ({ s -> if (savedOk) s.bannerSavedToGallery else s.bannerSaveFailed })
            } else {
                val count = saveMediaAlbumToGallery(context, att)
                savedOk = count > 0
                val total = att.items.size
                ({ s ->
                    if (savedOk) {
                        if (count == total) s.bannerAlbumSavedCount(count)
                        else s.bannerAlbumSavedPartial(count, total)
                    } else s.bannerAlbumSaveFailed
                })
            }
        }
        is MessageAttachment.File -> {
            savedOk = saveFileToDeviceDownloads(context, att)
            ({ s -> if (savedOk) s.bannerFileSavedToDownloads else s.bannerFileSaveFailed })
        }
        is MessageAttachment.FileGroup -> {
            var saved = 0
            for (f in att.items) {
                if (saveFileToDeviceDownloads(context, f)) saved++
            }
            savedOk = saved > 0
            val total = att.items.size
            ({ s -> if (savedOk) s.bannerFilesSavedPartial(saved, total) else s.bannerFilesSaveFailed })
        }
        is MessageAttachment.Music -> {
            savedOk = saveMusicToDeviceMusic(context, att)
            ({ s -> if (savedOk) s.bannerTrackSavedToMusic else s.bannerTrackSaveFailed })
        }
        is MessageAttachment.MusicGroup -> {
            var saved = 0
            for (m in att.items) {
                if (saveMusicToDeviceMusic(context, m)) saved++
            }
            savedOk = saved > 0
            val total = att.items.size
            ({ s -> if (savedOk) s.bannerTracksSavedPartial(saved, total) else s.bannerTracksSaveFailed })
        }
        else -> {
            ({ s -> s.bannerNothingToSave })
        }
    }
    ChatActionBannerBus.show(
        icon = if (savedOk) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
        isError = !savedOk,
        resolveText = resolveText
    )
}

suspend fun shareMessageMediaSystem(
    context: Context,
    msg: ChatMessage,
    subPhotoIdx: Int? = null
) {
    when (val att = msg.attachment) {
        is MessageAttachment.VideoCircle -> {
            val ok = VideoCircleExportHelper.shareVideoCircleSystemChooser(context, att.uri)
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerShareVideoPrepareFailed
                }
            }
        }
        is MessageAttachment.Voice -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "audio/mp4",
                chooserTitle = "Поделиться голосовым",
                suggestedFileName = "voice_${msg.id}.m4a"
            )
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerVoiceUnavailable
                }
            }
        }
        is MessageAttachment.Photo -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = if (att.isVideo) "video/mp4" else "image/jpeg",
                chooserTitle = if (att.isVideo) "Поделиться видео" else "Поделиться фото"
            )
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerFileUnavailable
                }
            }
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
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerFileUnavailable
                }
            }
        }
        is MessageAttachment.File -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "*/*",
                chooserTitle = att.name,
                suggestedFileName = att.name
            )
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerFileUnavailable
                }
            }
        }
        is MessageAttachment.Music -> {
            val ok = shareMediaUriSystemChooser(
                context = context,
                uriString = att.uri,
                mimeType = "audio/*",
                chooserTitle = "${att.artist} - ${att.title}"
            )
            if (!ok) {
                ChatActionBannerBus.show(Icons.Outlined.ErrorOutline, isError = true) {
                    it.bannerAudioUnavailable
                }
            }
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
