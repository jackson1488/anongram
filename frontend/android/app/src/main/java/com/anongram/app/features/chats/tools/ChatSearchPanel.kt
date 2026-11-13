package com.anongram.app.features.chats.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.chats.tools.i18n.ToolsI18n

/**
 * Верхняя парящая полоска поиска Telegram-style:
 * ДВА РАЗДЕЛЬНЫХ ОСТРОВКА одинаковой высоты (50.dp):
 * 1) Круглый островок «Назад» (выход из поиска)
 * 2) Островок поиска с полем ввода и кнопкой очистки «✕»
 */
@Composable
internal fun ChatSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Островок 1: Кнопка Назад (выход из поиска, 50dp)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = strings.closeSearch,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Островок 2: Поле ввода поиска (50dp, одинаковая толщина с кнопкой)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(25.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = strings.searchPlaceholder,
                        color = scheme.textMuted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(scheme.accent)
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = strings.clearQuery,
                        tint = scheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Плавающие круглые кнопки перехода по найденным сообщениям (вверх / вниз):
 * Располагаются справа над нижним островком поиска.
 * Непрозрачные круглые островки с обводкой.
 */
@Composable
internal fun ChatSearchNavigationButtons(
    canNavigate: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка «Вверх»
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, CircleShape)
                .clickable(enabled = canNavigate, onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = strings.prevMatch,
                tint = if (canNavigate) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Кнопка «Вниз»
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, CircleShape)
                .clickable(enabled = canNavigate, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = strings.nextMatch,
                tint = if (canNavigate) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Нижний парящий островок поиска в чате Telegram-style (Фото 6 и Фото 8):
 * Счётчик результатов («1 из 3» или «273 результата») и кнопка «Списком» / «В чате».
 * Слева дату убрали по запросу пользователя («убрать слева дату»).
 */
@Composable
internal fun ChatSearchBottomBar(
    counter: String,
    isListMode: Boolean = false,
    enabled: Boolean = true,
    onListClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.islandBg)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Счётчик результатов («1 из 3» или «273 результата»)
        Text(
            text = if (counter.isBlank()) strings.zeroOfZero else counter,
            color = if (enabled) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // Кнопка переключения режима («Списком» или «В чате»)
        Text(
            text = if (isListMode) strings.viewInChat else strings.viewAsList,
            color = if (enabled) scheme.accent else scheme.textMuted.copy(alpha = 0.35f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(enabled = enabled, onClick = onListClick)
        )
    }
}

/**
 * Склонение количества результатов поиска.
 */
@Composable
internal fun formatResultsCount(count: Int): String =
    ToolsI18n.current.formatResultsCount(count)

/**
 * Экран списка результатов поиска:
 * аватар слева, имя отправителя, время, сниппет с подсветкой совпадений.
 */
@Composable
internal fun ChatSearchResultsList(
    query: String,
    matches: List<ChatMessage>,
    peerName: String,
    onSelectMatch: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 76.dp,
    bottomPadding: Dp = 80.dp
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.nothingFound,
                    color = scheme.textMuted,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
            ) {
                items(matches, key = { it.id }) { message ->
                    val isIncoming = message.direction == MessageDirection.INCOMING
                    val senderName = if (isIncoming) peerName else strings.youLabel

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMatch(message) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Аватар слева
                            if (isIncoming) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(scheme.accent.copy(alpha = 0.22f))
                                        .border(1.dp, scheme.accent.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = peerName.take(1).uppercase(),
                                        color = scheme.accent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(scheme.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.youLabel,
                                        color = scheme.onAccent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Имя, сниппет и время
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = senderName,
                                        color = scheme.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = message.timeLabel(),
                                        color = scheme.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // Сниппет с подсветкой
                                when (val att = message.attachment) {
                                    is MessageAttachment.Sticker -> {
                                        Text(
                                            text = strings.stickerLabel(att.symbol),
                                            color = scheme.accent,
                                            fontSize = 14.sp
                                        )
                                    }
                                    is MessageAttachment.File -> {
                                        Text(
                                            text = buildHighlightedSnippet(
                                                text = att.name,
                                                query = query,
                                                highlightColor = scheme.accent,
                                                highlightTextColor = scheme.textPrimary
                                            ),
                                            color = scheme.accent,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is MessageAttachment.Photo -> {
                                        val photoText = if (message.text.isNotBlank()) message.text else strings.photoLabel
                                        Text(
                                            text = buildHighlightedSnippet(
                                                text = photoText,
                                                query = query,
                                                highlightColor = scheme.accent,
                                                highlightTextColor = scheme.textPrimary
                                            ),
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is MessageAttachment.Voice -> {
                                        Text(
                                            text = strings.voiceMessageLabel,
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    is MessageAttachment.Music -> {
                                        Text(
                                            text = buildHighlightedSnippet(
                                                text = "🎵 ${att.artist} — ${att.title}",
                                                query = query,
                                                highlightColor = scheme.accent,
                                                highlightTextColor = scheme.textPrimary
                                            ),
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is MessageAttachment.Geo -> {
                                        Text(
                                            text = buildHighlightedSnippet(
                                                text = "📍 ${att.address}",
                                                query = query,
                                                highlightColor = scheme.accent,
                                                highlightTextColor = scheme.textPrimary
                                            ),
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is MessageAttachment.Contact -> {
                                        Text(
                                            text = buildHighlightedSnippet(
                                                text = "👤 ${att.name} (${att.phone})",
                                                query = query,
                                                highlightColor = scheme.accent,
                                                highlightTextColor = scheme.textPrimary
                                            ),
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    else -> {
                                        val annotated = buildHighlightedSnippet(
                                            text = message.text,
                                            query = query,
                                            highlightColor = scheme.accent,
                                            highlightTextColor = scheme.textPrimary
                                        )
                                        Text(
                                            text = annotated,
                                            color = scheme.textSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Тонкий разделитель под текстом
                        HorizontalDivider(
                            color = scheme.divider.copy(alpha = 0.55f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Подсветка совпадений поискового запроса в тексте сообщения.
 */
internal fun buildHighlightedSnippet(
    text: String,
    query: String,
    highlightColor: Color,
    highlightTextColor: Color = Color.White
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val builder = AnnotatedString.Builder()
    var currentIndex = 0
    val lowerText = text.lowercase()
    val lowerQuery = query.trim().lowercase()

    var matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    if (matchIndex < 0) return AnnotatedString(text)

    while (matchIndex >= 0) {
        if (matchIndex > currentIndex) {
            builder.append(text.substring(currentIndex, matchIndex))
        }
        val endMatch = matchIndex + lowerQuery.length
        builder.pushStyle(
            SpanStyle(
                background = highlightColor.copy(alpha = 0.35f),
                color = highlightTextColor,
                fontWeight = FontWeight.Bold
            )
        )
        builder.append(text.substring(matchIndex, endMatch))
        builder.pop()
        currentIndex = endMatch
        matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    }
    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }
    return builder.toAnnotatedString()
}
