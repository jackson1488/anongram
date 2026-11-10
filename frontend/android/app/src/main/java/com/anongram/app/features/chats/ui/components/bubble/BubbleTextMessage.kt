package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
 * Модуль отображения форматированного текста сообщения:
 * - Блоки кода по макету media_1794245820653.png;
 * - Математические формулы LaTeX (BubbleLatexFormula);
 * - Сворачиваемые блоки по макету media_1794256738911.png (BubbleCollapsibleBlock);
 * - Выноски и цитаты с авторами (BubbleCalloutBlock);
 * - Интерактивные чек-боксы (☐ / ☑) по макету media_1794255690560.png;
 * - Заголовки H1..H6 с пропорциональными размерами;
 * - Двухсторонние анимированные спойлеры (||текст||);
 * - Жирный, курсив, подчёркнутый, зачёркнутый и шрифты.
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
    val trimmed = rawText.trim()

    // 1. Проверяем, является ли сообщение блоком кода
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

    // 2. Проверяем формулу LaTeX (\(...\) или $$...$$)
    if ((trimmed.startsWith("\\(") && trimmed.endsWith("\\)")) ||
        (trimmed.startsWith("$$") && trimmed.endsWith("$$"))
    ) {
        Column(modifier = modifier) {
            BubbleLatexFormula(formula = trimmed)
            Spacer(modifier = Modifier.height(4.dp))
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

    // 3. Проверяем сворачиваемый блок (+++ Заголовок\nТело\n+++) или (^ Заголовок\nТело)
    val isCollapsiblePlus = trimmed.startsWith("+++") && trimmed.endsWith("+++") && trimmed.lines().size >= 2
    val isCollapsibleChevron = trimmed.startsWith("^") && trimmed.lines().size >= 2
    if (isCollapsiblePlus || isCollapsibleChevron) {
        val (title, body) = if (isCollapsiblePlus) {
            val lines = trimmed.removePrefix("+++").removeSuffix("+++").trim().lines()
            Pair(lines.firstOrNull() ?: "", lines.drop(1).joinToString("\n").trim())
        } else {
            val lines = trimmed.lines()
            val titleLine = lines.first().removePrefix("^").trim()
            val bodyLines = lines.drop(1).joinToString("\n").trim()
            Pair(titleLine, bodyLines)
        }
        Column(modifier = modifier) {
            BubbleCollapsibleBlock(title = title, content = body)
            Spacer(modifier = Modifier.height(4.dp))
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

    // 4. Проверяем выноску (>>> Текст\n— Автор) или цитату (> Текст\n— Автор)
    if (trimmed.startsWith(">>>") || (trimmed.startsWith(">") && !trimmed.startsWith(">>"))) {
        val isCallout = trimmed.startsWith(">>>")
        val clean = if (isCallout) trimmed.removePrefix(">>>").trim() else trimmed.removePrefix(">").trim()
        val lines = clean.lines()
        val authorLine = lines.lastOrNull { it.trim().startsWith("—") || it.trim().startsWith("- ") }
        val bodyText = if (authorLine != null) {
            lines.filter { it != authorLine }.joinToString("\n").trim()
        } else {
            clean
        }

        Column(modifier = modifier) {
            BubbleCalloutBlock(
                text = bodyText,
                author = authorLine?.trim()?.removePrefix("-")?.trim(),
                isQuote = !isCallout
            )
            Spacer(modifier = Modifier.height(4.dp))
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

    // 5. Проверяем наличие чек-листа (☐ или ☑)
    if (trimmed.lines().any { it.trimStart().startsWith("☐") || it.trimStart().startsWith("☑") }) {
        Column(modifier = modifier) {
            trimmed.lines().forEach { line ->
                val lineTrimmed = line.trimStart()
                if (lineTrimmed.startsWith("☐") || lineTrimmed.startsWith("☑")) {
                    InteractiveChecklistItem(
                        initialChecked = lineTrimmed.startsWith("☑"),
                        text = lineTrimmed.substring(1).trim(),
                        textSizeSp = textSizeSp
                    )
                } else if (line.isNotBlank()) {
                    Text(
                        text = line,
                        color = TextPrimary,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.26f).sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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

    // 6. Проверяем спойлер ||текст||
    if (rawText.contains("||") && rawText.indexOf("||") != rawText.lastIndexOf("||")) {
        val lines = rawText.split("\n")
        Column(modifier = modifier) {
            lines.forEachIndexed { lineIdx, line ->
                if (line.contains("||") && line.indexOf("||") != line.lastIndexOf("||")) {
                    InlineSpoilerLine(
                        line = line,
                        textSizeSp = textSizeSp,
                        accent = scheme.accent
                    )
                } else if (line.isNotEmpty()) {
                    Text(
                        text = buildRichAnnotatedText(line, scheme.accent),
                        color = TextPrimary,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.26f).sp
                    )
                }
                if (lineIdx < lines.size - 1 && line.isEmpty()) {
                    Spacer(modifier = Modifier.height((textSizeSp * 0.5f).dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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

    // 7. Проверяем заголовки H1..H6 (# ... ######)
    val firstLine = rawText.lines().firstOrNull() ?: ""
    val headingMatch = Regex("^(#{1,6})\\s+(.+)").find(firstLine)
    if (headingMatch != null && rawText.lines().size <= 5) {
        val hashes = headingMatch.groupValues[1]
        val headingTitle = headingMatch.groupValues[2]
        val remaining = rawText.lines().drop(1).joinToString("\n").trim()
        val level = hashes.length
        val (headingSizeSp, headingWeight) = when (level) {
            1 -> 24.sp to FontWeight.Bold
            2 -> 21.sp to FontWeight.Bold
            3 -> 18.sp to FontWeight.Bold
            4 -> 16.sp to FontWeight.SemiBold
            5 -> 14.sp to FontWeight.SemiBold
            else -> 12.sp to FontWeight.SemiBold
        }

        Column(modifier = modifier) {
            Text(
                text = headingTitle,
                color = Color.White,
                fontSize = headingSizeSp,
                fontWeight = headingWeight,
                lineHeight = (headingSizeSp.value * 1.25f).sp
            )
            if (remaining.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildRichAnnotatedText(remaining, scheme.accent),
                    color = TextPrimary,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * 1.26f).sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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

    // 8. Стандартный текст с богатым форматированием
    val isOverLimit = rawText.length > 4000
    val textToDisplay = if (isOverLimit) rawText.take(4000) + "…" else rawText

    val annotatedText = if (searchQuery.isNotBlank()) {
        buildHighlightedText(
            text = textToDisplay,
            query = searchQuery,
            highlightColor = scheme.accent
        )
    } else {
        buildRichAnnotatedText(textToDisplay, scheme.accent)
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

/**
 * Интерактивный элемент чек-листа с крупным фиолетовым чекбоксом по макету media_1794255690560.png.
 */
@Composable
private fun InteractiveChecklistItem(
    initialChecked: Boolean,
    text: String,
    textSizeSp: Int,
    modifier: Modifier = Modifier
) {
    var isChecked by remember { mutableStateOf(initialChecked) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { isChecked = !isChecked }
            .padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isChecked) Color(0xFF8B5CF6) else Color(0xFF26262E))
                .border(
                    width = if (isChecked) 0.dp else 1.5.dp,
                    color = if (isChecked) Color.Transparent else Color(0x66FFFFFF),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Выполнено",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (isChecked) Color(0xFF94A3B8) else TextPrimary,
            fontSize = textSizeSp.sp,
            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
            lineHeight = (textSizeSp * 1.26f).sp
        )
    }
}

/**
 * Инлайн-спойлер внутри строки: слова и скрытые частицы располагаются в FlowRow
 * без разрыва предложения на отдельные строки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineSpoilerLine(
    line: String,
    textSizeSp: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val parts = line.split("||")
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        for ((index, part) in parts.withIndex()) {
            if (index % 2 == 1) {
                // Скрытый фрагмент
                BubbleSpoilerText(
                    text = part,
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.26f).sp
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            } else if (part.isNotEmpty()) {
                Text(
                    text = buildRichAnnotatedText(part, accent),
                    color = TextPrimary,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * 1.26f).sp
                )
            }
        }
    }
}

/**
 * Разбирает разметку Markdown / HTML / Шрифтов в AnnotatedString:
 * - **жирный**
 * - *курсив*
 * - ~~зачёркнутый~~
 * - <u>подчёркнутый</u>
 * - `код`
 * - [font=serif]...[/font], [font=mono]...[/font], [font=cursive]...[/font]
 */
fun buildRichAnnotatedText(source: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = source.length

        // Регулярные выражения для инлайн-стилей
        val tokenRegex = Regex(
            "(\\*\\*([^*]+)\\*\\*)|" +                     // 1: **bold**
            "(\\*([^*]+)\\*)|" +                           // 3: *italic*
            "(~~([^~]+)~~)|" +                             // 5: ~~strike~~
            "(<u>([^<]+)<\\/u>)|" +                        // 7: <u>underline</u>
            "(`([^`]+)`)|" +                               // 9: `code`
            "(\\[font=(sans|serif|mono|cursive)\\]([^\\[]+)\\[\\/font\\])" // 11: font
        )

        val matches = tokenRegex.findAll(source).toList()
        if (matches.isEmpty()) {
            append(source)
            return@buildAnnotatedString
        }

        var lastEnd = 0
        for (m in matches) {
            if (m.range.first > lastEnd) {
                append(source.substring(lastEnd, m.range.first))
            }

            when {
                // **bold**
                m.groupValues[1].isNotEmpty() -> {
                    val inner = m.groupValues[2]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                // *italic*
                m.groupValues[3].isNotEmpty() -> {
                    val inner = m.groupValues[4]
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                // ~~strike~~
                m.groupValues[5].isNotEmpty() -> {
                    val inner = m.groupValues[6]
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                }
                // <u>underline</u>
                m.groupValues[7].isNotEmpty() -> {
                    val inner = m.groupValues[8]
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(inner)
                    pop()
                }
                // `code`
                m.groupValues[9].isNotEmpty() -> {
                    val inner = m.groupValues[10]
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33A1A1AA),
                            color = Color(0xFFF1F5F9)
                        )
                    )
                    append(" $inner ")
                    pop()
                }
                // [font=...]...[/font]
                m.groupValues[11].isNotEmpty() -> {
                    val fontType = m.groupValues[12]
                    val inner = m.groupValues[13]
                    val family = when (fontType) {
                        "serif" -> FontFamily.Serif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.SansSerif
                    }
                    pushStyle(SpanStyle(fontFamily = family))
                    append(inner)
                    pop()
                }
            }
            lastEnd = m.range.last + 1
        }

        if (lastEnd < len) {
            append(source.substring(lastEnd, len))
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
