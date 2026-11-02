package com.anongram.app.features.chats.ui.components.reply

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Краткое текстовое описание сообщения/вложения для плашки ответа над полем ввода (FusedReplyBar)
 * и цитаты внутри пузыря сообщения (QuoteBlock).
 * Поддерживает все виды вложений: одиночное фото, альбом, файлы, музыку, голосовые, кружки, контакты, гео, стикеры.
 */
fun replyPreviewSnippet(message: ChatMessage): String {
    val attLabel = when (val att = message.attachment) {
        is MessageAttachment.Photo -> if (att.isVideo) "Видео" else "Фотография"
        is MessageAttachment.MediaAlbum -> "Альбом (${att.items.size} фото)"
        is MessageAttachment.File -> att.name
        is MessageAttachment.FileGroup -> "Файлы (${att.items.size} шт.)"
        is MessageAttachment.Music -> {
            if (att.artist.isNotBlank() && att.artist != "Неизвестен" && !att.title.contains(att.artist)) {
                "${att.artist} — ${att.title}"
            } else {
                att.title
            }
        }
        is MessageAttachment.MusicGroup -> "Аудиозаписи (${att.items.size} шт.)"
        is MessageAttachment.Voice -> "Голосовое сообщение (${att.durationLabel})"
        is MessageAttachment.VideoCircle -> "Видеосообщение (${att.durationLabel})"
        is MessageAttachment.Contact -> "Контакт: ${att.name}"
        is MessageAttachment.Geo -> "Геопозиция: ${att.address}"
        is MessageAttachment.Sticker -> "${att.symbol} Стикер"
        null -> ""
    }
    return when {
        message.text.isNotBlank() && attLabel.isNotBlank() && message.attachment !is MessageAttachment.Photo && message.attachment !is MessageAttachment.MediaAlbum ->
            "$attLabel • ${message.text}"
        message.text.isNotBlank() -> message.text
        attLabel.isNotBlank() -> attLabel
        else -> "Сообщение"
    }
}

/**
 * Квадратная миниатюра (34.dp) для плашки ответа и цитаты внутри бабла:
 * - Для Photo, MediaAlbum и фото-файлов без сжатия (FileKind.IMAGE) загружает реальную миниатюру фотографии.
 * - Для документов, музыки, голосовых, видеокружков, контактов, гео и стикеров рисует аккуратную иконку.
 */
@Composable
fun ReplyAttachmentMiniThumb(
    message: ChatMessage,
    accent: Color,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    val attachment = message.attachment ?: return
    val context = LocalContext.current

    val imageUriString: String? = remember(attachment) {
        when (attachment) {
            is MessageAttachment.Photo -> attachment.uri
            is MessageAttachment.MediaAlbum -> attachment.items.firstOrNull()?.uri
            is MessageAttachment.File -> {
                val isImg = attachment.kind == FileKind.IMAGE ||
                    attachment.name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")
                if (isImg) attachment.uri else null
            }
            is MessageAttachment.FileGroup -> {
                val first = attachment.items.firstOrNull()
                if (first != null) {
                    val isImg = first.kind == FileKind.IMAGE ||
                        first.name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")
                    if (isImg) first.uri else null
                } else null
            }
            else -> null
        }
    }

    var thumbBitmap by remember(imageUriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageUriString) {
        val parsed = imageUriString?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (parsed != null) {
            val bmp = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsed) }
            thumbBitmap = bmp?.asImageBitmap()
        } else {
            thumbBitmap = null
        }
    }

    when (attachment) {
        is MessageAttachment.Photo,
        is MessageAttachment.MediaAlbum -> {
            val isVid = (attachment as? MessageAttachment.Photo)?.isVideo == true ||
                (attachment as? MessageAttachment.MediaAlbum)?.items?.firstOrNull()?.isVideo == true
            val seed = (attachment as? MessageAttachment.Photo)?.colorSeed ?: 1
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(attachmentColor(seed).copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (isVid) Icons.Outlined.Videocam else Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
                if (isVid && thumbBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.48f)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.34f)
                        )
                    }
                }
            }
        }

        is MessageAttachment.File,
        is MessageAttachment.FileGroup -> {
            val file = (attachment as? MessageAttachment.File)
                ?: (attachment as? MessageAttachment.FileGroup)?.items?.firstOrNull()
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val icon = when {
                        file?.kind == FileKind.APK || file?.name?.endsWith(".apk", ignoreCase = true) == true -> Icons.Outlined.Android
                        file?.kind == FileKind.PDF || file?.kind == FileKind.DOC -> Icons.Outlined.Description
                        else -> Icons.Outlined.InsertDriveFile
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(size * 0.56f)
                    )
                }
            }
        }

        is MessageAttachment.Music,
        is MessageAttachment.MusicGroup -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Headphones,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }

        is MessageAttachment.Voice -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }

        is MessageAttachment.VideoCircle -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }

        is MessageAttachment.Contact -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }

        is MessageAttachment.Geo -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(size * 0.56f)
                )
            }
        }

        is MessageAttachment.Sticker -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = attachment.symbol,
                    fontSize = (size.value * 0.58f).sp
                )
            }
        }
    }
}
