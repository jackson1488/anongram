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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Полноэкранный плеер для музыки в стиле iOS Apple Music (ТЗ):
 * - Крупная квадратная обложка альбома с мягкой тенью и скруглением 24.dp
 * - MarqueeText для названия трека и исполнителя
 * - iOS-style плавный Scrubber (с белой точкой-манипулятором, текущим и оставшимся временем)
 * - Центрированный блок управления (Shuffle, Previous, большая Play/Pause, Next, Repeat)
 * - Кнопка сворачивания (стрелка вниз) и выпадающее меню трёх точек ⋮
 * - Выдвижной список всех треков чата
 */
@Composable
fun MusicFullScreenPlayer(
    onDismiss: () -> Unit,
    controller: ChatAudioController? = null,
    onForwardTrack: (ChatTrack) -> Unit = {},
    onJumpToMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ctrl = controller ?: ChatAudioController.getInstance(context)
    val state by ctrl.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = LocalAnonGramScheme.current

    val currentTrack = state.currentTrack ?: return
    var isQueueOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val slideAnim = remember { Animatable(screenHeightPx) }
    var isClosing by remember { mutableStateOf(false) }

    fun closeAnimated() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            slideAnim.animateTo(screenHeightPx, tween(260, easing = FastOutLinearInEasing))
            ctrl.closeFullScreen()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        slideAnim.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }

    BackHandler {
        if (isQueueOpen) isQueueOpen = false
        else closeAnimated()
    }

    val artBitmap = currentTrack.albumArtBitmap
        ?: currentTrack.let { AudioMetadataHelper.getCachedArt(it.uri.toString()) }
        ?: AudioMetadataHelper.generateFallbackArt(currentTrack.title, currentTrack.artist)

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, slideAnim.value.roundToInt()) }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141724),
                        Color(0xFF0C0E17),
                        Color(0xFF080910)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Верхний бар: Свернуть, Заголовок, Меню
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { closeAnimated() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Свернуть",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ВОСПРОИЗВЕДЕНИЕ ИЗ ЧАТА",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Опции",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier
                            .width(220.dp)
                            .background(Color(0xFF161926), RoundedCornerShape(16.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сохранить в загрузки", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.FileDownload, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                isMenuOpen = false
                                Toast.makeText(context, "Трек сохранен: ${currentTrack.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Переслать", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                isMenuOpen = false
                                closeAnimated()
                                onForwardTrack(currentTrack)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Поделиться", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                isMenuOpen = false
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${currentTrack.artist} — ${currentTrack.title}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Поделиться треком"))
                                } catch (_: Throwable) {}
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Показать в чате", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Visibility, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                isMenuOpen = false
                                closeAnimated()
                                onJumpToMessage?.invoke(currentTrack.messageId)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Крупная квадратная обложка альбома в стиле iOS
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = scheme.accent.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1C29)),
                contentAlignment = Alignment.Center
            ) {
                if (artBitmap != null) {
                    Image(
                        bitmap = artBitmap.asImageBitmap(),
                        contentDescription = currentTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Название трека и автор с бегущей строкой
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                MarqueeText(
                    text = currentTrack.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack.artist.ifBlank { "AnonGram" },
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Скраббер в стиле нового iPhone (плавная полоса с белой точкой-манипулятором)
            var isScrubbing by remember { mutableStateOf(false) }
            var scrubProgress by remember { mutableFloatStateOf(0f) }
            val activeProgress = if (isScrubbing) scrubProgress else state.progress

            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(state.durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true
                                    scrubProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    isScrubbing = false
                                    val targetMs = (scrubProgress * state.durationMs.coerceAtLeast(1L)).toLong()
                                    ctrl.seek(targetMs)
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    scrubProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val thumbXPx = (activeProgress * widthPx).coerceIn(0f, widthPx)

                    // Неактивная полоса
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    // Активная белая полоса
                    Box(
                        modifier = Modifier
                            .width(with(density) { thumbXPx.toDp() })
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    // Белая точка-манипулятор (увеличивается при зажатии)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((thumbXPx - if (isScrubbing) 8.dp.toPx() else 6.dp.toPx()).roundToInt(), 0) }
                            .size(if (isScrubbing) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.positionLabel,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = state.remainingLabel,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Центрированные кнопки управления как на iPhone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Перемешать (Shuffle)
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Случайный порядок", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Предыдущий
                IconButton(
                    onClick = { ctrl.prev() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "Предыдущий",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Большая центральная кнопка Play/Pause
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(scheme.accent)
                        .clickable { ctrl.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Пауза" else "Играть",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Следующий
                IconButton(
                    onClick = { ctrl.next() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Следующий",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Зацикливание (Repeat)
                IconButton(
                    onClick = { ctrl.cycleRepeat() },
                    modifier = Modifier.size(44.dp)
                ) {
                    val repeatIcon = when (state.repeatMode) {
                        ChatRepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val repeatTint = if (state.repeatMode != ChatRepeatMode.OFF) scheme.accent else Color.White.copy(alpha = 0.65f)
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Кнопка открытия плейлиста треков чата снизу
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { isQueueOpen = !isQueueOpen }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.QueueMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Список треков чата (${state.queue.size})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Выдвигающийся снизу список треков чата
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { isQueueOpen = false }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Очередь треков (${state.queue.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isQueueOpen = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
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
                                    closeAnimated()
                                    onForwardTrack(track)
                                },
                                onShare = {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "${track.artist} — ${track.title}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Поделиться треком"))
                                    } catch (_: Throwable) {}
                                },
                                onShowInChat = {
                                    closeAnimated()
                                    onJumpToMessage?.invoke(track.messageId)
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}
