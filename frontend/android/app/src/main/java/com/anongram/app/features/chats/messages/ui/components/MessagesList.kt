package com.anongram.app.features.chats.messages.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CyberTeal
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.isSameDay

/** Окно между сообщениями одной группы — 2dp, между группами — 8dp. */
private const val GROUP_GAP_MS = 5 * 60_000L

/**
 * Список сообщений (логика Telegram): reverseLayout — новейшие внизу,
 * группировка подряд идущих сообщений одного направления, дат-чипы при
 * смене суток, отступы 2/8dp внутри и между группами.
 */
@Composable
internal fun MessagesList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onReply: (ChatMessage) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    if (messages.isEmpty()) {
        EmptyThreadHint()
        return
    }

    val renderItems = remember(messages) { buildRenderItems(messages) }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)
    ) {
        itemsIndexed(
            renderItems,
            key = { _, item -> item.message.id }
        ) { _, item ->
            Column(modifier = Modifier.padding(top = item.spacingAboveDp.dp)) {
                if (item.dateChipAbove) {
                    DateChip(epochMs = item.message.epochMs)
                }
                MessageBubble(
                    item = item,
                    onReply = onReply,
                    onCopy = onCopy,
                    onDelete = onDelete
                )
            }
        }
    }
}

private fun buildRenderItems(messages: List<ChatMessage>): List<MessageRenderItem> =
    messages.mapIndexed { index, message ->
        val previous = messages.getOrNull(index - 1)
        val next = messages.getOrNull(index + 1)
        val groupedWithPrev = previous != null &&
            previous.direction == message.direction &&
            (message.epochMs - previous.epochMs) < GROUP_GAP_MS
        val groupedWithNext = next != null &&
            next.direction == message.direction &&
            (next.epochMs - message.epochMs) < GROUP_GAP_MS
        MessageRenderItem(
            message = message,
            dateChipAbove = previous == null || !isSameDay(previous.epochMs, message.epochMs),
            isFirstInGroup = !groupedWithPrev,
            isLastInGroup = !groupedWithNext,
            spacingAboveDp = if (groupedWithPrev) 2 else 8
        )
    }.asReversed()

@Composable
private fun EmptyThreadHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.ic_stat_anongram),
                contentDescription = null,
                tint = CyberTeal.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Сообщений пока нет",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Напишите первое зашифрованное сообщение",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
