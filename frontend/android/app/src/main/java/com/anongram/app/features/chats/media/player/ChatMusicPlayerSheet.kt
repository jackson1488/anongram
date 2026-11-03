package com.anongram.app.features.chats.media.player

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Адаптивная шторка музыкального плеера чата (Фото 4 и 5):
 * - Список треков ТОЛЬКО текущего чата
 * - Адаптивная высота (для 1..3 треков компактно, при большем количестве — скролл)
 * - У каждого трека кнопка трёх точек ⋮ (Фото 5): Сохранить в..., Переслать, Поделиться, Показать в чате
 * - Нижняя карточка управления: Seekbar, зацикливание Repeat, перемотка треков, Play/Pause
 */
@Composable
fun ChatMusicPlayerSheet(
    onDismiss: () -> Unit,
    controller: ChatAudioController? = null,
    onForwardTrack: (ChatTrack) -> Unit = {},
    onShareTrack: ((ChatTrack) -> Unit)? = null,
    onShowInChat: ((ChatTrack) -> Unit)? = null,
    onJumpToMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ctrl = controller ?: ChatAudioController.getInstance(context)
    val state by ctrl.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = LocalAnonGramScheme.current

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val sheetOffsetAnim = remember { Animatable(screenHeightPx) }
    var isClosing by remember { mutableStateOf(false) }

    fun dismissAnimated() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            sheetOffsetAnim.animateTo(screenHeightPx, tween(260, easing = FastOutLinearInEasing))
            ctrl.closeSheet()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        sheetOffsetAnim.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }

    BackHandler {
        dismissAnimated()
    }

    val scrimAlpha = ((1f - (sheetOffsetAnim.value / screenHeightPx)) * 0.65f).coerceIn(0f, 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissAnimated() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, sheetOffsetAnim.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(IslandBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                // Полоска-держатель (Drag Handle)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 12.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // 1. Список треков текущего чата (адаптивная высота)
                val trackCount = state.queue.size
                val maxListHeight = when {
                    trackCount <= 2 -> 140.dp
                    trackCount <= 4 -> 240.dp
                    else -> 320.dp
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .padding(horizontal = 14.dp)
                ) {
                    items(state.queue, key = { it.id }) { track ->
                        val isCurrent = track.id == state.currentTrack?.id
                        ChatQueueTrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && state.isPlaying,
                            accent = scheme.accent,
                            onPlay = { ctrl.play(state.queue, track) },
                            onSave = {
                                Toast.makeText(context, "Сохранено: ${track.title}", Toast.LENGTH_SHORT).show()
                            },
                            onForward = {
                                dismissAnimated()
                                onForwardTrack(track)
                            },
                            onShare = {
                                shareTrack(context, track)
                            },
                            onShowInChat = {
                                dismissAnimated()
                                if (onShowInChat != null) {
                                    onShowInChat(track)
                                } else {
                                    onJumpToMessage?.invoke(track.messageId)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Разделитель
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                // 2. Нижняя карточка плеера (Full Player)
                val currentTrack = state.currentTrack
                if (currentTrack != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Название и артист + обложка справа
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTrack.title,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTrack.artist.ifBlank { "AnonGram Audio" },
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Мини-обложка справа
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.accent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seekbar ползунок
                        var userSliderPosition by remember { mutableFloatStateOf(-1f) }
                        val activeSliderProgress = if (userSliderPosition >= 0f) userSliderPosition else state.progress

                        Slider(
                            value = activeSliderProgress,
                            onValueChange = { userSliderPosition = it },
                            onValueChangeFinished = {
                                if (userSliderPosition >= 0f && state.durationMs > 0) {
                                    ctrl.seek((userSliderPosition * state.durationMs).toLong())
                                }
                                userSliderPosition = -1f
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = scheme.accent,
                                activeTrackColor = scheme.accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Таймкоды (текущее время слева, длительность справа)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.positionLabel,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = currentTrack.durationLabel,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Кнопки управления (Repeat, Prev, Play/Pause, Next, More)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Кнопка зацикливания (Repeat)
                            IconButton(onClick = { ctrl.cycleRepeat() }) {
                                Icon(
                                    imageVector = if (state.repeatMode == ChatRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Зациклить",
                                    tint = if (state.repeatMode != ChatRepeatMode.OFF) scheme.accent else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Предыдущий трек
                            IconButton(onClick = { ctrl.prev() }) {
                                Icon(
                                    imageVector = Icons.Rounded.FastRewind,
                                    contentDescription = "Предыдущий",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Крупный Play / Pause
                            IconButton(
                                onClick = { ctrl.toggle() },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(scheme.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (state.isPlaying) "Пауза" else "Слушать",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Следующий трек
                            IconButton(onClick = { ctrl.next() }) {
                                Icon(
                                    imageVector = Icons.Rounded.FastForward,
                                    contentDescription = "Следующий",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Меню 3 точки
                            var isBottomMenuOpen by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { isBottomMenuOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Опции",
                                        tint = TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = isBottomMenuOpen,
                                    onDismissRequest = { isBottomMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Сохранить в музыку") },
                                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                                        onClick = {
                                            isBottomMenuOpen = false
                                            Toast.makeText(context, "Сохранено: ${currentTrack.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Переслать") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.Reply,
                                                null,
                                                modifier = Modifier.graphicsLayer { scaleX = -1f }
                                            )
                                        },
                                        onClick = {
                                            isBottomMenuOpen = false
                                            dismissAnimated()
                                            onForwardTrack(currentTrack)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Поделиться") },
                                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                        onClick = {
                                            isBottomMenuOpen = false
                                            if (onShareTrack != null) {
                                                onShareTrack(currentTrack)
                                            } else {
                                                shareTrack(context, currentTrack)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Показать в чате") },
                                        leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
                                        onClick = {
                                            isBottomMenuOpen = false
                                            dismissAnimated()
                                            if (onShowInChat != null) {
                                                onShowInChat(currentTrack)
                                            } else {
                                                onJumpToMessage?.invoke(currentTrack.messageId)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Нижняя фиолетовая кнопка «Добавить в профиль» (Фото 4)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(scheme.accent)
                                .clickable {
                                    Toast.makeText(context, "Добавлено в профиль", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Добавить в профиль",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatQueueTrackRow(
    track: ChatTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onForward: () -> Unit,
    onShare: () -> Unit,
    onShowInChat: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Круглая иконка play / pause
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isCurrent) accent else CardDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isCurrent) Color.White else accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) accent else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist.ifBlank { "Неизвестный исполнитель" },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Опции",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Выпадающее меню трёх точек (Фото 5)
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Сохранить в...") },
                    leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                    onClick = {
                        isMenuOpen = false
                        onSave()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Переслать") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Reply,
                            null,
                            modifier = Modifier.graphicsLayer { scaleX = -1f }
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        onForward()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick = {
                        isMenuOpen = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Показать в чате") },
                    leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
                    onClick = {
                        isMenuOpen = false
                        onShowInChat()
                    }
                )
            }
        }
    }
}

private fun shareTrack(context: Context, track: ChatTrack) {
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.uri)
            putExtra(Intent.EXTRA_TEXT, "${track.artist} - ${track.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Поделиться треком"))
    }.onFailure {
        Toast.makeText(context, "Не удалось поделиться треком", Toast.LENGTH_SHORT).show()
    }
}
