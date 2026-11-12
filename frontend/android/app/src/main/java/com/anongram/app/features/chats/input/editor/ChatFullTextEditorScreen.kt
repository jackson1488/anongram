package com.anongram.app.features.chats.input.editor

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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import com.anongram.app.core.theme.AnonGramScheme
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.input.emoji.EmojiStickerPanel
import com.anongram.app.features.chats.ui.components.bubble.ChatRealPhotoBox
import com.anongram.app.features.chats.input.reply.ReplyAttachmentMiniThumb
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import java.util.Locale

/**
 * Полноэкранный редактор текста с форматированием, меню списков, вводом LaTeX,
 * центрированным островком инструментов, плавающей панелью выделения и стеком медиа.
 */
@Composable
fun ChatFullTextEditorScreen(
    initialText: String,
    onClose: (String) -> Unit,
    onSend: (text: String, format: TextStyleFormat, language: String, attachment: MessageAttachment?) -> Unit,
    onOpenNativeAttach: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val initialFontMatch = remember(initialText) {
        Regex("^\\[font=(sans|serif|mono|cursive)\\]([\\s\\S]*?)\\[\\/font\\]$").matchEntire(initialText.trim())
    }
    val cleanInitialText = remember(initialText) {
        initialFontMatch?.groupValues?.get(2) ?: initialText
    }
    val initialFontKey = remember(initialText) {
        initialFontMatch?.groupValues?.get(1) ?: "sans"
    }

    val undoState = rememberUndoRedoState(cleanInitialText)
    val focusRequester = remember { FocusRequester() }

    var selectedFontFamily by remember { mutableStateOf(initialFontKey) }
    var selectedFormat by remember { mutableStateOf(TextStyleFormat.NORMAL) }
    var selectedHeadingLevel by remember { mutableIntStateOf(1) }
    var selectedLanguage by remember { mutableStateOf("ABNF") }
    var selectedListStyle by remember { mutableStateOf(ListStyle.NONE) }

    var isAaMenuOpen by remember { mutableStateOf(false) }
    var isListMenuOpen by remember { mutableStateOf(false) }
    var isFormulaOpen by remember { mutableStateOf(false) }
    var isLanguageMenuOpen by remember { mutableStateOf(false) }
    var isEmojiOpen by remember { mutableStateOf(false) }
    var isEditorAttachOpen by remember { mutableStateOf(false) }

    fun getFinalText(): String {
        val raw = undoState.currentText
        return if (selectedFontFamily != "sans" && raw.isNotBlank()) {
            "[font=$selectedFontFamily]$raw[/font]"
        } else {
            raw
        }
    }

    // Стек прикреплённых медиа
    var attachedMediaList by remember { mutableStateOf<List<MessageAttachment>>(emptyList()) }

    // Лаунчеры выбора медиа (как резервный путь)
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

    // Хелпер оборачивания выделенного текста тегами (или вставка тегов с курсором между ними)
    fun wrapSelection(prefix: String, suffix: String = prefix) {
        val currentVal = undoState.textFieldValue
        val start = minOf(currentVal.selection.start, currentVal.selection.end)
        val end = maxOf(currentVal.selection.start, currentVal.selection.end)
        val text = currentVal.text
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = prefix + selected + suffix
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            val newSelection = TextRange(start + prefix.length, start + prefix.length + selected.length)
            undoState.updateValue(TextFieldValue(newText, newSelection))
        } else {
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            val newSelection = TextRange(start + prefix.length)
            undoState.updateValue(TextFieldValue(newText, newSelection))
        }
    }

    // Хелпер вставки сворачиваемого блока (^ Заголовок\nВведите текст...)
    fun insertCollapsibleBlock() {
        val currentVal = undoState.textFieldValue
        val text = currentVal.text
        val start = minOf(currentVal.selection.start, currentVal.selection.end)
        val end = maxOf(currentVal.selection.start, currentVal.selection.end)
        val selected = if (start != end) text.substring(start, end).trim() else ""
        val body = if (selected.isNotEmpty()) selected else strings.collapsibleTemplateBody
        val title = strings.collapsibleTemplateTitle
        val block = "^ $title\n$body\n"
        val newText = text.substring(0, start) + block + text.substring(end)
        val titleStart = start + 2
        val titleEnd = titleStart + title.length
        undoState.updateValue(TextFieldValue(newText, TextRange(titleStart, titleEnd)))
    }

    // Хелпер применения префикса к текущей строке/параграфу (заголовки, цитаты)
    fun applyLinePrefix(prefix: String) {
        val currentVal = undoState.textFieldValue
        val text = currentVal.text
        val cursor = currentVal.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val cleanLine = currentLine.replaceFirst(Regex("^(#{1,6}|>>>|>|\\^)\\s*"), "")
        val newLine = prefix + cleanLine
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (lineStart + newLine.length).coerceAtMost(newText.length)
        undoState.updateValue(TextFieldValue(newText, TextRange(newCursor)))
    }

    val hasSelection = undoState.textFieldValue.selection.length > 0

    BackHandler {
        when {
            isEditorAttachOpen -> isEditorAttachOpen = false
            isFormulaOpen -> isFormulaOpen = false
            isListMenuOpen -> isListMenuOpen = false
            isAaMenuOpen -> isAaMenuOpen = false
            isLanguageMenuOpen -> isLanguageMenuOpen = false
            isEmojiOpen -> isEmojiOpen = false
            else -> onClose(getFinalText())
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.editorBg)
            .padding(top = topPadding)
            // Тап в пустое место экрана закрывает формулу/меню и возвращает стандартную панель инструментов + фокус
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isFormulaOpen) isFormulaOpen = false
                if (isAaMenuOpen) isAaMenuOpen = false
                if (isListMenuOpen) isListMenuOpen = false
                if (isEmojiOpen) isEmojiOpen = false
                focusRequester.requestFocus()
                keyboardController?.show()
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель: Назад, Undo, Redo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onClose(getFinalText()) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
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
                        contentDescription = strings.undo,
                        tint = if (undoState.canUndo) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { undoState.redo() },
                    enabled = undoState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = strings.redo,
                        tint = if (undoState.canRedo) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.45f),
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
                    // Превью прикреплённых медиафайлов: полноразмерное фото или сетка-альбом
                    if (attachedMediaList.isNotEmpty()) {
                        val photos = attachedMediaList.filterIsInstance<MessageAttachment.Photo>()
                        if (photos.size == 1 && attachedMediaList.size == 1) {
                            // Одиночное фото / видео: крупная карточка с реальным изображением
                            val photo = photos.first()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(scheme.editorSurface)
                                    .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            ) {
                                if (photo.uri != null) {
                                    ChatRealPhotoBox(
                                        uriString = photo.uri,
                                        isVideo = photo.isVideo,
                                        hasSpoiler = photo.hasSpoiler,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                // Кнопка удаления
                                IconButton(
                                    onClick = { attachedMediaList = emptyList() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = strings.removeAttachment,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Кнопка «+ Добавить ещё»
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .clickable { isEditorAttachOpen = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.addMore,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else if (photos.size >= 2 && photos.size == attachedMediaList.size) {
                            // Альбом фотографий: компактная сетка с кнопкой добавления
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(scheme.card)
                                    .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.albumPhotos(photos.size),
                                        color = scheme.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(scheme.accent.copy(alpha = 0.2f))
                                            .clickable { isEditorAttachOpen = true }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null,
                                            tint = scheme.accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.addMore,
                                            color = scheme.accent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(photos) { index, item ->
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(scheme.surface)
                                        ) {
                                            if (item.uri != null) {
                                                ChatRealPhotoBox(
                                                    uriString = item.uri,
                                                    isVideo = item.isVideo,
                                                    hasSpoiler = item.hasSpoiler,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Outlined.Image, contentDescription = null, tint = scheme.textSecondary)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    attachedMediaList = attachedMediaList.filterIndexed { i, _ -> i != index }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = strings.delete,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Файлы, музыка или смешанные вложения
                            val activeAttachment = attachedMediaList.last()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .shadow(8.dp, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(scheme.editorSurface)
                                    .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (title, subtitle, icon) = when (activeAttachment) {
                                        is MessageAttachment.Music -> Triple(
                                            activeAttachment.title,
                                            "${activeAttachment.artist} • ${activeAttachment.durationLabel}",
                                            Icons.Outlined.MusicNote
                                        )
                                        is MessageAttachment.File -> Triple(
                                            activeAttachment.name,
                                            "${activeAttachment.kind.label} • ${activeAttachment.sizeLabel}",
                                            Icons.Outlined.Description
                                        )
                                        else -> Triple(strings.attachment, strings.file, Icons.Outlined.Description)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(scheme.accentSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(24.dp))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            color = scheme.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            color = scheme.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (attachedMediaList.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(scheme.accentSoft)
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

                                    IconButton(
                                        onClick = { attachedMediaList = attachedMediaList.dropLast(1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = strings.removeAttachment,
                                            tint = scheme.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
                                    .background(scheme.editorSurface)
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
                                onDismissRequest = { isLanguageMenuOpen = false },
                                containerColor = scheme.card
                            ) {
                                CodeLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = lang,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                                color = if (lang == selectedLanguage) scheme.accent else scheme.textPrimary
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

                    val baseEditorBoxModifier = when (selectedFormat) {
                        TextStyleFormat.CODE -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.editorSurface)
                            .border(1.dp, scheme.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                        TextStyleFormat.QUOTE -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.editorSurface)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(8.dp))
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                        TextStyleFormat.CALLOUT -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.accentSoft)
                            .border(1.dp, scheme.accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                        else -> Modifier.fillMaxWidth()
                    }

                    val editorBoxModifier = baseEditorBoxModifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isEmojiOpen) isEmojiOpen = false
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }

                    val currentFontFamily = when (selectedFontFamily) {
                        "serif" -> FontFamily.Serif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.Default
                    }

                    val textStyle = when (selectedFormat) {
                        TextStyleFormat.HEADING -> {
                            val headingSp = when (selectedHeadingLevel) {
                                1 -> 24.sp
                                2 -> 21.sp
                                3 -> 18.sp
                                4 -> 16.sp
                                5 -> 14.sp
                                else -> 12.sp
                            }
                            TextStyle(
                                color = scheme.textPrimary,
                                fontSize = headingSp,
                                fontWeight = if (selectedHeadingLevel <= 3) FontWeight.Bold else FontWeight.SemiBold,
                                fontFamily = currentFontFamily,
                                lineHeight = (headingSp.value * 1.25f).sp
                            )
                        }
                        TextStyleFormat.CODE -> TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                        TextStyleFormat.QUOTE -> TextStyle(
                            color = scheme.textSecondary,
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            fontFamily = currentFontFamily,
                            lineHeight = 22.sp
                        )
                        else -> TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontFamily = currentFontFamily,
                            lineHeight = 22.sp
                        )
                    }

                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    Box(modifier = editorBoxModifier) {
                        if (undoState.currentText.isEmpty()) {
                            Text(
                                text = when (selectedFormat) {
                                    TextStyleFormat.CODE -> strings.codePlaceholder(selectedLanguage)
                                    TextStyleFormat.HEADING -> strings.headingPlaceholder(selectedHeadingLevel)
                                    TextStyleFormat.QUOTE -> strings.quotePlaceholder
                                    TextStyleFormat.CALLOUT -> strings.calloutPlaceholder
                                    else -> if (selectedListStyle == ListStyle.COLLAPSIBLE) strings.enterHeadingPlaceholder else strings.enterTextPlaceholder
                                },
                                color = scheme.textMuted,
                                style = textStyle
                            )
                        } else if (undoState.currentText.trim() == "^") {
                            Text(
                                text = "  ${strings.collapsibleTemplateTitle}\n  ${strings.collapsibleTemplateBody}",
                                color = scheme.textMuted,
                                style = textStyle
                            )
                        } else if (undoState.currentText.startsWith("^") && !undoState.currentText.contains("\n") && undoState.currentText.trim().length > 1) {
                            Text(
                                text = "\n  ${strings.collapsibleTemplateBody}",
                                color = scheme.textMuted,
                                style = textStyle
                            )
                        } else if (undoState.currentText.trim() == ">" || undoState.currentText.trim() == ">>>") {
                            Text(
                                text = "  ${strings.quotePlaceholder}",
                                color = scheme.textMuted,
                                style = textStyle
                            )
                        }

                        BasicTextField(
                            value = undoState.textFieldValue,
                            onValueChange = { newVal ->
                                val processed = SmartListEnterHelper.processEnter(undoState.textFieldValue, newVal)
                                undoState.updateValue(processed)
                            },
                            onTextLayout = { textLayoutResult = it },
                            visualTransformation = remember(scheme) { EditorWysiwygVisualTransformation(scheme) },
                            textStyle = textStyle,
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .pointerInput(undoState.textFieldValue.text) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val down = event.changes.firstOrNull { it.changedToDown() } ?: continue
                                            if (isEmojiOpen) isEmojiOpen = false
                                            val layout = textLayoutResult ?: continue
                                            val text = undoState.textFieldValue.text
                                            if (text.isEmpty()) continue

                                            val offset = layout.getOffsetForPosition(down.position)
                                            if (offset in text.indices) {
                                                val lineIndex = layout.getLineForOffset(offset)
                                                val lineStart = layout.getLineStart(lineIndex)
                                                val lineEnd = layout.getLineEnd(lineIndex)
                                                val line = text.substring(lineStart, lineEnd)
                                                val t = line.trimStart()
                                                val leadingSpaces = line.length - t.length
                                                val checkStart = lineStart + leadingSpaces

                                                if (offset in checkStart..(checkStart + 5).coerceAtMost(text.length)) {
                                                    val (hasMatch, toggledPrefix, oldLen) = when {
                                                        t.startsWith("☐") -> Triple(true, "☑", 1)
                                                        t.startsWith("☑") -> Triple(true, "☐", 1)
                                                        t.startsWith("[ ]") -> Triple(true, "[x]", 3)
                                                        t.startsWith("[x]", ignoreCase = true) -> Triple(true, "[ ]", 3)
                                                        t.startsWith("- [ ]") -> Triple(true, "- [x]", 5)
                                                        t.startsWith("- [x]", ignoreCase = true) -> Triple(true, "- [ ]", 5)
                                                        else -> Triple(false, "", 0)
                                                    }
                                                    if (hasMatch) {
                                                        down.consume()
                                                        val newText = text.substring(0, checkStart) + toggledPrefix + text.substring(checkStart + oldLen)
                                                        undoState.updateText(newText)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }

            // Нижняя панель над клавиатурой
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isEmojiOpen) Modifier else Modifier.imePadding())
                    .padding(bottom = if (isEmojiOpen) 0.dp else navBottom)
            ) {
                // 1. Плавающая панель выделения текста (макет media_1794257026295.png)
                AnimatedVisibility(
                    visible = hasSelection && !isFormulaOpen,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextSelectionFloatingBar(
                            onBold = { wrapSelection("**") },
                            onItalic = { wrapSelection("*") },
                            onUnderline = { wrapSelection("<u>", "</u>") },
                            onStrikethrough = { wrapSelection("~~") },
                            onSpoiler = { wrapSelection("||") },
                            onFontSelected = { font -> selectedFontFamily = font }
                        )
                    }
                }

                // 2. Всплывающее меню стилей форматирования Aa с подменю H1..H6
                AnimatedVisibility(
                    visible = isAaMenuOpen && !isFormulaOpen,
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
                            onSelectFormat = { format ->
                                selectedFormat = format
                                when (format) {
                                    TextStyleFormat.BOLD -> wrapSelection("**")
                                    TextStyleFormat.ITALIC -> wrapSelection("*")
                                    TextStyleFormat.UNDERLINE -> wrapSelection("<u>", "</u>")
                                    TextStyleFormat.STRIKE -> wrapSelection("~~")
                                    TextStyleFormat.SPOILER -> wrapSelection("||")
                                    TextStyleFormat.CODE -> wrapSelection("`")
                                    TextStyleFormat.QUOTE -> applyLinePrefix("> ")
                                    TextStyleFormat.CALLOUT -> applyLinePrefix(">>> ")
                                    TextStyleFormat.NORMAL -> applyLinePrefix("")
                                    TextStyleFormat.HEADING -> { /* Открывает подменю */ }
                                    TextStyleFormat.FONT -> { /* Открывает подменю */ }
                                }
                            },
                            selectedHeadingLevel = selectedHeadingLevel,
                            onSelectHeading = { level ->
                                selectedHeadingLevel = level
                                selectedFormat = TextStyleFormat.HEADING
                                applyLinePrefix("#".repeat(level) + " ")
                            },
                            onSelectFont = { fontKey ->
                                selectedFontFamily = fontKey
                            },
                            onSelectQuoteVariant = { isCollapsible ->
                                if (isCollapsible) {
                                    insertCollapsibleBlock()
                                } else {
                                    applyLinePrefix("> ")
                                }
                            },
                            onSelectCalloutVariant = { isCollapsible ->
                                if (isCollapsible) {
                                    insertCollapsibleBlock()
                                } else {
                                    applyLinePrefix(">>> ")
                                }
                            },
                            onDismiss = { isAaMenuOpen = false }
                        )
                    }
                }

                // 3. Всплывающее меню списков ≡ (макет media_1794253331200.png)
                AnimatedVisibility(
                    visible = isListMenuOpen && !isFormulaOpen,
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
                                val currentVal = undoState.textFieldValue
                                val text = currentVal.text
                                val selStart = minOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val selEnd = maxOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }
                                val targetBlock = text.substring(lineStart, lineEnd)

                                val formattedBlock = ListFormattingHelper.applyStyle(
                                    targetBlock,
                                    style,
                                    strings.collapsibleTemplateTitle,
                                    strings.collapsibleTemplateBody
                                )
                                val newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd)
                                val newCursor = (lineStart + formattedBlock.length).coerceAtMost(newText.length)
                                undoState.updateValue(TextFieldValue(newText, TextRange(newCursor)))
                            },
                            onIndent = {
                                val currentVal = undoState.textFieldValue
                                val text = currentVal.text
                                val selStart = minOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val selEnd = maxOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }
                                val targetBlock = text.substring(lineStart, lineEnd)

                                val formattedBlock = ListFormattingHelper.indent(targetBlock)
                                val newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd)
                                undoState.updateValue(TextFieldValue(newText, TextRange((lineStart + formattedBlock.length).coerceAtMost(newText.length))))
                            },
                            onUnindent = {
                                val currentVal = undoState.textFieldValue
                                val text = currentVal.text
                                val selStart = minOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val selEnd = maxOf(currentVal.selection.start, currentVal.selection.end).coerceIn(0, text.length)
                                val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }
                                val targetBlock = text.substring(lineStart, lineEnd)

                                val formattedBlock = ListFormattingHelper.unindent(targetBlock)
                                val newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd)
                                undoState.updateValue(TextFieldValue(newText, TextRange((lineStart + formattedBlock.length).coerceAtMost(newText.length))))
                            },
                            onDismiss = { isListMenuOpen = false }
                        )
                    }
                }

                // 4. Панель формулы LaTeX строго по макету media_1794253317328.png
                // Островок скрывается, на экране отображается только поле ввода + кнопка «Готово»
                if (isFormulaOpen) {
                    LatexFormulaInputPanel(
                        onInsertFormula = { formula ->
                            val space = if (undoState.currentText.isNotEmpty() && !undoState.currentText.endsWith(" ")) " " else ""
                            undoState.updateText(undoState.currentText + space + formula + " ")
                        },
                        onDismiss = { isFormulaOpen = false }
                    )
                } else {
                    // Центрированная парящая панель инструментов форматирования
                    TextFormattingToolbar(
                        isAaActive = isAaMenuOpen || selectedFormat != TextStyleFormat.NORMAL,
                        onToggleAa = {
                            isAaMenuOpen = !isAaMenuOpen
                            if (isAaMenuOpen) {
                                isListMenuOpen = false
                                isFormulaOpen = false
                                isEmojiOpen = false
                            }
                        },
                        isEmojiActive = isEmojiOpen,
                        onToggleEmoji = {
                            isEmojiOpen = !isEmojiOpen
                            if (isEmojiOpen) {
                                keyboardController?.hide()
                                isAaMenuOpen = false
                                isListMenuOpen = false
                                isFormulaOpen = false
                            }
                        },
                        isListActive = isListMenuOpen,
                        onToggleList = {
                            isListMenuOpen = !isListMenuOpen
                            if (isListMenuOpen) {
                                isAaMenuOpen = false
                                isFormulaOpen = false
                                isEmojiOpen = false
                            }
                        },
                        isFormulaActive = false,
                        onToggleFormula = {
                            isFormulaOpen = true
                            isAaMenuOpen = false
                            isListMenuOpen = false
                            isEmojiOpen = false
                        },
                        onOpenAttach = {
                            isEditorAttachOpen = true
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
                            onSend(getFinalText(), selectedFormat, selectedLanguage, resolvedAttachment)
                        }
                    )
                }

                if (isEmojiOpen && !isFormulaOpen) {
                    EmojiStickerPanel(
                        onEmojiClick = { emoji -> undoState.updateText(undoState.currentText + emoji) },
                        onStickerClick = { symbol, _ -> undoState.updateText(undoState.currentText + symbol) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    )
                }
            }
        }

        // Изолированная шторка вложений редактора текста
        EditorAttachSheet(
            visible = isEditorAttachOpen,
            onDismiss = { isEditorAttachOpen = false },
            onAttachSelected = { newAttachments ->
                attachedMediaList = attachedMediaList + newAttachments
            }
        )
    }
}

