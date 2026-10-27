package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection

private val QUICK_REACTIONS = listOf("❤️", "👍", "👎", "🔥", "🥰", "👏", "😁")

/**
 * Всплывающее контекстное меню сообщения по простому тапу (Фото 2):
 * - Полоска быстрых реакций с шевроном
 * - Меню действий: Ответить, Копировать, Переслать, Закрепить/Открепить, Удалить
 */
@Composable
internal fun MessageContextMenu(
    message: ChatMessage,
    isPinned: Boolean = false,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMyMessage = message.direction == MessageDirection.OUTGOING
    val isEditable = isMyMessage && (System.currentTimeMillis() - message.epochMs) < 48 * 3600_000L

    val isIncoming = message.direction == MessageDirection.INCOMING

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 68.dp),
            horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Полоска быстрых реакций (Фото 2: ❤️, 👍, 👎, 🔥, 🥰, 👏, 😁, ∨)
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

            // Меню действий (Фото 2)
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
                ContextMenuRow(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Копировать",
                    onClick = { onCopy(); onDismiss() }
                )
                if (isEditable) {
                    ContextMenuRow(
                        icon = Icons.Outlined.Edit,
                        label = "Изменить",
                        onClick = { onEdit(); onDismiss() }
                    )
                }
                ContextMenuRow(
                    icon = Icons.Outlined.Share,
                    label = "Переслать",
                    onClick = { onForward(); onDismiss() }
                )
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

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
