package com.anongram.app.features.chats.viewer

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.viewer.i18n.ViewerI18n
import com.anongram.app.features.chats.viewer.video.ViewerVideoPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ViewerOverlayGlass = Color(0xCC0D0F14)
private val ViewerOverlayGlassExpanded = Color(0xEB0D0F14)

@Composable
internal fun ViewerTopBar(
    senderTitle: String,
    dateSubTitle: String,
    isVideo: Boolean,
    globalMediaCounterText: String?,
    activePageScale: Float,
    playbackSpeed: Float,
    isLooping: Boolean,
    isSettingsMenuOpen: Boolean,
    isMoreMenuOpen: Boolean,
    onClose: () -> Unit,
    onForwardClick: () -> Unit,
    onSettingsMenuOpenChange: (Boolean) -> Unit,
    onMoreMenuOpenChange: (Boolean) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onLoopToggled: (Boolean) -> Unit,
    onSaveToGallery: () -> Unit,
    onShowInChat: () -> Unit,
    onReply: () -> Unit,
    onShare: () -> Unit,
    onOpenInSystem: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ViewerI18n.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ViewerOverlayGlass)
                .safeStatusBarPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = senderTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateSubTitle,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Кнопка «Переслать» (как в Telegram, вместо Поделиться и Кисточки)
                IconButton(onClick = onForwardClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = strings.forward,
                        tint = Color.White,
                        modifier = Modifier
                            .size(23.dp)
                            .graphicsLayer { scaleX = -1f }
                    )
                }

                // Кнопка «⚙» (настройки скорости и зацикливания — только для видео)
                if (isVideo) {
                    Box {
                        IconButton(onClick = { onSettingsMenuOpenChange(true) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = strings.settings,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isSettingsMenuOpen,
                            onDismissRequest = { onSettingsMenuOpenChange(false) },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = scheme.card,
                            border = BorderStroke(1.dp, scheme.islandBorder),
                            modifier = Modifier.width(240.dp)
                        ) {
                            Text(
                                text = "${strings.speedTitle}: ${formatSpeedLabel(playbackSpeed)}",
                                color = scheme.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            val speedOptions = listOf(
                                0.2f to "0.2x  ${strings.speedVerySlow}",
                                0.5f to "0.5x  ${strings.speedSlow}",
                                1.0f to "1x     ${strings.speedNormal}",
                                1.5f to "1.5x  ${strings.speedFast}",
                                2.0f to "2x     ${strings.speedVeryFast}"
                            )
                            speedOptions.forEach { (speedVal, label) ->
                                val selected = kotlin.math.abs(playbackSpeed - speedVal) < 0.05f
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (selected) scheme.accent else scheme.textPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = scheme.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onSpeedSelected(speedVal)
                                        onSettingsMenuOpenChange(false)
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = scheme.divider,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.loopVideo,
                                        color = scheme.textPrimary,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Repeat,
                                        contentDescription = null,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isLooping,
                                        onCheckedChange = { onLoopToggled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = scheme.onAccent,
                                            checkedTrackColor = scheme.accent
                                        )
                                    )
                                },
                                onClick = { onLoopToggled(!isLooping) }
                            )
                        }
                    }
                }

                // Кнопка «⋮» (три точки) — без «Создать стикер», с «Открыть в...»
                Box {
                    IconButton(onClick = { onMoreMenuOpenChange(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = strings.menu,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { onMoreMenuOpenChange(false) },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = scheme.card,
                        border = BorderStroke(1.dp, scheme.islandBorder),
                        modifier = Modifier.width(235.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.saveToGallery, color = scheme.textPrimary, fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Download, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onSaveToGallery()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.showInChat, color = scheme.textPrimary, fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Reply,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onShowInChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.reply, color = scheme.textPrimary, fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onReply()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.share, color = scheme.textPrimary, fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.openIn, color = scheme.textPrimary, fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onOpenInSystem()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.delete, color = Color(0xFFFF5252), fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                onMoreMenuOpenChange(false)
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        if (globalMediaCounterText != null && activePageScale <= 1.02f) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ViewerOverlayGlass)
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = globalMediaCounterText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun ViewerBottomBar(
    caption: String,
    isCaptionExpanded: Boolean,
    onToggleCaptionExpanded: (Boolean) -> Unit,
    albumUris: List<String>,
    albumVideoFlags: List<Boolean>,
    activePosInAlbum: Int,
    thumbListState: LazyListState,
    onSelectThumb: (Int) -> Unit,
    isVideo: Boolean,
    videoPlaybackState: ViewerVideoPlaybackState,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ViewerI18n.current
    val captionScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isCaptionExpanded) ViewerOverlayGlassExpanded else ViewerOverlayGlass,
                shape = if (isCaptionExpanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RoundedCornerShape(0.dp)
            )
            .safeNavigationBarPadding()
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (caption.isNotBlank()) {
            val isLongCaption = caption.length > 80
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (isLongCaption) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onToggleCaptionExpanded(!isCaptionExpanded) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.45f))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isCaptionExpanded) {
                                Modifier
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(captionScroll)
                            } else {
                                Modifier.clickable {
                                    if (isLongCaption) onToggleCaptionExpanded(true)
                                }
                            }
                        )
                ) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 19.sp,
                        maxLines = if (isCaptionExpanded) Int.MAX_VALUE else 3,
                        overflow = if (isCaptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }

                if (!isCaptionExpanded && isLongCaption) {
                    Text(
                        text = strings.showAllCaption,
                        color = scheme.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable { onToggleCaptionExpanded(true) }
                    )
                } else if (isCaptionExpanded && isLongCaption) {
                    Text(
                        text = strings.collapseCaption,
                        color = scheme.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { onToggleCaptionExpanded(false) }
                    )
                }
            }
        }

        if (albumUris.size > 1) {
            LazyRow(
                state = thumbListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                itemsIndexed(albumUris) { thumbIdx, uriStr ->
                    val isSelected = (thumbIdx == activePosInAlbum)
                    val isThumbVideo = albumVideoFlags.getOrElse(thumbIdx) { false }
                    val thumbWidth by animateDpAsState(
                        targetValue = if (isSelected) 48.dp else 36.dp,
                        animationSpec = tween(200),
                        label = "thumbW"
                    )
                    val thumbHeight by animateDpAsState(
                        targetValue = if (isSelected) 54.dp else 46.dp,
                        animationSpec = tween(200),
                        label = "thumbH"
                    )

                    ViewerMiniThumbnail(
                        uriString = uriStr,
                        isVideo = isThumbVideo,
                        isSelected = isSelected,
                        onClick = { onSelectThumb(thumbIdx) },
                        modifier = Modifier.size(width = thumbWidth, height = thumbHeight)
                    )
                }
            }
        }

        // Полоса перемотки видео + таймер 00:16 / 00:28 (как на скрине 2)
        if (isVideo) {
            val durMs = videoPlaybackState.durationMs.coerceAtLeast(1L)
            val posMs = videoPlaybackState.currentPositionMs.coerceIn(0L, durMs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        videoPlaybackState.seekTo((frac * durMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatDurationMmSs(posMs)} / ${formatDurationMmSs(videoPlaybackState.durationMs)}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun ViewerMiniThumbnail(
    uriString: String,
    isVideo: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }
    var thumbBitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            thumbBitmap = quick?.asImageBitmap()
        }
    }

    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF22252E))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color.White, shape)
                } else {
                    Modifier.border(0.75.dp, Color.White.copy(alpha = 0.22f), shape)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (thumbBitmap != null) {
            Image(
                bitmap = thumbBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
internal fun ViewerForwardChoiceDialog(
    albumCount: Int,
    onDismiss: () -> Unit,
    onForwardSingle: () -> Unit,
    onForwardAll: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ViewerI18n.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.card,
        titleContentColor = scheme.textPrimary,
        textContentColor = scheme.textSecondary,
        title = {
            Text(
                text = strings.forwardDialogTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = strings.forwardDialogMessage,
                fontSize = 15.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = Color(0xFFFF5252))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onForwardSingle) {
                    Text(strings.forwardSingleMedia, color = scheme.accent)
                }
                TextButton(onClick = onForwardAll) {
                    Text(strings.forwardAllMedia(albumCount), color = scheme.accent)
                }
            }
        }
    )
}

private fun formatSpeedLabel(speed: Float): String {
    return when {
        kotlin.math.abs(speed - 0.2f) < 0.05f -> "0.2x"
        kotlin.math.abs(speed - 0.5f) < 0.05f -> "0.5x"
        kotlin.math.abs(speed - 1.5f) < 0.05f -> "1.5x"
        kotlin.math.abs(speed - 2.0f) < 0.05f -> "2x"
        else -> "1x"
    }
}

private fun formatDurationMmSs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}
