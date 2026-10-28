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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.IconButton
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
 * Компактный парящий островок по центру под верхней панелью,
 * вертикальная полоска акцента, заголовок «Закреплённое сообщение (1/2)»,
 * текст и превью вложения.
 * Слева/центр — переход к сообщению в чате (циклическое переключение),
 * справа — кнопка-булавка для открытия полного экрана закреплённых сообщений.
 */
@Composable
internal fun PinnedBar(
    pinned: ChatMessage,
    currentIndex: Int = 0,
    totalCount: Int = 1,
    onNavigateToPinned: () -> Unit,
    onOpenAllPinned: () -> Unit,
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

    val headerTitle = if (totalCount > 1) {
        "Закреплённое сообщение (${currentIndex + 1}/$totalCount)"
    } else {
        "Закреплённое сообщение"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(min = 180.dp, max = 340.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(IslandBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Тело: клик для перехода к сообщению / циклического переключения
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToPinned)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Вертикальная полоска акцента
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(scheme.accent)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Текстовая информация
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = headerTitle,
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = titleText,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Мини-превью вложения (миниатюра фото или иконка файла/медиа)
                when (val att = pinned.attachment) {
                    is MessageAttachment.Photo -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(attachmentColor(att.colorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (att.isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.File -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.Music -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(attachmentColor(att.colorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Audiotrack,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.Voice -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.VideoCircle -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(attachmentColor(att.colorSeed)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.Contact -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.Geo -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is MessageAttachment.Sticker -> {
                        Text(
                            text = att.symbol,
                            fontSize = 20.sp
                        )
                    }
                    null -> Unit
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Иконка булавки справа — открывает все закреплённые сообщения
            IconButton(
                onClick = onOpenAllPinned,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Все закреплённые сообщения",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
