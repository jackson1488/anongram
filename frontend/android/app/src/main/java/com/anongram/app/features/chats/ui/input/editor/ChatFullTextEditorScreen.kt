package com.anongram.app.features.chats.ui.input.editor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.ui.components.EmojiStickerPanel
import com.anongram.app.features.chats.ui.components.reply.ReplyAttachmentMiniThumb
import java.util.Locale

/**
 * Полноэкранный редактор текста с форматированием, меню списков, вводом LaTeX,
 * центрированным островком инструментов и стеком медиа-вложений (фото, файл, музыка).
 */
@Composable
fun ChatFullTextEditorScreen(
    initialText: String,
    onClose: (String) -> Unit,
    onSend: (text: String, format: TextStyleFormat, language: String, attachment: MessageAttachment?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val undoState = rememberUndoRedoState(initialText)
    val focusRequester = remember { FocusRequester() }

    var selectedFormat by remember { mutableStateOf(TextStyleFormat.NORMAL) }
    var selectedLanguage by remember { mutableStateOf("ABNF") }
    var selectedListStyle by remember { mutableStateOf(ListStyle.NONE) }

    var isAaMenuOpen by remember { mutableStateOf(false) }
    var isListMenuOpen by remember { mutableStateOf(false) }
    var isFormulaOpen by remember { mutableStateOf(false) }
    var isLanguageMenuOpen by remember { mutableStateOf(false) }
    var isAttachMenuOpen by remember { mutableStateOf(false) }
    var isEmojiOpen by remember { mutableStateOf(false) }

    // Стек прикреплённых медиа (фото, файлы, музыка)
    var attachedMediaList by remember { mutableStateOf<List<MessageAttachment>>(emptyList()) }

    // Лаунчеры выбора медиа
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val (name, _) = queryUriMetadata(context, uri)
            val newAttachment = MessageAttachment.Photo(
                label = name,
                colorSeed = (attachedMediaList.size % 5) + 1,
                isVideo = name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mov", ignoreCase = true),
                uri = uri.toString()
            )
            attachedMediaList = attachedMediaList + newAttachment
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryUriMetadata(context, uri)
            val kind = mapExtensionToFileKind(name)
            val newAttachment = MessageAttachment.File(
                name = name,
                sizeLabel = formatFileSize(size),
                kind = kind,
                uri = uri.toString()
            )
            attachedMediaList = attachedMediaList + newAttachment
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryUriMetadata(context, uri)
            val trackTitle = name.substringBeforeLast('.')
            val newAttachment = MessageAttachment.Music(
                title = trackTitle,
                artist = "Аудиозапись",
                durationLabel = "3:30",
                colorSeed = (attachedMediaList.size % 5) + 1,
                uri = uri.toString(),
                sizeLabel = formatFileSize(size)
            )
            attachedMediaList = attachedMediaList + newAttachment
        }
    }

    BackHandler {
        when {
            isFormulaOpen -> isFormulaOpen = false
            isListMenuOpen -> isListMenuOpen = false
            isAaMenuOpen -> isAaMenuOpen = false
            isAttachMenuOpen -> isAttachMenuOpen = false
            isLanguageMenuOpen -> isLanguageMenuOpen = false
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

            // Поле ввода текста и предпросмотр прикреплённого медиа
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
                    // Карточка активного прикреплённого медиа со стеком
                    val activeAttachment = attachedMediaList.lastOrNull()
                    if (activeAttachment != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1E24))
                                .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Миниатюра вложения
                                val fakeMsg = remember(activeAttachment) {
                                    ChatMessage(
                                        id = "draft_preview",
                                        direction = MessageDirection.OUTGOING,
                                        epochMs = System.currentTimeMillis(),
                                        attachment = activeAttachment
                                    )
                                }
                                ReplyAttachmentMiniThumb(
                                    message = fakeMsg,
                                    accent = scheme.accent,
                                    size = 44.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Описание вложения
                                Column(modifier = Modifier.weight(1f)) {
                                    val (title, subtitle) = when (activeAttachment) {
                                        is MessageAttachment.Photo -> {
                                            val t = if (activeAttachment.isVideo) "Видеозапись" else "Фотография"
                                            val sub = if (activeAttachment.label.isNotBlank()) activeAttachment.label else "Медиафайл"
                                            Pair(t, sub)
                                        }
                                        is MessageAttachment.Music -> {
                                            Pair(activeAttachment.title, "${activeAttachment.artist} • ${activeAttachment.durationLabel}")
                                        }
                                        is MessageAttachment.File -> {
                                            Pair(activeAttachment.name, "${activeAttachment.kind.label} • ${activeAttachment.sizeLabel}")
                                        }
                                        else -> Pair("Вложение", "Файл")
                                    }

                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Счётчик в стеке (если прикреплено несколько)
                                if (attachedMediaList.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(scheme.accent.copy(alpha = 0.25f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+${attachedMediaList.size - 1}",
                                            color = scheme.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Кнопка крестика ✕: удаляет верхний элемент, предыдущий открывается обратно!
                                IconButton(
                                    onClick = {
                                        attachedMediaList = attachedMediaList.dropLast(1)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Удалить вложение",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

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
                // Всплывающее меню стилей форматирования Aa
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

                // Всплывающее меню списков ≡ (макет media_1794253331200.png)
                AnimatedVisibility(
                    visible = isListMenuOpen,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ListPopupMenu(
                            selectedStyle = selectedListStyle,
                            onSelectStyle = { style ->
                                selectedListStyle = style
                                undoState.updateText(ListFormattingHelper.applyStyle(undoState.currentText, style))
                            },
                            onIndent = {
                                undoState.updateText(ListFormattingHelper.indent(undoState.currentText))
                            },
                            onUnindent = {
                                undoState.updateText(ListFormattingHelper.unindent(undoState.currentText))
                            },
                            onDismiss = { isListMenuOpen = false }
                        )
                    }
                }

                // Всплывающая панель формулы LaTeX ∑ (макет media_1794253317328.png)
                AnimatedVisibility(
                    visible = isFormulaOpen,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LatexFormulaInputPanel(
                            onInsertFormula = { formula ->
                                val space = if (undoState.currentText.isNotEmpty() && !undoState.currentText.endsWith(" ")) " " else ""
                                undoState.updateText(undoState.currentText + space + formula + " ")
                            },
                            onDismiss = { isFormulaOpen = false }
                        )
                    }
                }

                // Всплывающее меню выбора типа вложения 📎
                AnimatedVisibility(
                    visible = isAttachMenuOpen,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF232328))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                .padding(vertical = 6.dp)
                                .width(200.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Фото / Видео
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isAttachMenuOpen = false
                                            photoLauncher.launch("image/*")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = "Фото или видео",
                                        tint = scheme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Фото или видео",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                // Файл / Документ
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isAttachMenuOpen = false
                                            fileLauncher.launch("*/*")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = "Файл",
                                        tint = scheme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Файл",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                // Музыка / Аудио
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isAttachMenuOpen = false
                                            audioLauncher.launch("audio/*")
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MusicNote,
                                        contentDescription = "Музыка",
                                        tint = scheme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Музыка",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Центрированная парящая панель инструментов форматирования
                TextFormattingToolbar(
                    isAaActive = isAaMenuOpen || selectedFormat != TextStyleFormat.NORMAL,
                    onToggleAa = {
                        isAaMenuOpen = !isAaMenuOpen
                        if (isAaMenuOpen) {
                            isListMenuOpen = false
                            isFormulaOpen = false
                            isAttachMenuOpen = false
                            isEmojiOpen = false
                        }
                    },
                    onToggleEmoji = {
                        isEmojiOpen = !isEmojiOpen
                        if (isEmojiOpen) {
                            isAaMenuOpen = false
                            isListMenuOpen = false
                            isFormulaOpen = false
                            isAttachMenuOpen = false
                        }
                    },
                    isListActive = isListMenuOpen,
                    onToggleList = {
                        isListMenuOpen = !isListMenuOpen
                        if (isListMenuOpen) {
                            isAaMenuOpen = false
                            isFormulaOpen = false
                            isAttachMenuOpen = false
                            isEmojiOpen = false
                        }
                    },
                    onInsertTable = {
                        val table = "\n| Колонка 1 | Колонка 2 |\n|-----------|-----------|\n| Значение  | Данные    |\n"
                        undoState.updateText(undoState.currentText + table)
                    },
                    isFormulaActive = isFormulaOpen,
                    onToggleFormula = {
                        isFormulaOpen = !isFormulaOpen
                        if (isFormulaOpen) {
                            isAaMenuOpen = false
                            isListMenuOpen = false
                            isAttachMenuOpen = false
                            isEmojiOpen = false
                        }
                    },
                    onOpenAttach = {
                        isAttachMenuOpen = !isAttachMenuOpen
                        if (isAttachMenuOpen) {
                            isAaMenuOpen = false
                            isListMenuOpen = false
                            isFormulaOpen = false
                            isEmojiOpen = false
                        }
                    },
                    onSend = {
                        val resolvedAttachment: MessageAttachment? = when {
                            attachedMediaList.isEmpty() -> null
                            attachedMediaList.size == 1 -> attachedMediaList.first()
                            attachedMediaList.all { it is MessageAttachment.Photo } -> {
                                MessageAttachment.MediaAlbum(
                                    items = attachedMediaList.filterIsInstance<MessageAttachment.Photo>().map {
                                        MessageAttachment.AlbumMediaItem(it.uri ?: "", it.isVideo, it.hasSpoiler)
                                    }
                                )
                            }
                            attachedMediaList.all { it is MessageAttachment.File } -> {
                                MessageAttachment.FileGroup(
                                    items = attachedMediaList.filterIsInstance<MessageAttachment.File>()
                                )
                            }
                            attachedMediaList.all { it is MessageAttachment.Music } -> {
                                MessageAttachment.MusicGroup(
                                    items = attachedMediaList.filterIsInstance<MessageAttachment.Music>()
                                )
                            }
                            else -> attachedMediaList.lastOrNull()
                        }
                        onSend(undoState.currentText, selectedFormat, selectedLanguage, resolvedAttachment)
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

private fun queryUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = uri.lastPathSegment ?: "file"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIndex >= 0) name = c.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = c.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Pair(name, size)
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    return if (kb >= 1024.0) {
        String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0)
    } else {
        String.format(Locale.getDefault(), "%.1f KB", kb.coerceAtLeast(0.1))
    }
}

private fun mapExtensionToFileKind(name: String): FileKind {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> FileKind.IMAGE
        "pdf" -> FileKind.PDF
        "apk" -> FileKind.APK
        "doc", "docx", "txt", "rtf" -> FileKind.DOC
        "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
        "mp4", "mkv", "mov", "avi" -> FileKind.VIDEO
        else -> FileKind.UNKNOWN
    }
}
