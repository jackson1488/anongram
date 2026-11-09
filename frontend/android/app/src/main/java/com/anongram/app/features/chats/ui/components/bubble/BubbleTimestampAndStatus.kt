package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.MessageStatus

/**
 * Иконка статуса доставки сообщения (отправка, отправлено, прочитано, ошибка с повтором).
 */
@Composable
fun DeliveryStatus(
    status: MessageStatus,
    onRetry: () -> Unit,
    iconSize: Dp = 13.dp
) {
    val scheme = LocalAnonGramScheme.current
    when (status) {
        MessageStatus.SENDING -> Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = "Отправляется",
            tint = TextMuted,
            modifier = Modifier.size(iconSize)
        )

        MessageStatus.SENT -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_single),
            contentDescription = "Отправлено",
            tint = TextMuted,
            modifier = Modifier.size(iconSize)
        )

        MessageStatus.READ -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_double),
            contentDescription = "Прочитано",
            tint = scheme.accent,
            modifier = Modifier.size(iconSize)
        )

        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = "Ошибка доставки. Нажмите для повтора",
            tint = FlameOrange,
            modifier = Modifier
                .size(iconSize)
                .clickable(onClick = onRetry)
        )
    }
}

/**
 * Строка времени, пометки «изменено» и галочек статуса.
 */
@Composable
fun BubbleTimestampRow(
    timeLabel: String,
    isEdited: Boolean,
    isIncoming: Boolean,
    status: MessageStatus,
    textScale: Float,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFontSize = (10f * textScale).coerceIn(5.5f, 14f).sp
    val editedFontSize = (9f * textScale).coerceIn(5f, 12.5f).sp
    val tickSize = (13f * textScale).coerceIn(7f, 17f).dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEdited) {
            Text(
                text = "изменено",
                color = TextMuted,
                fontSize = editedFontSize,
                fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.width((4f * textScale).coerceIn(2f, 5f).dp))
        }
        Text(
            text = timeLabel,
            color = TextMuted,
            fontSize = timeFontSize,
            fontFamily = FontFamily.Monospace
        )
        if (!isIncoming) {
            Spacer(modifier = Modifier.width((4f * textScale).coerceIn(2f, 5f).dp))
            DeliveryStatus(status = status, onRetry = onRetry, iconSize = tickSize)
        }
    }
}

/**
 * Форма пузыря сообщения: скругления и хвостик у последнего сообщения в цепочке.
 */
fun bubbleShape(
    item: MessageRenderItem,
    isIncoming: Boolean,
    scale: Float = 1f
): RoundedCornerShape {
    val radius = (16f * scale).coerceIn(7f, 20f).dp
    val tail = (5f * scale).coerceIn(2.5f, 6.5f).dp
    val bottomTail = if (item.isLastInGroup) tail else radius
    return if (isIncoming) {
        RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = bottomTail,
            bottomEnd = radius
        )
    } else {
        RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = radius,
            bottomEnd = bottomTail
        )
    }
}
