package com.anongram.app.features.chats.ui.components.bubble

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import android.media.ExifInterface
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.components.SpoilerOverlay
import com.anongram.app.features.chats.attach.tabs.gallery.calculateAlbumRows
import com.anongram.app.features.chats.attach.tabs.gallery.getRowHeightDp
import com.anongram.app.features.chats.attach.tabs.gallery.loadHdBitmap
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val mediaSizeCache = LruCache<String, Pair<Int, Int>>(256)

private suspend fun resolveMediaDimensions(
    context: Context,
    uriString: String,
    isVideo: Boolean
): Pair<Int, Int>? {
    mediaSizeCache.get(uriString)?.let { return it }
    return withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null
        val dims: Pair<Int, Int>? = if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    if (rot == 90 || rot == 270) h to w else w to h
                } else null
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                var w = opts.outWidth
                var h = opts.outHeight
                if (w > 0 && h > 0) {
                    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                        orientation == ExifInterface.ORIENTATION_TRANSVERSE
                    ) {
                        val tmp = w
                        w = h
                        h = tmp
                    }
                    w to h
                } else null
            } catch (_: Exception) {
                null
            }
        }
        if (dims != null) {
            mediaSizeCache.put(uriString, dims)
        }
        dims
    }
}

/**
 * Одиночное сжатое фото или видео в пузыре сообщения.
 */
