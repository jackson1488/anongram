package com.anongram.app.features.chats.input.bar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

enum class RichSpanStyle {
    BOLD,
    ITALIC,
    MONO,
    STRIKE,
    UNDERLINE,
    SPOILER,
    QUOTE,
    LINK
}

data class RichInputSpan(
    val start: Int,
    val end: Int,
    val style: RichSpanStyle,
    val url: String = ""
)

fun adjustSpansForTextEdit(
    spans: List<RichInputSpan>,
    oldText: String,
    newText: String
): List<RichInputSpan> {
    if (spans.isEmpty() || oldText == newText) return spans

    val minLen = minOf(oldText.length, newText.length)
    var prefixLen = 0
    while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
        prefixLen++
    }

    var suffixLen = 0
    while (
        suffixLen < (minLen - prefixLen) &&
        oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
    ) {
        suffixLen++
    }

    val oldChangedEnd = oldText.length - suffixLen
    val delta = newText.length - oldText.length

    return spans.mapNotNull { span ->
        val s = span.start
        val e = span.end
        when {
            e <= prefixLen -> span
            s >= oldChangedEnd -> span.copy(start = s + delta, end = e + delta)
            else -> {
                val newS = if (s <= prefixLen) s else prefixLen
                val newE = if (e >= oldChangedEnd) e + delta else prefixLen
                if (newE > newS) span.copy(start = newS, end = newE) else null
            }
        }
    }.filter { it.start < it.end && it.start >= 0 && it.end <= newText.length }
}

fun createSpanVisualTransformation(
    spans: List<RichInputSpan>,
    textPrimary: Color,
    accentColor: Color,
    revealedSpoilerIndices: Set<Int> = emptySet()
): VisualTransformation {
    return VisualTransformation { original ->
        val text = original.text
        if (text.isEmpty() || spans.isEmpty()) {
            return@VisualTransformation TransformedText(original, OffsetMapping.Identity)
        }
        val annotated = buildAnnotatedString {
            append(text)
            var spoilerIdx = 0
            spans.forEach { span ->
                val s = span.start.coerceIn(0, text.length)
                val e = span.end.coerceIn(0, text.length)
                if (s < e) {
                    val style = when (span.style) {
                        RichSpanStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        RichSpanStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                        RichSpanStyle.MONO -> SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = accentColor.copy(alpha = 0.16f)
                        )
                        RichSpanStyle.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                        RichSpanStyle.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                        RichSpanStyle.SPOILER -> {
                            val isRev = spoilerIdx in revealedSpoilerIndices
                            spoilerIdx++
                            if (isRev) {
                                SpanStyle(background = accentColor.copy(alpha = 0.12f))
                            } else {
                                SpanStyle(color = Color.Transparent)
                            }
                        }
                        RichSpanStyle.QUOTE -> SpanStyle(
                            background = accentColor.copy(alpha = 0.14f),
                            fontStyle = FontStyle.Italic
                        )
                        RichSpanStyle.LINK -> SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                    addStyle(style, s, e)
                }
            }
        }
        TransformedText(annotated, OffsetMapping.Identity)
    }
}

