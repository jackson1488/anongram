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
import kotlin.math.roundToInt
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Панель редактирования, обрезки и предпрослушивания голосового сообщения перед отправкой (Фото 5):
 * - Слева: белая корзина [ 🗑 ] для удаления записи
 * - По центру: фиолетово-сиреневый контейнер:
 *   - Левый ползунок обрезки [ ❙ ]
 *   - Интерактивная аудиоволна с прогрессом
 *   - Кнопка воспроизведения/паузы с таймером [ ▶ 0:04 ]
 *   - Правый ползунок обрезки [ ❙ ]
 * - Справа: круглая кнопка отправки с белым самолётиком [ ➤ ]
 */
@Composable
fun VoiceTrimBar(
    voice: MessageAttachment.Voice?,
    onDiscard: () -> Unit,
    onSend: (startFraction: Float, endFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
    fallbackDurationSeconds: Int = 4,
    fallbackWaveform: List<Int> = remember { listOf(4, 9, 6, 14, 8, 12, 5, 10, 7, 13, 6, 11, 4, 9, 7, 12, 8, 14, 6, 10) }
) {
    val scheme = LocalAnonGramScheme.current
    val effectiveWaveform = if (voice != null && voice.waveform.isNotEmpty()) voice.waveform else fallbackWaveform
    val totalSeconds = if (voice != null) {
        val parts = voice.durationLabel.split(":")
        if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: fallbackDurationSeconds)
        } else fallbackDurationSeconds
    } else fallbackDurationSeconds

    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var trimStartFraction by remember { mutableFloatStateOf(0f) }
    var trimEndFraction by remember { mutableFloatStateOf(1f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(voice?.uri) {
        onDispose {
            mediaPlayer?.run {
                runCatching { stop() }
                release()
            }
            mediaPlayer = null
        }
    }

    fun seekAndPlay() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            val uriStr = voice?.uri
            if (!uriStr.isNullOrBlank()) {
                val file = File(uriStr)
                if (file.exists()) {
                    try {
                        if (mediaPlayer == null) {
                            val mp = MediaPlayer()
                            mp.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            mp.setDataSource(file.absolutePath)
                            mp.prepare()
                            mp.setOnCompletionListener {
                                isPlaying = false
                                currentProgress = trimStartFraction
                            }
                            mediaPlayer = mp
                        }
                        val mp = mediaPlayer!!
                        val targetMs = (trimStartFraction * mp.duration).toInt()
                        mp.seekTo(targetMs)
                        mp.start()
                        isPlaying = true
                        return
                    } catch (_: Exception) {
                    }
                }
            }
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying, trimEndFraction) {
        while (isActive && isPlaying) {
            delay(50)
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                val dur = mp.duration.coerceAtLeast(1)
                val prog = (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                currentProgress = prog
                if (prog >= trimEndFraction) {
                    mp.pause()
                    isPlaying = false
                    currentProgress = trimStartFraction
                }
            } else if (mp == null) {
                currentProgress += (0.05f / totalSeconds.toFloat().coerceAtLeast(1f))
                if (currentProgress >= trimEndFraction) {
                    currentProgress = trimStartFraction
                    isPlaying = false
                }
            }
        }
    }

    val displaySeconds = if (isPlaying) {
        (currentProgress * totalSeconds).toInt()
    } else {
        ((trimEndFraction - trimStartFraction) * totalSeconds).roundToInt().coerceAtLeast(1)
    }
    val displayLabel = "%d:%02d".format(displaySeconds / 60, displaySeconds % 60)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка удаления (белая корзина слева)
        IconButton(
            onClick = {
                mediaPlayer?.run {
                    runCatching { stop() }
                    release()
                }
                mediaPlayer = null
                voice?.uri?.let { runCatching { File(it).delete() } }
                onDiscard()
            },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить запись",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Фиолетово-сиреневый контейнер с волной и ползунками обрезки
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.accent.copy(alpha = 0.9f))
                .border(1.dp, scheme.accent, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val density = LocalDensity.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левый ползунок обрезки [ ❙ ]
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .pointerInput(totalWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount.x / totalWidthPx
                                trimStartFraction = (trimStartFraction + deltaFraction).coerceIn(0f, trimEndFraction - 0.1f)
                                if (currentProgress < trimStartFraction) {
                                    currentProgress = trimStartFraction
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(18.dp)
                            .background(Color(0xFF4A3464))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Кнопка воспроизведения / паузы с таймером [ ▶ 0:04 ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable { seekAndPlay() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Аудиоволна
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = effectiveWaveform.size.coerceAtLeast(1)
                    effectiveWaveform.forEachIndexed { index, height ->
                        val barFraction = index.toFloat() / count.toFloat()
                        val isInTrimZone = barFraction in trimStartFraction..trimEndFraction
                        val isPlayed = barFraction <= currentProgress

                        val barColor = when {
                            isPlayed && isInTrimZone -> Color.White
                            isInTrimZone -> Color.White.copy(alpha = 0.55f)
                            else -> Color.White.copy(alpha = 0.2f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((height * 1.5f).dp.coerceIn(4.dp, 24.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Правый ползунок обрезки [ ❙ ]
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .pointerInput(totalWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount.x / totalWidthPx
                                trimEndFraction = (trimEndFraction + deltaFraction).coerceIn(trimStartFraction + 0.1f, 1f)
                                if (currentProgress > trimEndFraction) {
                                    currentProgress = trimEndFraction
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(18.dp)
                            .background(Color(0xFF4A3464))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Круглая кнопка отправки с белым самолётиком [ ➤ ]
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
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Отправить",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
