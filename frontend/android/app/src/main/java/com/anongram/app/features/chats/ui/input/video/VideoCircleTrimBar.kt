package com.anongram.app.features.chats.ui.input.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Панель предпросмотра и обрезки видео-кружочка (после нажатия Стоп/Пауза или при редактировании):
 * - Слева: корзина [ 🗑 ] для удаления
 * - По центру: [ ▶ 0:04 ] + лента раскадровки с ДВУМЯ подвижными ползунками обрезки в обе стороны и перемоткой
 * - Справа: кнопка отправки [ ➤ ] (или сохранения [ ✓ ] при редактировании)
 */
@Composable
fun VideoCircleTrimBar(
    video: MessageAttachment.VideoCircle,
    durationMs: Long,
    isPlaying: Boolean,
    currentProgress: Float,
    trimStartFraction: Float,
    trimEndFraction: Float,
    onTogglePlay: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onTrimStartChange: (Float) -> Unit,
    onTrimEndChange: (Float) -> Unit,
    onDiscard: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    isEditingMode: Boolean = false
) {
    val scheme = LocalAnonGramScheme.current
    val density = LocalDensity.current

    val thumbFrames by produceState<List<Bitmap>>(initialValue = emptyList(), key1 = video.uri) {
        val path = video.uri
        if (!path.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                val file = File(path)
                if (!file.exists()) return@withContext emptyList()
                val mmr = MediaMetadataRetriever()
                val frames = mutableListOf<Bitmap>()
                try {
                    mmr.setDataSource(file.absolutePath)
                    val durUs = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 1000L) * 1000L
                    val count = 6
                    for (i in 0 until count) {
                        val tUs = (durUs * i / count).coerceAtLeast(0L)
                        mmr.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frames.add(it) }
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { mmr.release() }
                }
                frames
            }
        }
    }

    val totalSec = (durationMs / 1000f).coerceAtLeast(1f)
    val displaySec = if (isPlaying) {
        ((currentProgress - trimStartFraction).coerceAtLeast(0f) * totalSec).toInt()
    } else {
        ((trimEndFraction - trimStartFraction) * totalSec).roundToInt().coerceAtLeast(1)
    }
    val displayLabel = "%d:%02d".format(displaySec / 60, displaySec % 60)

    val currentTrimStart by rememberUpdatedState(trimStartFraction)
    val currentTrimEnd by rememberUpdatedState(trimEndFraction)
    val currentProg by rememberUpdatedState(currentProgress)
    val updatedOnTrimStart by rememberUpdatedState(onTrimStartChange)
    val updatedOnTrimEnd by rememberUpdatedState(onTrimEndChange)
    val updatedOnSeek by rememberUpdatedState(onSeekFraction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Корзина удаления
        IconButton(
            onClick = onDiscard,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.deleteVideo,
                tint = scheme.textPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.accent.copy(alpha = 0.92f))
                .border(1.dp, scheme.accent, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.onAccent.copy(alpha = 0.20f))
                    .clickable { onTogglePlay() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.pause else com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.watch,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayLabel,
                    color = scheme.onAccent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val handleWidthDp = 14.dp
                val handleWidthPx = with(density) { handleWidthDp.toPx() }
                val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(handleWidthPx * 3f)
                val usableWidthPx = (totalWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)
                val minSpan = 0.08f

                // Полоса раскадровки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = handleWidthDp / 2f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(usableWidthPx) {
                            detectTapGestures { offset ->
                                val frac = ((offset.x - handleWidthPx / 2f) / usableWidthPx).coerceIn(0f, 1f)
                                updatedOnSeek(frac.coerceIn(currentTrimStart, currentTrimEnd))
                            }
                        }
                ) {
                    if (thumbFrames.isNotEmpty()) {
                        thumbFrames.forEach { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }

                // Затемнение обрезанных зон слева и справа
                val leftXPx = (trimStartFraction * usableWidthPx).roundToInt()
                val rightXPx = (handleWidthPx + trimEndFraction * usableWidthPx).roundToInt()
                val playheadXPx = (handleWidthPx + currentProgress.coerceIn(trimStartFraction, trimEndFraction) * usableWidthPx).roundToInt()

                if (leftXPx > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = handleWidthDp / 2f)
                            .width(with(density) { leftXPx.toDp() })
                            .height(30.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                val rightTrimWidthPx = ((1f - trimEndFraction) * usableWidthPx).roundToInt()
                if (rightTrimWidthPx > 0) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rightXPx + (handleWidthPx / 2f).roundToInt(), 0) }
                            .width(with(density) { rightTrimWidthPx.toDp() })
                            .height(30.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                val activeWidthDp = with(density) { (rightXPx - leftXPx + handleWidthPx.roundToInt()).toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftXPx, 0) }
                        .width(activeWidthDp)
                        .height(34.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                )

                // Линия текущей позиции
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadXPx, 0) }
                        .width(2.dp)
                        .height(32.dp)
                        .background(Color.White)
                )

                // Подвижный левый ползунок [ ❙ ]
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftXPx, 0) }
                        .size(width = handleWidthDp, height = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .pointerInput(usableWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val next = (currentTrimStart + dragAmount.x / usableWidthPx)
                                    .coerceIn(0f, currentTrimEnd - minSpan)
                                updatedOnTrimStart(next)
                                updatedOnSeek(next)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(16.dp)
                            .background(scheme.accent)
                    )
                }

                // Подвижный правый ползунок [ ❙ ]
                Box(
                    modifier = Modifier
                        .offset { IntOffset(rightXPx, 0) }
                        .size(width = handleWidthDp, height = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .pointerInput(usableWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val next = (currentTrimEnd + dragAmount.x / usableWidthPx)
                                    .coerceIn(currentTrimStart + minSpan, 1f)
                                updatedOnTrimEnd(next)
                                if (currentProg > next) updatedOnSeek(next)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(16.dp)
                            .background(scheme.accent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(scheme.accent)
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isEditingMode) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.Send,
                contentDescription = if (isEditingMode) com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.save else com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.send,
                tint = scheme.onAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
