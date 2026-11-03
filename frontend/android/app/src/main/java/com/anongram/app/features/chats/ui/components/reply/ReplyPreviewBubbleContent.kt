package com.anongram.app.features.chats.ui.components.reply

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.chats.ui.components.bubble.ChatMediaAlbumGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Интерактивный пузырь сообщения в окне превью ответа (ReplyPreviewDialog):
 * Поддерживает ВСЕ типы сообщений и вложений:
 * - Одиночное фото / видео (или 1 выбранное фото из альбома) с реальной картинкой;
 * - Весь альбом (MediaAlbum), если ответили свайпом по всему баблу;
 * - Одиночный файл (File), фото без сжатия и группу файлов (FileGroup);
 * - Музыку (Music и MusicGroup);
 * - Голосовое (Voice), Видеокружок (VideoCircle), Контакт (Contact), Геопозицию (Geo), Стикер (Sticker) и Текст.
 */
@Composable
internal fun InteractiveReplyBubble(
    message: ChatMessage,
    peerName: String,
    onJumpToMessage: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    var isExpandedPhotoOpen by remember { mutableStateOf(false) }
    var isFileDownloaded by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    when (val attachment = message.attachment) {
        is MessageAttachment.Photo -> {
            var photoBitmap by remember(attachment.uri) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(attachment.uri) {
                val parsed = attachment.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (parsed != null) {
                    val bmp = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsed) }
                    photoBitmap = bmp?.asImageBitmap()
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(attachmentColor(attachment.colorSeed))
                        .clickable(onClick = onJumpToMessage),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap!!,
                            contentDescription = "Фотография",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 260.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (attachment.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                            contentDescription = if (attachment.isVideo) "Видео" else "Фотография",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(if (attachment.isVideo) 54.dp else 46.dp)
                        )
                    }

                    if (attachment.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Воспроизвести видео",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = message.timeLabel(),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onJumpToMessage)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }

        is MessageAttachment.MediaAlbum -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable(onClick = onJumpToMessage)
                    .padding(8.dp)
            ) {
                ChatMediaAlbumGrid(
                    items = attachment.items,
                    accent = scheme.accent,
                    onPhotoClick = { onJumpToMessage() }
                )

                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        is MessageAttachment.File -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                ReplyFileRowContent(
                    file = attachment,
                    accent = scheme.accent,
                    timeLabel = if (message.text.isBlank()) message.timeLabel() else null,
                    isFileDownloaded = isFileDownloaded,
                    onToggleDownload = { isFileDownloaded = !isFileDownloaded },
                    onJumpToMessage = onJumpToMessage
                )
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onJumpToMessage)
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        is MessageAttachment.FileGroup -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                attachment.items.forEachIndexed { idx, file ->
                    ReplyFileRowContent(
                        file = file,
                        accent = scheme.accent,
                        timeLabel = if (idx == attachment.items.lastIndex && message.text.isBlank()) message.timeLabel() else null,
                        isFileDownloaded = false,
                        onToggleDownload = {},
                        onJumpToMessage = onJumpToMessage
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onJumpToMessage)
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        is MessageAttachment.Music -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                ReplyMusicRowContent(
                    music = attachment,
                    accent = scheme.accent,
                    timeLabel = if (message.text.isBlank()) message.timeLabel() else null,
                    onJumpToMessage = onJumpToMessage
                )
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        is MessageAttachment.MusicGroup -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachment.items.forEachIndexed { idx, track ->
                    ReplyMusicRowContent(
                        music = track,
                        accent = scheme.accent,
                        timeLabel = if (idx == attachment.items.lastIndex && message.text.isBlank()) message.timeLabel() else null,
                        onJumpToMessage = onJumpToMessage
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        is MessageAttachment.Voice -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { isAudioPlaying = !isAudioPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Прослушать",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Голосовое (${attachment.durationLabel})",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onJumpToMessage)
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        is MessageAttachment.VideoCircle -> {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable(onClick = onJumpToMessage)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.25f))
                        .border(2.dp, scheme.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Видеосообщение",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Видеосообщение • ${attachment.durationLabel} • ${message.timeLabel()}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        is MessageAttachment.Geo -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onJumpToMessage)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Геолокация",
                        tint = scheme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = attachment.address,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        is MessageAttachment.Contact -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onJumpToMessage)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Контакт",
                        tint = scheme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = attachment.phone,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        is MessageAttachment.Sticker -> {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onJumpToMessage)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = attachment.symbol,
                    fontSize = 72.sp
                )
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onJumpToMessage)
                ) {
                    Text(
                        text = message.text.ifBlank { "Сообщение" },
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyAlbumCellThumb(
    uriString: String,
    isVideo: Boolean,
    extraCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bmp by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
        if (parsed != null) {
            val loaded = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsed) }
            bmp = loaded?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF1E222D)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extraCount",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReplyFileRowContent(
    file: MessageAttachment.File,
    accent: Color,
    timeLabel: String?,
    isFileDownloaded: Boolean,
    onToggleDownload: () -> Unit,
    onJumpToMessage: () -> Unit
) {
    val context = LocalContext.current
    val isImageFile = file.kind == FileKind.IMAGE ||
        file.name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")

    var thumbBitmap by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file.uri, isImageFile) {
        val parsed = file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (isImageFile && parsed != null) {
            val loaded = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsed) }
            thumbBitmap = loaded?.asImageBitmap()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isImageFile && thumbBitmap != null) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E222D))
                    .clickable(onClick = onJumpToMessage)
            ) {
                Image(
                    bitmap = thumbBitmap!!,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isFileDownloaded) Color(0xFF2E7D32) else accent)
                    .clickable(onClick = onToggleDownload),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    isFileDownloaded -> Icons.Outlined.DownloadDone
                    file.kind == FileKind.APK || file.name.endsWith(".apk", ignoreCase = true) -> Icons.Outlined.Android
                    file.kind == FileKind.PDF || file.kind == FileKind.DOC -> Icons.Outlined.Description
                    else -> Icons.Outlined.FileDownload
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Файл",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onJumpToMessage)
        ) {
            Text(
                text = file.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${file.sizeLabel} ${file.kind.label}",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
        if (timeLabel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeLabel,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ReplyMusicRowContent(
    music: MessageAttachment.Music,
    accent: Color,
    timeLabel: String?,
    onJumpToMessage: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = "Воспроизвести",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onJumpToMessage)
        ) {
            Text(
                text = music.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${music.artist} • ${music.durationLabel}",
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (timeLabel != null) {
            Text(
                text = timeLabel,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
