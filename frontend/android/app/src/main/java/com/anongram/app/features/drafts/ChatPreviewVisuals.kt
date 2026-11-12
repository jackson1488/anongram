package com.anongram.app.features.drafts

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
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
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Очищенный текст черновика для превью карточки чата:
 * строго набранный текст без служебных префиксов «Ответ: ».
 */
fun cleanDraftPreviewText(draft: ChatDraft?): String? {
    if (draft == null || draft.isEmpty) return null
    return draft.previewText.ifBlank { null }
}

/**
 * Динамическая сортировка чатов:
 * 1. Закрепленные чаты (до [maxPinned] штук) всегда находятся на первых позициях.
 * 2. Незакрепленные чаты упорядочиваются по максимальному времени активности:
 *    время последнего сообщения или момент изменения/набора черновика.
 *    При появлении черновика или отправке сообщения чат немедленно перемещается наверх под закрепами.
 */
fun sortSessionsWithDrafts(
    sessions: List<IChatSession>,
    drafts: Map<String, ChatDraft>,
    maxPinned: Int = 10
): List<IChatSession> {
    val pinned = mutableListOf<IChatSession>()
    val unpinned = mutableListOf<IChatSession>()

    for (session in sessions) {
        if (session.isPinned && pinned.size < maxPinned) {
            pinned.add(session)
        } else {
            unpinned.add(session)
        }
    }

    val sortedUnpinned = unpinned.sortedByDescending { session ->
        val draft = drafts[session.id]
        val draftTime = if (draft != null && !draft.isEmpty) draft.updatedAtEpochMs else 0L
        maxOf(session.lastMessageEpochMs, draftTime)
    }

    return pinned + sortedUnpinned
}

/**
 * Компактная микро-миниатюра для строки чата (18x18 dp со скруглением 3 dp).
 */
@Composable
fun ChatPreviewMicroThumb(
    uriString: String?,
    seed: Int = 0,
    isVideo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = remember(uriString) {
        if (!uriString.isNullOrBlank()) runCatching { Uri.parse(uriString) }.getOrNull() else null
    }
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            if (quick != null) bitmap = quick.asImageBitmap()
        }
    }

    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(shape)
            .background(attachmentColor(seed)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isVideo) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/**
 * Единая общая функция отображения медиа-вложений для карточки чата:
 * - Фотоальбомы (MediaAlbum): до 3 микро-превью в ряд + акцентный текст «N фото»
 * - Фото/Видео (Photo): 1 микро-превью + «Фото» / «Видео»
 * - Музыка (Music / MusicGroup): иконка ноты/обложка + «Музыка»
 * - Видеокружок (VideoCircle): иконка кружка + «Видеосообщение»
 * - Голосовое (Voice): иконка микрофона + «Голосовое сообщение»
 * - Файл (File / FileGroup): иконка документа + имя файла
 */
@Composable
fun ChatMediaAttachmentPreview(
    attachment: MessageAttachment?,
    modifier: Modifier = Modifier
) {
    if (attachment == null) return
    val scheme = LocalAnonGramScheme.current
    val accentColor = scheme.accent

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (attachment) {
            is MessageAttachment.MediaAlbum -> {
                val thumbs = attachment.items.take(3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    thumbs.forEachIndexed { idx, item ->
                        ChatPreviewMicroThumb(
                            uriString = item.uri,
                            seed = idx,
                            isVideo = item.isVideo
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${attachment.items.size} фото",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            is MessageAttachment.Photo -> {
                ChatPreviewMicroThumb(
                    uriString = attachment.uri,
                    seed = attachment.colorSeed,
                    isVideo = attachment.isVideo
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (attachment.isVideo) "Видео" else "Фото",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            is MessageAttachment.Music -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Музыка: ",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = attachment.title,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is MessageAttachment.MusicGroup -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${attachment.items.size} треков",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            is MessageAttachment.VideoCircle -> {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Видеосообщение",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            is MessageAttachment.Voice -> {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Голосовое сообщение",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            is MessageAttachment.File -> {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = attachment.name,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is MessageAttachment.FileGroup -> {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${attachment.items.size} файлов",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            else -> {}
        }
    }
}