@Composable
internal fun ChatSinglePhotoSection(
    attachment: MessageAttachment.Photo,
    accent: Color,
    searchQuery: String,
    singlePhotoStretch: Boolean,
    isSelectionMode: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onOpenPhotoAt: (Int) -> Unit
) {
    if (attachment.uri != null) {
        val context = LocalContext.current
        val density = LocalDensity.current.density
        val dims by produceState<Pair<Int, Int>?>(
            initialValue = mediaSizeCache.get(attachment.uri),
            key1 = attachment.uri,
            key2 = attachment.isVideo
        ) {
            if (value == null) {
                value = resolveMediaDimensions(context, attachment.uri, attachment.isVideo)
            }
        }

        val sizeModifier = if (dims != null) {
            val (rawW, rawH) = dims!!
            val aspect = (rawW.toFloat() / rawH.toFloat()).coerceIn(0.35f, 3.2f)
            val maxW = if (singlePhotoStretch) 300f else 270f
            val minW = 120f
            val maxH = 380f
            val minH = 90f
            val naturalW = (rawW / density).coerceIn(minW, maxW)
            var targetW = if (singlePhotoStretch) maxW else naturalW
            var targetH = targetW / aspect
            if (targetH > maxH) {
                targetH = maxH
                targetW = (targetH * aspect).coerceIn(minW, maxW)
            } else if (targetH < minH) {
                targetH = minH
                targetW = (targetH * aspect).coerceIn(minW, maxW)
            }
            if (singlePhotoStretch && aspect >= 1.15f) {
                Modifier
                    .fillMaxWidth()
                    .height(targetH.dp)
                    .clip(RoundedCornerShape(14.dp))
            } else {
                Modifier
                    .width(targetW.dp)
                    .height(targetH.dp)
                    .clip(RoundedCornerShape(14.dp))
            }
        } else if (singlePhotoStretch) {
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(14.dp))
        } else {
            Modifier
                .width(225.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(14.dp))
        }

        ChatRealPhotoBox(
            uriString = attachment.uri,
            isVideo = attachment.isVideo,
            hasSpoiler = attachment.hasSpoiler,
            onPhotoClick = {
                if (isSelectionMode) onSelect() else onOpenPhotoAt(0)
            },
            onPhotoLongClick = onLongClick,
            modifier = sizeModifier
        )
    } else {
        var spoilerRevealed by remember { mutableStateOf(false) }
        val showSpoiler = attachment.hasSpoiler && !spoilerRevealed
        Box(
            modifier = (if (singlePhotoStretch) Modifier.fillMaxWidth() else Modifier.width(225.dp))
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(attachmentColor(attachment.colorSeed))
                .then(
                    if (showSpoiler) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { spoilerRevealed = true }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (attachment.isVideo) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (attachment.label.isNotBlank()) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(attachment.label, searchQuery, accent)
                    } else {
                        AnnotatedString(attachment.label)
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
            if (showSpoiler) {
                SpoilerOverlay(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Отрисовка альбома медиа в чате:
 * - Если выбрано одно (или не все) фото — ничего не светится, горит только галочка на выбранных фото
 * - Если выбран весь альбом целиком (isFullAlbumSelected == true) — светится весь альбом целиком
 */
@Composable
internal fun ChatMediaAlbumGrid(
    items: List<MessageAttachment.AlbumMediaItem>,
    accent: Color,
    isSelectionMode: Boolean = false,
    selectedPhotoIndices: Set<Int> = emptySet(),
    onPhotoClick: (Int) -> Unit = {},
    onPhotoLongClick: (Int) -> Unit = {}
) {
    val count = items.size
    val dividerColor = accent.copy(alpha = 0.55f)
    val isFullAlbumSelected = isSelectionMode && count > 0 && selectedPhotoIndices.size == count

    val context = LocalContext.current
    val aspects by produceState<List<Float>>(
        initialValue = items.map { item ->
            mediaSizeCache.get(item.uri)?.let { it.first.toFloat() / it.second.toFloat() } ?: 0.8f
        },
        key1 = items.map { it.uri }
    ) {
        value = items.map { item ->
            val dims = resolveMediaDimensions(context, item.uri, item.isVideo)
            if (dims != null) dims.first.toFloat() / dims.second.toFloat() else 0.8f
        }
    }
    val allVertical = aspects.all { it < 1.1f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        when (count) {
            2 -> {
                if (allVertical) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        ChatRealPhotoBox(
                            uriString = items[0].uri,
                            isVideo = items[0].isVideo,
                            hasSpoiler = items[0].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 0 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(0) },
                            onPhotoLongClick = { onPhotoLongClick(0) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(dividerColor)
                        )
                        ChatRealPhotoBox(
                            uriString = items[1].uri,
                            isVideo = items[1].isVideo,
                            hasSpoiler = items[1].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 1 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(1) },
                            onPhotoLongClick = { onPhotoLongClick(1) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    val h0 = (270f / aspects.getOrElse(0) { 1.5f }.coerceIn(0.9f, 2.5f)).coerceIn(110f, 185f)
                    val h1 = (270f / aspects.getOrElse(1) { 1.5f }.coerceIn(0.9f, 2.5f)).coerceIn(110f, 185f)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ChatRealPhotoBox(
                            uriString = items[0].uri,
                            isVideo = items[0].isVideo,
                            hasSpoiler = items[0].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 0 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(0) },
                            onPhotoLongClick = { onPhotoLongClick(0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(h0.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(dividerColor)
                        )
                        ChatRealPhotoBox(
                            uriString = items[1].uri,
                            isVideo = items[1].isVideo,
                            hasSpoiler = items[1].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 1 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(1) },
                            onPhotoLongClick = { onPhotoLongClick(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(h1.dp)
                        )
                    }
                }
            }
            3 -> {
                if (!allVertical && aspects.firstOrNull()?.let { it >= 1.1f } == true) {
                    val topH = (270f / aspects[0].coerceIn(1.1f, 2.5f)).coerceIn(120f, 185f)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ChatRealPhotoBox(
                            uriString = items[0].uri,
                            isVideo = items[0].isVideo,
                            hasSpoiler = items[0].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 0 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(0) },
                            onPhotoLongClick = { onPhotoLongClick(0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topH.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(dividerColor)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            ChatRealPhotoBox(
                                uriString = items[1].uri,
                                isVideo = items[1].isVideo,
                                hasSpoiler = items[1].hasSpoiler,
                                showSelectionBadge = isSelectionMode,
                                isPhotoSelected = 1 in selectedPhotoIndices,
                                accent = accent,
                                onPhotoClick = { onPhotoClick(1) },
                                onPhotoLongClick = { onPhotoLongClick(1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .fillMaxHeight()
                                    .background(dividerColor)
                            )
                            ChatRealPhotoBox(
                                uriString = items[2].uri,
                                isVideo = items[2].isVideo,
                                hasSpoiler = items[2].hasSpoiler,
                                showSelectionBadge = isSelectionMode,
                                isPhotoSelected = 2 in selectedPhotoIndices,
                                accent = accent,
                                onPhotoClick = { onPhotoClick(2) },
                                onPhotoLongClick = { onPhotoLongClick(2) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                ) {
                    ChatRealPhotoBox(
                        uriString = items[0].uri,
                        isVideo = items[0].isVideo,
                        hasSpoiler = items[0].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 0 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(0) },
                        onPhotoLongClick = { onPhotoLongClick(0) },
                        modifier = Modifier
                            .weight(1.12f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    ) {
                        ChatRealPhotoBox(
                            uriString = items[1].uri,
                            isVideo = items[1].isVideo,
                            hasSpoiler = items[1].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 1 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(1) },
                            onPhotoLongClick = { onPhotoLongClick(1) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(dividerColor)
                        )
                        ChatRealPhotoBox(
                            uriString = items[2].uri,
                            isVideo = items[2].isVideo,
                            hasSpoiler = items[2].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 2 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(2) },
                            onPhotoLongClick = { onPhotoLongClick(2) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
                }
            }
            4 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) {
                    ChatRealPhotoBox(
                        uriString = items[0].uri,
                        isVideo = items[0].isVideo,
                        hasSpoiler = items[0].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 0 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(0) },
                        onPhotoLongClick = { onPhotoLongClick(0) },
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.82f)
                            .fillMaxHeight()
                    ) {
                        for (idx in 1..3) {
                            if (idx > 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(dividerColor)
                                )
                            }
                            ChatRealPhotoBox(
                                uriString = items[idx].uri,
                                isVideo = items[idx].isVideo,
                                hasSpoiler = items[idx].hasSpoiler,
                                showSelectionBadge = isSelectionMode,
                                isPhotoSelected = idx in selectedPhotoIndices,
                                accent = accent,
                                onPhotoClick = { onPhotoClick(idx) },
                                onPhotoLongClick = { onPhotoLongClick(idx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
            else -> {
                val rowCounts = remember(count) { calculateAlbumRows(count) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    var itemPointer = 0
                    rowCounts.forEachIndexed { rowIndex, countInRow ->
                        if (rowIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(dividerColor)
                            )
                        }
                        val rowHeight = getRowHeightDp(countInRow)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                        ) {
                            for (c in 0 until countInRow) {
                                val currentIdx = itemPointer + c
                                if (currentIdx < items.size) {
                                    if (c > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .fillMaxHeight()
                                                .background(dividerColor)
                                        )
                                    }
                                    val m = items[currentIdx]
                                    ChatRealPhotoBox(
                                        uriString = m.uri,
                                        isVideo = m.isVideo,
                                        hasSpoiler = m.hasSpoiler,
                                        showSelectionBadge = isSelectionMode,
                                        isPhotoSelected = currentIdx in selectedPhotoIndices,
                                        accent = accent,
                                        onPhotoClick = { onPhotoClick(currentIdx) },
                                        onPhotoLongClick = { onPhotoLongClick(currentIdx) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                        itemPointer += countInRow
                    }
                }
            }
        }

        // Свечение при выделении всего альбома целиком
        if (isFullAlbumSelected) {
            val scheme = LocalAnonGramScheme.current
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scheme.accent.copy(alpha = 0.24f))
                    .border(2.dp, scheme.accent, RoundedCornerShape(14.dp))
            )
        }
    }
}

/**
 * Ячейка настоящего фото/видео в чате:
 * - При поштучном выборе (isPhotoSelected == true) сама ячейка НЕ светится, горит только кружок с галочкой
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatRealPhotoBox(
    uriString: String,
    isVideo: Boolean,
    hasSpoiler: Boolean,
    showSelectionBadge: Boolean = false,
    isPhotoSelected: Boolean = false,
    accent: Color = LocalAnonGramScheme.current.accent,
    onPhotoClick: () -> Unit = {},
    onPhotoLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    var localSpoilerRevealed by remember(uriString) { mutableStateOf(false) }
    val showSpoiler = hasSpoiler && !localSpoilerRevealed

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            if (bitmap == null) bitmap = quick?.asImageBitmap()
            val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, parsedUri) }
            if (hd != null) bitmap = hd.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .background(scheme.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (showSelectionBadge) {
                        onPhotoClick()
                    } else if (showSpoiler) {
                        localSpoilerRevealed = true
                    } else {
                        onPhotoClick()
                    }
                },
                onLongClick = onPhotoLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showSpoiler) Modifier.blur(22.dp) else Modifier)
            )
        }

        if (isVideo && !showSpoiler) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Воспроизвести",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        // В режиме выбора на отдельном фото отображается только круглая галочка (без подсветки самого фото)
        if (showSelectionBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isPhotoSelected) scheme.accent else Color.Black.copy(alpha = 0.45f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isPhotoSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Выбрано",
                        tint = scheme.onAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
