package com.anongram.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.data.WebChatSampleData
import com.anongram.web.model.MessageDirection
import com.anongram.web.model.MessageStatus
import com.anongram.web.model.WebChatMessage
import com.anongram.web.model.WebChatSession
import com.anongram.web.theme.LocalWebScheme
import com.anongram.web.theme.WebAnonGramScheme

@Composable
fun App() {
    val scheme = remember { WebAnonGramScheme() }

    CompositionLocalProvider(LocalWebScheme provides scheme) {
        val sessions = remember { WebChatSampleData.sampleSessions }
        var selectedSessionId by remember { mutableStateOf(sessions.firstOrNull()?.id ?: "1") }
        val activeSession = sessions.find { it.id == selectedSessionId } ?: sessions.first()

        var messagesMap by remember {
            mutableStateOf(
                sessions.associate { it.id to WebChatSampleData.getInitialMessages(it.id) }
            )
        }
        val currentMessages = messagesMap[selectedSessionId] ?: emptyList()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
        ) {
            // Левая колонка: Список чатов
            ChatSidebar(
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                onSelectSession = { selectedSessionId = it },
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .background(scheme.surface)
                    .border(width = 1.dp, color = scheme.divider)
            )

            // Правая колонка: Окно переписки и ввод
            ChatDialogueArea(
                session = activeSession,
                messages = currentMessages,
                onSendMessage = { text ->
                    if (text.isNotBlank()) {
                        val newMsg = WebChatMessage(
                            id = "msg_${System.currentTimeMillis()}",
                            direction = MessageDirection.OUTGOING,
                            text = text.trim(),
                            time = "Сейчас",
                            status = MessageStatus.SENT
                        )
                        val updated = (messagesMap[selectedSessionId] ?: emptyList()) + newMsg
                        messagesMap = messagesMap + (selectedSessionId to updated)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun ChatSidebar(
    sessions: List<WebChatSession>,
    selectedSessionId: String,
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalWebScheme.current

    Column(modifier = modifier) {
        // Заголовок сайдбара
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.15f))
                        .border(1.dp, scheme.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AnonGram Web",
                        color = scheme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compose Wasm (E2EE)",
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        HorizontalDivider(color = scheme.divider)

        // Список чатов
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(sessions, key = { it.id }) { session ->
                val isSelected = session.id == selectedSessionId
                val itemBg = if (isSelected) scheme.card.copy(alpha = 0.85f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(itemBg)
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватар
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) scheme.accent.copy(alpha = 0.25f)
                                else scheme.islandBg
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) scheme.accent else scheme.islandBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = session.title.take(2).uppercase(),
                            color = if (isSelected) scheme.accent else scheme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title,
                                color = scheme.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = session.timeLabel,
                                color = scheme.textMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.lastMessagePreview,
                                color = scheme.textSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (session.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(scheme.accent)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = session.unreadCount.toString(),
                                        color = scheme.onAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = scheme.divider.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun ChatDialogueArea(
    session: WebChatSession,
    messages: List<WebChatMessage>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalWebScheme.current
    var inputText by remember(session.id) { mutableStateOf("") }

    Column(modifier = modifier) {
        // Шапка активного чата
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.title.take(2).uppercase(),
                        color = scheme.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.title,
                        color = scheme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Отпечаток: ${session.fingerprint}",
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Статус защиты
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🔒 E2EE Active",
                    color = scheme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(color = scheme.divider)

        // Тело сообщений
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            reverseLayout = false
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg = msg)
                Spacer(modifier = Modifier.height(10.dp))
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        HorizontalDivider(color = scheme.divider)

        // Поле ввода сообщения
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Напишите защищённое сообщение…",
                        color = scheme.textMuted,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        color = scheme.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.accent,
                    contentColor = scheme.onAccent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Text(
                    text = "➤",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MessageBubble(msg: WebChatMessage) {
    val scheme = LocalWebScheme.current
    val isOutgoing = msg.direction == MessageDirection.OUTGOING

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isOutgoing) {
                        Brush.linearGradient(
                            listOf(scheme.outgoingBubbleStart, scheme.outgoingBubbleEnd)
                        )
                    } else {
                        SolidColor(scheme.incomingBubbleBg)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isOutgoing) scheme.outgoingBubbleBorder else scheme.incomingBubbleBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = msg.text,
                    color = if (isOutgoing) scheme.outgoingText else scheme.incomingText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = msg.time,
                        color = scheme.textMuted,
                        fontSize = 10.sp
                    )
                    if (isOutgoing) {
                        Text(
                            text = when (msg.status) {
                                MessageStatus.READ -> "✓✓"
                                MessageStatus.SENT -> "✓"
                                MessageStatus.SENDING -> "…"
                                MessageStatus.FAILED -> "!"
                            },
                            color = scheme.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
