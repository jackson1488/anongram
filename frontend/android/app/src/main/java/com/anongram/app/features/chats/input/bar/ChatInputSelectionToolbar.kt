package com.anongram.app.features.chats.input.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import kotlin.math.roundToInt

@Stable
class ChatInputSelectionController(
    private val getValue: () -> TextFieldValue,
    private val onValueChange: (TextFieldValue) -> Unit
) {
    var isShown by mutableStateOf(false)
        private set
    var anchorRect by mutableStateOf(Rect.Zero)
        private set
    var isFormatMenuOpen by mutableStateOf(false)
        internal set
    var isLinkDialogOpen by mutableStateOf(false)
        internal set
    var linkUrlInput by mutableStateOf("https://")
        internal set
    var savedSelectionForLink by mutableStateOf(TextRange.Zero)
        internal set

    internal var onCopyAction by mutableStateOf<(() -> Unit)?>(null)
    internal var onPasteAction by mutableStateOf<(() -> Unit)?>(null)
    internal var onCutAction by mutableStateOf<(() -> Unit)?>(null)
    internal var onSelectAllAction by mutableStateOf<(() -> Unit)?>(null)

    val textToolbar: TextToolbar = object : TextToolbar {
        override val status: TextToolbarStatus
            get() = if (isShown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {
            anchorRect = rect
            onCopyAction = onCopyRequested
            onPasteAction = onPasteRequested
            onCutAction = onCutRequested
            onSelectAllAction = onSelectAllRequested
            isFormatMenuOpen = false
            isShown = true
        }

        override fun hide() {
            isShown = false
            isFormatMenuOpen = false
        }
    }

    fun currentValue(): TextFieldValue = getValue()

    fun hide() {
        textToolbar.hide()
    }

    fun applyWrap(prefix: String, suffix: String = prefix) {
        val current = getValue()
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start >= end) return
        val selected = current.text.substring(start, end)
        val wrapped = "$prefix$selected$suffix"
        val updatedText = current.text.substring(0, start) + wrapped + current.text.substring(end)
        onValueChange(
            current.copy(
                text = updatedText,
                selection = TextRange(start + prefix.length, start + prefix.length + selected.length)
            )
        )
        hide()
    }

    fun applyQuote() {
        val current = getValue()
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start >= end) return
        val selected = current.text.substring(start, end)
        val quoted = selected.lines().joinToString("\n") { line ->
            if (line.startsWith("> ")) line else "> $line"
        }
        val updatedText = current.text.substring(0, start) + quoted + current.text.substring(end)
        onValueChange(
            current.copy(
                text = updatedText,
                selection = TextRange(start, start + quoted.length)
            )
        )
        hide()
    }

    fun clearFormatting() {
        val current = getValue()
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start >= end) return
        val selected = current.text.substring(start, end)
        val cleaned = stripFormattingMarkers(selected)
        val updatedText = current.text.substring(0, start) + cleaned + current.text.substring(end)
        onValueChange(
            current.copy(
                text = updatedText,
                selection = TextRange(start, start + cleaned.length)
            )
        )
        hide()
    }

    fun confirmLink() {
        val url = linkUrlInput.trim()
        if (url.isNotBlank()) {
            val current = getValue()
            val s = minOf(savedSelectionForLink.start, savedSelectionForLink.end).coerceIn(0, current.text.length)
            val e = maxOf(savedSelectionForLink.start, savedSelectionForLink.end).coerceIn(0, current.text.length)
            if (s < e) {
                val selected = current.text.substring(s, e)
                val replacement = "[$selected]($url)"
                val updated = current.text.substring(0, s) + replacement + current.text.substring(e)
                onValueChange(
                    current.copy(
                        text = updated,
                        selection = TextRange(s + replacement.length)
                    )
                )
            }
        }
        isLinkDialogOpen = false
    }
}

@Composable
fun rememberChatInputSelectionController(
    getValue: () -> TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
): ChatInputSelectionController {
    return remember {
        ChatInputSelectionController(getValue = getValue, onValueChange = onValueChange)
    }
}

private class AnchorRectPopupPositionProvider(
    private val rect: Rect,
    private val gapPx: Int,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val centerX = ((rect.left + rect.right) / 2f).roundToInt()
        val rawX = centerX - popupContentSize.width / 2
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = rawX.coerceIn(marginPx, maxX)

        val aboveY = rect.top.roundToInt() - popupContentSize.height - gapPx
        val belowY = rect.bottom.roundToInt() + gapPx
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val y = if (aboveY >= marginPx) aboveY else belowY.coerceIn(marginPx, maxY)
        return IntOffset(x, y)
    }
}

