package com.anongram.app.features.chats.mediaeditor.ui.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

@Composable
fun VideoTimelineBar(
    videoUri: Uri,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    onOpenCoverPicker: () -> Unit,
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
                val count = 8
                val extracted = (0 until count).mapNotNull { idx ->
                    val timeUs = (totalMs * idx / count) * 1000L
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.let { bmp ->
                            Bitmap.createScaledBitmap(bmp, 96, 96, true).asImageBitmap()
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
    val effectiveStart = trimStartMs.coerceIn(0L, (effectiveEnd - 500L).coerceAtLeast(0L))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.islandBg.copy(alpha = 0.92f))
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Top controls: Play/Pause, Mute, Duration, Change Cover >
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(scheme.card)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) scheme.accent.copy(alpha = 0.18f) else scheme.card)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (isMuted) strings.unmuteVideo else strings.muteVideo,
                        tint = if (isMuted) scheme.accent else scheme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "${formatDuration(effectiveStart)} – ${formatDuration(effectiveEnd)}",
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = strings.changeCover,
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenCoverPicker() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frame strip with draggable trim handles
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.card)
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val startFrac = (effectiveStart.toFloat() / effectiveDuration).coerceIn(0f, 0.95f)
            val endFrac = (effectiveEnd.toFloat() / effectiveDuration).coerceIn(startFrac + 0.05f, 1f)

            Row(modifier = Modifier.fillMaxSize()) {
                if (frames.isNotEmpty()) {
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
            }

            // Dimmed outside trimmed area
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (startFrac * totalWidthPx).toDp() })
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(with(density) { ((1f - endFrac) * totalWidthPx).toDp() })
                    .background(Color.Black.copy(alpha = 0.6f))
            )

            // Left trim handle
            val handleWidthPx = with(density) { 14.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset { IntOffset((startFrac * (totalWidthPx - handleWidthPx)).roundToInt(), 0) }
                    .width(14.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.accent)
                    .pointerInput(effectiveDuration, effectiveEnd) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / totalWidthPx * effectiveDuration).toLong()
                            val nextStart = (effectiveStart + deltaMs).coerceIn(0L, (effectiveEnd - 500L).coerceAtLeast(0L))
                            onTrimChange(nextStart, effectiveEnd)
                        }
                    }
            )

            // Right trim handle
            Box(
                modifier = Modifier
                    .offset { IntOffset(((endFrac * totalWidthPx) - handleWidthPx).coerceAtLeast(0f).roundToInt(), 0) }
                    .width(14.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(scheme.accent)
                    .pointerInput(effectiveDuration, effectiveStart) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / totalWidthPx * effectiveDuration).toLong()
                            val nextEnd = (effectiveEnd + deltaMs).coerceIn((effectiveStart + 500L).coerceAtMost(effectiveDuration), effectiveDuration)
                            onTrimChange(effectiveStart, nextEnd)
                        }
                    }
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
