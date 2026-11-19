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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
 * Чистая выезжающая снизу шторка (82% высоты экрана) для просмотра документов:
 * PDF (нативный PdfRenderer с зумом), DOCX/XLSX/TXT/Код (с выделением текста), CSV-таблицы и ZIP-архивы.
 */
@Composable
fun DocumentViewerBottomSheetHost(modifier: Modifier = Modifier) {
    val file by InAppDocumentViewerStore.activeFile.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

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
        Box(
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
            AnimatedVisibility(
                visible = file != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val activeFile = file ?: return@AnimatedVisibility
                var content by remember(activeFile.uri, activeFile.name) {
                    mutableStateOf<ViewerContent>(ViewerContent.Loading)
                }
                var pdfZoom by remember(activeFile.uri) { mutableFloatStateOf(1f) }
                var showPdfAsText by remember(activeFile.uri) { mutableStateOf(false) }

                LaunchedEffect(activeFile.uri, activeFile.name) {
                    content = loadDocumentContent(context, activeFile, isRu)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(scheme.surface)
                        .border(
                            width = 0.8.dp,
                            color = scheme.islandBorder,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .navigationBarsPadding()
                ) {
                    // Ручка сверху
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.textMuted.copy(alpha = 0.45f))
                        )
                    }

                    // Шапка шторки: имя, размер, копировать, открыть в системе, закрыть
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFile.name,
                                color = scheme.textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeFile.sizeLabel.ifBlank {
                                    activeFile.name.substringAfterLast('.', "").uppercase()
                                },
                                color = scheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }

                        val copyableText = when (val c = content) {
                            is ViewerContent.TextDoc -> c.text
                            is ViewerContent.CsvTable -> c.rawText
                            is ViewerContent.PdfPages -> c.extractedText
                            else -> ""
                        }
                        if (copyableText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText(activeFile.name, copyableText))
                                    Toast.makeText(
                                        context,
                                        if (isRu) "Текст скопирован" else "Copied",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = if (isRu) "Копировать" else "Copy",
                                    tint = scheme.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { openFileInSystemViewer(context, activeFile) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = if (isRu) "Открыть в приложении" else "Open with",
                                tint = scheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { InAppDocumentViewerStore.close() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = if (isRu) "Закрыть" else "Close",
                                tint = scheme.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Панель управления PDF (зум + переключение на выделяемый текст)
                    if (content is ViewerContent.PdfPages) {
                        val pdf = content as ViewerContent.PdfPages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isRu) "Стр: ${pdf.pages.size}" else "Pages: ${pdf.pages.size}",
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (pdf.extractedText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (showPdfAsText) scheme.accent else scheme.card)
                                        .clickable { showPdfAsText = !showPdfAsText }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (showPdfAsText) {
                                            if (isRu) "Страницы" else "Pages"
                                        } else {
                                            if (isRu) "Текст" else "Text"
                                        },
                                        color = if (showPdfAsText) scheme.onAccent else scheme.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (!showPdfAsText) {
                                Icon(
                                    imageVector = Icons.Outlined.ZoomOut,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { pdfZoom = (pdfZoom - 0.25f).coerceAtLeast(1f) }
                                )
                                Text(
                                    text = "${(pdfZoom * 100).toInt()}%",
                                    color = scheme.textSecondary,
                                    fontSize = 11.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ZoomIn,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { pdfZoom = (pdfZoom + 0.25f).coerceAtMost(2.5f) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    // Контент документа (с поддержкой выделения текста)
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
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (pdfZoom > 1.01f) {
                                                    Modifier.horizontalScroll(rememberScrollState())
                                                } else Modifier
                                            ),
                                        contentPadding = PaddingValues(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        itemsIndexed(c.pages) { idx, bmp ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = if (pdfZoom > 1.01f) {
                                                    Modifier.width((340f * pdfZoom).dp)
                                                } else {
                                                    Modifier.fillMaxWidth()
                                                }
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