@Composable
fun ChatInputSelectionToolbarOverlay(
    controller: ChatInputSelectionController
) {
    val density = LocalDensity.current
    if (controller.isShown) {
        val hasSelection = !controller.currentValue().selection.collapsed
        val rect = controller.anchorRect
        val positionProvider = remember(rect, density) {
            with(density) {
                AnchorRectPopupPositionProvider(
                    rect = rect,
                    gapPx = 10.dp.roundToPx(),
                    marginPx = 10.dp.roundToPx()
                )
            }
        }
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { controller.hide() },
            properties = PopupProperties(focusable = false, clippingEnabled = true)
        ) {
            if (!controller.isFormatMenuOpen) {
                SelectionActionBar(controller = controller, hasSelection = hasSelection)
            } else {
                SelectionFormatMenu(controller = controller, hasSelection = hasSelection)
            }
        }
    }

    if (controller.isLinkDialogOpen) {
        SelectionLinkDialog(controller = controller)
    }
}

@Composable
private fun SelectionActionBar(
    controller: ChatInputSelectionController,
    hasSelection: Boolean
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    Row(
        modifier = Modifier
            .height(46.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.card)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Порядок как в Telegram: Копировать -> Вставить -> Вырезать -> ⋮
        controller.onCopyAction?.let { copy ->
            ToolbarTextButton(label = strings.selectionCopy) {
                copy()
                controller.hide()
            }
        }
        controller.onPasteAction?.let { paste ->
            ToolbarTextButton(label = strings.selectionPaste) {
                paste()
                controller.hide()
            }
        }
        controller.onCutAction?.let { cut ->
            ToolbarTextButton(label = strings.selectionCut) {
                cut()
                controller.hide()
            }
        }
        if (hasSelection) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { controller.isFormatMenuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = strings.selectionMore,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionFormatMenu(
    controller: ChatInputSelectionController,
    hasSelection: Boolean
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 248.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            SelectionFormatItemsTop(controller = controller, hasSelection = hasSelection)
            SelectionFormatItemsStyles(controller = controller, hasSelection = hasSelection)
        }
        HorizontalDivider(color = scheme.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { controller.isFormatMenuOpen = false }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.selectionBack,
                tint = scheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = strings.selectionBack,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SelectionFormatItemsTop(
    controller: ChatInputSelectionController,
    hasSelection: Boolean
) {
    val strings = LocalInputStrings.current
    if (hasSelection) {
        FormatMenuItem(label = strings.selectionQuote) { controller.applyQuote() }
        FormatMenuItem(label = strings.selectionSpoiler) { controller.applyWrap("||") }
    }
    controller.onSelectAllAction?.let { selectAll ->
        FormatMenuItem(label = strings.selectionSelectAll) {
            selectAll()
            controller.isFormatMenuOpen = false
        }
    }
}

@Composable
private fun SelectionFormatItemsStyles(
    controller: ChatInputSelectionController,
    hasSelection: Boolean
) {
    if (!hasSelection) return
    val strings = LocalInputStrings.current
    FormatMenuItem(label = strings.selectionBold, fontWeight = FontWeight.Bold) { controller.applyWrap("**") }
    FormatMenuItem(label = strings.selectionItalic, fontStyle = FontStyle.Italic) { controller.applyWrap("*") }
    FormatMenuItem(label = strings.selectionMono, fontFamily = FontFamily.Monospace) { controller.applyWrap("`") }
    FormatMenuItem(label = strings.selectionStrike, textDecoration = TextDecoration.LineThrough) { controller.applyWrap("~~") }
    FormatMenuItem(label = strings.selectionUnderline, textDecoration = TextDecoration.Underline) { controller.applyWrap("<u>", "</u>") }
    FormatMenuItem(label = strings.selectionAddLink) {
        controller.savedSelectionForLink = controller.currentValue().selection
        controller.linkUrlInput = "https://"
        controller.isLinkDialogOpen = true
        controller.hide()
    }
    FormatMenuItem(label = strings.selectionPlain) { controller.clearFormatting() }
}

@Composable
private fun SelectionLinkDialog(controller: ChatInputSelectionController) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    Dialog(onDismissRequest = { controller.isLinkDialogOpen = false }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.selectionAddLink,
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.selectionUrlLabel,
                    color = scheme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                BasicTextField(
                    value = controller.linkUrlInput,
                    onValueChange = { controller.linkUrlInput = it },
                    singleLine = true,
                    textStyle = TextStyle(color = scheme.textPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = scheme.accent, thickness = 1.5.dp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = strings.selectionCancel,
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { controller.isLinkDialogOpen = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.selectionOk,
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { controller.confirmLink() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * VisualTransformation, скрывающий маркеры форматирования (**, *, ~~, <u>, </u>, `, ||, [текст](url))
 * и сразу отображающий стили (включая спойлер-плашку) прямо в поле ввода.
 */
fun createRichInputVisualTransformation(
    textPrimary: Color,
    accentColor: Color
): VisualTransformation {
    val tokenRegex = Regex(
        "(\\*\\*([^*]+)\\*\\*)|" +              // 1: **bold**
        "(\\*([^*]+)\\*)|" +                    // 3: *italic*
        "(~~([^~]+)~~)|" +                      // 5: ~~strike~~
        "(<u>([^<]+)</u>)|" +                    // 7: <u>underline</u>
        "(`([^`]+)`)|" +                        // 9: `code`
        "(\\|\\|([^|]+)\\|\\|)|" +               // 11: ||spoiler||
        "(\\[([^\\]]+)\\]\\(([^)]+)\\))"        // 13: [text](url)
    )

    return VisualTransformation { original ->
        val src = original.text
        if (src.isEmpty()) return@VisualTransformation TransformedText(original, OffsetMapping.Identity)

        val matches = tokenRegex.findAll(src).toList()
        if (matches.isEmpty()) return@VisualTransformation TransformedText(original, OffsetMapping.Identity)

        val origToTrans = IntArray(src.length + 1)
        val transToOrig = ArrayList<Int>(src.length + 1)
        var transLen = 0

        fun emitVisibleRange(start: Int, endExclusive: Int) {
            for (i in start until endExclusive) {
                origToTrans[i] = transLen
                transToOrig.add(i)
                transLen++
            }
        }

        fun markHiddenRange(start: Int, endExclusive: Int) {
            for (i in start until endExclusive) {
                origToTrans[i] = transLen
            }
        }

        val annotated = buildAnnotatedString {
            var cursor = 0
            for (m in matches) {
                if (m.range.first > cursor) {
                    emitVisibleRange(cursor, m.range.first)
                    append(src.substring(cursor, m.range.first))
                }

                val (innerGroupIdx, style) = when {
                    m.groupValues[1].isNotEmpty() -> 2 to SpanStyle(fontWeight = FontWeight.Bold)
                    m.groupValues[3].isNotEmpty() -> 4 to SpanStyle(fontStyle = FontStyle.Italic)
                    m.groupValues[5].isNotEmpty() -> 6 to SpanStyle(textDecoration = TextDecoration.LineThrough)
                    m.groupValues[7].isNotEmpty() -> 8 to SpanStyle(textDecoration = TextDecoration.Underline)
                    m.groupValues[9].isNotEmpty() -> 10 to SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = accentColor.copy(alpha = 0.16f)
                    )
                    m.groupValues[11].isNotEmpty() -> 12 to SpanStyle(
                        background = accentColor.copy(alpha = 0.28f),
                        color = textPrimary.copy(alpha = 0.38f)
                    )
                    else -> 14 to SpanStyle(
                        color = accentColor,
                        textDecoration = TextDecoration.Underline
                    )
                }

                val innerGroup = m.groups[innerGroupIdx]
                if (innerGroup != null) {
                    val innerStart = innerGroup.range.first
                    val innerEnd = innerGroup.range.last + 1
                    markHiddenRange(m.range.first, innerStart)
                    emitVisibleRange(innerStart, innerEnd)
                    pushStyle(style)
                    append(src.substring(innerStart, innerEnd))
                    pop()
                    markHiddenRange(innerEnd, m.range.last + 1)
                } else {
                    emitVisibleRange(m.range.first, m.range.last + 1)
                    append(m.value)
                }
                cursor = m.range.last + 1
            }

            if (cursor < src.length) {
                emitVisibleRange(cursor, src.length)
                append(src.substring(cursor))
            }
        }

        origToTrans[src.length] = transLen
        transToOrig.add(src.length)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, src.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transToOrig.lastIndex)]
            }
        }

        TransformedText(annotated, mapping)
    }
}

@Composable
private fun ToolbarTextButton(
    label: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Text(
        text = label,
        color = scheme.textPrimary,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    )
}

@Composable
private fun FormatMenuItem(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    fontFamily: FontFamily = FontFamily.Default,
    textDecoration: TextDecoration = TextDecoration.None,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Text(
        text = label,
        color = scheme.textPrimary,
        fontSize = 15.sp,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    )
}

private fun stripFormattingMarkers(text: String): String {
    return text
        .lines()
        .joinToString("\n") { line -> line.removePrefix("> ") }
        .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1")
        .replace("**", "")
        .replace("~~", "")
        .replace("||", "")
        .replace("<u>", "")
        .replace("</u>", "")
        .replace("`", "")
        .replace(Regex("(?<!\\*)\\*(?!\\*)"), "")
}