fun serializeRichInput(plainText: String, spans: List<RichInputSpan>): String {
    if (plainText.isEmpty() || spans.isEmpty()) return plainText

    val quoteRanges = spans.filter { it.style == RichSpanStyle.QUOTE }
    val inlineSpans = spans.filter { it.style != RichSpanStyle.QUOTE }

    val openMap = HashMap<Int, MutableList<RichInputSpan>>()
    val closeMap = HashMap<Int, MutableList<RichInputSpan>>()
    inlineSpans.forEach { sp ->
        val s = sp.start.coerceIn(0, plainText.length)
        val e = sp.end.coerceIn(0, plainText.length)
        if (s < e) {
            openMap.getOrPut(s) { mutableListOf() }.add(sp)
            closeMap.getOrPut(e) { mutableListOf() }.add(sp)
        }
    }

    val sb = StringBuilder(plainText.length + spans.size * 6)
    for (i in 0..plainText.length) {
        closeMap[i]?.asReversed()?.forEach { sp ->
            sb.append(
                when (sp.style) {
                    RichSpanStyle.BOLD -> "**"
                    RichSpanStyle.ITALIC -> "*"
                    RichSpanStyle.MONO -> "`"
                    RichSpanStyle.STRIKE -> "~~"
                    RichSpanStyle.UNDERLINE -> "</u>"
                    RichSpanStyle.SPOILER -> "||"
                    RichSpanStyle.LINK -> "](${sp.url})"
                    RichSpanStyle.QUOTE -> ""
                }
            )
        }
        if (i < plainText.length) {
            openMap[i]?.forEach { sp ->
                sb.append(
                    when (sp.style) {
                        RichSpanStyle.BOLD -> "**"
                        RichSpanStyle.ITALIC -> "*"
                        RichSpanStyle.MONO -> "`"
                        RichSpanStyle.STRIKE -> "~~"
                        RichSpanStyle.UNDERLINE -> "<u>"
                        RichSpanStyle.SPOILER -> "||"
                        RichSpanStyle.LINK -> "["
                        RichSpanStyle.QUOTE -> ""
                    }
                )
            }
            sb.append(plainText[i])
        }
    }

    if (quoteRanges.isEmpty()) return sb.toString()

    // Применяем `> ` построчно для строк, задетых QUOTE-спанами
    val quotedLines = mutableSetOf<Int>()
    var lineIdx = 0
    var pos = 0
    plainText.split('\n').forEach { line ->
        val lineEnd = pos + line.length
        if (quoteRanges.any { q -> q.start < lineEnd && q.end > pos }) {
            quotedLines.add(lineIdx)
        }
        pos = lineEnd + 1
        lineIdx++
    }
    return sb.toString()
        .split('\n')
        .mapIndexed { idx, line ->
            if (idx in quotedLines && !line.startsWith("> ")) "> $line" else line
        }
        .joinToString("\n")
}

fun parseRichInput(rawText: String): Pair<String, List<RichInputSpan>> {
    if (rawText.isEmpty()) return "" to emptyList()

    val spans = mutableListOf<RichInputSpan>()
    val plainBuilder = StringBuilder()

    // Сначала обрабатываем строки с `> `
    val lines = rawText.split('\n')
    val quoteLineIndices = mutableSetOf<Int>()
    val strippedLines = lines.mapIndexed { idx, line ->
        if (line.startsWith("> ")) {
            quoteLineIndices.add(idx)
            line.removePrefix("> ")
        } else {
            line
        }
    }
    val tokenRegex = Regex(
        "(\\*\\*([^*]+)\\*\\*)|" +
        "(\\*([^*]+)\\*)|" +
        "(~~([^~]+)~~)|" +
        "(<u>([^<]+)</u>)|" +
        "(`([^`]+)`)|" +
        "(\\|\\|([^|]+)\\|\\|)|" +
        "(\\[([^\\]]+)\\]\\(([^)]+)\\))"
    )

    strippedLines.forEachIndexed { idx, line ->
        val lineStart = plainBuilder.length
        var cursor = 0
        for (m in tokenRegex.findAll(line)) {
            if (m.range.first > cursor) {
                plainBuilder.append(line, cursor, m.range.first)
            }
            val spanStart = plainBuilder.length
            when {
                m.groupValues[1].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[2])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.BOLD))
                }
                m.groupValues[3].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[4])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.ITALIC))
                }
                m.groupValues[5].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[6])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.STRIKE))
                }
                m.groupValues[7].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[8])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.UNDERLINE))
                }
                m.groupValues[9].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[10])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.MONO))
                }
                m.groupValues[11].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[12])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.SPOILER))
                }
                m.groupValues[13].isNotEmpty() -> {
                    plainBuilder.append(m.groupValues[14])
                    spans.add(RichInputSpan(spanStart, plainBuilder.length, RichSpanStyle.LINK, url = m.groupValues[15]))
                }
            }
            cursor = m.range.last + 1
        }
        if (cursor < line.length) {
            plainBuilder.append(line, cursor, line.length)
        }
        if (idx in quoteLineIndices && plainBuilder.length > lineStart) {
            spans.add(RichInputSpan(lineStart, plainBuilder.length, RichSpanStyle.QUOTE))
        }
        if (idx < strippedLines.lastIndex) {
            plainBuilder.append('\n')
        }
    }

    return plainBuilder.toString() to spans
}

fun stripRichFormattingForClipboard(rawText: String): String {
    if (rawText.isEmpty()) return ""
    val withoutFont = rawText.replace(Regex("\\[font=(sans|serif|mono|cursive)\\]|\\[/font\\]"), "")
    return parseRichInput(withoutFont).first
}
