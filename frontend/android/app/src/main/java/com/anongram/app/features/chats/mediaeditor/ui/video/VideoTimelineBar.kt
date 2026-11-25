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
    coverUri: Uri? = null,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
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
    val effectiveStart = trimStartMs.coerceIn(0L, (effectiveEnd - 500L).coerceAtLeast(0L))

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Row above strip: Mute circle on left, "Изменить обложку >" pill centered, and "Снять обложку" if custom cover is set
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

            Spacer(modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Compact white-bordered frame timeline strip (36.dp)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.card)
        ) {
            val density = LocalDensity.current
            val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val startFrac = (effectiveStart.toFloat() / effectiveDuration).coerceIn(0f, 0.94f)
            val endFrac = (effectiveEnd.toFloat() / effectiveDuration).coerceIn(startFrac + 0.06f, 1f)

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
                    .background(Color.Black.copy(alpha = 0.65f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(with(density) { ((1f - endFrac) * totalWidthPx).toDp() })
                    .background(Color.Black.copy(alpha = 0.65f))
            )

            // Active trim window border
            val trimLeftDp = with(density) { (startFrac * totalWidthPx).toDp() }
            val trimWidthDp = with(density) { ((endFrac - startFrac) * totalWidthPx).coerceAtLeast(20f).toDp() }
            Box(
                modifier = Modifier
                    .offset(x = trimLeftDp)
                    .width(trimWidthDp)
                    .fillMaxHeight()
                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
            )

            // Left white handle with dark slot
            val handleWidthPx = with(density) { 11.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset { IntOffset((startFrac * (totalWidthPx - handleWidthPx)).roundToInt(), 0) }
                    .width(11.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(Color.White)
                    .pointerInput(effectiveDuration, effectiveEnd) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / totalWidthPx * effectiveDuration).toLong()
                            val nextStart = (effectiveStart + deltaMs).coerceIn(0L, (effectiveEnd - 500L).coerceAtLeast(0L))
                            onTrimChange(nextStart, effectiveEnd)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }

            // Right white handle with dark slot
            Box(
                modifier = Modifier
                    .offset { IntOffset(((endFrac * totalWidthPx) - handleWidthPx).coerceAtLeast(0f).roundToInt(), 0) }
                    .width(11.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Color.White)
                    .pointerInput(effectiveDuration, effectiveStart) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / totalWidthPx * effectiveDuration).toLong()
                            val nextEnd = (effectiveEnd + deltaMs).coerceIn((effectiveStart + 500L).coerceAtMost(effectiveDuration), effectiveDuration)
                            onTrimChange(effectiveStart, nextEnd)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}
