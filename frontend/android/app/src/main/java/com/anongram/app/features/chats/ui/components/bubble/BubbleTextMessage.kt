package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.chats.ui.input.editor.CodeLanguages
import kotlin.math.ceil

/**
 * Модуль отображения текста сообщения:
 * - Выделяет блоки кода по макету media_1794245820653.png;
 * - Поддерживает интерактивные анимированные спойлеры (||текст||);
 * - Лимит 4000 символов с кнопкой «Читать дальше»;
 * - Адаптивная раскладка времени без перекрытия текста.
 */
@Composable
fun BubbleTextMessage(
    message: ChatMessage,
    textSizeSp: Int,
    textScale: Float,
    searchQuery: String,
    onOpenFullText: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val rawText = message.text
    val isIncoming = message.direction == MessageDirection.INCOMING

    // Проверяем, является ли сообщение блоком кода
    val trimmed = rawText.trim()
    val isCodeBlock = trimmed.startsWith("```") ||
        (trimmed.lines().size > 1 && CodeLanguages.any { trimmed.startsWith(it, ignoreCase = true) })

    if (isCodeBlock) {
        val (lang, codeContent) = parseCodeBlock(trimmed)
        BubbleCodeBlock(
            message = message,
            language = lang,
            codeText = codeContent,
            textScale = textScale,
            onOpenFullText = onOpenFullText,
            onRetry = onRetry,
            modifier = modifier
        )
        return
    }

    // Проверяем, есть ли спойлер ||текст||
    if (rawText.contains("||") && rawText.indexOf("||") != rawText.lastIndexOf("||")) {
        val parts = rawText.split("||")
        Column(modifier = modifier) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // Внутри спойлера
                    BubbleSpoilerText(
                        text = part,
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = textSizeSp.sp,
                            lineHeight = (textSizeSp * 1.26f).sp
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else if (part.isNotEmpty()) {
                    Text(
                        text = part,
                        color = TextPrimary,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.26f).sp
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 4.dp))

            BubbleTimestampRow(
                timeLabel = message.timeLabel(),
                isEdited = message.isEdited,
                isIncoming = isIncoming,
                status = message.status,
                textScale = textScale,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.End)
            )
        }
        return
    }

    // Лимит 4000 символов для обычного текста
    val isOverLimit = rawText.length > 4000
    val textToDisplay = if (isOverLimit) rawText.take(4000) + "…" else rawText

    val annotatedText = if (searchQuery.isNotBlank()) {
        buildHighlightedText(
            text = textToDisplay,
            query = searchQuery,
            highlightColor = scheme.accent
        )
    } else {
        AnnotatedString(textToDisplay)
    }

    Column(modifier = modifier) {
        AdaptiveMessageTextWithTimeLayout(
            text = annotatedText,
            textSizeSp = textSizeSp,
            textScale = textScale,
            timeLabel = message.timeLabel(),
            isEdited = message.isEdited,
            isIncoming = isIncoming,
            status = message.status,
            onRetry = onRetry
        )

        if (isOverLimit) {
            Text(
                text = "Читать дальше (${rawText.length} символов) →",
                color = scheme.accent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .clickable(onClick = onOpenFullText)
                    .padding(top = 4.dp, bottom = 2.dp)
            )
        }
    }
}

private fun parseCodeBlock(raw: String): Pair<String, String> {
    if (raw.startsWith("```")) {
        val withoutTicks = raw.removePrefix("```")
        val firstLine = withoutTicks.substringBefore('\n').trim()
        val lang = if (firstLine.isNotBlank() && firstLine.length <= 15) firstLine else "CODE"
        val body = withoutTicks.substringAfter('\n').removeSuffix("```").trim()
        return lang to body
    }
    val firstLine = raw.substringBefore('\n').trim()
    val matched = CodeLanguages.firstOrNull { firstLine.equals(it, ignoreCase = true) }
    if (matched != null) {
        return matched to raw.substringAfter('\n').trim()
    }
    return "CODE" to raw
}

/**
 * Адаптивная верстка текста и блока времени:
 * - Для коротких сообщений («привет») время идёт справа на той же строке;
 * - Для многострочных сообщений со свободным местом справа — время в конце последней строки;
 * - Для длинных строк — время переносится на нижнюю строчку без наложения.
 */
@Composable
fun AdaptiveMessageTextWithTimeLayout(
    text: AnnotatedString,
    textSizeSp: Int,
    textScale: Float,
    timeLabel: String,
    isEdited: Boolean,
    isIncoming: Boolean,
    status: MessageStatus,
    onRetry: () -> Unit,
    timeColor: Color = TextMuted,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textLineHeight = (textSizeSp * 1.26f).coerceAtLeast(textSizeSp + 2f).sp
    val timeFontSize = (10.5f * textScale).coerceIn(5.5f, 14.5f).sp
    val editedFontSize = (9f * textScale).coerceIn(5f, 12.5f).sp
    val tickSize = (13f * textScale).coerceIn(7f, 17.5f).dp
    val horizontalGapDp = (8f * textScale).coerceIn(4f, 11f).dp
    val verticalExtraDp = (2f * textScale).coerceIn(1f, 4f).dp

    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = textSizeSp.sp,
                lineHeight = textLineHeight,
                onTextLayout = { textLayoutResult = it }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEdited) {
                    Text(
                        text = "изменено",
                        color = timeColor,
                        fontSize = editedFontSize,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.width((4f * textScale).coerceIn(2f, 5f).dp))
                }
                Text(
                    text = timeLabel,
                    color = timeColor,
                    fontSize = timeFontSize,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width((3f * textScale).coerceIn(1.5f, 4.5f).dp))
                    DeliveryStatus(
                        status = status,
                        onRetry = onRetry,
                        iconSize = tickSize
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val timePlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )
        val textPlaceable = measurables[0].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        val gapPx = horizontalGapDp.roundToPx()
        val vStepPx = verticalExtraDp.roundToPx()
        val timeW = timePlaceable.width
        val timeH = timePlaceable.height
        val textW = textPlaceable.width
        val textH = textPlaceable.height

        val layoutRes = textLayoutResult
        val lineCount = layoutRes?.lineCount ?: 1
        val lastLineRightPx = if (layoutRes != null && lineCount > 0) {
            ceil(layoutRes.getLineRight(lineCount - 1)).toInt()
        } else {
            textW
        }

        if (lineCount <= 1 && textW + gapPx + timeW <= constraints.maxWidth) {
            val totalW = (textW + gapPx + timeW).coerceAtLeast(constraints.minWidth)
            val totalH = maxOf(textH, timeH)
            layout(totalW, totalH) {
                textPlaceable.placeRelative(0, (totalH - textH) / 2)
                timePlaceable.placeRelative(totalW - timeW, totalH - timeH)
            }
        } else if (lastLineRightPx + gapPx + timeW <= constraints.maxWidth) {
            val totalW = maxOf(textW, lastLineRightPx + gapPx + timeW).coerceAtLeast(constraints.minWidth)
            val totalH = maxOf(textH, timeH)
            layout(totalW, totalH) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(totalW - timeW, totalH - timeH)
            }
        } else {
            val totalW = maxOf(textW, timeW).coerceAtLeast(constraints.minWidth)
            val totalH = textH + vStepPx + timeH
            layout(totalW, totalH) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(totalW - timeW, textH + vStepPx)
            }
        }
    }
}