/**
 * Визуальная трансформация для редактора текста:
 * Отображает спойлер ||...|| мерцающим фиолетовым фоном сразу при вводе (WYSIWYG),
 * а также подсвечивает полужирный **...** и курсив *...* без искажения индексов курсора.
 */
private class EditorWysiwygVisualTransformation(private val scheme: AnonGramScheme) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)
        val markerStyle = SpanStyle(color = scheme.textMuted.copy(alpha = 0.55f))
        val spoilerMarkerStyle = SpanStyle(color = scheme.accent.copy(alpha = 0.55f))
        val headingMarkerStyle = SpanStyle(color = scheme.accent.copy(alpha = 0.6f))

        // 1. Спойлер ||...|| -> маркеры мягко подсвечены, скрытый текст на мягком акцентном фоне
        val spoilerRegex = Regex("\\|\\|([\\s\\S]*?)\\|\\|")
        spoilerRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start >= 4) {
                builder.addStyle(spoilerMarkerStyle, start, start + 2)
                builder.addStyle(
                    SpanStyle(
                        background = scheme.accentSoft,
                        color = scheme.textPrimary
                    ),
                    start + 2,
                    end - 2
                )
                builder.addStyle(spoilerMarkerStyle, end - 2, end)
            }
        }

        // 2. Полужирный **...**
        val boldRegex = Regex("\\*\\*([\\s\\S]*?)\\*\\*")
        boldRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start >= 4) {
                builder.addStyle(markerStyle, start, start + 2)
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                    start + 2,
                    end - 2
                )
                builder.addStyle(markerStyle, end - 2, end)
            }
        }

        // 3. Курсив *...*
        val italicRegex = Regex("(?<!\\*)\\*(?!\\*)([\\s\\S]*?)(?<!\\*)\\*(?!\\*)")
        italicRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start >= 2) {
                builder.addStyle(markerStyle, start, start + 1)
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = scheme.textPrimary),
                    start + 1,
                    end - 1
                )
                builder.addStyle(markerStyle, end - 1, end)
            }
        }

        // 4. Зачёркнутый ~~...~~
        val strikeRegex = Regex("~~([\\s\\S]*?)~~")
        strikeRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start >= 4) {
                builder.addStyle(markerStyle, start, start + 2)
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough, color = scheme.textMuted),
                    start + 2,
                    end - 2
                )
                builder.addStyle(markerStyle, end - 2, end)
            }
        }

        // 5. Подчёркнутый <u>...</u>
        val underlineRegex = Regex("<u>([\\s\\S]*?)</u>")
        underlineRegex.findAll(raw).forEach { match ->
            val fullStart = match.range.first
            val fullEnd = match.range.last + 1
            val innerStart = fullStart + 3
            val innerEnd = fullEnd - 4
            if (innerEnd >= innerStart) {
                builder.addStyle(markerStyle, fullStart, innerStart)
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline, color = scheme.textPrimary),
                    innerStart,
                    innerEnd
                )
                builder.addStyle(markerStyle, innerEnd, fullEnd)
            }
        }

        // 6. Моноширинный `...`
        val codeRegex = Regex("`([^`]+)`")
        codeRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end - start >= 2) {
                builder.addStyle(markerStyle, start, start + 1)
                builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = scheme.accentSoft.copy(alpha = 0.65f),
                        color = scheme.textPrimary
                    ),
                    start + 1,
                    end - 1
                )
                builder.addStyle(markerStyle, end - 1, end)
            }
        }

        // 7. Кастомные шрифты [font=...]...[/font]
        val fontRegex = Regex("\\[font=(sans|serif|mono|cursive)\\]([\\s\\S]*?)\\[\\/font\\]")
        fontRegex.findAll(raw).forEach { match ->
            val fullStart = match.range.first
            val fullEnd = match.range.last + 1
            val fontType = match.groupValues[1]
            val inner = match.groupValues[2]
            val innerStart = fullStart + fontType.length + 7
            val innerEnd = innerStart + inner.length
            val family = when (fontType) {
                "serif" -> FontFamily.Serif
                "mono" -> FontFamily.Monospace
                "cursive" -> FontFamily.Cursive
                else -> FontFamily.SansSerif
            }
            builder.addStyle(markerStyle, fullStart, innerStart)
            builder.addStyle(SpanStyle(fontFamily = family, color = scheme.textPrimary), innerStart, innerEnd)
            builder.addStyle(markerStyle, innerEnd, fullEnd)
        }

        // 8. Заголовки #{1,6}
        val headingRegex = Regex("(?m)^(#{1,6}\\s+)(.*)$")
        headingRegex.findAll(raw).forEach { match ->
            val prefix = match.groupValues[1]
            val headingText = match.groupValues[2]
            val prefixStart = match.range.first
            val prefixEnd = prefixStart + prefix.length
            val textEnd = prefixEnd + headingText.length

            builder.addStyle(headingMarkerStyle, prefixStart, prefixEnd)
            builder.addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = scheme.textPrimary
                ),
                prefixEnd,
                textEnd
            )
        }

        // 9. Сворачиваемые блоки ^ ...
        val collapsibleRegex = Regex("(?m)^(\\^\\s*)(.*)$")
        collapsibleRegex.findAll(raw).forEach { match ->
            val prefix = match.groupValues[1]
            val title = match.groupValues[2]
            val prefixStart = match.range.first
            val prefixEnd = prefixStart + prefix.length
            val titleEnd = prefixEnd + title.length
            builder.addStyle(headingMarkerStyle, prefixStart, prefixEnd)
            builder.addStyle(
                SpanStyle(color = scheme.accent, fontWeight = FontWeight.Bold),
                prefixEnd,
                titleEnd
            )
        }

        // 10. Цитаты > и >>>
        val quoteRegex = Regex("(?m)^((?:>>>|>)\\s*)(.*)$")
        quoteRegex.findAll(raw).forEach { match ->
            val prefix = match.groupValues[1]
            val quoteText = match.groupValues[2]
            val prefixStart = match.range.first
            val prefixEnd = prefixStart + prefix.length
            val textEnd = prefixEnd + quoteText.length
            builder.addStyle(headingMarkerStyle, prefixStart, prefixEnd)
            builder.addStyle(
                SpanStyle(color = scheme.textPrimary, fontStyle = FontStyle.Italic),
                prefixEnd,
                textEnd
            )
        }

        // 11. Подсветка выполненных пунктов чек-листа
        val checkedRegex = Regex("(?m)^(\\s*)(☑|\\[x\\]|-\\s*\\[x\\])\\s*(.*)$", RegexOption.IGNORE_CASE)
        checkedRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    color = scheme.textMuted
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
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
