package com.anongram.app.features.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.player.api.RepeatMode
import com.anongram.app.features.player.api.Track
import com.anongram.app.features.player.logic.AudioPlayerController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PurpleAccent = Color(0xFF8B5CF6)
private val DarkBg = Color(0xFF0F1626)
private val CardBg = Color(0xFF182238)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AudioPlayerController.getInstance(context) }
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    // Смещение окна: 0f = открыто, screenHeightPx = скрыто внизу
    val sheetOffsetAnim = remember { Animatable(screenHeightPx) }
    var isClosing by remember { mutableStateOf(false) }

    fun dismissAnimated() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            sheetOffsetAnim.animateTo(screenHeightPx, tween(260, easing = FastOutLinearInEasing))
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
            .clickable { dismissAnimated() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, sheetOffsetAnim.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DarkBg)
                .clickable(enabled = false) {}
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta > 0f) {
                            scope.launch {
                                sheetOffsetAnim.snapTo((sheetOffsetAnim.value + delta).coerceAtLeast(0f))
                            }
                        }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (sheetOffsetAnim.value > screenHeightPx * 0.22f || velocity > 800f) {
                            dismissAnimated()
                        } else {
                            scope.launch {
                                sheetOffsetAnim.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ручка для перетягивания (Drag handle)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FFFFFF))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Список очереди воспроизведения сверху (без меню "⋮", ТЗ шаг 7)
            if (state.queue.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Очередь треков (${state.queue.size})",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(state.queue, key = { _, track -> track.id }) { index, track ->
                        val isCurrent = state.currentTrack?.id == track.id
                        QueueTrackItem(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && state.isPlaying,
                            onPlay = { controller.play(state.queue, index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.weight(0.1f))
            }

            // Большая карточка трека с обложкой и метаданными
            val current = state.currentTrack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PurpleAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current?.title ?: "AnonGram Audio",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = current?.artist ?: "Unknown Artist",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Ползунок перемотки с временем слева и справа
            val maxPos = if (state.durationMs > 0) state.durationMs.toFloat() else 1f
            val currentPos = state.positionMs.toFloat().coerceIn(0f, maxPos)
            var isUserSeeking by remember { mutableStateOf(false) }
            var seekSliderValue by remember { mutableFloatStateOf(0f) }

            Slider(
                value = if (isUserSeeking) seekSliderValue else currentPos,
                onValueChange = {
                    isUserSeeking = true
                    seekSliderValue = it
                },
                onValueChangeFinished = {
                    isUserSeeking = false
                    controller.seek(seekSliderValue.toLong())
                },
                valueRange = 0f..maxPos,
                colors = SliderDefaults.colors(
                    thumbColor = PurpleAccent,
                    activeTrackColor = PurpleAccent,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPos = if (isUserSeeking) seekSliderValue.toLong() else state.positionMs
                Text(
                    text = formatMs(displayPos),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = formatMs(state.durationMs),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ряд кнопок строго по ТЗ (шаг 7):
            // слева ПЕРЕМЕШАТЬ, затем назад, большая пауза/play, вперёд, справа ЗАЦИКЛИТЬ.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Слева: ПЕРЕМЕШАТЬ (Shuffle)
                IconButton(
                    onClick = { controller.toggleShuffle() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (state.isShuffle) PurpleAccent else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Назад: Предыдущий трек
                IconButton(
                    onClick = { controller.prev() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Предыдущий",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 3. По центру: Большая кнопка Play / Pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent)
                        .clickable { controller.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Пауза" else "Воспроизведение",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // 4. Вперёд: Следующий трек
                IconButton(
                    onClick = { controller.next() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Следующий",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 5. Справа: ЗАЦИКЛИТЬ (Repeat)
                IconButton(
                    onClick = {
                        val nextMode = when (state.repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        controller.setRepeat(nextMode)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    val icon = if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                    val tint = if (state.repeatMode == RepeatMode.OFF) Color(0xFF64748B) else PurpleAccent
                    Icon(
                        imageVector = icon,
                        contentDescription = "Зациклить",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTrackItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) PurpleAccent.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onPlay() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCurrent) PurpleAccent else Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) PurpleAccent else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${formatMs(track.durationMs)}",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}
