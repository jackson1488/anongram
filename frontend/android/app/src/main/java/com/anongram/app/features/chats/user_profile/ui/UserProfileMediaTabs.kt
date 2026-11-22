package com.anongram.app.features.chats.user_profile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.ui.components.bubble.openFileInSystemViewer
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import com.anongram.app.features.chats.user_profile.model.UserProfileAudioItem
import com.anongram.app.features.chats.user_profile.model.UserProfileBuckets
import com.anongram.app.features.chats.user_profile.model.UserProfileFileItem
import com.anongram.app.features.chats.user_profile.model.UserProfileLinkItem
import com.anongram.app.features.chats.user_profile.model.UserProfileMediaItem
import com.anongram.app.features.chats.user_profile.model.UserProfileTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun UserProfileTabBar(
    tabs: List<UserProfileTab>,
    selectedTab: UserProfileTab,
    hazeState: dev.chrisbanes.haze.HazeState,
    strings: UserProfileStrings,
    onSelectTab: (UserProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val scheme = LocalAnonGramScheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = scheme.card.copy(alpha = 0.50f),
                        tint = HazeTint(color = scheme.surface.copy(alpha = 0.45f)),
                        blurRadius = 20.dp,
                        noiseFactor = 0.02f
                    )
                )
                .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = when (tab) {
                    UserProfileTab.MEDIA -> strings.tabMedia
                    UserProfileTab.FILES -> strings.tabFiles
                    UserProfileTab.LINKS -> strings.tabLinks
                    UserProfileTab.GIF -> strings.tabGif
                    UserProfileTab.VOICE -> strings.tabVoice
                    UserProfileTab.AUDIO -> strings.tabAudio
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) scheme.accentSoft else Color.Transparent)
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) scheme.accent else scheme.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UserProfileMediaGridRow(
    rowItems: List<UserProfileMediaItem>,
    columns: Int,
    selectedKeys: Set<String>,
    onTilePositioned: (String, Rect) -> Unit = { _, _ -> },
    onMediaClick: (UserProfileMediaItem) -> Unit,
    onMediaLongClick: (UserProfileMediaItem) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (col in 0 until columns) {
            val item = rowItems.getOrNull(col)
            if (item != null) {
                val isSelected = item.itemKey in selectedKeys
                val tileScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 0.84f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(160),
                    label = "mediaTileScale"
                )
                val thumb by produceState<ImageBitmap?>(initialValue = null, item.uri) {
                    value = if (item.uri.isBlank()) null else withContext(Dispatchers.IO) {
                        runCatching { loadMediaThumbnail(context, Uri.parse(item.uri))?.asImageBitmap() }.getOrNull()
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(scheme.background)
                        .onGloballyPositioned { coords ->
                            onTilePositioned(item.itemKey, coords.boundsInRoot())
                        }
                        .combinedClickable(
                            onClick = { onMediaClick(item) },
                            onLongClick = { onMediaLongClick(item) }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = tileScale
                                scaleY = tileScale
                            }
                            .clip(RoundedCornerShape(if (isSelected) 10.dp else 0.dp))
                            .background(scheme.card),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isVideoCircle) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.76f)
                                    .clip(CircleShape)
                                    .background(attachmentColor(item.colorSeed))
                                    .border(1.dp, scheme.islandBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb!!,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.50f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
internal fun UserProfileFileRow(
    item: UserProfileFileItem
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openFileInSystemViewer(context, item.file) }
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.file.name,
                    color = scheme.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.file.sizeLabel.isNotBlank()) {
                    Text(
                        text = item.file.sizeLabel,
                        color = scheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        HorizontalDivider(
            color = scheme.divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

@Composable
internal fun UserProfileLinkRow(
    item: UserProfileLinkItem
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.card)
                    .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.url,
                color = scheme.accent,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            color = scheme.divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

@Composable
internal fun UserProfileAudioRow(
    item: UserProfileAudioItem,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onAudioClick: (UserProfileAudioItem) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val dateLabel = remember(item.message.epochMs) {
        if (item.message.epochMs > 0L) {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(item.message.epochMs))
        } else {
            ""
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAudioClick(item) }
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) scheme.accent else scheme.accentSoft)
                    .clickable { onAudioClick(item) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isActive && isPlaying -> Icons.Outlined.Pause
                        item.isVoice -> Icons.Outlined.PlayArrow
                        else -> if (isActive) Icons.Outlined.PlayArrow else Icons.Outlined.MusicNote
                    },
                    contentDescription = null,
                    tint = if (isActive) scheme.onAccent else scheme.accent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = if (isActive) scheme.accent else scheme.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (dateLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateLabel,
                            color = scheme.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        color = scheme.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isActive) {
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = scheme.accent,
                        trackColor = scheme.accentSoft
                    )
                }
            }
        }
        HorizontalDivider(
            color = scheme.divider,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 70.dp, end = 16.dp)
        )
    }
}

@Composable
internal fun UserProfileFastScrollBar(
    listState: LazyListState,
    itemEpochs: List<Long>,
    visible: Boolean,
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (itemEpochs.isEmpty()) return
    val scheme = LocalAnonGramScheme.current
    val density = LocalDensity.current
    var trackHeightPx by remember { mutableStateOf(1f) }
    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val usableHeightPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

    val totalItems = itemEpochs.size
    val firstIdx = listState.firstVisibleItemIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
    val autoFraction = (firstIdx.toFloat() / (totalItems - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
    var dragFraction by remember { mutableStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else autoFraction
    val thumbOffsetY = (activeFraction * usableHeightPx).roundToInt()

    val currentEpoch = itemEpochs.getOrNull(firstIdx) ?: 0L
    val dateLabel = remember(currentEpoch) {
        if (currentEpoch > 0L) {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(currentEpoch))
        } else {
            ""
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
        ) {
            if (dateLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = -with(density) { 40.dp.roundToPx() },
                                y = thumbOffsetY + with(density) { 8.dp.roundToPx() }
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.islandBg.copy(alpha = 0.96f))
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dateLabel,
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = 0, y = thumbOffsetY) }
                    .width(34.dp)
                    .height(thumbHeightDp)
                    .pointerInput(totalItems, usableHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragFraction = autoFraction
                                onDraggingChange(true)
                            },
                            onDragEnd = { onDraggingChange(false) },
                            onDragCancel = { onDraggingChange(false) },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragFraction = (dragFraction + dragAmount / usableHeightPx).coerceIn(0f, 1f)
                                val target = (dragFraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                                onScrollToIndex(target)
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isDragging) scheme.accent else scheme.islandBg.copy(alpha = 0.94f))
                        .border(1.dp, if (isDragging) scheme.accent else scheme.islandBorder, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        tint = if (isDragging) scheme.onAccent else scheme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

internal fun tabSummaryText(
    tab: UserProfileTab,
    buckets: UserProfileBuckets,
    strings: UserProfileStrings
): String = when (tab) {
    UserProfileTab.MEDIA -> strings.mediaCountFormat(buckets.media.size)
    UserProfileTab.FILES -> strings.filesCountFormat(buckets.files.size)
    UserProfileTab.LINKS -> strings.linksCountFormat(buckets.links.size)
    UserProfileTab.GIF -> strings.gifCountFormat(buckets.gifs.size)
    UserProfileTab.VOICE -> strings.voiceCountFormat(buckets.voice.size)
    UserProfileTab.AUDIO -> strings.audioCountFormat(buckets.audio.size)
}
