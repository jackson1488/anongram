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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

object InAppDocumentViewerStore {
    private val _activeFile = MutableStateFlow<MessageAttachment.File?>(null)
    val activeFile: StateFlow<MessageAttachment.File?> = _activeFile.asStateFlow()

    private val supportedExtensions = setOf(
        "pdf", "docx", "xlsx", "csv", "tsv", "zip",
        "txt", "md", "log", "json", "xml", "html", "htm", "css",
        "js", "ts", "kt", "java", "py", "sh", "yaml", "yml",
        "sql", "c", "cpp", "h", "hpp", "rs", "go",
        "ini", "cfg", "conf", "env", "properties", "gradle", "kts", "toml", "rtf"
    )

    fun canPreview(file: MessageAttachment.File): Boolean {
        if (file.uri.isNullOrBlank()) return false
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return ext in supportedExtensions
    }

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

private val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val SmoothInOutEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * Плавная шторка просмотра документов (физика как в AttachSheet: offsetPx + NestedScrollConnection):
 * - старт на 80% высоты, перетаскивание 50%..100%, закрытие свайпом вниз;
 * - автоскрытие шапки при прокрутке вниз;
 * - двойное касание экрана переключает иммерсивный полноэкранный режим (скрывает шапку + 100%);
 * - в PDF зум выполняется только жестом двумя пальцами (pinch-to-zoom);
 * - при ошибке чтения автоматически открывает системный просмотрщик.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DocumentViewerBottomSheetHost(modifier: Modifier = Modifier) {
    val file by InAppDocumentViewerStore.activeFile.collectAsState()
    val activeFile = file ?: return

    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(45f)
    ) {
        val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val openOffset = totalHeightPx * 0.20f // 80% видимой высоты
        val halfOffset = totalHeightPx * 0.50f // 50% видимой высоты
        val fullOffset = 0f                    // 100% экрана

        var offsetPx by remember(activeFile.uri, totalHeightPx) { mutableFloatStateOf(totalHeightPx) }
        var isClosing by remember(activeFile.uri) { mutableStateOf(false) }
        var animJob by remember(activeFile.uri) { mutableStateOf<Job?>(null) }
        var isHeaderVisible by remember(activeFile.uri) { mutableStateOf(true) }

        fun animateOffsetTo(
            target: Float,
            durationMs: Int = 260,
            isFling: Boolean = false,
            onEnd: () -> Unit = {}
        ) {
            animJob?.cancel()
            val easing = if (isFling) DecelerateEasing else SmoothInOutEasing
            animJob = scope.launch {
                animate(
                    initialValue = offsetPx,
                    targetValue = target,
                    animationSpec = tween(durationMs, easing = easing)
                ) { v, _ -> offsetPx = v }
                onEnd()
            }
        }

        fun dismissAnimated() {
            if (isClosing) return
            isClosing = true
            animateOffsetTo(totalHeightPx, durationMs = 220, isFling = true) {
                InAppDocumentViewerStore.close()
            }
        }

        fun releaseSheet(velocityY: Float) {
            when {
                velocityY > 1800f && offsetPx > openOffset * 0.8f -> dismissAnimated()
                velocityY < -1800f -> animateOffsetTo(fullOffset, durationMs = 240, isFling = true)
                offsetPx > totalHeightPx * 0.58f -> dismissAnimated()
                offsetPx > (openOffset + halfOffset) / 2f -> {
                    isHeaderVisible = true
                    animateOffsetTo(halfOffset, durationMs = 220)
                }
                offsetPx < (openOffset + fullOffset) / 2f -> {
                    animateOffsetTo(fullOffset, durationMs = 220)
                }
                else -> {
                    isHeaderVisible = true
                    animateOffsetTo(openOffset, durationMs = 220)
                }
            }
        }

        fun toggleImmersiveFullscreen() {
            val isCurrentlyFullscreen = offsetPx <= totalHeightPx * 0.05f && !isHeaderVisible
            if (isCurrentlyFullscreen) {
                isHeaderVisible = true
                animateOffsetTo(openOffset, durationMs = 260)
            } else {
                isHeaderVisible = false
                animateOffsetTo(fullOffset, durationMs = 260)
            }
        }

        LaunchedEffect(activeFile.uri, totalHeightPx) {
            isClosing = false
            isHeaderVisible = true
            animateOffsetTo(openOffset, durationMs = 280, isFling = true)
        }

        BackHandler {
            dismissAnimated()
        }

        var content by remember(activeFile.uri, activeFile.name) {
            mutableStateOf<ViewerContent>(ViewerContent.Loading)
        }
        var showPdfAsText by remember(activeFile.uri) { mutableStateOf(false) }

        LaunchedEffect(activeFile.uri, activeFile.name) {
            content = loadDocumentContent(context, activeFile, isRu)
        }

        // Автоматический системный фолбэк, если встроенный просмотр не смог разобрать файл
        LaunchedEffect(content) {
            if (content is ViewerContent.Error) {
                InAppDocumentViewerStore.close()
                openFileInSystemViewer(context, activeFile)
            }
        }

        val nestedScrollConnection = remember(totalHeightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val dy = available.y
                    // При скролле вверх (dy < 0) сначала разворачиваем шторку к 100%, если она ниже fullOffset
                    if (dy < 0f && offsetPx > fullOffset) {
                        animJob?.cancel()
                        val next = (offsetPx + dy).coerceAtLeast(fullOffset)
                        val used = next - offsetPx
                        offsetPx = next
                        return Offset(0f, used)
                    }
                    // Управление автоскрытием шапки по направлению прокрутки контента
                    if (dy < -8f && isHeaderVisible) {
                        isHeaderVisible = false
                    } else if (dy > 10f && !isHeaderVisible) {
                        isHeaderVisible = true
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val dy = available.y
                    // Если список упёрся в верх и пользователь тянет дальше вниз — тянем всю шторку
                    if (dy > 0f) {
                        animJob?.cancel()
                        isHeaderVisible = true
                        val next = (offsetPx + dy).coerceAtMost(totalHeightPx)
                        val used = next - offsetPx
                        offsetPx = next
                        return Offset(0f, used)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (offsetPx > fullOffset && offsetPx < totalHeightPx) {
                        releaseSheet(available.y)
                    }
                    return Velocity.Zero
                }
            }
        }

        val scrimAlpha = (0.48f * (1f - (offsetPx / totalHeightPx).coerceIn(0f, 1f))).coerceIn(0f, 0.48f)
        val isFullScreen = offsetPx <= totalHeightPx * 0.04f
        val topRadius = if (isFullScreen) 0.dp else 22.dp

        // Затемнённый фон
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissAnimated() }
                )
        )

        // Сама шторка, позиционируемая через IntOffset без re-measure контента
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetPx.roundToInt()) }
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
                .nestedScroll(nestedScrollConnection)
                .then(if (isFullScreen) Modifier.statusBarsPadding() else Modifier)
                .navigationBarsPadding()
        ) {
            // Перетаскиваемая верхняя область (ручка + скрываемая при прокрутке шапка)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(totalHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { animJob?.cancel() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                offsetPx = (offsetPx + dragAmount).coerceIn(fullOffset, totalHeightPx)
                                if (dragAmount > 4f) isHeaderVisible = true
                            },
                            onDragEnd = { releaseSheet(0f) },
                            onDragCancel = { animateOffsetTo(openOffset, durationMs = 200) }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isHeaderVisible = !isHeaderVisible }
                        )
                        .padding(top = 8.dp, bottom = 6.dp),
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

                AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = expandVertically(tween(180, easing = SmoothInOutEasing)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(180, easing = SmoothInOutEasing)) + fadeOut(tween(120))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 10.dp, top = 0.dp, bottom = 8.dp),
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
                                    .clickable { dismissAnimated() },
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

                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                    }
                }
            }

            // Контент документа (двойной тап переключает скрытие шапки + 100% экран)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.background)
                    .pointerInput(activeFile.uri) {
                        detectTapGestures(
                            onDoubleTap = { toggleImmersiveFullscreen() }
                        )
                    }
            ) {
                when (val c = content) {
                    ViewerContent.Loading -> {
                        CircularProgressIndicator(
                            color = scheme.accent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is ViewerContent.Error -> {
                        // Перенаправляется в системный просмотрщик через LaunchedEffect(content)
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
                                val nextScale = (scale * zoomChange).coerceIn(1f, 3.5f)
                                scale = nextScale
                                if (nextScale > 1.02f) {
                                    offsetX = (offsetX + panChange.x).coerceIn(-650f * nextScale, 650f * nextScale)
                                    offsetY = (offsetY + panChange.y).coerceIn(-650f * nextScale, 650f * nextScale)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .transformable(
                                        state = transformState,
                                        canPan = { scale > 1.02f }
                                    )
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
