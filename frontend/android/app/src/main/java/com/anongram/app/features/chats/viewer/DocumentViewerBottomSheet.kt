package com.anongram.app.features.chats.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.ui.components.bubble.openFileInSystemViewer
import com.anongram.app.features.services.ai.data.DocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object InAppDocumentViewerStore {
    private val _activeFile = MutableStateFlow<MessageAttachment.File?>(null)
    val activeFile: StateFlow<MessageAttachment.File?> = _activeFile.asStateFlow()

    fun open(file: MessageAttachment.File) {
        _activeFile.value = file
    }

    fun close() {
        _activeFile.value = null
    }
}

private sealed interface ViewerContent {
    data object Loading : ViewerContent
    data class PdfPages(val pages: List<Bitmap>, val extractedText: String) : ViewerContent
    data class TextDoc(val text: String, val isCode: Boolean) : ViewerContent
    data class CsvTable(val rows: List<List<String>>, val rawText: String) : ViewerContent
    data class ZipArchive(val entries: List<Pair<String, Long>>) : ViewerContent
    data class Error(val message: String) : ViewerContent
}

/**
 * Выезжающая снизу шторка (старт 80%, перетаскивание 50%..100%, свайп вниз — закрытие)
 * для просмотра PDF (жест pinch-to-zoom + двойной тап), DOCX/XLSX/TXT/кода, CSV и ZIP.
 */
