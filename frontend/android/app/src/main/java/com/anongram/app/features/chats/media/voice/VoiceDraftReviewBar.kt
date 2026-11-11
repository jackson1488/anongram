package com.anongram.app.features.chats.media.voice

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Плашка предпрослушивания заблокированного голосового перед отправкой:
 * - Слева: кнопка удаления [ 🗑 ]
 * - По центру: фиолетовая полоса с реальной аудиоволной, кнопкой воспроизведения и таймером
 * - Справа: круглая фиолетовая кнопка отправки [ ✈ ]
 * - Поддерживает реальное воспроизведение аудио через MediaPlayer
 */
@Composable
fun VoiceDraftReviewBar(
    voice: com.anongram.app.features.chats.messages.domain.MessageAttachment.Voice? = null,
    durationSeconds: Int = 7,
    waveform: List<Int> = remember { listOf(4, 9, 6, 14, 8, 12, 5, 10, 7, 13, 6, 11, 4, 9, 7, 12, 8, 14, 6, 10) },
    onDiscard: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val effectiveWaveform = if (voice != null && voice.waveform.isNotEmpty()) voice.waveform else waveform
    val effectiveSeconds = if (voice != null) {
        val parts = voice.durationLabel.split(":")
        if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 7)
        } else 7
    } else durationSeconds

    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
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

    fun togglePlay() {
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
                                currentProgress = 0f
                            }
                            mediaPlayer = mp
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                        return
                    } catch (_: Exception) {
                    }
                }
            }
            // Fallback если файла нет
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            delay(50)
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                val dur = mp.duration.coerceAtLeast(1)
                currentProgress = (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            } else if (mp == null) {
                currentProgress += (0.05f / effectiveSeconds.toFloat().coerceAtLeast(1f))
                if (currentProgress >= 1f) {
                    currentProgress = 0f
                    isPlaying = false
                }
            }
        }
    }

    val elapsedSeconds = (currentProgress * effectiveSeconds).toInt()
    val elapsedLabel = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка «Удалить» (корзина слева)
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
                tint = scheme.textPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Центральная акцентная капсула с предпрослушиванием аудиоволны
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.accent.copy(alpha = 0.88f))
                .border(1.dp, scheme.accent, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка Play / Pause внутри капсулы
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(scheme.onAccent.copy(alpha = 0.22f))
                        .clickable { togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        tint = scheme.onAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = elapsedLabel,
                    color = scheme.onAccent,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Волна аудиодорожки
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = effectiveWaveform.size.coerceAtLeast(1)
                    effectiveWaveform.forEachIndexed { index, height ->
                        val barFraction = index.toFloat() / count.toFloat()
                        val isPassed = barFraction <= currentProgress
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((height * 1.5f).dp.coerceIn(4.dp, 20.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isPassed) scheme.onAccent else scheme.onAccent.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Круглая акцентная кнопка «Отправить» справа
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(scheme.accent)
                .clickable {
                    mediaPlayer?.run {
                        runCatching { stop() }
                        release()
                    }
                    mediaPlayer = null
                    onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Отправить голосовое",
                tint = scheme.onAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
