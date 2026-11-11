package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CyberTeal
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.isSameDay
import com.anongram.app.features.chats.media.player.ChatTrack
import com.anongram.app.features.chats.ui.animateScrollAndCenterItem
import com.anongram.app.features.chats.ui.components.bubble.MessageBubble
import kotlinx.coroutines.launch

/** Позволяет медиа-превью в пузырях откладывать старт декодеров на время активного скролла без перекомпозиции списка */
internal val LocalIsListScrollingState = compositionLocalOf<State<Boolean>> { mutableStateOf(false) }

/** Размер текста сообщений, прокинутый один раз на весь список */
internal val LocalMessageTextSizeSp = compositionLocalOf { 16 }

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
    pinnedIds: Set<String> = emptySet(),
    isVideoCircleExpanded: Boolean = false,
    topPadding: androidx.compose.ui.unit.Dp = 80.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 10.dp,
    onClick: (ChatMessage, Float, Float) -> Unit = { _, _, _ -> },
    onLongClick: (ChatMessage) -> Unit = {},
    onSwipeReply: (ChatMessage) -> Unit = {},
    onReplyMessage: (ChatMessage, Int?) -> Unit = { _, _ -> },
    onToggleReaction: (String, String) -> Unit = { _, _ -> },
    onSelectMessage: (String) -> Unit = {},
    onToggleAlbumPhoto: (String, Int, Int) -> Unit = { _, _, _ -> },
    onToggleFileGroupItem: (String, Int, Int) -> Unit = { _, _, _ -> },
    onToggleMusicGroupItem: (String, Int, Int) -> Unit = { _, _, _ -> },
    onEnterAlbumPhotoSelection: (String) -> Unit = {},
    onPinMessage: (String) -> Unit = {},
    onEditMessage: (ChatMessage, Int?) -> Unit = { _, _ -> },
    onRetryMessage: (String) -> Unit = {},
    onHighlightMessage: (String) -> Unit = {},
    onOpenMediaViewer: (ChatMessage, Int) -> Unit = { _, _ -> },
    onOpenUncompressedPhoto: (ChatMessage, String) -> Unit = { _, _ -> },
    onOpenContactChat: (String, String) -> Unit = { _, _ -> },
    onForwardSubItem: (String) -> Unit = {},
    onDeleteSubItem: (String) -> Unit = {},
    onPlayVideoCircle: (ChatMessage) -> Unit = {},
    onEmptyAreaTap: () -> Unit = {},
    onPlayVoice: (ChatMessage) -> Unit = {},
    onPlayMusic: (ChatTrack) -> Unit = {},
    onOpenFullTextMessage: (ChatMessage) -> Unit = {}
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
    val scrollingState = remember(listState) { derivedStateOf { listState.isScrollInProgress } }
    val messageTextSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()

    CompositionLocalProvider(
        LocalIsListScrollingState provides scrollingState,
        LocalMessageTextSizeSp provides messageTextSizeSp
    ) {
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isVideoCircleExpanded) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onEmptyAreaTap() })
                    }
                } else Modifier
            ),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = topPadding, bottom = bottomPadding)
    ) {
        itemsIndexed(
            renderItems,
            key = { _, item -> item.message.id },
            contentType = { _, item ->
                item.message.attachment?.let { it::class.java } ?: String::class.java
            }
        ) { _, item ->
            Column(modifier = Modifier.padding(top = item.spacingAboveDp.dp)) {
                if (item.dateChipAbove) {
                    DateChip(epochMs = item.message.epochMs)
                }

                if (item.unreadDividerAbove) {
                    UnreadDivider()
                }

                val att = item.message.attachment
                val groupCountAndPrefix: Pair<Int, String>? = when (att) {
                    is MessageAttachment.MediaAlbum -> att.items.size to "photo"
                    is MessageAttachment.FileGroup -> att.items.size to "file"
                    is MessageAttachment.MusicGroup -> att.items.size to "music"
                    else -> null
                }

                val selectedSubIndices = if (groupCountAndPrefix != null && selection.isNotEmpty()) {
                    val (count, prefix) = groupCountAndPrefix
                    if (item.message.id in selection) {
                        (0 until count).toSet()
                    } else {
                        (0 until count).filter { idx ->
                            "${item.message.id}#${prefix}_$idx" in selection
                        }.toSet()
                    }
                } else {
                    emptySet()
                }

                val isBubbleSelected = if (groupCountAndPrefix != null) {
                    val (count, _) = groupCountAndPrefix
                    (item.message.id in selection) || (count > 0 && selectedSubIndices.size == count)
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
                    selectedPhotoIndices = selectedSubIndices,
                    isHighlighted = item.message.id == highlightedMessageId,
                    isPinned = item.message.id in pinnedIds,
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
                    onClick = { topY, bottomY -> onClick(item.message, topY, bottomY) },
                    onLongClick = { onLongClick(item.message) },
                    onSwipeReply = { onSwipeReply(item.message) },
                    onReplySubItem = { subIdx -> onReplyMessage(item.message, subIdx) },
                    onToggleReaction = { emoji -> onToggleReaction(item.message.id, emoji) },
                    onSelect = { onSelectMessage(item.message.id) },
                    onToggleAlbumPhoto = { subIdx ->
                        when (att) {
                            is MessageAttachment.MediaAlbum ->
                                onToggleAlbumPhoto(item.message.id, subIdx, att.items.size)
                            is MessageAttachment.FileGroup ->
                                onToggleFileGroupItem(item.message.id, subIdx, att.items.size)
                            is MessageAttachment.MusicGroup ->
                                onToggleMusicGroupItem(item.message.id, subIdx, att.items.size)
                            else -> Unit
                        }
                    },
                    onEnterAlbumPhotoSelection = { subIdx ->
                        val prefix = groupCountAndPrefix?.second ?: "photo"
                        onEnterAlbumPhotoSelection("${item.message.id}#${prefix}_$subIdx")
                    },
                    onPin = { onPinMessage(item.message.id) },
                    onEdit = { onEditMessage(item.message, null) },
                    onEditSubItem = { subIdx -> onEditMessage(item.message, subIdx) },
                    onRetry = { onRetryMessage(item.message.id) },
                    onOpenMediaViewer = { photoIdx -> onOpenMediaViewer(item.message, photoIdx) },
                    onOpenUncompressedPhoto = { uri -> onOpenUncompressedPhoto(item.message, uri) },
                    onOpenContactChat = onOpenContactChat,
                    onForwardSubItem = onForwardSubItem,
                    onDeleteSubItem = onDeleteSubItem,
                    onPlayVideoCircle = onPlayVideoCircle,
                    onPlayVoice = onPlayVoice,
                    onPlayMusic = onPlayMusic,
                    onOpenFullTextMessage = onOpenFullTextMessage
                )
            }
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
