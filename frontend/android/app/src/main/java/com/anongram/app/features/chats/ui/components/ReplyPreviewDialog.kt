package com.anongram.app.features.chats.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.wallpapers.ui.WallpaperBackground

/**
 * Модальное мини-окно превью ответа на сообщение Telegram-style (Фото 2):
 * - Сверху и снизу отступы ~5 см (72.dp) с полупрозрачным затемнением;
 * - Нажатие на верхнюю или нижнюю затемнённую область закрывает мини-окно;
 * - Внутри карточки:
 *   - Заголовок «Ответ на сообщение»
 *   - Обои чата
 *   - Интерактивное сообщение: фото можно открыть и посмотреть, файл скачать/открыть, аудио послушать;
 *   - Меню под сообщением (Фото 2): «Ответить в другом чате», «Применить изменения», «Убрать ответ».
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        // Внешний фон: нажатие по затемнённой области сверху и снизу (5 см) закрывает окно
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Центрированная плавающая карточка с отступами ~5 см сверху и снизу (72dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(IslandBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Блокируем закрытие при клике внутри самой карточки
                    )
            ) {
                // Шапка карточки: «Ответ на сообщение»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ответ на сообщение",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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
                        .heightIn(min = 300.dp, max = 540.dp)
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Интерактивный пузырь сообщения (тап скроллит к оригиналу в чате)
                        InteractiveReplyBubble(
                            message = replyMessage,
                            peerName = peerName,
                            onJumpToMessage = {
                                onJumpToMessage()
                                onDismiss()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Меню под сообщением в точности как на скриншоте Фото 2
                        ReplyActionMenu(
                            onChangeChat = onDismiss,
                            onApplyChanges = onDismiss,
                            onRemoveReply = {
                                onRemoveReply()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Интерактивный пузырь сообщения в окне превью:
 * Фото можно развернуть и посмотреть, файл скачать/открыть, аудио послушать.
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

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(18.dp))
            .background(CardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        when (val attachment = message.attachment) {
            is MessageAttachment.File -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Кнопка скачивания файла
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

            is MessageAttachment.Photo -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Мини-превью / превью фото с возможностью просмотра
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isExpandedPhotoOpen) 220.dp else 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(attachmentColor(attachment.colorSeed))
                            .clickable { isExpandedPhotoOpen = !isExpandedPhotoOpen },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (attachment.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                            contentDescription = "Просмотр фотографии",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(if (isExpandedPhotoOpen) 56.dp else 36.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isExpandedPhotoOpen) "Свернуть" else "Посмотреть",
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

            is MessageAttachment.Music -> {
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

            is MessageAttachment.Voice -> {
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

            is MessageAttachment.Contact -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onJumpToMessage)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = attachment.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = attachment.phone, color = TextMuted, fontSize = 12.sp)
                    }
                    Text(text = message.timeLabel(), color = TextMuted, fontSize = 11.sp)
                }
            }

            is MessageAttachment.Geo -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onJumpToMessage)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = FlameOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = attachment.address, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(text = message.timeLabel(), color = TextMuted, fontSize = 11.sp)
                }
            }

            else -> {
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

/**
 * Меню действий под сообщением в точности по скриншоту (Фото 2 / media_1793132892184.png):
 * 1) «Ответить в другом чате»
 * 2) «Применить изменения»
 * 3) «Убрать ответ»
 */
@Composable
private fun ReplyActionMenu(
    onChangeChat: () -> Unit,
    onApplyChanges: () -> Unit,
    onRemoveReply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .background(IslandBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // 1. Ответить в другом чате
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChangeChat)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Sync,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Ответить в другом чате",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder.copy(alpha = 0.4f))
        )

        // 2. Применить изменения
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onApplyChanges)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Применить изменения",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder.copy(alpha = 0.4f))
        )

        // 3. Убрать ответ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRemoveReply)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
