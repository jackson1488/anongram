package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection

private val QUICK_REACTIONS = listOf("❤️", "👍", "👎", "🔥", "🥰", "👏", "😁")

/**
 * Контекстное меню сообщения по быстрому тапу (рядом с пузырём или в свободной области строки).
 * Пункты зависят от типа содержимого:
 * - Текст: Копировать, Изменить (свои без лимита времени), Ответить, Переслать, Закрепить, Удалить
 * - ГС: Сохранить в Загрузки, Изменить, Поделиться, Ответить, Переслать, Закрепить, Удалить
 * - Видео-кружок: Сохранить в галерею, Поделиться, Изменить, Ответить, Переслать, Закрепить, Удалить
 * - Фото / видео / альбом: Сохранить в галерею (все N элементов), Поделиться, Копировать (если есть подпись), Изменить, ...
 * - Файл / музыка: Сохранить в Загрузки / Музыку, Поделиться, ...
 */
@Composable
internal fun MessageContextMenu(
    message: ChatMessage,
    anchorYPx: Float = 0f,
    isPinned: Boolean = false,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMyMessage = message.direction == MessageDirection.OUTGOING
    val att = message.attachment
    val isEditable = isMyMessage && att !is MessageAttachment.Sticker

    val saveLabel: String? = when (att) {
        is MessageAttachment.VideoCircle -> "Сохранить в галерею"
        is MessageAttachment.Photo -> "Сохранить в галерею"
        is MessageAttachment.MediaAlbum -> "Сохранить в галерею (${att.items.size})"
        is MessageAttachment.Voice -> "Сохранить в Загрузки"
        is MessageAttachment.File -> "Сохранить в Загрузки"
        is MessageAttachment.FileGroup -> "Сохранить файлы (${att.items.size})"
        is MessageAttachment.Music -> "Сохранить в Музыку"
        is MessageAttachment.MusicGroup -> "Сохранить треки (${att.items.size})"
        else -> null
    }

    val showShare: Boolean = when (att) {
        is MessageAttachment.VideoCircle,
        is MessageAttachment.Voice,
        is MessageAttachment.Photo,
        is MessageAttachment.File,
        is MessageAttachment.Music -> true
        else -> false
    }

    val showCopy: Boolean = when (att) {
        is MessageAttachment.Voice,
        is MessageAttachment.VideoCircle -> false
        is MessageAttachment.Photo,
        is MessageAttachment.MediaAlbum,
        is MessageAttachment.File,
        is MessageAttachment.FileGroup,
        is MessageAttachment.Music,
        is MessageAttachment.MusicGroup -> message.text.isNotBlank()
        else -> message.text.isNotBlank() ||
            att is MessageAttachment.Contact ||
            att is MessageAttachment.Geo
    }

    val itemCount = 4 +
        (if (saveLabel != null) 1 else 0) +
        (if (showCopy) 1 else 0) +
        (if (isEditable) 1 else 0) +
        (if (showShare) 1 else 0)
    val estimatedMenuDp = (74 + itemCount * 42).dp

    val isIncoming = message.direction == MessageDirection.INCOMING
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val menuHeightPx = with(density) { estimatedMenuDp.toPx() }
    val topBoundPx = with(density) { 64.dp.toPx() }
    val bottomBoundPx = screenHeightPx - with(density) { 68.dp.toPx() }
    val maxTop = (bottomBoundPx - menuHeightPx).coerceAtLeast(topBoundPx)

    val computedYPx = if (anchorYPx > 0f) {
        if (anchorYPx > screenHeightPx * 0.48f) {
            (anchorYPx - menuHeightPx - with(density) { 8.dp.toPx() }).coerceIn(topBoundPx, maxTop)
        } else {
            (anchorYPx + with(density) { 44.dp.toPx() }).coerceIn(topBoundPx, maxTop)
        }
    } else {
        (screenHeightPx - menuHeightPx - with(density) { 80.dp.toPx() }).coerceIn(topBoundPx, maxTop)
    }

    val yOffsetDp = with(density) { computedYPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = yOffsetDp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
        ) {
            Column(
                modifier = Modifier
                    .width(268.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF21252E))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onReaction(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2F3543)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Ещё реакции",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF21252E))
                        .padding(vertical = 4.dp)
                ) {
                    ContextMenuRow(
                        icon = Icons.Outlined.Reply,
                        label = "Ответить",
                        onClick = { onReply(); onDismiss() }
                    )
                    if (saveLabel != null) {
                        ContextMenuRow(
                            icon = Icons.Outlined.FileDownload,
                            label = saveLabel,
                            onClick = { onSave(); onDismiss() }
                        )
                    }
                    if (showCopy) {
                        ContextMenuRow(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Копировать",
                            onClick = { onCopy(); onDismiss() }
                        )
                    }
                    if (isEditable) {
                        ContextMenuRow(
                            icon = Icons.Outlined.Edit,
                            label = "Изменить",
                            onClick = { onEdit(); onDismiss() }
                        )
                    }
                    ContextMenuRow(
                        icon = Icons.Outlined.Reply,
                        label = "Переслать",
                        flipIcon = true,
                        onClick = { onForward(); onDismiss() }
                    )
                    if (showShare) {
                        ContextMenuRow(
                            icon = Icons.Outlined.Share,
                            label = "Поделиться",
                            onClick = { onShare(); onDismiss() }
                        )
                    }
                    ContextMenuRow(
                        icon = Icons.Outlined.PushPin,
                        label = if (isPinned) "Открепить" else "Закрепить",
                        onClick = { onPin(); onDismiss() }
                    )
                    HorizontalDivider(
                        color = CardBorder,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    ContextMenuRow(
                        icon = Icons.Outlined.Delete,
                        label = "Удалить",
                        color = Color(0xFFFF5252),
                        onClick = { onDelete(); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = TextPrimary,
    flipIcon: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .then(if (flipIcon) Modifier.graphicsLayer { scaleX = -1f } else Modifier)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
