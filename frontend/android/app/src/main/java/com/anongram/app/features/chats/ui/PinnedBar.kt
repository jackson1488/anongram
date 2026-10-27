package com.anongram.app.features.chats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor

/**
 * Закреплённое сообщение (шаг 12, Фото 4):
 * Плавающий островок под верхней панелью, вертикальная полоска акцента,
 * заголовок «Закреплённое сообщение», текст, превью вложения (если фото без текста —
 * пишется «Фотография» с мини-превью квадратом) и иконка булавки.
 * Форма острова RoundedCornerShape(22.dp) как у поиска.
 */
@Composable
internal fun PinnedBar(
    pinned: ChatMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    val titleText = when (val att = pinned.attachment) {
        is MessageAttachment.Photo -> {
            if (pinned.text.isNotBlank()) pinned.text else if (att.isVideo) "Видеозапись" else "Фотография"
        }
        is MessageAttachment.File -> {
            if (pinned.text.isNotBlank()) pinned.text else att.name
        }
        is MessageAttachment.Music -> {
            if (pinned.text.isNotBlank()) pinned.text else "${att.artist} — ${att.title}"
        }
        is MessageAttachment.Voice -> {
            if (pinned.text.isNotBlank()) pinned.text else "Голосовое сообщение (${att.durationLabel})"
        }
        is MessageAttachment.VideoCircle -> {
            if (pinned.text.isNotBlank()) pinned.text else "Видеосообщение"
        }
        is MessageAttachment.Contact -> {
            if (pinned.text.isNotBlank()) pinned.text else "Контакт: ${att.name}"
        }
        is MessageAttachment.Geo -> {
            if (pinned.text.isNotBlank()) pinned.text else att.address.ifBlank { "Геопозиция" }
        }
        is MessageAttachment.Sticker -> {
            if (pinned.text.isNotBlank()) pinned.text else "Стикер ${att.symbol}"
        }
        null -> {
            pinned.text.ifBlank { "Сообщение" }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(IslandBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Вертикальная полоска акцента
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(10.dp))

        // Текстовая информация
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Закреплённое сообщение",
                color = scheme.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = titleText,
                color = TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Мини-превью вложения (миниатюра фото или иконка файла/медиа)
        when (val att = pinned.attachment) {
            is MessageAttachment.Photo -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(attachmentColor(att.colorSeed)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (att.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.File -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.Music -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(attachmentColor(att.colorSeed)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.Voice -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.VideoCircle -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(attachmentColor(att.colorSeed)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.Contact -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.Geo -> {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is MessageAttachment.Sticker -> {
                Text(
                    text = att.symbol,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            null -> Unit
        }

        // Иконка булавки
        Icon(
            imageVector = Icons.Outlined.PushPin,
            contentDescription = "Список закреплённых сообщений",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
