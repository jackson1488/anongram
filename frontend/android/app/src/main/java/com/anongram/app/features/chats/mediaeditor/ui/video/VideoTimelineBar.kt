package com.anongram.app.features.chats.mediaeditor.ui.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class TrimDragMode {
    LEFT,
    RIGHT,
    PLAYHEAD
}

@Composable
fun VideoTimelineBar(
    videoUri: Uri,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    currentPositionMs: Long = 0L,
    isPlaying: Boolean,
    isMuted: Boolean,
    coverUri: Uri? = null,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    onTrimFinished: ((startMs: Long, endMs: Long) -> Unit)? = null,
    onSeekPosition: ((positionMs: Long) -> Unit)? = null,
    onDraggingTimelineChange: ((Boolean) -> Unit)? = null,
    onOpenCoverPicker: () -> Unit,
    onRemoveCover: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current

    var frames by remember(videoUri) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var detectedDurationMs by remember(videoUri, durationMs) { mutableStateOf(durationMs) }

    LaunchedEffect(videoUri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val totalMs = (durStr?.toLongOrNull() ?: durationMs).coerceAtLeast(1000L)
                detectedDurationMs = totalMs
                val count = 12
                val extracted = (0 until count).mapNotNull { idx ->
                    val timeUs = (totalMs * idx / count) * 1000L
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.let { bmp ->
                            Bitmap.createScaledBitmap(bmp, 72, 72, true).asImageBitmap()
                        }
                }
                frames = extracted
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    val effectiveDuration = detectedDurationMs.coerceAtLeast(1000L)
    val effectiveEnd = if (trimEndMs > 0L) trimEndMs.coerceAtMost(effectiveDuration) else effectiveDuration
    val effectiveStart = trimStartMs.coerceIn(0L, (effectiveEnd - 400L).coerceAtLeast(0L))
    val trimmedDurationMs = (effectiveEnd - effectiveStart).coerceAtLeast(400L)
    val clampedPlayheadMs = currentPositionMs.coerceIn(effectiveStart, effectiveEnd)

    val currentStartState by rememberUpdatedState(effectiveStart)
    val currentEndState by rememberUpdatedState(effectiveEnd)
    val currentDurationState by rememberUpdatedState(effectiveDuration)
    val currentOnTrimChange by rememberUpdatedState(onTrimChange)
    val currentOnTrimFinished by rememberUpdatedState(onTrimFinished)
    val currentOnSeekPosition by rememberUpdatedState(onSeekPosition)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingTimelineChange)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Row above strip: Mute circle on left, "Изменить обложку >" pill centered, and trimmed duration badge on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, CircleShape)
                    .clickable { onToggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isMuted) strings.unmuteVideo else strings.muteVideo,
                    tint = if (isMuted) scheme.accent else scheme.textPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (coverUri != null) "✓ ${strings.changeCover}" else strings.changeCover,
                    color = if (coverUri != null) scheme.accent else scheme.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.islandBg)
                        .border(1.dp, if (coverUri != null) scheme.accent else scheme.islandBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenCoverPicker() }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )

                if (coverUri != null && onRemoveCover != null) {
                    Text(
                        text = strings.removeCover,
                        color = Color(0xFFFF453A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.islandBg)
                            .border(1.dp, Color(0x66FF453A), RoundedCornerShape(16.dp))
                            .clickable { onRemoveCover() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Live trimmed duration badge (e.g. "0:05")
            val totalSec = (trimmedDurationMs / 1000L).coerceAtLeast(1L)
            Text(
                text = String.format("%d:%02d", totalSec / 60, totalSec % 60),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Interactive Frame Timeline Strip (44.dp tall with 6.dp vertical touch padding, locks pager scroll on touch!)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.card)
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val startFrac = (effectiveStart.toFloat() / effectiveDuration).coerceIn(0f, 0.94f)
            val endFrac = (effectiveEnd.toFloat() / effectiveDuration).coerceIn(startFrac + 0.05f, 1f)
            val playheadFrac = (clampedPlayheadMs.toFloat() / effectiveDuration).coerceIn(startFrac, endFrac)
            val handleHitRadiusPx = with(density) { 26.dp.toPx() }
            val visualHandleWidthDp = 15.dp
            val visualHandleWidthPx = with(density) { visualHandleWidthDp.toPx() }
            val playheadWidthDp = 3.5.dp
            val playheadWidthPx = with(density) { playheadWidthDp.toPx() }

            // Unified gesture detector across the entire timeline track:
            // Immediately consumes touch-down and notifies parent (onDraggingTimelineChange(true)) so HorizontalPager NEVER steals the drag!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(videoUri) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            currentOnDraggingChange?.invoke(true)

                            try {
                                val dur = currentDurationState.coerceAtLeast(1000L)
                                val curStart = currentStartState
                                val curEnd = currentEndState
                                val leftHandleCenterX = (curStart.toFloat() / dur) * totalWidthPx
                                val rightHandleCenterX = (curEnd.toFloat() / dur) * totalWidthPx

                                val distLeft = abs(down.position.x - leftHandleCenterX)
                                val distRight = abs(down.position.x - rightHandleCenterX)

                                val dragMode = when {
                                    distLeft <= handleHitRadiusPx && distLeft <= distRight -> TrimDragMode.LEFT
                                    distRight <= handleHitRadiusPx -> TrimDragMode.RIGHT
                                    down.position.x in leftHandleCenterX..rightHandleCenterX -> TrimDragMode.PLAYHEAD
                                    distLeft < distRight -> TrimDragMode.LEFT
                                    else -> TrimDragMode.RIGHT
                                }

                                var workingStart = curStart
                                var workingEnd = curEnd
                                val minSpanMs = 500L.coerceAtMost(dur)

                                // If user touched inside the trim window, immediately jump playhead to touch position (clamped inside [workingStart..workingEnd])
                                if (dragMode == TrimDragMode.PLAYHEAD) {
                                    val initialSeekMs = ((down.position.x / totalWidthPx) * dur).toLong()
                                        .coerceIn(workingStart, workingEnd)
                                    currentOnSeekPosition?.invoke(initialSeekMs)
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.pressed } ?: break
                                    val dx = change.positionChange().x
                                    change.consume()
                                    if (dx != 0f || dragMode == TrimDragMode.PLAYHEAD) {
                                        val deltaMs = ((dx / totalWidthPx) * dur).toLong()
                                        when (dragMode) {
                                            TrimDragMode.LEFT -> {
                                                workingStart = (workingStart + deltaMs)
                                                    .coerceIn(0L, (workingEnd - minSpanMs).coerceAtLeast(0L))
                                                currentOnTrimChange(workingStart, workingEnd)
                                                currentOnSeekPosition?.invoke(workingStart)
                                            }
                                            TrimDragMode.RIGHT -> {
                                                workingEnd = (workingEnd + deltaMs)
                                                    .coerceIn((workingStart + minSpanMs).coerceAtMost(dur), dur)
                                                currentOnTrimChange(workingStart, workingEnd)
                                            }
                                            TrimDragMode.PLAYHEAD -> {
                                                val seekMs = ((change.position.x / totalWidthPx) * dur).toLong()
                                                    .coerceIn(workingStart, workingEnd)
                                                currentOnSeekPosition?.invoke(seekMs)
                                            }
                                        }
                                    }
                                }

                                if (dragMode != TrimDragMode.PLAYHEAD) {
                                    currentOnTrimFinished?.invoke(workingStart, workingEnd)
                                }
                            } finally {
                                currentOnDraggingChange?.invoke(false)
                            }
                        }
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    frames.forEach { frame ->
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // Dimmed outside trimmed area
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { (startFrac * totalWidthPx).toDp() })
                        .background(Color.Black.copy(alpha = 0.62f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(with(density) { ((1f - endFrac) * totalWidthPx).toDp() })
                        .background(Color.Black.copy(alpha = 0.62f))
                )

                // Active trim window border
                val trimLeftDp = with(density) { (startFrac * totalWidthPx).toDp() }
                val trimWidthDp = with(density) { ((endFrac - startFrac) * totalWidthPx).coerceAtLeast(28f).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = trimLeftDp)
                        .width(trimWidthDp)
                        .fillMaxHeight()
                        .border(2.2.dp, Color.White, RoundedCornerShape(7.dp))
                )

                // Left white handle with dark slot
                Box(
                    modifier = Modifier
                        .offset { IntOffset((startFrac * (totalWidthPx - visualHandleWidthPx)).roundToInt(), 0) }
                        .width(visualHandleWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(15.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                    )
                }

                // Right white handle with dark slot
                Box(
                    modifier = Modifier
                        .offset { IntOffset(((endFrac * totalWidthPx) - visualHandleWidthPx).coerceAtLeast(0f).roundToInt(), 0) }
                        .width(visualHandleWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(15.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                    )
                }

                // Vertical Playhead Scrubber Bar strictly clamped inside the left and right trim handles
                val minPlayheadX = (startFrac * totalWidthPx + visualHandleWidthPx).coerceAtMost(totalWidthPx - playheadWidthPx)
                val maxPlayheadX = ((endFrac * totalWidthPx) - visualHandleWidthPx - playheadWidthPx).coerceAtLeast(minPlayheadX)
                val playheadX = (playheadFrac * totalWidthPx - playheadWidthPx * 0.5f).coerceIn(minPlayheadX, maxPlayheadX)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadX.roundToInt(), 0) }
                        .width(playheadWidthDp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                        .border(0.6.dp, Color.Black.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
