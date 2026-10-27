package com.anongram.app.features.chats.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.wallpapers.ui.WallpaperBackground

/**
 * Экран «Закреплённые сообщения» (Фото 5):
 * - Сверху плавающий островок возврата и заголовок с количеством
 * - Список закреплённых сообщений с поддержкой тапа (контекстное меню) и долгого тапа (режим выбора)
 * - Внизу кнопка «Открепить все сообщения»
 */
@Composable
internal fun PinnedMessagesScreen(
    chatId: String,
    pinnedMessages: List<ChatMessage>,
    peerName: String,
    selection: Set<String> = emptySet(),
    onBack: () -> Unit,
    onMessageClick: (ChatMessage) -> Unit,
    onMessageLongClick: (ChatMessage) -> Unit,
    onSelectMessage: (String) -> Unit,
    onSwipeReply: (ChatMessage) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onUnpinAll: () -> Unit
) {
    BackHandler(onBack = onBack)

    val count = pinnedMessages.size
    val titleText = when {
        count % 10 == 1 && count % 100 != 11 -> "$count закреплённое сообщение"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count закреплённых сообщения"
        else -> "$count закреплённых сообщений"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Обои чата как фон
        WallpaperBackground(
            chatId = chatId,
            modifier = Modifier.matchParentSize()
        )

        // Список закреплённых сообщений
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding()
        ) {
            // Верхняя плавающая панель в стиле островков
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Островок 1: Кнопка назад
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IslandBackground)
                        .border(1.dp, CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Островок 2: Заголовок
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(IslandBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(21.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = titleText,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Список сообщений
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (pinnedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет закреплённых сообщений",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        items(pinnedMessages, key = { it.id }) { message ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                DateChip(epochMs = message.epochMs)
                                Spacer(modifier = Modifier.height(4.dp))
                                MessageBubble(
                                    item = MessageRenderItem(
                                        message = message,
                                        isGroupedWithPrev = false,
                                        isGroupedWithNext = false,
                                        isLastInGroup = true,
                                        spacingAboveDp = 4,
                                        dateChipAbove = false,
                                        unreadDividerAbove = false
                                    ),
                                    replyTo = null,
                                    peerName = peerName,
                                    isSelectionMode = selection.isNotEmpty(),
                                    isSelected = message.id in selection,
                                    onClick = {
                                        if (selection.isNotEmpty()) onSelectMessage(message.id) else onMessageClick(message)
                                    },
                                    onLongClick = { onMessageLongClick(message) },
                                    onSwipeReply = { onSwipeReply(message) },
                                    onToggleReaction = { emoji -> onToggleReaction(message.id, emoji) },
                                    onSelect = { onSelectMessage(message.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Нижняя плавающая кнопка «Открепить все сообщения» (Фото 5)
            if (pinnedMessages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(IslandBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .clickable(onClick = onUnpinAll)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Открепить все сообщения",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
