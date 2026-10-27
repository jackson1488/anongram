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
import androidx.compose.material.icons.outlined.CheckCircle
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
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection

private val QUICK_REACTIONS = listOf("👍", "❤️", "🔥", "😂", "😮", "👏")

/**
 * Меню действий с сообщением по долгому тапу (шаг 9):
 * затемнение, полоска реакций, пункты меню (Ответить, Копировать, Изменить,
 * Переслать, Закрепить, Выбрать, Удалить).
 */
@Composable
internal fun MessageContextMenu(
    message: ChatMessage,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val isMyMessage = message.direction == MessageDirection.OUTGOING
    val isEditable = isMyMessage && (System.currentTimeMillis() - message.epochMs) < 48 * 3600_000L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clickable(enabled = false) {}
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Полоска быстрых реакций
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                onReaction(emoji)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Меню действий
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(vertical = 4.dp)
            ) {
                MenuItem(
                    icon = Icons.Outlined.Reply,
                    label = "Ответить",
                    onClick = { onReply(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Копировать",
                    onClick = { onCopy(); onDismiss() }
                )
                if (isEditable) {
                    MenuItem(
                        icon = Icons.Outlined.Edit,
                        label = "Изменить",
                        onClick = { onEdit(); onDismiss() }
                    )
                }
                MenuItem(
                    icon = Icons.Outlined.Share,
                    label = "Переслать",
                    onClick = { onForward(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Outlined.PushPin,
                    label = "Закрепить",
                    onClick = { onPin(); onDismiss() }
                )
                MenuItem(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Выбрать",
                    onClick = { onSelect(); onDismiss() }
                )
                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                MenuItem(
                    icon = Icons.Outlined.Delete,
                    label = "Удалить",
                    color = FlameOrange,
                    onClick = { onDelete(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
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
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