@Composable
fun DocumentViewerBottomSheetHost(modifier: Modifier = Modifier) {
    val file by InAppDocumentViewerStore.activeFile.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val scope = rememberCoroutineScope()

    BackHandler(enabled = file != null) {
        InAppDocumentViewerStore.close()
    }

    AnimatedVisibility(
        visible = file != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxSize()
            .zIndex(45f)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { InAppDocumentViewerStore.close() }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx().coerceAtLeast(1f) }

            AnimatedVisibility(
                visible = file != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val activeFile = file ?: return@AnimatedVisibility
                var content by remember(activeFile.uri, activeFile.name) {
                    mutableStateOf<ViewerContent>(ViewerContent.Loading)
                }
                var showPdfAsText by remember(activeFile.uri) { mutableStateOf(false) }

                // Высота шторки: старт 0.80f, диапазон закрепления 0.50f..1.0f
                val sheetFraction = remember(activeFile.uri) { Animatable(0.80f) }

                LaunchedEffect(activeFile.uri, activeFile.name) {
                    content = loadDocumentContent(context, activeFile, isRu)
                }

                val isFullScreen = sheetFraction.value >= 0.96f
                val topRadius = if (isFullScreen) 0.dp else 22.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(sheetFraction.value.coerceIn(0.1f, 1f))
                        .clip(RoundedCornerShape(topStart = topRadius, topEnd = topRadius))
                        .background(scheme.surface)
                        .then(
                            if (!isFullScreen) {
                                Modifier.border(
                                    width = 0.7.dp,
                                    color = scheme.islandBorder,
                                    shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius)
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .then(if (isFullScreen) Modifier.statusBarsPadding() else Modifier)
                        .navigationBarsPadding()
                ) {
                    // Перетаскиваемая шапка (ручка + компактная строка управления)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(totalHeightPx) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaFrac = -dragAmount / totalHeightPx
                                        val next = (sheetFraction.value + deltaFrac).coerceIn(0.15f, 1f)
                                        scope.launch { sheetFraction.snapTo(next) }
                                    },
                                    onDragEnd = {
                                        val cur = sheetFraction.value
                                        scope.launch {
                                            when {
                                                cur < 0.42f -> InAppDocumentViewerStore.close()
                                                cur < 0.65f -> sheetFraction.animateTo(0.50f, spring(stiffness = Spring.StiffnessMediumLow))
                                                cur > 0.90f -> sheetFraction.animateTo(1.0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                else -> sheetFraction.animateTo(0.80f, spring(stiffness = Spring.StiffnessMediumLow))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            sheetFraction.animateTo(0.80f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(scheme.textMuted.copy(alpha = 0.45f))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeFile.name,
                                    color = scheme.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = buildString {
                                    append(activeFile.sizeLabel.ifBlank {
                                        activeFile.name.substringAfterLast('.', "").uppercase()
                                    })
                                    if (content is ViewerContent.PdfPages) {
                                        val pages = (content as ViewerContent.PdfPages).pages.size
                                        append(if (isRu) " · $pages стр." else " · $pages p.")
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    color = scheme.textSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1
                                )
                            }

                            if (content is ViewerContent.PdfPages && (content as ViewerContent.PdfPages).extractedText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (showPdfAsText) scheme.accent else scheme.card)
                                        .clickable { showPdfAsText = !showPdfAsText }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (showPdfAsText) {
                                            if (isRu) "Стр." else "Pages"
                                        } else {
                                            if (isRu) "Текст" else "Text"
                                        },
                                        color = if (showPdfAsText) scheme.onAccent else scheme.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            val copyableText = when (val c = content) {
                                is ViewerContent.TextDoc -> c.text
                                is ViewerContent.CsvTable -> c.rawText
                                is ViewerContent.PdfPages -> c.extractedText
                                else -> ""
                            }
                            if (copyableText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText(activeFile.name, copyableText))
                                            Toast.makeText(
                                                context,
                                                if (isRu) "Текст скопирован" else "Copied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = if (isRu) "Копировать" else "Copy",
                                        tint = scheme.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { openFileInSystemViewer(context, activeFile) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = if (isRu) "Открыть в приложении" else "Open with",
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { InAppDocumentViewerStore.close() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = if (isRu) "Закрыть" else "Close",
                                    tint = scheme.textMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    // Контент документа (с поддержкой выделения текста и жеста зума для PDF)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(scheme.background)
                    ) {
                        when (val c = content) {
                            ViewerContent.Loading -> {
                                CircularProgressIndicator(
                                    color = scheme.accent,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            is ViewerContent.Error -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = c.message,
                                        color = scheme.textSecondary,
                                        fontSize = 13.5.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(scheme.accent)
                                            .clickable { openFileInSystemViewer(context, activeFile) }
                                            .padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = if (isRu) "Открыть в приложении" else "Open in app",
                                            color = scheme.onAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            is ViewerContent.TextDoc -> {
                                SelectionContainer {
                                    Text(
                                        text = c.text,
                                        color = scheme.textPrimary,
                                        fontSize = if (c.isCode) 13.sp else 14.5.sp,
                                        lineHeight = if (c.isCode) 19.sp else 21.sp,
                                        fontFamily = if (c.isCode) FontFamily.Monospace else FontFamily.Default,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp)
                                    )
                                }
                            }

                            is ViewerContent.PdfPages -> {
                                if (showPdfAsText) {
                                    SelectionContainer {
                                        Text(
                                            text = c.extractedText,
                                            color = scheme.textPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp)
                                        )
                                    }
                                } else {
                                    var scale by remember(activeFile.uri) { mutableFloatStateOf(1f) }
                                    var offsetX by remember(activeFile.uri) { mutableFloatStateOf(0f) }
                                    var offsetY by remember(activeFile.uri) { mutableFloatStateOf(0f) }
                                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                                        val nextScale = (scale * zoomChange).coerceIn(1f, 3f)
                                        scale = nextScale
                                        if (nextScale > 1.02f) {
                                            offsetX = (offsetX + panChange.x).coerceIn(-600f * nextScale, 600f * nextScale)
                                            offsetY = (offsetY + panChange.y).coerceIn(-600f * nextScale, 600f * nextScale)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(activeFile.uri) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        if (scale > 1.1f) {
                                                            scale = 1f
                                                            offsetX = 0f
                                                            offsetY = 0f
                                                        } else {
                                                            scale = 2f
                                                        }
                                                    }
                                                )
                                            }
                                            .transformable(state = transformState)
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                    translationX = offsetX
                                                    translationY = offsetY
                                                },
                                            contentPadding = PaddingValues(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            userScrollEnabled = scale <= 1.05f
                                        ) {
                                            itemsIndexed(c.pages) { idx, bmp ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "Page ${idx + 1}",
                                                        contentScale = ContentScale.FillWidth,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White)
                                                    )
                                                    Text(
                                                        text = "${idx + 1} / ${c.pages.size}",
                                                        color = scheme.textMuted,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is ViewerContent.CsvTable -> {
                                SelectionContainer {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .horizontalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        c.rows.forEachIndexed { rIdx, row ->
                                            Row(
                                                modifier = Modifier
                                                    .background(
                                                        if (rIdx == 0) scheme.card else Color.Transparent
                                                    )
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                row.forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        color = scheme.textPrimary,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = if (rIdx == 0) FontWeight.SemiBold else FontWeight.Normal,
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .padding(horizontal = 6.dp),
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }

                            is ViewerContent.ZipArchive -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    itemsIndexed(c.entries) { _, (entryName, size) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entryName,
                                                color = scheme.textPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (size >= 1024L) "${size / 1024L} KB" else "$size B",
                                                color = scheme.textMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadDocumentContent(
    context: Context,
    file: MessageAttachment.File,
    isRu: Boolean
): ViewerContent = withContext(Dispatchers.IO) {
    val rawUri = file.uri ?: return@withContext ViewerContent.Error(
        if (isRu) "Файл недоступен локально" else "File URI is unavailable"
    )
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
        ?: return@withContext ViewerContent.Error("Invalid URI")
    val ext = file.name.substringAfterLast('.', "").lowercase()

    if (ext == "pdf") {
        val pages = renderPdfPages(context, uri)
        val extracted = DocumentParser.extractText(context, uri, false, "application/pdf", file.name).orEmpty()
        return@withContext if (pages.isNotEmpty()) {
            ViewerContent.PdfPages(pages, extracted)
        } else if (extracted.isNotBlank()) {
            ViewerContent.TextDoc(extracted, isCode = false)
        } else {
            ViewerContent.Error(if (isRu) "Не удалось открыть PDF" else "Failed to open PDF")
        }
    }

    if (ext == "zip") {
        val entries = mutableListOf<Pair<String, Long>>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { rawIn ->
                ZipInputStream(rawIn).use { zis ->
                    while (entries.size < 300) {
                        val entry = zis.nextEntry ?: break
                        if (!entry.isDirectory) {
                            entries.add(entry.name to entry.size.coerceAtLeast(0L))
                        }
                        zis.closeEntry()
                    }
                }
            }
        }
        return@withContext if (entries.isNotEmpty()) {
            ViewerContent.ZipArchive(entries)
        } else {
            ViewerContent.Error(if (isRu) "Архив пуст или повреждён" else "Empty or invalid ZIP")
        }
    }

    if (ext == "csv" || ext == "tsv") {
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull().orEmpty()
        if (raw.isNotBlank()) {
            val delim = if (ext == "tsv") '\t' else ','
            val rows = raw.lineSequence()
                .filter { it.isNotBlank() }
                .take(250)
                .map { line -> line.split(delim).take(20) }
                .toList()
            return@withContext ViewerContent.CsvTable(rows, raw)
        }
    }

    val extracted = DocumentParser.extractText(context, uri, false, "", file.name)
    if (!extracted.isNullOrBlank()) {
        val isCode = ext in setOf("kt", "java", "py", "js", "ts", "json", "xml", "html", "css", "sh", "yaml", "yml", "sql", "c", "cpp", "rs", "go")
        return@withContext ViewerContent.TextDoc(extracted, isCode = isCode)
    }

    ViewerContent.Error(
        if (isRu) "Формат .$ext не поддерживается во встроенном просмотре" else "Format .$ext is not supported inline"
    )
}

private fun renderPdfPages(context: Context, uri: Uri): List<Bitmap> {
    val tmp = File(context.cacheDir, "viewer_tmp_${System.currentTimeMillis()}.pdf")
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { out -> input.copyTo(out) }
        } ?: return emptyList()
        ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val maxPages = minOf(renderer.pageCount, 30)
                (0 until maxPages).mapNotNull { idx ->
                    renderer.openPage(idx).use { page ->
                        val targetW = 1080
                        val targetH = (targetW.toFloat() * page.height / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        tmp.delete()
    }
}
