package com.anongram.app.features.chats.ui.components.bubble

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.components.SpoilerOverlay
import com.anongram.app.features.chats.attach.tabs.gallery.AlbumLayoutPlan
import com.anongram.app.features.chats.attach.tabs.gallery.computeAlbumLayoutPlan
import com.anongram.app.features.chats.attach.tabs.gallery.loadHdBitmap
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.attach.tabs.gallery.mediaSizeCache
import com.anongram.app.features.chats.attach.tabs.gallery.rememberMediaAspects
import com.anongram.app.features.chats.attach.tabs.gallery.resolveMediaDimensions
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            val aspect = (rawW.toFloat() / rawH.toFloat()).coerceIn(0.42f, 2.8f)
            if (aspect >= 1.1f) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect.coerceIn(1.1f, 2.4f))
                    .clip(RoundedCornerShape(14.dp))
            } else if (singlePhotoStretch) {
                val targetH = (325f / aspect).coerceIn(180f, 490f)
                Modifier
                    .fillMaxWidth()
                    .height(targetH.dp)
                    .clip(RoundedCornerShape(14.dp))
            } else {
                val maxW = 325f
                val minW = 190f
                val maxH = 490f
                val minH = 180f
                var targetW = maxW
                var targetH = targetW / aspect
                if (targetH > maxH) {
                    targetH = maxH
                    targetW = (targetH * aspect).coerceIn(minW, maxW)
                } else if (targetH < minH) {
                    targetH = minH
                    targetW = (targetH * aspect).coerceIn(minW, maxW)
                }
                Modifier
                    .width(targetW.dp)
                    .height(targetH.dp)
                    .clip(RoundedCornerShape(14.dp))
            }
        } else if (singlePhotoStretch) {
            Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(14.dp))
        } else {
            Modifier
                .width(305.dp)
                .height(390.dp)
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

    val aspects = rememberMediaAspects(items.map { it.uri to it.isVideo })
    val plan = computeAlbumLayoutPlan(aspects, targetWidthDp = 270f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        when (plan) {
            is AlbumLayoutPlan.TwoVertical -> {
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
            is AlbumLayoutPlan.TwoStacked -> {
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
                            .height(plan.topHeightDp.dp)
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
                            .height(plan.bottomHeightDp.dp)
                    )
                }
            }
            is AlbumLayoutPlan.ThreeLeftTall -> {
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
                        for (idx in 1..2) {
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
            is AlbumLayoutPlan.ThreeTopWide -> {
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
                            .height(plan.topHeightDp.dp)
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
                            .height(plan.bottomHeightDp.dp)
                    ) {
                        for (i in 0..1) {
                            if (i > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(dividerColor)
                                )
                            }
                            val idx = i + 1
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
                                    .weight(plan.bottomWeights.getOrElse(i) { 1f })
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
            is AlbumLayoutPlan.FourLeftTall -> {
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
            is AlbumLayoutPlan.FourTopWide -> {
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
                            .height(plan.topHeightDp.dp)
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
                            .height(plan.bottomHeightDp.dp)
                    ) {
                        for (i in 0..2) {
                            if (i > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(dividerColor)
                                )
                            }
                            val idx = i + 1
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
                                    .weight(plan.bottomWeights.getOrElse(i) { 1f })
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
            is AlbumLayoutPlan.MultiRow -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    plan.rows.forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(dividerColor)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(row.heightDp.dp)
                        ) {
                            row.indices.forEachIndexed { colIdx, currentIdx ->
                                if (currentIdx < items.size) {
                                    if (colIdx > 0) {
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
                                            .weight(row.weights.getOrElse(colIdx) { 1f })
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
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

    LaunchedEffect(parsedUri, isVideo) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            if (bitmap == null) bitmap = quick?.asImageBitmap()
            if (!isVideo) {
                val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, parsedUri) }
                if (hd != null) bitmap = hd.asImageBitmap()
            }
        }
    }

    var isVideoPrepared by remember(uriString) { mutableStateOf(false) }
    var videoDurationMs by remember(uriString) { mutableIntStateOf(0) }
    var videoProgress by remember(uriString) { mutableFloatStateOf(0f) }
    var videoRemainingMs by remember(uriString) { mutableIntStateOf(0) }

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

        if (isVideo && !showSpoiler && parsedUri != null) {
            InlineMutedVideoPreview(
                uri = parsedUri,
                onPrepared = { durMs ->
                    videoDurationMs = durMs
                    videoRemainingMs = durMs
                    isVideoPrepared = true
                },
                onProgress = { posMs, durMs ->
                    if (durMs > 0) {
                        videoDurationMs = durMs
                        videoRemainingMs = (durMs - posMs).coerceAtLeast(0)
                        videoProgress = (posMs.toFloat() / durMs).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isVideoPrepared) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (videoDurationMs > 0) {
                val totalSec = (videoRemainingMs / 1000).coerceAtLeast(0)
                val badgeText = String.format("%d:%02d", totalSec / 60, totalSec % 60)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.52f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Outlined.VolumeOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else if (isVideo && !showSpoiler) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
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

@Composable
private fun InlineMutedVideoPreview(
    uri: Uri,
    onPrepared: (Int) -> Unit,
    onProgress: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember(uri) { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(mediaPlayer) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        while (true) {
            runCatching {
                if (mp.isPlaying) {
                    onProgress(mp.currentPosition, mp.duration)
                }
            }
            delay(100L)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
            mediaPlayer = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        val mp = MediaPlayer()
                        mediaPlayer = mp
                        runCatching {
                            mp.setDataSource(ctx, uri)
                            mp.setSurface(Surface(surface))
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            mp.setOnVideoSizeChangedListener { _, videoW, videoH ->
                                if (videoW > 0 && videoH > 0 && width > 0 && height > 0) {
                                    val viewRatio = width.toFloat() / height
                                    val videoRatio = videoW.toFloat() / videoH
                                    val scaleX: Float
                                    val scaleY: Float
                                    if (videoRatio > viewRatio) {
                                        scaleX = videoRatio / viewRatio
                                        scaleY = 1f
                                    } else {
                                        scaleX = 1f
                                        scaleY = viewRatio / videoRatio
                                    }
                                    val matrix = Matrix()
                                    matrix.setScale(scaleX, scaleY, width / 2f, height / 2f)
                                    setTransform(matrix)
                                }
                            }
                            mp.setOnPreparedListener { preparedMp ->
                                preparedMp.setVolume(0f, 0f)
                                preparedMp.isLooping = true
                                preparedMp.start()
                                onPrepared(preparedMp.duration)
                            }
                            mp.prepareAsync()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        runCatching {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        }
                        mediaPlayer = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        }
    )
}
