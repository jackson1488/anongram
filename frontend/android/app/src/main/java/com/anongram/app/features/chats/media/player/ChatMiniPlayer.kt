package com.anongram.app.features.chats.media.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Плавающий мини-плеер музыки чата (Фото 2 и 3):
 * - Слева: кнопка воспроизведения / паузы
 * - По центру: бегущая строка Marquee («Исполнитель — Название»)
 * - Справа: крестик закрытия плеера [ X ]
 * - Снизу: тонкий фиолетовый индикатор прогресса (1.5 dp)
 * - По клику на тело: открывает адаптивную шторку треков чата
 */
@Composable
fun ChatMiniPlayer(
    controller: ChatAudioController? = null,
    onOpenSheet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ctrl = controller ?: ChatAudioController.getInstance(context)
    val state by ctrl.state.collectAsState()
    val scheme = LocalAnonGramScheme.current

    AnimatedVisibility(
        visible = state.isVisible && state.currentTrack != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val track = state.currentTrack ?: return@AnimatedVisibility
        val label = if (track.artist.isNotBlank()) "${track.artist} — ${track.title}" else track.title

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                .clickable {
                    onOpenSheet?.invoke()
                    ctrl.openSheet()
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка Play / Pause
                    IconButton(
                        onClick = { ctrl.toggle() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(scheme.accent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Пауза" else "Слушать",
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Название трека и исполнитель с циклической бегущей строкой
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        MarqueeText(
                            text = label,
                            color = scheme.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка закрытия [ X ]
                    IconButton(
                        onClick = { ctrl.stop() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Закрыть плеер",
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Нижняя тонкая линия прогресса
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(scheme.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress)
                            .height(2.dp)
                            .background(scheme.accent)
                    )
                }
            }
        }
    }
}
