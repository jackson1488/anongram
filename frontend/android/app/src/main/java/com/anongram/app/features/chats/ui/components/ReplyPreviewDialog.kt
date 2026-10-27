package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.wallpapers.ui.WallpaperBackground

/**
 * Полноэкранный оверлей превью ответа на сообщение точно по образцу Telegram (media_1793136789840.png):
 * - Задний фон чата размыт красивым блюром (RenderEffect blur) и слегка затемнён;
 * - Верхняя карточка начинается под статус-баром и занимает верхнюю часть экрана;
 * - Внутри карточки: заголовок «Ответ на сообщение», обои чата и интерактивное сообщение (фото/видео с просмотром, файл, аудио);
 * - Под карточкой справа — аккуратная плавающая кнопка «Убрать ответ» (FlameOrange);
 * - Тап по размытой области вокруг закрывает оверлей.
 */
@Composable
internal fun ReplyPreviewDialog(
    chatId: String,
    replyMessage: ChatMessage,
    peerName: String,
    onRemoveReply: () -> Unit,
    onJumpToMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    // Внешний фон оверлея с затемнением — клик в любую пустую область закрывает окно
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .safeStatusBarPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 24.dp)
        ) {
            // Карточка превью с обоями чата (Фото: media_1793136789840.png)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 360.dp, max = 560.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(IslandBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // предотвращаем закрытие при тапе по телу карточки
                    )
            ) {
                // Шапка карточки: «Ответ на сообщение»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF20232B))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ответ на сообщение",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Закрыть",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Внутренняя область с обоями чата
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Обои чата
                    WallpaperBackground(
                        chatId = chatId,
                        modifier = Modifier.matchParentSize()
                    )

                    // Мягкое затемнение обоев
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )

                    // Пузырь сообщения над обоями
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        InteractiveReplyBubble(
                            message = replyMessage,
                            peerName = peerName,
                            onJumpToMessage = {
                                onJumpToMessage()
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Единственная кнопка под карточкой справа: «Убрать ответ» (Фото: media_1793136789840.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF20232B),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onRemoveReply()
                                onDismiss()
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Убрать ответ",
                            color = FlameOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Интерактивный пузырь сообщения в окне превью:
 * Фото / видео можно посмотреть или развернуть, файл скачать, аудио послушать.
 */
@Composable
private fun InteractiveReplyBubble(
    message: ChatMessage,
    peerName: String,
    onJumpToMessage: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var isExpandedPhotoOpen by remember { mutableStateOf(false) }
    var isFileDownloaded by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    when (val attachment = message.attachment) {
        is MessageAttachment.Photo -> {
            // Превью фото / видео точно по фото media_1793136789840.png
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isExpandedPhotoOpen) 280.dp else 190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(attachmentColor(attachment.colorSeed))
                        .clickable { isExpandedPhotoOpen = !isExpandedPhotoOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (attachment.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                        contentDescription = if (attachment.isVideo) "Видео" else "Фотография",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(if (attachment.isVideo) 54.dp else 46.dp)
                    )

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
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (attachment.isVideo) {
                                if (isExpandedPhotoOpen) "Свернуть видео" else "Воспроизвести видео"
                            } else {
                                if (isExpandedPhotoOpen) "Свернуть" else "Посмотреть"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isFileDownloaded) Color(0xFF2E7D32) else scheme.accent)
                            .clickable { isFileDownloaded = !isFileDownloaded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFileDownloaded) Icons.Outlined.DownloadDone else Icons.Outlined.FileDownload,
                            contentDescription = "Скачать файл",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onJumpToMessage)
                    ) {
                        Text(
                            text = attachment.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFileDownloaded) "Файл скачан • Нажмите чтобы открыть" else "${attachment.sizeLabel} ${attachment.kind.label}",
                            color = if (isFileDownloaded) scheme.accent else TextMuted,
                            fontSize = 12.sp
                        )
                    }
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
                            text = message.timeLabel(),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        is MessageAttachment.Music -> {
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { isAudioPlaying = !isAudioPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
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
                            text = attachment.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${attachment.artist} • ${attachment.durationLabel}",
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

        is MessageAttachment.Location -> {
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
                        text = attachment.address ?: "${attachment.latitude}, ${attachment.longitude}",
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

        else -> {
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
