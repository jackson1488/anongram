package com.anongram.app.features.chats.messages.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.MessageStatus
import com.anongram.app.features.chats.messages.domain.timeLabel

/** Пузырь исходящего сообщения: тёмно-синий корпус в акцентной рамке темы. */
private val OutgoingBubbleBackground = Color(0xFF0C243B)
private val ReplyStripIncoming = Color(0xFF0B121E)
private val ReplyStripOutgoing = Color(0xFF081A2C)

/**
 * Пузырь сообщения (логика Telegram): ширина до 78% экрана, хвостик-уголок
 * у первого сообщения группы, цитата ответа, время и тик статуса доставки
 * внутри пузыря. Long-press открывает контекстное меню.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    item: MessageRenderItem,
    onReply: (ChatMessage) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val message = item.message
    val isIncoming = message.direction == MessageDirection.INCOMING
    var menuOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isIncoming) Alignment.TopStart else Alignment.TopEnd
    ) {
        Surface(
            shape = bubbleShape(item),
            color = if (isIncoming) CardDark else OutgoingBubbleBackground,
            border = if (isIncoming) {
                null
            } else {
                BorderStroke(1.dp, scheme.accent.copy(alpha = 0.35f))
            },
            modifier = Modifier
                .widthIn(max = maxWidth * 0.78f)
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
            ) {
                message.replyToSnippet?.let { snippet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isIncoming) ReplyStripIncoming else ReplyStripOutgoing)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = snippet,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }

                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timeLabel(),
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isIncoming) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DeliveryStatus(status = message.status)
                    }
                }
            }
        }

        MessageContextMenu(
            expanded = menuOpen,
            canDelete = !isIncoming,
            onReply = { onReply(message) },
            onCopy = { onCopy(message.text) },
            onDelete = { onDelete(message) },
            onDismiss = { menuOpen = false }
        )
    }
}

@Composable
private fun DeliveryStatus(status: MessageStatus) {
    val scheme = LocalAnonGramScheme.current
    when (status) {
        MessageStatus.SENDING -> Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = "Отправляется",
            tint = TextMuted,
            modifier = Modifier.size(12.dp)
        )

        MessageStatus.SENT -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_single),
            contentDescription = "Отправлено",
            tint = TextMuted,
            modifier = Modifier.size(13.dp)
        )

        MessageStatus.READ -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_double),
            contentDescription = "Прочитано",
            tint = scheme.accent,
            modifier = Modifier.size(13.dp)
        )
    }
}

/** Хвостик пузыря: маленький уголок у первого сообщения группы (как в TG). */
private fun bubbleShape(item: MessageRenderItem): RoundedCornerShape {
    val radius = 16.dp
    val tail = 5.dp
    val isIncoming = item.message.direction == MessageDirection.INCOMING
    return if (isIncoming) {
        RoundedCornerShape(
            topStart = if (item.isFirstInGroup) tail else radius,
            topEnd = radius,
            bottomStart = if (item.isLastInGroup) tail else radius,
            bottomEnd = radius
        )
    } else {
        RoundedCornerShape(
            topStart = radius,
            topEnd = if (item.isFirstInGroup) tail else radius,
            bottomStart = radius,
            bottomEnd = if (item.isLastInGroup) tail else radius
        )
    }
}
