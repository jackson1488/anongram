package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.isSameDay
import com.anongram.app.features.chats.ui.animateScrollAndCenterItem
import kotlinx.coroutines.launch

/** Окно между сообщениями одной группы — 2dp, между группами — 8dp. */
private const val GROUP_GAP_MS = 5 * 60_000L

/**
 * Список сообщений (шаги 1, 9–11, 13): reverseLayout — новейшие внизу,
 * группировка одного автора, дат-чипы при смене суток, разделитель непрочитанных,
 * тап по цитате скроллит к оригиналу, выделение, свайп для ответа.
 */
@Composable
internal fun MessagesList(
    messages: List<ChatMessage>,
    peerName: String,
    listState: LazyListState,
    searchQuery: String = "",
    selection: Set<String> = emptySet(),
    firstUnreadId: String? = null,
    highlightedMessageId: String? = null,
    topPadding: androidx.compose.ui.unit.Dp = 80.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 10.dp,
    onClick: (ChatMessage, Float) -> Unit = { _, _ -> },
    onLongClick: (ChatMessage) -> Unit = {},
    onSwipeReply: (ChatMessage) -> Unit = {},
    onToggleReaction: (String, String) -> Unit = { _, _ -> },
    onSelectMessage: (String) -> Unit = {},
    onToggleAlbumPhoto: (String, Int, Int) -> Unit = { _, _, _ -> },
    onEnterAlbumPhotoSelection: (String) -> Unit = {},
    onRetryMessage: (String) -> Unit = {},
    onHighlightMessage: (String) -> Unit = {},
    onOpenMediaViewer: (ChatMessage, Int) -> Unit = { _, _ -> }
) {
    if (messages.isEmpty()) {
        EmptyThreadHint()
        return
    }

    val renderItems = remember(messages, firstUnreadId) { buildRenderItems(messages, firstUnreadId) }
    val displayIndexById = remember(renderItems) {
        renderItems.withIndex().associate { (index, item) -> item.message.id to index }
    }
    val messageById = remember(messages) { messages.associateBy { it.id } }
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = topPadding, bottom = bottomPadding)
    ) {
        itemsIndexed(
            renderItems,
            key = { _, item -> item.message.id }
        ) { _, item ->
            Column(modifier = Modifier.padding(top = item.spacingAboveDp.dp)) {
                if (item.dateChipAbove) {
                    DateChip(epochMs = item.message.epochMs)
                }

                if (item.unreadDividerAbove) {
                    UnreadDivider()
                }

                val album = item.message.attachment as? MessageAttachment.MediaAlbum
                val selectedPhotoIndices = if (album != null && selection.isNotEmpty()) {
                    if (item.message.id in selection) {
                        (0 until album.items.size).toSet()
                    } else {
                        (0 until album.items.size).filter { idx ->
                            "${item.message.id}#photo_$idx" in selection
                        }.toSet()
                    }
                } else {
                    emptySet()
                }
                val isBubbleSelected = if (album != null) {
                    (item.message.id in selection) || (album.items.isNotEmpty() && selectedPhotoIndices.size == album.items.size)
                } else {
                    item.message.id in selection
                }

                MessageBubble(
                    item = item,
                    replyTo = item.message.replyToId?.let { originalId -> messageById[originalId] },
                    peerName = peerName,
                    searchQuery = searchQuery,
                    isSelectionMode = selection.isNotEmpty(),
                    isSelected = isBubbleSelected,
                    selectedPhotoIndices = selectedPhotoIndices,
                    isHighlighted = item.message.id == highlightedMessageId,
                    onQuoteClick = { originalId ->
                        onHighlightMessage(originalId)
                        val index = displayIndexById[originalId]
                        if (index != null) {
                            scope.launch {
                                runCatching {
                                    listState.animateScrollAndCenterItem(index = index)
                                }
                            }
                        }
                    },
                    onClick = { topY -> onClick(item.message, topY) },
                    onLongClick = { onLongClick(item.message) },
                    onSwipeReply = { onSwipeReply(item.message) },
                    onToggleReaction = { emoji -> onToggleReaction(item.message.id, emoji) },
                    onSelect = { onSelectMessage(item.message.id) },
                    onToggleAlbumPhoto = { photoIdx ->
                        if (album != null) {
                            onToggleAlbumPhoto(item.message.id, photoIdx, album.items.size)
                        }
                    },
                    onEnterAlbumPhotoSelection = { photoIdx ->
                        onEnterAlbumPhotoSelection("${item.message.id}#photo_$photoIdx")
                    },
                    onRetry = { onRetryMessage(item.message.id) },
                    onOpenMediaViewer = { photoIdx -> onOpenMediaViewer(item.message, photoIdx) }
                )
            }
        }
    }
}

private fun buildRenderItems(messages: List<ChatMessage>, firstUnreadId: String?): List<MessageRenderItem> =
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
            unreadDividerAbove = message.id == firstUnreadId,
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
            Icon(
                painter = painterResource(id = R.drawable.ic_stat_anongram),
                contentDescription = null,
                tint = CyberTeal.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(
                text = "Сообщений пока нет",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(
                text = "Напишите первое зашифрованное сообщение",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
