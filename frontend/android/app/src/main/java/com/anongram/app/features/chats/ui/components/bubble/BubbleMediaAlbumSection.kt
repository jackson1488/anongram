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
import com.anongram.app.features.attach.ui.components.SpoilerOverlay
import com.anongram.app.features.attach.ui.components.calculateAlbumRows
import com.anongram.app.features.attach.ui.components.getRowHeightDp
import com.anongram.app.features.attach.ui.tabs.loadHdBitmap
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        ChatRealPhotoBox(
            uriString = attachment.uri,
            isVideo = attachment.isVideo,
            hasSpoiler = attachment.hasSpoiler,
            onPhotoClick = {
                if (isSelectionMode) onSelect() else onOpenPhotoAt(0)
            },
            onPhotoLongClick = onLongClick,
            modifier = if (singlePhotoStretch) {
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(14.dp))
            } else {
                Modifier
                    .width(225.dp)
                    .height(370.dp)
                    .clip(RoundedCornerShape(14.dp))
            }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        when (count) {
            2 -> {
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
            }
            3 -> {
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

        // Светится ТОЛЬКО когда выбран весь альбом целиком!
        if (isFullAlbumSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(accent.copy(alpha = 0.22f))
                    .border(2.dp, accent, RoundedCornerShape(14.dp))
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
    accent: Color = Color(0xFF00E5FF),
    onPhotoClick: () -> Unit = {},
    onPhotoLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            .background(Color(0xFF1A1A20))
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
                    .background(if (isPhotoSelected) accent else Color.Black.copy(alpha = 0.45f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isPhotoSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Выбрано",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
