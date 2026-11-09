package com.anongram.app.features.chats.ui.input.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.features.chats.ui.components.EmojiStickerPanel

/**
 * Полноэкранный редактор текста с форматированием (макеты media_1794245794747.png и media_1794245806073.png).
 */
@Composable
fun ChatFullTextEditorScreen(
    initialText: String,
    onClose: (String) -> Unit,
    onSend: (text: String, format: TextStyleFormat, language: String) -> Unit,
    onOpenAttach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val undoState = rememberUndoRedoState(initialText)
    val focusRequester = remember { FocusRequester() }

    var selectedFormat by remember { mutableStateOf(TextStyleFormat.NORMAL) }
    var selectedLanguage by remember { mutableStateOf("ABNF") }
    var isAaMenuOpen by remember { mutableStateOf(false) }
    var isLanguageMenuOpen by remember { mutableStateOf(false) }
    var isEmojiOpen by remember { mutableStateOf(false) }

    BackHandler {
        when {
            isLanguageMenuOpen -> isLanguageMenuOpen = false
            isAaMenuOpen -> isAaMenuOpen = false
            isEmojiOpen -> isEmojiOpen = false
            else -> onClose(undoState.currentText)
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(top = topPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель: Назад, Undo, Redo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onClose(undoState.currentText) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { undoState.undo() },
                    enabled = undoState.canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "Отменить",
                        tint = if (undoState.canUndo) Color.White else Color(0xFF55555A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { undoState.redo() },
                    enabled = undoState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "Повторить",
                        tint = if (undoState.canRedo) Color.White else Color(0xFF55555A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Поле ввода текста
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Если выбран режим кода, показываем выбор языка сверху справа
                    if (selectedFormat == TextStyleFormat.CODE) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF26262B))
                                    .border(1.dp, scheme.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { isLanguageMenuOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedLanguage,
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isLanguageMenuOpen,
                                onDismissRequest = { isLanguageMenuOpen = false }
                            ) {
                                CodeLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = lang,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                                color = if (lang == selectedLanguage) scheme.accent else Color.White
                                            )
                                        },
                                        onClick = {
                                            selectedLanguage = lang
                                            isLanguageMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Контейнер с оформлением под стиль
                    val editorBoxModifier = when (selectedFormat) {
                        TextStyleFormat.CODE -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141419))
                            .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                        TextStyleFormat.QUOTE -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1D24))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                        TextStyleFormat.CALLOUT -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF231E28))
                            .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                        else -> Modifier.fillMaxWidth()
                    }

                    val textStyle = when (selectedFormat) {
                        TextStyleFormat.HEADING -> TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )
                        TextStyleFormat.CODE -> TextStyle(
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                        TextStyleFormat.QUOTE -> TextStyle(
                            color = Color(0xFFCBD5E1),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp
                        )
                        TextStyleFormat.SMALL -> TextStyle(
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        else -> TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }

                    Box(modifier = editorBoxModifier) {
                        if (undoState.currentText.isEmpty()) {
                            Text(
                                text = when (selectedFormat) {
                                    TextStyleFormat.CODE -> "Введите код ($selectedLanguage)..."
                                    TextStyleFormat.HEADING -> "Заголовок..."
                                    TextStyleFormat.QUOTE -> "Цитата..."
                                    TextStyleFormat.CALLOUT -> "Текст спойлера / выноски..."
                                    else -> "Введите текст..."
                                },
                                color = Color(0xFF64748B),
                                style = textStyle
                            )
                        }

                        BasicTextField(
                            value = undoState.currentText,
                            onValueChange = { undoState.updateText(it) },
                            textStyle = textStyle,
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                }
            }

            // Нижняя панель над клавиатурой
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(bottom = if (isEmojiOpen) 0.dp else navBottom)
            ) {
                // Всплывающее меню стилей форматирования
                AnimatedVisibility(
                    visible = isAaMenuOpen,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        StylePopupMenu(
                            selectedFormat = selectedFormat,
                            onSelectFormat = { selectedFormat = it },
                            onDismiss = { isAaMenuOpen = false }
                        )
                    }
                }

                TextFormattingToolbar(
                    isAaActive = isAaMenuOpen || selectedFormat != TextStyleFormat.NORMAL,
                    onToggleAa = { isAaMenuOpen = !isAaMenuOpen },
                    onToggleAi = {
                        // Быстрый шаблон ИИ-структурирования текста
                        if (undoState.currentText.isNotBlank()) {
                            undoState.updateText("✨ " + undoState.currentText.trim())
                        }
                    },
                    onToggleEmoji = {
                        isEmojiOpen = !isEmojiOpen
                        if (isEmojiOpen) isAaMenuOpen = false
                    },
                    onInsertList = {
                        undoState.updateText(undoState.currentText + "\n• ")
                    },
                    onInsertTable = {
                        undoState.updateText(undoState.currentText + "\n| Колонка 1 | Колонка 2 |\n|-----------|-----------|\n| Значение  | Данные    |")
                    },
                    onInsertFormula = {
                        undoState.updateText(undoState.currentText + " \\(E = mc^2\\) ")
                    },
                    onOpenAttach = onOpenAttach,
                    onSend = {
                        onSend(undoState.currentText, selectedFormat, selectedLanguage)
                    }
                )

                if (isEmojiOpen) {
                    EmojiStickerPanel(
                        onEmojiClick = { emoji -> undoState.updateText(undoState.currentText + emoji) },
                        onStickerClick = { symbol, _ -> undoState.updateText(undoState.currentText + symbol) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            }
        }
    }
}
