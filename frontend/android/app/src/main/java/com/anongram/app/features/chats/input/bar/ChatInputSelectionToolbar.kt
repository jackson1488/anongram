package com.anongram.app.features.chats.input.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import kotlin.math.roundToInt

@Composable
fun rememberChatInputSelectionToolbar(
    getValue: () -> TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
): TextToolbar {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val density = LocalDensity.current

    var toolbarStatus by remember { mutableStateOf(TextToolbarStatus.Hidden) }
    var anchorRect by remember { mutableStateOf(Rect.Zero) }
    var isFormatMenuOpen by remember { mutableStateOf(false) }

    var onCopyAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onPasteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onCutAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onSelectAllAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var isLinkDialogOpen by remember { mutableStateOf(false) }
    var linkUrlInput by remember { mutableStateOf("") }
    var savedSelectionForLink by remember { mutableStateOf(TextRange.Zero) }

    val toolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = toolbarStatus

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
                toolbarStatus = TextToolbarStatus.Shown
            }

            override fun hide() {
                toolbarStatus = TextToolbarStatus.Hidden
                isFormatMenuOpen = false
            }
        }
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
        toolbar.hide()
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
        toolbar.hide()
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
        toolbar.hide()
    }

    if (toolbarStatus == TextToolbarStatus.Shown) {
        val offsetY = with(density) { (anchorRect.top - 56.dp.toPx()).roundToInt().coerceAtLeast(8) }
        val offsetX = anchorRect.left.roundToInt().coerceAtLeast(12)

        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(offsetX, offsetY),
            onDismissRequest = { toolbar.hide() },
            properties = PopupProperties(focusable = false)
        ) {
            if (!isFormatMenuOpen) {
                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onCutAction?.let { cut ->
                        ToolbarTextButton(label = strings.selectionCut) {
                            cut()
                            toolbar.hide()
                        }
                    }
                    onCopyAction?.let { copy ->
                        ToolbarTextButton(label = strings.selectionCopy) {
                            copy()
                            toolbar.hide()
                        }
                    }
                    onPasteAction?.let { paste ->
                        ToolbarTextButton(label = strings.selectionPaste) {
                            paste()
                            toolbar.hide()
                        }
                    }

                    val hasSelection = !getValue().selection.collapsed
                    if (hasSelection || onSelectAllAction != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { isFormatMenuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = strings.selectionMore,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            } else {
                val hasSelection = !getValue().selection.collapsed
                Column(
                    modifier = Modifier
                        .widthIn(min = 190.dp, max = 240.dp)
                        .heightIn(max = 340.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        if (hasSelection) {
                            FormatMenuItem(label = strings.selectionQuote) { applyQuote() }
                            FormatMenuItem(label = strings.selectionSpoiler) { applyWrap("||") }
                        }
                        onSelectAllAction?.let { selectAll ->
                            FormatMenuItem(label = strings.selectionSelectAll) {
                                selectAll()
                                isFormatMenuOpen = false
                            }
                        }
                        if (hasSelection) {
                            FormatMenuItem(
                                label = strings.selectionBold,
                                fontWeight = FontWeight.Bold
                            ) { applyWrap("**") }
                            FormatMenuItem(
                                label = strings.selectionItalic,
                                fontStyle = FontStyle.Italic
                            ) { applyWrap("*") }
                            FormatMenuItem(
                                label = strings.selectionMono,
                                fontFamily = FontFamily.Monospace
                            ) { applyWrap("`") }
                            FormatMenuItem(
                                label = strings.selectionStrike,
                                textDecoration = TextDecoration.LineThrough
                            ) { applyWrap("~~") }
                            FormatMenuItem(
                                label = strings.selectionUnderline,
                                textDecoration = TextDecoration.Underline
                            ) { applyWrap("<u>", "</u>") }
                            FormatMenuItem(label = strings.selectionAddLink) {
                                savedSelectionForLink = getValue().selection
                                linkUrlInput = "https://"
                                isLinkDialogOpen = true
                                toolbar.hide()
                            }
                            FormatMenuItem(label = strings.selectionPlain) { clearFormatting() }
                        }
                    }

                    HorizontalDivider(color = scheme.divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFormatMenuOpen = false }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (isLinkDialogOpen) {
        Dialog(onDismissRequest = { isLinkDialogOpen = false }) {
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
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
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
                            .clickable { isLinkDialogOpen = false }
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
                            .clickable {
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
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    return toolbar
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
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
