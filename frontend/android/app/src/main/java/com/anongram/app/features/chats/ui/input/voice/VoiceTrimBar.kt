package com.anongram.app.features.chats.ui.input.voice

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.mutableIntStateOf
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.media.common.MediaTrimmer
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Панель редактирования, обрезки и предпрослушивания голосового сообщения:
 * - Слева: корзина [ 🗑 ] для удаления / отмены
 * - По центру: [ ▶ 0:04 ] + интерактивная волна с ДВУМЯ подвижными ползунками обрезки (левый и правый)
 *   и возможностью перемотки по волне в любую точку
 * - Справа: кнопка отправки [ ➤ ] (или сохранения [ ✓ ] в режиме редактирования отправленного ГС)
 */
@Composable
fun VoiceTrimBar(
    voice: MessageAttachment.Voice?,
    onDiscard: () -> Unit,
    onSend: (startFraction: Float, endFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
    isEditingMode: Boolean = false,
    fallbackDurationSeconds: Int = 4,
    fallbackWaveform: List<Int> = remember { listOf(4, 9, 6, 14, 8, 12, 5, 10, 7, 13, 6, 11, 4, 9, 7, 12, 8, 14, 6, 10) }
) {
    val scheme = LocalAnonGramScheme.current
    val density = LocalDensity.current
    val effectiveWaveform = if (voice != null && voice.waveform.isNotEmpty()) voice.waveform else fallbackWaveform

    val fallbackSec = remember(voice?.durationLabel, fallbackDurationSeconds) {
        if (voice != null) {
            val parts = voice.durationLabel.split(":")
            if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: fallbackDurationSeconds)
            } else fallbackDurationSeconds
        } else fallbackDurationSeconds
    }

    var actualDurationMs by remember(voice?.uri) { mutableIntStateOf(fallbackSec.coerceAtLeast(1) * 1000) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember(voice?.uri) { mutableFloatStateOf(0f) }
    var trimStartFraction by remember(voice?.uri) { mutableFloatStateOf(0f) }
    var trimEndFraction by remember(voice?.uri) { mutableFloatStateOf(1f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(voice?.uri) {
        val path = voice?.uri
        if (!path.isNullOrBlank()) {
            val ms = withContext(Dispatchers.IO) {
                MediaTrimmer.readDurationMs(File(path))
            }
            if (ms > 0L) actualDurationMs = ms.toInt()
        }
    }

    DisposableEffect(voice?.uri) {
        onDispose {
            mediaPlayer?.run {
                runCatching { stop() }
                release()
            }
            mediaPlayer = null
        }
    }

    fun ensurePlayer(): MediaPlayer? {
        mediaPlayer?.let { return it }
        val uriStr = voice?.uri ?: return null
        val file = File(uriStr)
        if (!file.exists()) return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                if (duration > 0) actualDurationMs = duration
                setOnCompletionListener {
                    isPlaying = false
                    currentProgress = trimStartFraction
                }
                mediaPlayer = this
            }
        } catch (_: Exception) {
            null
        }
    }

    fun seekToFraction(frac: Float) {
        val clamped = frac.coerceIn(trimStartFraction, trimEndFraction)
        currentProgress = clamped
        val mp = mediaPlayer
        if (mp != null) {
            val dur = mp.duration.takeIf { it > 0 } ?: actualDurationMs
            runCatching { mp.seekTo((clamped * dur).toInt()) }
        }
    }

    fun togglePlayPause() {
        if (isPlaying) {
            runCatching { mediaPlayer?.pause() }
            isPlaying = false
        } else {
            val mp = ensurePlayer()
            if (mp != null) {
                val dur = mp.duration.takeIf { it > 0 } ?: actualDurationMs
                val startPos = if (currentProgress < trimStartFraction || currentProgress >= trimEndFraction - 0.01f) {
                    trimStartFraction
                } else {
                    currentProgress
                }
                currentProgress = startPos
                runCatching {
                    mp.seekTo((startPos * dur).toInt())
                    mp.start()
                }
                isPlaying = true
            } else {
                if (currentProgress < trimStartFraction || currentProgress >= trimEndFraction) {
                    currentProgress = trimStartFraction
                }
                isPlaying = true
            }
        }
    }

    LaunchedEffect(isPlaying, trimEndFraction, trimStartFraction) {
        while (isActive && isPlaying) {
            delay(50)
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                val dur = mp.duration.coerceAtLeast(1)
                val prog = (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                currentProgress = prog
                if (prog >= trimEndFraction) {
                    runCatching { mp.pause() }
                    isPlaying = false
                    currentProgress = trimStartFraction
                }
            } else if (mp == null) {
                val step = 50f / actualDurationMs.toFloat().coerceAtLeast(1000f)
                currentProgress += step
                if (currentProgress >= trimEndFraction) {
                    currentProgress = trimStartFraction
                    isPlaying = false
                }
            }
        }
    }

    val totalSec = (actualDurationMs / 1000f).coerceAtLeast(1f)
    val displaySeconds = if (isPlaying) {
        ((currentProgress - trimStartFraction).coerceAtLeast(0f) * totalSec).toInt()
    } else {
        ((trimEndFraction - trimStartFraction) * totalSec).roundToInt().coerceAtLeast(1)
    }
    val displayLabel = "%d:%02d".format(displaySeconds / 60, displaySeconds % 60)

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
        // Кнопка удаления / сброса слева
        IconButton(
            onClick = {
                mediaPlayer?.run {
                    runCatching { stop() }
                    release()
                }
                mediaPlayer = null
                onDiscard()
            },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.deleteRecording,
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
                    .clickable { togglePlayPause() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.pause else com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current.listen,
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

            // Дорожка аудиоволны с двумя подвижными ползунками обрезки
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

                // Волна по всей ширине дорожки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = handleWidthDp / 2f)
                        .pointerInput(usableWidthPx, trimStartFraction, trimEndFraction) {
                            detectTapGestures { offset ->
                                val frac = ((offset.x - handleWidthPx / 2f) / usableWidthPx).coerceIn(0f, 1f)
                                seekToFraction(frac)
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = effectiveWaveform.size.coerceAtLeast(1)
                    effectiveWaveform.forEachIndexed { index, height ->
                        val barFraction = index.toFloat() / count.toFloat()
                        val isInTrimZone = barFraction in trimStartFraction..trimEndFraction
                        val isPlayed = barFraction <= currentProgress && barFraction >= trimStartFraction

                        val barColor = when {
                            isPlayed && isInTrimZone -> scheme.onAccent
                            isInTrimZone -> scheme.onAccent.copy(alpha = 0.60f)
                            else -> scheme.onAccent.copy(alpha = 0.20f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((height * 1.45f).dp.coerceIn(4.dp, 24.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                // Рамка активного выделенного фрагмента между левым и правым ползунком
                val leftXPx = (trimStartFraction * usableWidthPx).roundToInt()
                val rightXPx = (handleWidthPx + trimEndFraction * usableWidthPx).roundToInt()
                val activeWidthDp = with(density) { (rightXPx - leftXPx + handleWidthPx.roundToInt()).toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftXPx, 0) }
                        .width(activeWidthDp)
                        .height(34.dp)
                        .border(1.5.dp, scheme.onAccent.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                )

                // Подвижный левый ползунок [ ❙ ]
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftXPx, 0) }
                        .size(width = handleWidthDp, height = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(scheme.onAccent)
                        .pointerInput(usableWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount.x / usableWidthPx
                                trimStartFraction = (trimStartFraction + deltaFraction).coerceIn(0f, trimEndFraction - minSpan)
                                if (currentProgress < trimStartFraction) {
                                    seekToFraction(trimStartFraction)
                                }
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
                        .background(scheme.onAccent)
                        .pointerInput(usableWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount.x / usableWidthPx
                                trimEndFraction = (trimEndFraction + deltaFraction).coerceIn(trimStartFraction + minSpan, 1f)
                                if (currentProgress > trimEndFraction) {
                                    seekToFraction(trimEndFraction)
                                }
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

        // Круглая кнопка отправки [ ➤ ] или подтверждения правки [ ✓ ]
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(scheme.accent)
                .clickable {
                    mediaPlayer?.run {
                        runCatching { stop() }
                        release()
                    }
                    mediaPlayer = null
                    onSend(trimStartFraction, trimEndFraction)
                },
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
