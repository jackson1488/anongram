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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.draw.shadow
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.FlameOrange
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
    limitWarning: String? = null,
    onBack: () -> Unit,
    onMessageClick: (ChatMessage) -> Unit,
    onMessageLongClick: (ChatMessage) -> Unit,
    onSelectMessage: (String) -> Unit,
    onSwipeReply: (ChatMessage) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onUnpinAll: () -> Unit,
    onTriggerLimitDemo: () -> Unit = {},
    onDismissLimitWarning: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val count = pinnedMessages.size
    val titleText = when {
        count % 10 == 1 && count % 100 != 11 -> "$count закреплённое сообщение"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count закреплённых сообщения"
        else -> "$count закреплённых сообщений"
    }

    val scheme = LocalAnonGramScheme.current

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
            // Верхняя плавающая панель: кнопка назад слева и компактный центрированный островок с количеством
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Островок 1: Кнопка назад слева
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(IslandBackground)
                        .border(1.dp, CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Островок 2: Заголовок по центру — заканчивается ровно после слова «сообщение»
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(IslandBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = titleText,
                        color = TextPrimary,
                        fontSize = 14.sp,
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
                        itemsIndexed(pinnedMessages, key = { _, message -> message.id }) { index, message ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = scheme.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    DateChip(epochMs = message.epochMs)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                MessageBubble(
                                    item = MessageRenderItem(
                                        message = message,
                                        dateChipAbove = false,
                                        unreadDividerAbove = false,
                                        isFirstInGroup = true,
                                        isLastInGroup = true,
                                        spacingAboveDp = 4
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

        // Предупреждение о превышении лимита 99 сообщений (шаг 12)
        AnimatedVisibility(
            visible = limitWarning != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeNavigationBarPadding()
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
        ) {
            limitWarning?.let { warningText ->
                Box(
                    modifier = Modifier
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp), spotColor = CrimsonKillSwitch)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardDark)
                        .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onDismissLimitWarning)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = CrimsonKillSwitch,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = warningText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
