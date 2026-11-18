package com.anongram.app.features.chats.input.bar

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    private val getSpans: () -> List<RichInputSpan>,
    private val onSpansChange: (List<RichInputSpan>) -> Unit
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

    fun applyStyle(style: RichSpanStyle, url: String = "") {
        val current = getValue()
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start >= end) return
        val existing = getSpans().filterNot {
            it.style == style && it.start == start && it.end == end
        }
        onSpansChange(existing + RichInputSpan(start = start, end = end, style = style, url = url))
        hide()
    }

    fun clearFormatting() {
        val current = getValue()
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start >= end) return
        val updated = mutableListOf<RichInputSpan>()
        for (sp in getSpans()) {
            if (sp.end <= start || sp.start >= end) {
                updated.add(sp)
            } else {
                if (sp.start < start) updated.add(sp.copy(end = start))
                if (sp.end > end) updated.add(sp.copy(start = end))
            }
        }
        onSpansChange(updated)
        hide()
    }

    fun confirmLink() {
        val url = linkUrlInput.trim()
        if (url.isNotBlank()) {
            val current = getValue()
            val s = minOf(savedSelectionForLink.start, savedSelectionForLink.end).coerceIn(0, current.text.length)
            val e = maxOf(savedSelectionForLink.start, savedSelectionForLink.end).coerceIn(0, current.text.length)
            if (s < e) {
                val filtered = getSpans().filterNot {
                    it.style == RichSpanStyle.LINK && it.start < e && it.end > s
                }
                onSpansChange(filtered + RichInputSpan(start = s, end = e, style = RichSpanStyle.LINK, url = url))
            }
        }
        isLinkDialogOpen = false
    }
}

@Composable
fun rememberChatInputSelectionController(
    getValue: () -> TextFieldValue,
    getSpans: () -> List<RichInputSpan>,
    onSpansChange: (List<RichInputSpan>) -> Unit
): ChatInputSelectionController {
    return remember {
        ChatInputSelectionController(
            getValue = getValue,
            getSpans = getSpans,
            onSpansChange = onSpansChange
        )
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
        val left = rect.left.takeIf { it.isFinite() } ?: 0f
        val right = rect.right.takeIf { it.isFinite() } ?: left
        val top = rect.top.takeIf { it.isFinite() } ?: 0f
        val bottom = rect.bottom.takeIf { it.isFinite() } ?: top
        val centerX = ((left + right) / 2f).roundToInt()
        val rawX = centerX - popupContentSize.width / 2
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = rawX.coerceIn(marginPx, maxX)

        val aboveY = top.roundToInt() - popupContentSize.height - gapPx
        val belowY = bottom.roundToInt() + gapPx
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val y = if (aboveY >= marginPx) aboveY else belowY.coerceIn(marginPx, maxY)
        return IntOffset(x, y)
    }
}

@Composable
fun ChatInputSelectionToolbarOverlay(
    controller: ChatInputSelectionController
) {
    val scheme = LocalAnonGramScheme.current
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
            Box(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .animateContentSize(animationSpec = tween(180))
            ) {
                if (!controller.isFormatMenuOpen) {
                    SelectionActionBar(controller = controller, hasSelection = hasSelection)
                } else {
                    SelectionFormatMenu(controller = controller, hasSelection = hasSelection)
                }
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
            .height(48.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Порядок как в Telegram: Копировать -> Вставить -> Вырезать   ⋮
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
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
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
        modifier = Modifier.widthIn(min = 175.dp, max = 215.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 205.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 2.dp)
        ) {
            SelectionFormatItemsTop(controller = controller, hasSelection = hasSelection)
            SelectionFormatItemsStyles(controller = controller, hasSelection = hasSelection)
        }
        HorizontalDivider(color = scheme.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { controller.isFormatMenuOpen = false }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.selectionBack,
                tint = scheme.textPrimary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = strings.selectionBack,
                color = scheme.textPrimary,
                fontSize = 14.sp,
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
        FormatMenuItem(label = strings.selectionQuote) { controller.applyStyle(RichSpanStyle.QUOTE) }
        FormatMenuItem(label = strings.selectionSpoiler) { controller.applyStyle(RichSpanStyle.SPOILER) }
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
    FormatMenuItem(label = strings.selectionBold, fontWeight = FontWeight.Bold) { controller.applyStyle(RichSpanStyle.BOLD) }
    FormatMenuItem(label = strings.selectionItalic, fontStyle = FontStyle.Italic) { controller.applyStyle(RichSpanStyle.ITALIC) }
    FormatMenuItem(label = strings.selectionMono, fontFamily = FontFamily.Monospace) { controller.applyStyle(RichSpanStyle.MONO) }
    FormatMenuItem(label = strings.selectionStrike, textDecoration = TextDecoration.LineThrough) { controller.applyStyle(RichSpanStyle.STRIKE) }
    FormatMenuItem(label = strings.selectionUnderline, textDecoration = TextDecoration.Underline) { controller.applyStyle(RichSpanStyle.UNDERLINE) }
    FormatMenuItem(label = strings.selectionAddLink) {
        controller.savedSelectionForLink = controller.currentValue().selection
        controller.linkUrlInput = ""
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
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (controller.linkUrlInput.isEmpty()) {
                            Text(
                                text = "https://...",
                                color = scheme.textMuted,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
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
            .padding(horizontal = 15.dp, vertical = 11.dp)
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
        fontSize = 14.sp,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
